package com.nyaai.data.model

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

interface ModelClient {
    suspend fun generateContent(prompt: String): Result<String>
}

class GeminiModelClient(
    private val apiKeyProvider: () -> String?,
    private val models: List<String> = listOf("gemini-1.5-flash", "gemini-2.0-flash"),
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 3000L
) : ModelClient {

    companion object {
        private const val TAG = "GeminiModelClient"
    }

    override suspend fun generateContent(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()?.trim() ?: ""
        if (apiKey.isBlank() || apiKey == "YOUR_NEW_API_KEY_HERE" || apiKey == "YOUR_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("No valid Gemini API key configured"))
        }

        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
        }
        val payloadBytes = payload.toString().toByteArray(Charsets.UTF_8)

        for (model in models) {
            for (attempt in 1..maxRetries) {
                try {
                    val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.connectTimeout = 15000
                    conn.readTimeout = 60000
                    conn.doOutput = true

                    conn.outputStream.use { it.write(payloadBytes) }

                    val responseCode = conn.responseCode
                    Log.d(TAG, "[$model] Attempt $attempt/$maxRetries — response: $responseCode")

                    when (responseCode) {
                        200 -> {
                            val response = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                            val jsonResponse = JSONObject(response)
                            val candidates = jsonResponse.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val content = candidates.getJSONObject(0).optJSONObject("content")
                                if (content != null) {
                                    val parts = content.optJSONArray("parts")
                                    if (parts != null && parts.length() > 0) {
                                        val answer = parts.getJSONObject(0).optString("text", "")
                                        if (answer.isNotBlank()) {
                                            return@withContext Result.success(answer)
                                        }
                                    }
                                }
                            }
                            return@withContext Result.failure(IllegalStateException("Empty or malformed candidate response from $model"))
                        }
                        429 -> {
                            Log.w(TAG, "Rate limited on $model (attempt $attempt). Waiting...")
                            if (attempt < maxRetries) {
                                delay(retryDelayMs * attempt)
                                continue
                            }
                        }
                        401, 403 -> {
                            Log.e(TAG, "Auth error: $responseCode for model $model")
                            return@withContext Result.failure(IllegalStateException("Authentication failed (HTTP $responseCode)"))
                        }
                        404 -> {
                            Log.w(TAG, "Model $model not found, trying next model...")
                            break // Try next model in list
                        }
                        else -> {
                            val errorBody = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                            Log.w(TAG, "API error $responseCode on $model: $errorBody")
                            if (attempt < maxRetries) {
                                delay(1500L)
                                continue
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Connection error on $model (attempt $attempt): ${e.message}")
                    if (attempt < maxRetries) {
                        delay(1500L)
                    }
                }
            }
        }

        Result.failure(IllegalStateException("All models and retries exhausted"))
    }
}

class BackendModelClient(
    private val baseUrl: String = "http://10.0.2.2:8000",
    private val fallbackClient: ModelClient? = null,
    private val timeoutMs: Int = 30000
) : ModelClient {

    companion object {
        private const val TAG = "BackendModelClient"
    }

    override suspend fun generateContent(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        if (cleanBaseUrl.isNotBlank()) {
            try {
                val url = URL("$cleanBaseUrl/api/v1/generate")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 8000
                conn.readTimeout = timeoutMs
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("prompt", prompt)
                    put("system_prompt", "You are NYAAI, a trusted Indian legal navigation assistant.")
                    put("temperature", 0.3)
                    put("json_mode", false)
                }
                val payloadBytes = payload.toString().toByteArray(Charsets.UTF_8)
                conn.outputStream.use { it.write(payloadBytes) }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val json = JSONObject(responseText)
                    val text = json.optString("text", "")
                    if (text.isNotBlank()) {
                        Log.d(TAG, "Successfully generated response via backend server")
                        return@withContext Result.success(text)
                    }
                }
                Log.w(TAG, "Backend returned HTTP $responseCode, trying fallback...")
            } catch (e: Exception) {
                Log.w(TAG, "Backend call failed (${e.message}), trying fallback...")
            }
        }

        // Fallback to local client if backend is unreachable or disabled
        fallbackClient?.generateContent(prompt)
            ?: Result.failure(IllegalStateException("Backend unavailable and no fallback configured"))
    }
}

class FakeModelClient(
    var response: String = "",
    var shouldFail: Boolean = false
) : ModelClient {
    override suspend fun generateContent(prompt: String): Result<String> {
        return if (shouldFail) {
            Result.failure(RuntimeException("Fake model client simulated failure"))
        } else {
            Result.success(response)
        }
    }
}

