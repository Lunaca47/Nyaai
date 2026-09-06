package com.nyaai.data.local

import android.util.Log
import com.nyaai.ui.state.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AiService(private val ragDao: RagDao, private val apiKey: String) {

    companion object {
        private const val TAG = "AiService"
        private const val MODEL = "gemini-2.5-flash"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 5000L
    }

    suspend fun generateAnswer(userQuery: String, responseLanguage: AppLanguage): Pair<String, Double> = withContext(Dispatchers.IO) {
        val langName = responseLanguage.displayName
        val query = userQuery.lowercase().trim()

        // 1. RETRIEVAL (RAG)
        val ftsStopWords = setOf("and", "or", "not", "near", "match")
        val keywords = query.replace(Regex("[^a-z0-9 ]"), " ").split(" ")
            .map { it.trim() }
            .filter { it.length >= 3 && it !in ftsStopWords }
        val numbersInQuery = Regex("\\d+").findAll(query).map { it.value }.toList()

        val rawContexts = mutableListOf<DocumentEntity>()

        suspend fun safeSearch(ftsQuery: String): List<DocumentEntity> {
            return try {
                ragDao.search(ftsQuery)
            } catch (e: Exception) {
                Log.w(TAG, "FTS search failed for '$ftsQuery': ${e.message}")
                emptyList()
            }
        }

        if (keywords.isNotEmpty()) {
            val phraseQuery = "\"${keywords.joinToString(" ")}\""
            rawContexts.addAll(safeSearch(phraseQuery).take(3))
        }
        if (rawContexts.size < 2 && numbersInQuery.isNotEmpty()) {
            val numSearch = numbersInQuery.joinToString(" OR ") { "article $it" }
            rawContexts.addAll(safeSearch(numSearch).take(3))
        }
        if (rawContexts.isEmpty() && keywords.isNotEmpty()) {
            val andQuery = keywords.joinToString(" ") { "$it*" }
            rawContexts.addAll(safeSearch(andQuery).take(5))
        }
        if (rawContexts.isEmpty() && keywords.isNotEmpty()) {
            for (kw in keywords.take(3)) {
                rawContexts.addAll(safeSearch("$kw*").take(2))
                if (rawContexts.size >= 3) break
            }
        }

        // 2. CONFIDENCE
        var confidence = when {
            rawContexts.size >= 3 -> 0.92
            rawContexts.size == 2 -> 0.85
            rawContexts.size == 1 -> 0.70
            else -> 0.40
        }
        if (numbersInQuery.isNotEmpty() && rawContexts.any { ctx -> numbersInQuery.any { num -> ctx.content.contains(num) } }) {
            confidence += 0.05
        }
        if (confidence > 0.99) confidence = 0.99

        val uniqueContexts = rawContexts.distinctBy { it.rowid }.take(5)
        val contextData = uniqueContexts.joinToString("\n\n") { "[Source: ${it.sourcePath}]\n${it.content}" }

        // 3. GEMINI API CALL with retries
        if (apiKey.isNotBlank() && apiKey != "YOUR_NEW_API_KEY_HERE") {

            val promptText = """
You are "Nyaai", a friendly legal assistant that makes Indian law SIMPLE and EASY to understand for everyday people — farmers, students, workers, homemakers — anyone.

YOUR MISSION: Take complex legal jargon and explain it like you're talking to a friend. Make law accessible to ALL.

CONTEXT FROM LEGAL DOCUMENTS:
$contextData

USER'S QUESTION: $userQuery

HOW TO ANSWER:
1. Start with a clear, one-line simple answer that anyone can understand.
2. Then explain the key points using simple everyday language — NO legal jargon. If you must use a legal term, explain it in brackets.
3. Give a real-life example if possible to make it relatable.
4. Keep it under 150 words. Be warm and helpful.
5. End by mentioning which law/article this comes from.
6. LANGUAGE: Detect the language of the question and reply in the SAME language. If unsure, use $langName.
7. If the context doesn't have the answer, honestly say so and suggest what they could search for instead.

FORMAT: Use bullet points (•) for key points. Keep sentences short and simple.
""".trimIndent()

            val payload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", promptText) })
                        })
                    })
                })
            }
            val payloadBytes = payload.toString().toByteArray()

            for (attempt in 1..MAX_RETRIES) {
                try {
                    val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.connectTimeout = 15000
                    conn.readTimeout = 60000
                    conn.doOutput = true

                    conn.outputStream.use { it.write(payloadBytes) }

                    val responseCode = conn.responseCode
                    Log.d(TAG, "Attempt $attempt/$MAX_RETRIES — response: $responseCode")

                    when (responseCode) {
                        200 -> {
                            val response = conn.inputStream.bufferedReader().use { it.readText() }
                            val jsonResponse = JSONObject(response)
                            val candidates = jsonResponse.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val content = candidates.getJSONObject(0).optJSONObject("content")
                                if (content != null) {
                                    val parts = content.optJSONArray("parts")
                                    if (parts != null && parts.length() > 0) {
                                        val answer = parts.getJSONObject(0).optString("text", "")
                                        if (answer.isNotBlank()) {
                                            return@withContext answer to confidence
                                        }
                                    }
                                }
                            }
                        }
                        429 -> {
                            Log.w(TAG, "Rate limited (attempt $attempt). Waiting...")
                            if (attempt < MAX_RETRIES) {
                                delay(RETRY_DELAY_MS * attempt)
                                continue
                            }
                        }
                        401, 403 -> {
                            Log.e(TAG, "Auth error: $responseCode")
                            break
                        }
                        else -> {
                            val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                            Log.w(TAG, "API error $responseCode: $errorBody")
                            if (attempt < MAX_RETRIES) {
                                delay(2000L)
                                continue
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Connection error (attempt $attempt): ${e.message}")
                    if (attempt < MAX_RETRIES) {
                        delay(2000L)
                    }
                }
            }
        }

        // 4. OFFLINE FALLBACK — only if ALL API attempts failed
        return@withContext buildOfflineAnswer(userQuery, keywords, uniqueContexts, confidence)
    }

    private fun buildOfflineAnswer(
        userQuery: String,
        queryKeywords: List<String>,
        contexts: List<DocumentEntity>,
        baseConfidence: Double
    ): Pair<String, Double> {

        if (contexts.isEmpty()) {
            return "Sorry, I couldn't find information about \"$userQuery\" in the legal documents. Try asking about specific articles, rights, or sections." to 0.20
        }

        val offlineConfidence = (baseConfidence * 0.75).coerceIn(0.25, 0.80)
        val sourceNames = mapOf(
            "coi.pdf" to "Constitution of India",
            "bns.pdf" to "Bharatiya Nyaya Sanhita",
            "bnss.pdf" to "Bharatiya Nagarik Suraksha Sanhita",
            "bsa.pdf" to "Bharatiya Sakshya Adhiniyam"
        )
        val sources = mutableSetOf<String>()
        val points = mutableListOf<String>()

        for (doc in contexts) {
            sources.add(sourceNames[doc.sourcePath] ?: doc.sourcePath)

            val text = doc.content
                .replace(Regex("^Page \\d+:\\s*"), "")
                .replace(Regex("\\s+"), " ")
                .trim()

            val sentences = text.split(Regex("(?<=\\.)\\s+"))
                .map { it.trim() }
                .filter { it.length in 25..300 }

            val best = sentences
                .map { s -> s to queryKeywords.count { s.lowercase().contains(it) } }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
                .take(2)

            best.forEach { (s, _) ->
                if (points.none { it.take(40) == s.take(40) }) {
                    points.add(s.trim())
                }
            }
        }

        val sb = StringBuilder()
        sb.appendLine("Here's what I found about \"${userQuery}\":\n")

        if (points.isNotEmpty()) {
            points.take(3).forEach { sb.appendLine("• $it\n") }
        } else {
            contexts.take(2).forEach { doc ->
                val text = doc.content.replace(Regex("^Page \\d+:\\s*"), "").replace(Regex("\\s+"), " ").trim()
                if (text.length > 30) {
                    val snippet = if (text.length > 200) text.take(200).substringBeforeLast(".") + "." else text
                    sb.appendLine("• $snippet\n")
                }
            }
        }

        sb.appendLine("📖 Source: ${sources.joinToString(", ")}")
        sb.append("\n⚡ Note: AI is temporarily unavailable. Showing direct excerpts from legal documents.")

        return sb.toString().trim() to offlineConfidence
    }
}
