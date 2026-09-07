package com.nyaai.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * CloudDatasetSyncService
 * Provides silent background synchronization of Nyaai's legal dataset from the cloud.
 * Maintains local Room SQLite FTS4 performance (< 15ms query time) while pulling OTA updates.
 */
class CloudDatasetSyncService(
    private val context: Context,
    private val dao: RagDao
) {
    companion object {
        private const val TAG = "CloudDatasetSync"
        private const val PREFS_NAME = "nyaai_cloud_sync_prefs"
        private const val KEY_VERSION = "cloud_dataset_version"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val MANIFEST_URL = "https://lunaca47.github.io/Nyaai/data/manifest.json"
        private const val DATASET_URL = "https://lunaca47.github.io/Nyaai/data/legal_dataset_master.json"
    }

    suspend fun syncDatasetIfNeeded(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentVersion = prefs.getString(KEY_VERSION, "2.0.0") ?: "2.0.0"

        try {
            Log.d(TAG, "Checking cloud manifest for legal dataset updates. Current version: $currentVersion")

            // 1. Fetch remote manifest
            val manifestJson = fetchUrl(MANIFEST_URL, connectTimeout = 4000, readTimeout = 6000)
            if (manifestJson == null) {
                Log.d(TAG, "Remote manifest unreachable or device offline; continuing with local database.")
                return@withContext false
            }

            val manifestObj = JSONObject(manifestJson)
            val remoteVersion = manifestObj.optString("version", currentVersion)
            val downloadUrl = manifestObj.optString("download_url", DATASET_URL)

            if (!force && remoteVersion == currentVersion) {
                Log.d(TAG, "Local dataset is up to date (version: $currentVersion).")
                return@withContext true
            }

            Log.i(TAG, "New cloud dataset detected: $remoteVersion > $currentVersion. Downloading update...")

            // 2. Fetch full cloud dataset
            val datasetJson = fetchUrl(downloadUrl, connectTimeout = 8000, readTimeout = 20000)
            if (datasetJson == null) {
                Log.w(TAG, "Failed to download cloud dataset from $downloadUrl")
                return@withContext false
            }

            val datasetObj = JSONObject(datasetJson)
            val curatedQaArray = datasetObj.optJSONArray("curated_qa")
            if (curatedQaArray != null && curatedQaArray.length() > 0) {
                val newExamples = ArrayList<TrainingExampleEntity>(curatedQaArray.length())
                for (i in 0 until curatedQaArray.length()) {
                    val item = curatedQaArray.getJSONObject(i)
                    newExamples.add(
                        TrainingExampleEntity(
                            id = item.optLong("id", 0L),
                            question = item.optString("question", ""),
                            answer = item.optString("answer", ""),
                            sourcePath = item.optString("source", item.optString("sourcePath", "")),
                            legalDomain = item.optString("legalDomain", item.optString("act", "Indian Law")),
                            reasoningQuality = 1
                        )
                    )
                }

                if (newExamples.isNotEmpty()) {
                    dao.insertAllTrainingExamples(newExamples)
                    Log.i(TAG, "Successfully synced ${newExamples.size} legal Q&A pairs into Room DB.")
                }
            }

            // 3. Update SharedPreferences version
            prefs.edit()
                .putString(KEY_VERSION, remoteVersion)
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                .apply()

            Log.i(TAG, "Cloud dataset sync completed successfully to version $remoteVersion.")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Cloud dataset sync failed gracefully (device remains fully functional locally): ${e.message}")
            false
        }
    }

    private fun fetchUrl(urlString: String, connectTimeout: Int, readTimeout: Int): String? {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = connectTimeout
            conn.readTimeout = readTimeout
            conn.setRequestProperty("User-Agent", "Nyaai-Android-CloudSync/2.1")
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                Log.w(TAG, "HTTP ${conn.responseCode} while fetching $urlString")
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Network fetch skipped ($urlString): ${e.message}")
            null
        }
    }
}
