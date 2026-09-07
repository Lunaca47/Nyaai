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

class AiService(
    private val ragDao: RagDao, 
    private val apiKey: String,
    private val customApiKeyProvider: (() -> String?)? = null
) {

    companion object {
        private const val TAG = "AiService"
        private val MODELS = listOf("gemini-1.5-flash", "gemini-2.0-flash")
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 3000L
    }

    suspend fun generateAnswer(userQuery: String, responseLanguage: AppLanguage): Pair<String, Double> = withContext(Dispatchers.IO) {
        val langName = responseLanguage.displayName
        val query = userQuery.lowercase().trim()

        // 0. CONVERSATIONAL INTENT: Greetings, small talk, identity, and gratitude
        val conversationalGreeting = getConversationalResponse(userQuery, responseLanguage)
        if (conversationalGreeting != null) {
            return@withContext conversationalGreeting to 0.99
        }

        // 1. RETRIEVAL: Training Examples
        val ftsStopWords = setOf("and", "or", "not", "near", "match", "the", "for", "with", "about", "what", "how", "give", "tell", "explain", "overview")
        val keywords = query.replace(Regex("[^a-z0-9 ]"), " ").split(" ")
            .map { it.trim() }
            .filter { it.length >= 3 && it !in ftsStopWords }
        val numbersInQuery = Regex("\\d+").findAll(query).map { it.value }.toList()

        val trainingMatches = mutableListOf<TrainingExampleEntity>()
        try {
            // First search full query
            trainingMatches.addAll(ragDao.searchTrainingExamples(query))

            // Search by substantive keywords (excluding generic legal terms)
            val substantiveKeywords = keywords.filter { it !in setOf("rights", "right", "laws", "law", "case", "act", "india", "legal") }
            for (kw in substantiveKeywords) {
                if (trainingMatches.size >= 5) break
                val matches = ragDao.searchTrainingExamples(kw)
                trainingMatches.addAll(matches)
            }
            if (trainingMatches.isEmpty()) {
                for (kw in keywords) {
                    val matches = ragDao.searchTrainingExamples(kw)
                    trainingMatches.addAll(matches)
                    if (trainingMatches.size >= 3) break
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Training example search failed: ${e.message}")
        }
        val uniqueTrainingMatches = trainingMatches.distinctBy { it.id }.take(3)

        // 2. RETRIEVAL: Room Document FTS
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

        // 3. CONFIDENCE SCORING
        var confidence = when {
            uniqueTrainingMatches.isNotEmpty() -> 0.95
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
        val trainingData = uniqueTrainingMatches.joinToString("\n\n") { "[Verified Act: ${it.sourcePath} | Domain: ${it.legalDomain}]\nQ: ${it.question}\nA: ${it.answer}" }

        // 4. GEMINI API CALL with retries and model fallback
        val effectiveApiKey = customApiKeyProvider?.invoke()?.trim()?.takeIf { it.isNotBlank() } ?: apiKey.trim()
        if (effectiveApiKey.isNotBlank() && effectiveApiKey != "YOUR_NEW_API_KEY_HERE" && effectiveApiKey != "YOUR_GEMINI_API_KEY") {

            val promptText = """
You are "Nyaai", a friendly legal assistant that makes Indian law SIMPLE and EASY to understand for everyday people — farmers, students, workers, homemakers — anyone.

YOUR MISSION: Take complex legal jargon and explain it like you're talking to a friend. Make law accessible to ALL.

VERIFIED STATUTORY QA & REASONING EXAMPLES:
$trainingData

CONTEXT FROM CODIFIED LEGAL DOCUMENTS:
$contextData

USER'S QUESTION: $userQuery

HOW TO ANSWER:
1. If the user begins with a greeting (e.g. "hi", "hello", "namaste"), warmly acknowledge it before answering.
2. Start with a clear, one-line simple answer that anyone can understand.
3. Then explain the key points using simple everyday language — NO legal jargon. If you must use a legal term, explain it in brackets.
4. Give a real-life example if possible to make it relatable.
5. Keep it under 150 words. Be warm and helpful.
6. End by mentioning which law/article/act this comes from.
7. LANGUAGE: Detect the language of the question and reply in the SAME language. If unsure, use $langName.
8. If the context doesn't have the answer, honestly say so and suggest what they could search for instead.

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

            for (model in MODELS) {
                for (attempt in 1..MAX_RETRIES) {
                    try {
                        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$effectiveApiKey")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.connectTimeout = 15000
                        conn.readTimeout = 60000
                        conn.doOutput = true

                        conn.outputStream.use { it.write(payloadBytes) }

                        val responseCode = conn.responseCode
                        Log.d(TAG, "[$model] Attempt $attempt/$MAX_RETRIES — response: $responseCode")

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
                                                 // Autonomous Training Loop: Cache high-confidence answer for zero-latency future hits
                                                 if (confidence >= 0.85 && uniqueTrainingMatches.isEmpty() && userQuery.length in 10..200) {
                                                     try {
                                                         ragDao.insertTrainingExample(
                                                             TrainingExampleEntity(
                                                                 question = userQuery.trim(),
                                                                 answer = answer.trim(),
                                                                 sourcePath = uniqueContexts.firstOrNull()?.sourcePath ?: "Gemini 2.5 Grounded Knowledge",
                                                                 legalDomain = "Learned Legal Intelligence",
                                                                 reasoningQuality = 1
                                                             )
                                                         )
                                                     } catch (_: Exception) {}
                                                 }
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
                                Log.e(TAG, "Auth error: $responseCode for model $model")
                                break
                            }
                            404 -> {
                                Log.w(TAG, "Model $model not found, trying next model...")
                                break
                            }
                            else -> {
                                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                                Log.w(TAG, "API error $responseCode: $errorBody")
                                if (attempt < MAX_RETRIES) {
                                    delay(1500L)
                                    continue
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Connection error on $model (attempt $attempt): ${e.message}")
                        if (attempt < MAX_RETRIES) {
                            delay(1500L)
                        }
                    }
                }
            }
        }

        // 5. OFFLINE FALLBACK — Grounded in training examples or document search
        return@withContext buildOfflineAnswer(userQuery, keywords, uniqueContexts, uniqueTrainingMatches, confidence)
    }

    private fun buildOfflineAnswer(
        userQuery: String,
        queryKeywords: List<String>,
        contexts: List<DocumentEntity>,
        trainingMatches: List<TrainingExampleEntity>,
        baseConfidence: Double
    ): Pair<String, Double> {

        // If matching pre-trained training example exists, return it with high accuracy
        if (trainingMatches.isNotEmpty()) {
            val bestMatch = trainingMatches.maxByOrNull { item ->
                val target = "${item.question} ${item.legalDomain} ${item.sourcePath}".lowercase()
                queryKeywords.count { target.contains(it) }
            } ?: trainingMatches.first()

            val sb = StringBuilder()
            sb.appendLine("Here's what I found about \"${userQuery}\":\n")
            sb.appendLine(bestMatch.answer.trim())
            sb.appendLine()
            sb.appendLine("📖 Source: ${bestMatch.sourcePath}")
            sb.append("\n⚡ Note: Verified statutory knowledge from Nyaai legal directory.")
            return sb.toString().trim() to 0.95
        }

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

        val numbersInQuery = Regex("\\d+").findAll(userQuery).map { it.value }.toList()
        val allQueryTerms = (queryKeywords + numbersInQuery).distinct()
        val genericTerms = setOf("rights", "right", "law", "laws", "court", "act")
        val substantiveQueryTerms = (queryKeywords.filter { it !in genericTerms } + numbersInQuery).distinct()

        for (doc in contexts) {
            val text = doc.content
                .replace(Regex("^Page \\d+:\\s*"), "")
                .replace(Regex("\\s+"), " ")
                .trim()

            val sentences = if (text.length <= 300) {
                listOf(text)
            } else {
                text.split(Regex("(?<=[a-zA-Z]{3}\\.)\\s+"))
                    .map { it.trim() }
                    .filter { it.length in 15..350 }
            }

            val best = sentences
                .map { s ->
                    val sLower = s.lowercase()
                    val substantiveScore = substantiveQueryTerms.count { sLower.contains(it) }
                    val totalScore = allQueryTerms.count { sLower.contains(it) }
                    Triple(s, substantiveScore, totalScore)
                }
                .filter { (_, subScore, totalScore) ->
                    if (substantiveQueryTerms.isNotEmpty()) subScore > 0 else totalScore > 0
                }
                .sortedWith(compareByDescending<Triple<String, Int, Int>> { it.second }.thenByDescending { it.third })
                .take(2)

            if (best.isNotEmpty()) {
                sources.add(sourceNames[doc.sourcePath] ?: doc.sourcePath)
                best.forEach { (s, _, _) ->
                    if (points.none { it.take(40) == s.take(40) }) {
                        points.add(s.trim())
                    }
                }
            }
        }

        if (points.isEmpty()) {
            return "Sorry, I couldn't find information about \"$userQuery\" in the legal documents. Try asking about specific articles, rights, or sections." to 0.20
        }

        val sb = StringBuilder()
        sb.appendLine("Here's what I found about \"${userQuery}\":\n")
        points.take(3).forEach { sb.appendLine("• $it\n") }
        sb.appendLine("📖 Source: ${sources.joinToString(", ")}")
        sb.append("\n⚡ Note: AI is temporarily unavailable. Showing direct excerpts from legal documents.")

        return sb.toString().trim() to offlineConfidence
    }

    private fun getConversationalResponse(query: String, language: AppLanguage): String? {
        val clean = query.lowercase().replace(Regex("[^a-z0-9 ]"), " ").trim()
        val tokens = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        // Substantive legal terms: if query contains these, treat as legal question, not pure greeting
        val substantiveLegalTerms = setOf(
            "section", "article", "bail", "arrest", "warrant", "fir", "police", "court",
            "law", "crime", "theft", "murder", "rights", "refund", "cheque", "property",
            "divorce", "cyber", "penalty", "judge", "bns", "bnss", "bsa", "ipc", "crpc"
        )
        if (tokens.any { it in substantiveLegalTerms }) {
            return null
        }

        val singleGreetings = setOf(
            "hi", "hello", "hey", "namaste", "namaskar", "pranam", "vanakkam",
            "namaskaram", "adaab", "satsriakal", "hola", "sup", "yo"
        )
        val multiGreetings = listOf(
            "good morning", "good afternoon", "good evening", "good day",
            "how are you", "how are you doing", "hows it going", "how is it going", "whats up", "what s up",
            "who are you", "what is your name", "what can you do", "introduce yourself", "tell me about yourself",
            "what is nyaai", "thank you", "thanks", "thank u", "dhanyawad", "shukriya", "nandri", "dhanyavada"
        )

        val isGreeting = clean in singleGreetings ||
            (tokens.size <= 3 && tokens.any { it in singleGreetings }) ||
            multiGreetings.any { clean == it || (clean.startsWith(it) && tokens.size <= 5) }

        if (!isGreeting) return null

        val isIdentity = clean.contains("who are you") || clean.contains("what can you do") || clean.contains("introduce") || clean.contains("your name") || clean.contains("what is nyaai")
        val isWellBeing = clean.contains("how are you") || clean.contains("hows it going") || clean.contains("how is it going") || clean.contains("whats up")
        val isThanks = clean.contains("thank") || clean.contains("dhanyawad") || clean.contains("shukriya") || clean.contains("nandri")

        return when (language) {
            AppLanguage.HINDI -> when {
                isIdentity -> "मैं न्यायAI (Nyaai) हूँ, आपका दोस्ताना कानूनी सहायक! 😊 मैं भारतीय कानूनों—संविधान, BNS, BNSS और नागरिक अधिकारों को सरल हिंदी में समझाने के लिए यहाँ हूँ। आप मुझसे एफआईआर, ज़मानत, या कोई भी कानूनी सवाल पूछ सकते हैं।"
                isWellBeing -> "मैं बिल्कुल ठीक हूँ, पूछने के लिए धन्यवाद! 😊 आशा है आपका दिन अच्छा जा रहा होगा। आज मैं आपकी कानूनी समझ में क्या सहायता कर सकता हूँ?"
                isThanks -> "आपका बहुत-बहुत स्वागत है! 😊 यदि आपके पास कोई और कानूनी प्रश्न या अधिकार से संबंधित संदेह हो, तो बेझिझक पूछें। सुरक्षित और जागरूक रहें!"
                else -> "नमस्ते! 😊 मैं न्यायAI (Nyaai) हूँ, आपका कानूनी सहायक। आज मैं आपकी क्या मदद कर सकता हूँ? आप मुझसे पुलिस प्रक्रिया, ज़मानत, उपभोक्ता अधिकार या किसी भी कानूनी धारा के बारे में पूछ सकते हैं।"
            }
            AppLanguage.BENGALI -> when {
                isIdentity -> "আমি Nyaai, আপনার ভারতীয় আইনি সহায়ক! 😊 আমি ভারতীয় আইন ও সংবিধানকে সহজ ভাষায় বোঝাতে সাহায্য করি। আপনি আমাকে এফআইআর, জামিন বা যেকোনো আইনি প্রশ্ন জিজ্ঞাসা করতে পারেন।"
                isWellBeing -> "আমি খুব ভালো আছি, ধন্যবাদ! 😊 আশা করি আপনার দিনটি ভালো কাটছে। আজ আপনাকে আইনি বিষয়ে কীভাবে সাহায্য করতে পারি?"
                isThanks -> "আপনাকে অনেক ধন্যবাদ! 😊 আপনার যেকোনো আইনি প্রশ্ন থাকলে নির্দ্বিধায় আমাকে জিজ্ঞাসা করতে পারেন।"
                else -> "নমস্কার! 😊 আমি Nyaai, আপনার আইনি সহায়ক। আজ আপনাকে কীভাবে সাহায্য করতে পারি? আপনি আমাকে নাগরিক অধিকার, এফআইআর, জামিন বা যেকোনো আইন সম্পর্কে জিজ্ঞাসা করতে পারেন।"
            }
            AppLanguage.TELUGU -> when {
                isIdentity -> "నేను Nyaai, మీ భారతీయ న్యాయ సహాయకుడిని! 😊 భారతీయ చట్టాలను మరియు రాజ్యాంగాన్ని సామాన్యులకు సులభంగా వివరించడానికి నేను ఇక్కడ ఉన్నాను. మీరు ఏదైనా చట్టపరమైన ప్రశ్న అడగవచ్చు."
                isWellBeing -> "నేను చాలా బాగున్నాను, అడిగినందుకు ధన్యవాదాలు! 😊 ఈరోజు మీకు ఏ చట్టపరమైన విషయంలో సహాయం కావాలి?"
                isThanks -> "చాలా ధన్యవాదాలు! 😊 మీకు భవిష్యత్తులో ఏవైనా చట్టపరమైన సందేహాలు ఉంటే ఎప్పుడైనా అడగవచ్చు."
                else -> "నమస్కారం! 😊 నేను Nyaai, మీ న్యాయ సహాయకుడిని. ఈరోజు నేను మీకు ఎలా సహాయపడగలను? మీరు ఎఫ్ఐఆర్, బెయిల్ లేదా పౌర హక్కుల గురించి ఏదైనా అడగవచ్చు."
            }
            AppLanguage.TAMIL -> when {
                isIdentity -> "நான் Nyaai, உங்கள் இந்திய சட்ட உதவியாளர்! 😊 இந்திய சட்டங்கள் மற்றும் அரசியலமைப்பை எளிய மொழியில் விளக்க நான் உதவுகிறேன். நீங்கள் எந்தவொரு சட்டக் கேள்வியையும் என்னிடம் கேட்கலாம்."
                isWellBeing -> "நான் நலமாக இருக்கிறேன், கேட்டதற்கு நன்றி! 😊 இன்று உங்களுக்கு சட்ட ரீதியாக நான் எவ்வாறு உதவ முடியும்?"
                isThanks -> "மிக்க நன்றி! 😊 உங்களுக்கு மேலும் ஏதேனும் சட்ட சந்தேகங்கள் இருந்தால் தயங்காமல் கேளுங்கள்."
                else -> "வணக்கம்! 😊 நான் Nyaai, உங்கள் சட்ட உதவியாளர். இன்று நான் உங்களுக்கு எவ்வாறு உதவ முடியும்? எஃப்.ஐ.ஆர், ஜாமீன் அல்லது குடிமக்கள் உரிமைகள் பற்றி நீங்கள் கேட்கலாம்."
            }
            else -> when {
                isIdentity -> "Hello! 😊 I am **Nyaai (न्यायAI)**, your friendly Indian legal assistant. My mission is to make Indian laws—including the Constitution of India, Bharatiya Nyaya Sanhita (BNS), BNSS, and citizen rights—simple, clear, and easy to understand for everyone. How can I help you today?"
                isWellBeing -> "I'm doing great, thank you for asking! 😊 Ready to help make Indian law simple and accessible for you. What's on your mind today?"
                isThanks -> "You're very welcome! 😊 If you have any more legal questions or need clarity on your rights and legal procedures, feel free to ask anytime. Stay safe and informed!"
                else -> "Hello! 😊 I'm **Nyaai**, your Indian legal assistant. How can I help you today? You can ask me about citizen rights, police procedures (FIR/arrest), bail provisions, consumer rights, or any specific legal scenario you're dealing with."
            }
        }
    }
}
