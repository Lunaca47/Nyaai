package com.nyaai.data.matter

import android.util.Log
import com.nyaai.data.local.MatterEntity
import com.nyaai.data.local.RagDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class CaseStateManager(
    private val ragDao: RagDao
) {

    companion object {
        private const val TAG = "CaseStateManager"

        fun toBackendUuid(userId: String): String {
            return try {
                UUID.fromString(userId).toString()
            } catch (_: Exception) {
                UUID.nameUUIDFromBytes(userId.toByteArray(Charsets.UTF_8)).toString()
            }
        }
    }

    suspend fun createMatter(matter: Matter): String {
        val encryptedJson = MatterCryptoService.encrypt(matter.toJson())
        val entity = MatterEntity(
            matterId = matter.matterId,
            userId = matter.userId,
            title = matter.title,
            domain = matter.domain,
            status = matter.status,
            jurisdictionState = matter.jurisdiction.state,
            jurisdictionConfidence = matter.jurisdiction.confidence.name,
            proceduralStage = matter.proceduralStage,
            createdAt = matter.createdAt,
            updatedAt = matter.updatedAt,
            caseStateJson = encryptedJson
        )
        ragDao.insertMatter(entity)
        return matter.matterId
    }

    suspend fun getMatter(matterId: String): Matter? {
        val entity = ragDao.getMatter(matterId) ?: return null
        val decryptedJson = MatterCryptoService.decrypt(entity.caseStateJson)
        return Matter.fromJson(decryptedJson)
    }

    suspend fun updateMatter(matter: Matter) {
        val updatedMatter = matter.copy(updatedAt = System.currentTimeMillis())
        val encryptedJson = MatterCryptoService.encrypt(updatedMatter.toJson())
        val entity = MatterEntity(
            matterId = updatedMatter.matterId,
            userId = updatedMatter.userId,
            title = updatedMatter.title,
            domain = updatedMatter.domain,
            status = updatedMatter.status,
            jurisdictionState = updatedMatter.jurisdiction.state,
            jurisdictionConfidence = updatedMatter.jurisdiction.confidence.name,
            proceduralStage = updatedMatter.proceduralStage,
            createdAt = updatedMatter.createdAt,
            updatedAt = updatedMatter.updatedAt,
            caseStateJson = encryptedJson
        )
        ragDao.updateMatter(entity)
    }

    suspend fun getAllMatters(): List<Matter> {
        return ragDao.getAllMatters().map { entity ->
            try {
                val decryptedJson = MatterCryptoService.decrypt(entity.caseStateJson)
                Matter.fromJson(decryptedJson)
            } catch (_: Exception) {
                Matter(
                    matterId = entity.matterId,
                    userId = entity.userId,
                    title = entity.title,
                    domain = entity.domain,
                    status = entity.status,
                    proceduralStage = entity.proceduralStage,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
            }
        }
    }

    suspend fun deleteMatter(matterId: String) {
        ragDao.deleteMatter(matterId)
    }

    private var cachedGuestToken: String? = null

    suspend fun fetchGuestToken(backendUrl: String): String? = withContext(Dispatchers.IO) {
        cachedGuestToken?.let { return@withContext it }
        val cleanUrl = backendUrl.trimEnd('/')
        if (cleanUrl.isBlank()) return@withContext null
        try {
            val url = URL("$cleanUrl/api/v1/matters/auth/guest-token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 3000
            conn.readTimeout = 5000
            if (conn.responseCode in 200..299) {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val token = json.optString("guest_token")
                if (token.isNotBlank()) {
                    cachedGuestToken = token
                    return@withContext token
                }
            }
        } catch (_: Exception) {}
        null
    }

    suspend fun syncMatterWithBackend(matterId: String, backendUrl: String, authToken: String? = null): Result<Matter> = withContext(Dispatchers.IO) {
        val localMatter = getMatter(matterId)
            ?: return@withContext Result.failure(IllegalArgumentException("Matter $matterId not found locally"))
        val cleanUrl = backendUrl.trimEnd('/')
        if (cleanUrl.isBlank()) {
            return@withContext Result.success(localMatter)
        }

        try {
            val url = URL("$cleanUrl/api/v1/matters")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            val token = authToken ?: fetchGuestToken(cleanUrl)
            if (!token.isNullOrBlank()) {
                if (token.startsWith("guest_v1:")) {
                    conn.setRequestProperty("X-Guest-Token", token)
                } else {
                    conn.setRequestProperty("Authorization", "Bearer $token")
                }
            }
            conn.connectTimeout = 5000
            conn.readTimeout = 10000
            conn.doOutput = true

            val backendUserId = toBackendUuid(localMatter.userId)
            val payload = JSONObject().apply {
                put("user_id", backendUserId)
                put("title", localMatter.title)
                put("domain", localMatter.domain)
                put("jurisdiction_state", localMatter.jurisdiction.state ?: JSONObject.NULL)
                put("procedural_stage", localMatter.proceduralStage)
                put("status", localMatter.status)
                put("case_state_json", localMatter.toJson())
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                Log.d(TAG, "Successfully synced Matter $matterId to backend with case state")
                return@withContext Result.success(localMatter)
            } else {
                Log.w(TAG, "Backend returned HTTP $responseCode during sync")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend sync failed (${e.message}), keeping local state")
        }
        // Resilient fallback to local state
        Result.success(localMatter)
    }

    /**
     * Recovers all Matters for the authenticated user from the backend store
     * and persists them in the local encrypted Room database.
     * Simulates "new device" recovery flow.
     */
    suspend fun restoreMattersFromBackend(userId: String, backendUrl: String, authToken: String? = null): Result<List<Matter>> = withContext(Dispatchers.IO) {
        val cleanUrl = backendUrl.trimEnd('/')
        if (cleanUrl.isBlank()) {
            return@withContext Result.success(getAllMatters())
        }

        val backendUserId = toBackendUuid(userId)

        try {
            val url = URL("$cleanUrl/api/v1/matters?user_id=$backendUserId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            val token = authToken ?: fetchGuestToken(cleanUrl)
            if (!token.isNullOrBlank()) {
                if (token.startsWith("guest_v1:")) {
                    conn.setRequestProperty("X-Guest-Token", token)
                } else {
                    conn.setRequestProperty("Authorization", "Bearer $token")
                }
            }
            conn.connectTimeout = 5000
            conn.readTimeout = 10000

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                val restored = restoreFromBackendJson(responseBody, userId)
                Log.d(TAG, "Restored ${restored.size} matters from backend for user $userId")
                return@withContext Result.success(restored)
            } else {
                Log.w(TAG, "Backend restore failed with HTTP $responseCode")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend restore failed (${e.message}), returning local matters")
        }

        Result.success(getAllMatters())
    }

    suspend fun restoreFromBackendJson(responseBody: String, fallbackUserId: String = "guest_user"): List<Matter> {
        val restoredMatters = mutableListOf<Matter>()
        val jsonArray = JSONArray(responseBody)

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val caseStateJson = obj.optString("case_state_json", "")
            val matter = if (caseStateJson.isNotBlank() && caseStateJson != "null") {
                try {
                    Matter.fromJson(caseStateJson)
                } catch (_: Exception) {
                    null
                }
            } else null

            val effectiveMatter = matter ?: Matter(
                matterId = obj.optString("id", UUID.randomUUID().toString()),
                userId = fallbackUserId,
                title = obj.optString("title", "Restored Case"),
                domain = obj.optString("domain", "general"),
                status = obj.optString("status", "active"),
                proceduralStage = obj.optString("procedural_stage", "intake")
            )

            // Persist to local Room database (with encryption)
            createMatter(effectiveMatter)
            restoredMatters.add(effectiveMatter)
        }
        return restoredMatters
    }
}
