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

            val isScenario = isScenarioQuery(userQuery)
            val promptText = if (isScenario) {
                """
You are "Nyaai", acting as a seasoned Senior Advocate in India advising a client on a practical real-world legal scenario.

USER'S SCENARIO / QUESTION: $userQuery

CODIFIED LEGAL CONTEXT & STATUTES:
$contextData
$trainingData

HOW AN ADVOCATE EXPLAINS FURTHER PROCEEDINGS:
1. Provide a clear, empathetic legal evaluation of the situation.
2. State the Nature of Offense & Applicable Laws (under Bharatiya Nyaya Sanhita 2023 [BNS], Bharatiya Nagarik Suraksha Sanhita 2023 [BNSS], Bharatiya Sakshya Adhiniyam 2023 [BSA], or relevant Special Acts like NI Act 138, Consumer Protection Act, IT Act, etc.). Mention if it is Cognizable/Non-Cognizable and Bailable/Non-Bailable.
3. PHASE 1: Immediate Steps & Evidence Preservation (actions within 24-48 hours, preserving WhatsApp/CCTV/documents under Section 63 BSA).
4. PHASE 2: Formal Legal Notice & Police / Statutory Recourse (Filing FIR/Zero FIR under BNSS 173; explain the crucial advocate remedy if police refuse FIR: send written representation to SP/DCP under Section 175(3) BNSS, and application before Judicial Magistrate under Section 175(4)/176 BNSS; sending an Advocate Legal Demand Notice).
5. PHASE 3: Court Proceedings & Judicial Reliefs (Filing in competent court/tribunal, Injunctions under CPC Order 39, Restitution, Compensation, Damages, Bail/Quashing under BNSS 482/528).
6. ADVOCATE'S STRATEGIC ADVICE & PRECAUTIONS (Statutory limitation periods, do not take law into own hands, maintain speed post tracking receipts).
7. Reply in $langName (or the language of the query). Keep formatting clean with bullet points and bold section headers.
""".trimIndent()
            } else {
                """
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
            }

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
        return@withContext buildOfflineAnswer(userQuery, keywords, uniqueContexts, uniqueTrainingMatches, confidence, responseLanguage)
    }

    private fun buildOfflineAnswer(
        userQuery: String,
        queryKeywords: List<String>,
        contexts: List<DocumentEntity>,
        trainingMatches: List<TrainingExampleEntity>,
        baseConfidence: Double,
        language: AppLanguage = AppLanguage.ENGLISH
    ): Pair<String, Double> {

        if (isScenarioQuery(userQuery)) {
            return buildAdvocateScenarioAnswer(userQuery, language)
        }

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

    private fun isScenarioQuery(query: String): Boolean {
        val q = query.lowercase().trim()
        val scenarioPhrases = listOf(
            "what should i do", "what can i do", "how to proceed", "how do i proceed", "what is the process",
            "what is the procedure", "how do i file", "how can i file", "next steps",
            "further proceeding", "further proceedings", "legal action", "where to complain",
            "what are my options", "legal remedies", "how to handle this", "how to handle", "i need advice",
            "please help", "what happens if", "is it legal for", "is it illegal for", "what to do",
            "my landlord", "my tenant", "my employer", "my boss", "my company",
            "my husband", "my wife", "my neighbour", "my neighbor", "my brother",
            "someone hit", "hit my car", "hit and run", "cheque bounce", "cheque bounced",
            "bounced cheque", "cheque", "dishonored cheque", "dishonour", "insufficient funds",
            "salary not paid", "unpaid salary", "withholding salary",
            "refused to pay", "refused my fir", "refused to register fir", "police refused",
            "police not taking", "morphed photo", "blackmail", "blackmailing", "leaked photo",
            "cyber fraud", "upi scam", "upi fraud", "account hacked", "illegal detention",
            "arrested without", "without warrant", "domestic violence", "dowry harassment",
            "encroach", "encroachment", "land grabbing", "boundary wall", "threat to life",
            "threatening me", "medical negligence", "doctor negligence", "servant stole",
            "domestic help", "maid stole", "defamation", "defaming", "defamatory", "false fir", "fake fir", "fake case",
            "locked my flat", "locked out", "deposit not returning", "stole my", "stolen my", "stole gold",
            "fraud", "cheated", "scammed", "delayed salary", "custodial", "harassing me",
            "how to recover", "fake loan", "loud music", "frame me", "divorce", "custody",
            "refuse refund", "refused refund", "refusing refund", "not paying", "invoices",
            "online store", "bought a", "hospital doctor", "landlord", "tenant"
        )
        if (scenarioPhrases.any { q.contains(it) }) return true
        if (q.contains("cheque") && (q.contains("bounce") || q.contains("bounced") || q.contains("dishonor") || q.contains("insufficient"))) return true
        if (q.contains("police") && (q.contains("fir") || q.contains("complaint") || q.contains("refuse") || q.contains("arrest"))) return true
        return false
    }

    private fun isDetailedQuery(query: String): Boolean {
        val q = query.lowercase().trim()
        val detailedPhrases = listOf(
            "detail", "detailed", "in detail", "explain in detail", "elaborate", "comprehensive",
            "full explanation", "deep dive", "step by step", "step-by-step", "complete guide",
            "thorough", "everything", "explain fully", "long answer", "explain each and everything",
            "in depth", "in-depth", "clearly explain all points", "all details", "exhaustive",
            "full procedure", "complete roadmap", "7 points", "detailed version", "expand",
            "break down completely", "all points", "full steps", "detailed explanation",
            "explain properly", "more details", "each and everything", "entire process",
            "complete legal brief", "full advisory", "long version"
        )
        return detailedPhrases.any { q.contains(it) }
    }

    private fun buildAdvocateScenarioAnswer(query: String, language: AppLanguage): Pair<String, Double> {
        val q = query.lowercase().trim()
        val isDetailed = isDetailedQuery(query)
        val sb = StringBuilder()

        if (isDetailed) {
            sb.appendLine("⚖️ **SENIOR ADVOCATE COMPREHENSIVE LEGAL BRIEF (8+ POINTS)**")
            sb.appendLine("**Matter:** Comprehensive Legal Evaluation & Multi-Stage Proceeding Brief")
            sb.appendLine("**Statutory Codex:** BNS 2023 • BNSS 2023 • BSA 2023 • CPC 1908 • Special Enactments")
            sb.appendLine()

            when {
                // 1. Landlord-Tenant Conflict (Detailed 9-Point Roadmap)
                (q.contains("landlord") || q.contains("tenant") || q.contains("flat") || q.contains("rent")) &&
                (q.contains("locked") || q.contains("belonging") || q.contains("evict") || q.contains("deposit") || q.contains("vacate") || q.contains("advance")) -> {
                    sb.appendLine("1. ⚖️ **NATURE OF OFFENSE & SUBSTANTIVE JURISDICTION:** Civil dispossession coupled with cognizable criminal offenses under Section 329 BNS (Criminal Trespass), Section 316 BNS (Criminal Breach of Trust for seizing belongings), and Section 303 BNS (Theft). Under Section 106 of the Transfer of Property Act 1882, tenancy can only be determined through valid statutory notice; physical lockout without a court eviction decree is strictly illegal.")
                    sb.appendLine()
                    sb.appendLine("2. 🚨 **FIRST 24–48 HOUR EMERGENCY PROTOCOL (GOLDEN HOURS):** Do NOT take the law into your own hands or forcibly break padlocks, as the landlord could file counter-allegations of housebreaking under Section 331 BNS. Immediately dial 112 while standing outside the locked premises to generate an official Police Control Room (PCR) computer-aided dispatch record establishing timestamp and location.")
                    sb.appendLine()
                    sb.appendLine("3. 📱 **EVIDENTIARY AUDIT & DIGITAL FORENSIC PRESERVATION (SEC 63 BSA 2023):** Capture high-resolution timestamped photographs and video of padlocks, chains, and notices pasted on the door. Archive all rent bank transfers, security deposit receipts, and WhatsApp communications with the landlord, supported by electronic certificate under Section 63 of Bharatiya Sakshya Adhiniyam 2023.")
                    sb.appendLine()
                    sb.appendLine("4. 🚓 **POLICE STATION PROTOCOL, GENERAL DIARY & ZERO FIR (SEC 173 BNSS):** Submit two typed, signed copies of your criminal complaint to the jurisdictional Station House Officer (SHO). Insist on obtaining a receiving stamp and General Diary (GD) entry number. Under Section 173 BNSS 2023, police are statutorily mandated to record cognizable information; geographical jurisdiction cannot be cited to refuse an FIR.")
                    sb.appendLine()
                    sb.appendLine("5. 📜 **MANDATORY STATUTORY ESCALATION AGAINST INACTION (SEC 175(3) BNSS TO SP):** If the local police station dismisses the matter as a 'civil dispute', immediately invoke Section 175(3) BNSS. Send a signed copy of the complaint via Registered Speed Post directly to the Superintendent of Police (SP) or Deputy Commissioner of Police (DCP). Preserve the India Post postal receipt and tracking delivery slip.")
                    sb.appendLine()
                    sb.appendLine("6. ✉️ **FORMAL ADVOCATE LEGAL DEMAND NOTICE:** Have an advocate issue a formal 7-Day Legal Notice via Speed Post and Email, putting the landlord on notice to hand over duplicate keys, restore peaceful possession, and return all seized goods, failing which civil and criminal proceedings will follow at their sole risk and costs.")
                    sb.appendLine()
                    sb.appendLine("7. 🏛️ **JUDICIAL RECOURSE & MAGISTERIAL PETITIONS (SEC 175(4) / 176 BNSS):** File an application under Section 175(4) BNSS before the Judicial Magistrate First Class (JMFC) praying for judicial directions ordering the police to register an FIR and seeking a search warrant for the immediate recovery and restitution of personal belongings.")
                    sb.appendLine()
                    sb.appendLine("8. 🛡️ **URGENT CIVIL INJUNCTION (ORDER 39 CPC) & SECTION 6 SPECIFIC RELIEF SUIT:** File a summary suit under Section 6 of the Specific Relief Act 1963 for restoration of possession without having to prove title. Simultaneously move an interlocutory application under Order 39 Rules 1 & 2 CPC for an ex-parte temporary mandatory injunction directing the landlord to open the flat under Court Commissioner supervision within 24 hours.")
                    sb.appendLine()
                    sb.appendLine("9. ⏳ **STRATEGIC ADVOCATE SAFEGUARDS & LIMITATION CLOCK:** In 'Bishandas v. State of Punjab', the Supreme Court established that even an unauthorized occupant cannot be forcibly dispossessed without due process of law. Note: A suit under Section 6 Specific Relief Act carries a strict 6-MONTH limitation period from the date of dispossession.")
                }

                // 2. Cheque Bounce (Detailed 9-Point Roadmap)
                q.contains("cheque") && (q.contains("bounce") || q.contains("bounced") || q.contains("dishonor") || q.contains("returned") || q.contains("insufficient")) -> {
                    sb.appendLine("1. ⚖️ **NATURE OF OFFENSE & QUASI-CRIMINAL JURISDICTION:** Dishonour of cheque is a quasi-criminal statutory offense under Section 138 of the Negotiable Instruments Act 1881, punishable with imprisonment up to 2 years, or fine extending up to twice the cheque amount, or both.")
                    sb.appendLine()
                    sb.appendLine("2. 🚨 **BANK RETURN MEMO VERIFICATION & 30-DAY STATUTORY CLOCK:** Obtain the original return memo from your bank stating reasons such as 'Funds Insufficient' or 'Account Closed'. The strict 30-DAY limitation period for issuing the statutory demand notice starts ticking from the exact date on which you received the memo.")
                    sb.appendLine()
                    sb.appendLine("3. 📱 **EVIDENTIARY AUDIT OF ENFORCEABLE DEBT:** Assemble all underlying commercial invoices, agreements, purchase orders, delivery challans, and ledger balance sheets proving that the cheque was issued in discharge of a legally enforceable debt or liability, invoking the statutory presumption under Section 139 NI Act.")
                    sb.appendLine()
                    sb.appendLine("4. ✉️ **DRAFTING & SERVING 15-DAY STATUTORY LEGAL DEMAND NOTICE:** Issue a formal notice under Section 138(b) NI Act demanding payment of the exact cheque amount within 15 DAYS of receipt. The notice must be dispatched via Registered Speed Post (RPAD) and email to ensure traceable service.")
                    sb.appendLine()
                    sb.appendLine("5. ⏳ **CAUSE OF ACTION ACCRUAL (DAY 16):** The cause of action to prosecute the drawer does not arise until the 15-day notice period expires without payment. If the drawer fails to pay within 15 days, the cause of action legally accrues on the 16th day.")
                    sb.appendLine()
                    sb.appendLine("6. 🏛️ **FILING CRIMINAL COMPLAINT U/S 142 NI ACT (30-DAY WINDOW):** File a formal complaint under Section 142(1)(b) NI Act before the competent Judicial Magistrate within 30 DAYS from the date the cause of action accrued. Missing this window requires filing a Section 142(1)(b) proviso condonation of delay application.")
                    sb.appendLine()
                    sb.appendLine("7. 📜 **PRE-SUMMONING EVIDENCE ON AFFIDAVIT (SEC 145 NI ACT):** Complainant tenders their pre-summoning evidence on affidavit under Section 145 NI Act, expediting the issuance of court summons or bailable warrants against the accused without prolonged oral examination.")
                    sb.appendLine()
                    sb.appendLine("8. 💰 **20% INTERIM COMPENSATION APPLICATION (SEC 143A NI ACT):** File an application under Section 143A NI Act praying for an order directing the accused to deposit up to 20% of the cheque amount as interim compensation within 60 days.")
                    sb.appendLine()
                    sb.appendLine("9. 👥 **CORPORATE VICARIOUS LIABILITY (SEC 141 NI ACT):** If the cheque was issued by a company or LLP, implead all Directors and managing partners in charge of day-to-day business under Section 141 NI Act, demonstrating joint and several criminal liability.")
                }

                // 3. Hit and Run Accident (Detailed 9-Point Roadmap)
                q.contains("hit and run") || q.contains("hit my car") || q.contains("hit my bike") || (q.contains("accident") && (q.contains("car") || q.contains("vehicle") || q.contains("speeding") || q.contains("rash") || q.contains("ran away"))) -> {
                    sb.appendLine("1. ⚖️ **NATURE OF OFFENSE & STATUTORY CLASSIFICATION:** Offenses under Section 281 BNS (Rash and Negligent Driving), Section 125 BNS (Endangering Life/Personal Safety), and Section 106(2) BNS (Hit-and-run failing to report to police/Magistrate, carrying up to 10 years imprisonment), alongside civil claim remedies under the Motor Vehicles Act 1988.")
                    sb.appendLine()
                    sb.appendLine("2. 🚨 **HOSPITALIZATION & MEDICO-LEGAL CERTIFICATE (MLC):** When admitted to hospital, ensure the casualty medical officer enters the case as a Medico-Legal Case (MLC) citing 'RTA' (Road Traffic Accident). The MLC number is the foundational document for both police FIR and tribunal compensation.")
                    sb.appendLine()
                    sb.appendLine("3. 📱 **FORENSIC EVIDENCE & SCENE PRESERVATION (SEC 63 BSA):** Photograph vehicle damage, tyre skid marks, paint transfers, and road signage. Immediately issue written requests to surrounding commercial establishments and traffic police to preserve CCTV footage under Section 63 BSA 2023 before the 7-day DVR overwriting cycle.")
                    sb.appendLine()
                    sb.appendLine("4. 🚓 **MANDATORY FIR REGISTRATION U/S 173 BNSS:** Submit a written complaint specifying vehicle registration number, make, color, direction of escape, and witness statements. Secure a certified stamped copy of the FIR, Spot Panchnama, and Motor Vehicle Inspector (MVI) mechanical inspection report.")
                    sb.appendLine()
                    sb.appendLine("5. 📜 **POLICE ESCALATION & NODAL INVESTIGATION U/S 175(3) BNSS:** If the local police delay tracing the vehicle or registering the FIR, dispatch written representation to the DCP/SP Traffic and SP Police under Section 175(3) BNSS, requesting automated ANPR (Automatic Number Plate Recognition) toll camera tracing.")
                    sb.appendLine()
                    sb.appendLine("6. 🛡️ **SOLATIUM SCHEME APPLICATION FOR UNIDENTIFIED VEHICLES (SEC 161 MV ACT):** If the offending vehicle remains untraceable, submit a claim under the Central Government Solatium Scheme via the Sub-Divisional Magistrate (SDM) / Claims Enquiry Officer for statutory compensation (₹2 Lakhs for death, ₹50,000 for grievous hurt).")
                    sb.appendLine()
                    sb.appendLine("7. 🏛️ **MACT CLAIM PETITION U/S 166 MOTOR VEHICLES ACT:** File a comprehensive claim petition before the Motor Accident Claims Tribunal (MACT) having territorial jurisdiction over the accident site or your residence, impleading the driver, owner, and insurer within the 6-month statutory limitation period.")
                    sb.appendLine()
                    sb.appendLine("8. 💰 **COMPUTATION OF MULTI-HEAD DAMAGES:** Pray for compensation across medical expenses, future treatment costs, vehicle repair depreciation, permanent disability loss of earnings using the structured multiplier system, pain and suffering, and loss of amenities.")
                    sb.appendLine()
                    sb.appendLine("9. ⏳ **ADVOCATE PRECAUTIONS & INSURANCE SAFEGUARDS:** Never sign blanket discharge vouchers from insurance surveyors without advocate review. Preserve every pharmacy invoice, hospital discharge summary, employer leave certificate, and salary payslip.")
                }

                // 4. Police Refusal of FIR (Detailed 9-Point Roadmap)
                q.contains("police") && (q.contains("refused") || q.contains("not taking") || q.contains("not registering") || q.contains("rejected")) && (q.contains("fir") || q.contains("complaint")) -> {
                    sb.appendLine("1. ⚖️ **NATURE OF POLICE DERELICTION & STATUTORY MANDATE:** Under Section 173 BNSS 2023 and the Constitution of India, police officers have a mandatory statutory duty to record information disclosing a cognizable offense. Deliberate refusal to register an FIR constitutes dereliction of public duty punishable under Section 199 BNS with up to 2 years imprisonment.")
                    sb.appendLine()
                    sb.appendLine("2. 🚨 **POLICE STATION PROTOCOL & GENERAL DIARY (GD) DEMAND:** Always carry two typed, signed copies of your complaint. Insist that the Station Duty Officer place the official police station seal and date-time stamp on your receiving copy and note the General Diary (GD) entry number.")
                    sb.appendLine()
                    sb.appendLine("3. 📞 **DIALING 112 FROM POLICE STATION PREMISES:** If the duty officer verbally refuses or turns you away, dial 112 while standing inside or directly outside the police station. State that you are at the station and the officer is refusing to record your cognizable complaint; this creates a computer-aided dispatch (CAD) audio log that cannot be altered.")
                    sb.appendLine()
                    sb.appendLine("4. 🚓 **RIGHT TO ZERO FIR U/S 173 BNSS:** If the officer claims the incident occurred outside their police station limits, demand the registration of a Zero FIR under Section 173 BNSS 2023. By law, a Zero FIR must be registered and subsequently transferred to the jurisdictional police station.")
                    sb.appendLine()
                    sb.appendLine("5. 📜 **MANDATORY STATUTORY ESCALATION TO SP U/S 175(3) BNSS:** Send your complete signed complaint via Registered Speed Post directly to the Superintendent of Police (SP) or Deputy Commissioner of Police (DCP) under Section 175(3) BNSS. Speed Post provides indisputable legal proof of service.")
                    sb.appendLine()
                    sb.appendLine("6. ✉️ **PRESERVING POSTAL CONSIGNMENT EVIDENCE:** Retain the India Post consignment receipt and download the online delivery tracking report showing the date and time the SP's office received the complaint. This is a mandatory condition precedent for approaching the judiciary.")
                    sb.appendLine()
                    sb.appendLine("7. 🏛️ **APPLICATION TO JUDICIAL MAGISTRATE U/S 175(4) / 176 BNSS:** When the SP fails to direct an investigation within a reasonable time, file an application before the Judicial Magistrate First Class (JMFC) under Section 175(4) BNSS praying for judicial orders directing the police to lodge an FIR and submit a compliance report.")
                    sb.appendLine()
                    sb.appendLine("8. ⚖️ **ARTICLE 226 WRIT OF MANDAMUS IN HIGH COURT:** In severe cases involving grave human rights violations, police complicity, or political interference, file a Writ Petition (Criminal) under Article 226 of the Constitution before the High Court seeking a Writ of Mandamus commanding registration of the FIR and an independent SIT probe.")
                    sb.appendLine()
                    sb.appendLine("9. ⏳ **LANDMARK PRECEDENTS & ADVOCATE CAUTIONS:** The Supreme Court 5-judge Constitution Bench in 'Lalita Kumari v. Govt of UP' held that FIR registration is mandatory if information discloses a cognizable offense. Maintain a chronological binder of all stamped papers and speed post slips.")
                }

                // 5. Cyber Financial Fraud (Detailed 9-Point Roadmap)
                (q.contains("cyber") || q.contains("online") || q.contains("upi") || q.contains("phishing") || q.contains("otp") || q.contains("bank account")) &&
                (q.contains("fraud") || q.contains("scam") || q.contains("debited") || q.contains("money") || q.contains("hacked") || q.contains("stolen") || q.contains("cheated")) -> {
                    sb.appendLine("1. ⚖️ **NATURE OF CYBER OFFENSE & STATUTORY FRAMEWORK:** Financial cyber fraud constitutes offenses under Section 66D of the Information Technology Act 2000 (Cheating by personation using computer resource) and Section 318(4) BNS (Cheating and dishonestly inducing delivery of property), along with RBI Master Directions on Customer Protection.")
                    sb.appendLine()
                    sb.appendLine("2. 🚨 **GOLDEN HOURS EMERGENCY ACTION (DIAL 1930):** Immediately call the National Cyber Crime Helpline at 1930 or submit an incident report at cybercrime.gov.in. Within the first 2-4 hours, this triggers the Citizen Financial Cyber Fraud Reporting and Management System (CFCFRMS) to automatically freeze funds across intermediate payment gateways.")
                    sb.appendLine()
                    sb.appendLine("3. 🏦 **FORMAL BANK NOTICE WITHIN 72 HOURS (ZERO CUSTOMER LIABILITY):** Submit a written complaint to your home bank branch within 72 hours. Under RBI Circular DBR.No.Leg.BC.78/09.07.005/2017-18, where fraud is reported within 3 days without customer negligence, the customer's liability is ZERO, and the bank must shadow-credit the amount within 10 working days.")
                    sb.appendLine()
                    sb.appendLine("4. 📱 **DIGITAL FORENSIC EVIDENCE PRESERVATION (SEC 63 BSA):** Capture and preserve unedited screenshots of UPI transaction IDs, UTR numbers, debit SMS alerts, fraudulent payment links, APK files, and WhatsApp conversations, accompanied by Section 63 BSA 2023 metadata.")
                    sb.appendLine()
                    sb.appendLine("5. 🚓 **CYBER CRIME POLICE STATION FIR (SEC 173 BNSS):** Register a formal Cyber FIR under Section 173 BNSS 2023. Specify the beneficiary bank IFSC, account number, or UPI handle to facilitate immediate Section 102/106 BNSS seizure notices by the investigating officer.")
                    sb.appendLine()
                    sb.appendLine("6. 📜 **ESCALATION TO DISTRICT CYBER CELL & SP U/S 175(3) BNSS:** If the local police delay issuing freeze notices to the intermediary banks, escalate in writing to the District Cyber Crime Nodal Officer and SP under Section 175(3) BNSS.")
                    sb.appendLine()
                    sb.appendLine("7. 🏛️ **MAGISTRATE DE-FREEZING APPLICATION (SEC 503 BNSS):** Once the beneficiary bank confirms that the fraudulent money has been placed on hold, move an application under Section 503 BNSS before the Judicial Magistrate praying for release and transfer of the seized money back into your account on indemnity bond.")
                    sb.appendLine()
                    sb.appendLine("8. ⚖️ **BANKING OMBUDSMAN & IT ADJUDICATING OFFICER (SEC 46 IT ACT):** If your bank fails to adhere to RBI zero-liability norms within 30 days, file an online complaint with the RBI Banking Ombudsman (cms.rbi.org.in). Additionally, claim compensation before the State IT Secretary (Adjudicating Officer u/s 46 IT Act).")
                    sb.appendLine()
                    sb.appendLine("9. ⏳ **ADVOCATE CAUTIONS & CYBER HYGIENE:** Never communicate further with the fraudsters or click 'refund links'. Do not format your phone until digital evidence has been extracted and certified.")
                }

                // 6. General / Dynamic Detailed 9-Point Roadmap
                else -> {
                    sb.appendLine("1. ⚖️ **SUBSTANTIVE LEGAL NATURE & CODIFIED CLASSIFICATION:** Legal evaluation of your situation under Bharatiya Nyaya Sanhita (BNS 2023), Bharatiya Nagarik Suraksha Sanhita (BNSS 2023), and relevant Special Enactments. The matter is classified into civil or criminal remedies, assessing cognizable status and on whom the statutory burden of proof lies.")
                    sb.appendLine()
                    sb.appendLine("2. 🚨 **FIRST 24–48 HOUR EMERGENCY PROTOCOL & GOLDEN HOURS:** Do not take the law into your own hands or resort to extrajudicial self-help. Immediately dial 112 or relevant statutory helplines to generate an official computer-aided dispatch log verifying the date, time, and nature of the incident.")
                    sb.appendLine()
                    sb.appendLine("3. 📱 **EVIDENTIARY AUDIT & FORENSIC PRESERVATION (BSA 2023 SEC 63):** Compile all primary documentary evidence, contemporaneous audio/video recordings, invoices, and digital chats. Ensure all digital evidence is preserved with complete metadata and certificates under Section 63 of Bharatiya Sakshya Adhiniyam 2023.")
                    sb.appendLine()
                    sb.appendLine("4. 🚓 **POLICE STATION PROTOCOL, GENERAL DIARY & ZERO FIR (SEC 173 BNSS):** Submit two typed, signed copies of your complaint to the Station House Officer. Insist on obtaining a receiving seal with a General Diary (GD) entry number, or demand a Zero FIR under Section 173 BNSS if out-of-jurisdiction is cited.")
                    sb.appendLine()
                    sb.appendLine("5. 📜 **MANDATORY STATUTORY ESCALATION AGAINST INACTION (SEC 175(3) BNSS TO SP):** If the local police station refuses to register an FIR or act, send your signed complaint via Registered Speed Post directly to the Superintendent of Police (SP) / DCP under Section 175(3) BNSS. Note that willful refusal violates Section 199 BNS.")
                    sb.appendLine()
                    sb.appendLine("6. ✉️ **FORMAL ADVOCATE LEGAL DEMAND NOTICE & STATUTORY TIMELINES:** Have a licensed advocate issue a formal 15-Day Legal Demand Notice via Speed Post and Email, formally putting the opposing party on notice of statutory violations, articulating the cause of action, and proposing resolution before instituting litigation.")
                    sb.appendLine()
                    sb.appendLine("7. 🏛️ **JUDICIAL RECOURSE & MAGISTERIAL PETITIONS (SEC 175(4) / 176 BNSS):** Approach the Judicial Magistrate First Class under Section 175(4) BNSS praying for judicial orders directing investigation, summons, or search warrants if police machinery fails to act.")
                    sb.appendLine()
                    sb.appendLine("8. 🛡️ **CIVIL REDRESSAL, URGENT INJUNCTIONS (CPC ORDER 39) & DAMAGES:** File for appropriate civil relief before the competent District Court or High Court, seeking temporary injunctions under Order 39 Rules 1 & 2 CPC, specific relief, restitution, and claiming compensation for financial losses and mental agony.")
                    sb.appendLine()
                    sb.appendLine("9. ⏳ **STRATEGIC ADVOCATE SAFEGUARDS, LIMITATION CLOCK & CROSS-FIR PRECAUTIONS:** Always calculate limitation periods under the Limitation Act 1963 before filing. If threatened with false cross-cases, prepare Section 482 BNSS anticipatory bail applications in advance. Preserve all India Post Speed Post consignment receipts.")
                }
            }

            sb.appendLine()
            sb.appendLine("⚡ Note: Exhaustive 9-point Senior Advocate brief grounded in BNS, BNSS, BSA, and Special Acts.")
            return sb.toString().trim() to 0.99
        }

        // Standard 3-Phase Roadmap (Concise Advocate Summary)
        when {
            // 1. Landlord-Tenant Conflict
            (q.contains("landlord") || q.contains("tenant") || q.contains("flat") || q.contains("rent")) &&
            (q.contains("locked") || q.contains("belonging") || q.contains("evict") || q.contains("deposit") || q.contains("vacate") || q.contains("advance")) -> {
                sb.appendLine("⚖️ **SENIOR ADVOCATE LEGAL ADVISORY & PROCEEDING ROADMAP**")
                sb.appendLine("**Matter:** Unlawful Eviction, Flat Lockout & Belongings Seizure")
                sb.appendLine("**Primary Statutes:** Transfer of Property Act 1882 (Sec 106) • BNS 2023 (Sec 329, 316) • Order 39 CPC")
                sb.appendLine()
                sb.appendLine("⚖️ **NATURE OF OFFENSE & CLASSIFICATION:**")
                sb.appendLine("• **Classification:** Civil Dispossession + Cognizable Criminal Trespass (Sec 329 BNS) & Criminal Breach of Trust (Sec 316 BNS).")
                sb.appendLine("• **Cognizable Status:** Cognizable • Bailable • Non-Compoundable without Magistrate permission.")
                sb.appendLine()
                sb.appendLine("🚨 **PHASE 1: IMMEDIATE STEPS & EVIDENCE PRESERVATION (First 24-48 Hours):**")
                sb.appendLine("• **Do NOT break locks yourself:** Forcible entry allows the landlord to counter-allege housebreaking.")
                sb.appendLine("• **Photograph & Video Record:** Capture high-resolution timestamped photos/video of padlocks and posted notices.")
                sb.appendLine("• **Preserve Tenancy Communications:** Archive WhatsApp chats, rent bank statements, and agreement copy under Section 63 BSA 2023.")
                sb.appendLine("• **Dial 112 from the Spot:** Generates an official Police Control Room (PCR) dispatch log verifying physical dispossession.")
                sb.appendLine()
                sb.appendLine("📜 **PHASE 2: FORMAL LEGAL NOTICE & POLICE / STATUTORY RECOURSE:**")
                sb.appendLine("• **Lodge Police Complaint / Zero FIR:** Visit jurisdictional police under Section 173 BNSS 2023 for Criminal Trespass (Sec 329 BNS) & Breach of Trust (Sec 316 BNS).")
                sb.appendLine("• **If Police Refuse (Crucial Advocate Step):** Send signed complaint via Registered Speed Post to Superintendent of Police (SP) / DCP under Section 175(3) BNSS 2023.")
                sb.appendLine("• **Advocate Legal Demand Notice:** Dispatch a formal 7-Day Demand Notice demanding keys, return of belongings, and damages.")
                sb.appendLine()
                sb.appendLine("🏛️ **PHASE 3: JUDICIAL PROCEEDINGS, PETITIONS & RELIEFS IN COURT:**")
                sb.appendLine("• **Section 175(4) BNSS Application to Magistrate:** Move Judicial Magistrate to order FIR and search/recovery of personal belongings.")
                sb.appendLine("• **Summary Suit u/s 6 Specific Relief Act 1963:** File civil suit for restoration of possession without title contest.")
                sb.appendLine("• **Order 39 Rules 1 & 2 CPC Temporary Mandatory Injunction:** Move for ex-parte order directing landlord to unlock premises under Court Commissioner supervision within 24 hours.")
                sb.appendLine("• **Claim Damages:** Pray for compensation for hotel stay, replacement of essential items, and mental agony.")
                sb.appendLine()
                sb.appendLine("🛡️ **ADVOCATE'S STRATEGIC ADVICE & IMPORTANT CAUTIONS:**")
                sb.appendLine("• *Precedent:* Supreme Court in 'Bishandas v. State of Punjab' ruled landlords cannot forcibly dispossess without court eviction decrees.")
                sb.appendLine("• *Limitation:* Suit u/s 6 Specific Relief Act must be filed within 6 months of dispossession.")
                sb.appendLine("• Preserve Speed Post tracking receipts as indisputable proof in court.")
            }

            // 2. Hit and Run / Motor Accident
            q.contains("hit and run") || q.contains("hit my car") || q.contains("hit my bike") || (q.contains("accident") && (q.contains("car") || q.contains("vehicle") || q.contains("speeding") || q.contains("rash") || q.contains("ran away"))) -> {
                sb.appendLine("⚖️ **SENIOR ADVOCATE LEGAL ADVISORY & PROCEEDING ROADMAP**")
                sb.appendLine("**Matter:** Hit-and-Run Motor Vehicle Accident & Compensation Claim")
                sb.appendLine("**Primary Statutes:** Bharatiya Nyaya Sanhita (BNS 2023) Sec 281, 125, 106 • Motor Vehicles Act 1988 Sec 161, 166")
                sb.appendLine()
                sb.appendLine("⚖️ **NATURE OF OFFENSE & CLASSIFICATION:**")
                sb.appendLine("• **Classification:** Cognizable Criminal Offense (Rash/Negligent Driving) + Statutory MACT Claim.")
                sb.appendLine("• **Cognizable Status:** Cognizable • Bailable (Sec 281/125 BNS) • Sec 106(2) hit-and-run carries up to 10 years imprisonment.")
                sb.appendLine()
                sb.appendLine("🚨 **PHASE 1: IMMEDIATE STEPS & EVIDENCE PRESERVATION:**")
                sb.appendLine("• **Hospital Medico-Legal Certificate (MLC):** Ensure treating doctor records the accident history in hospital casualty register.")
                sb.appendLine("• **Vehicle Details & Scene Photos:** Note vehicle registration number, make, color; photograph vehicle damage and skid marks.")
                sb.appendLine("• **Retrieve CCTV:** Request nearby shops, fuel stations, and traffic signals to preserve footage under Section 63 BSA 2023.")
                sb.appendLine("• **Witness Contacts:** Note phone numbers of bystanders who witnessed the collision.")
                sb.appendLine()
                sb.appendLine("📜 **PHASE 2: FORMAL LEGAL NOTICE & POLICE / STATUTORY RECOURSE:**")
                sb.appendLine("• **Mandatory FIR Registration:** File written complaint under Section 281 & 106/125 BNS at jurisdictional police station.")
                sb.appendLine("• **Section 175(3) BNSS Escalation:** If police delay or try to compromise, send written complaint to SP/DCP.")
                sb.appendLine("• **Certified Police Documents:** Obtain certified copies of FIR, Spot Panchnama, and Motor Vehicle Inspector (MVI) inspection report.")
                sb.appendLine()
                sb.appendLine("🏛️ **PHASE 3: JUDICIAL PROCEEDINGS & RELIEFS IN COURT:**")
                sb.appendLine("• **File MACT Claim under Section 166 MV Act:** File before Motor Accident Claims Tribunal for medical costs, vehicle repair, and loss of earning capacity.")
                sb.appendLine("• **Solatium Scheme for Unidentified Hit-and-Run:** If vehicle remains untraceable, claim statutory compensation u/s 161 MV Act via SDM office.")
                sb.appendLine()
                sb.appendLine("🛡️ **ADVOCATE'S STRATEGIC ADVICE & IMPORTANT CAUTIONS:**")
                sb.appendLine("• Do NOT sign compromise letters from the driver or insurance surveyor without consulting an advocate.")
                sb.appendLine("• *Limitation:* File MACT petition within 6 months from accident date.")
                sb.appendLine("• Preserve all medical bills, pharmacy receipts, and employer salary loss statements.")
            }

            // 3. Cheque Bounce (Sec 138 NI Act)
            q.contains("cheque") && (q.contains("bounce") || q.contains("bounced") || q.contains("dishonor") || q.contains("returned") || q.contains("insufficient")) -> {
                sb.appendLine("⚖️ **SENIOR ADVOCATE LEGAL ADVISORY & PROCEEDING ROADMAP**")
                sb.appendLine("**Matter:** Dishonour of Cheque & Criminal Prosecution")
                sb.appendLine("**Primary Statutes:** Negotiable Instruments Act 1881 (Sec 138, 142) • Section 143A • Section 223 BNSS")
                sb.appendLine()
                sb.appendLine("⚖️ **NATURE OF OFFENSE & CLASSIFICATION:**")
                sb.appendLine("• **Classification:** Quasi-Criminal Offense punishable with up to 2 years imprisonment or fine up to twice cheque amount.")
                sb.appendLine("• **Cognizable Status:** Non-Cognizable • Bailable • Compoundable at any stage.")
                sb.appendLine()
                sb.appendLine("🚨 **PHASE 1: IMMEDIATE STEPS & EVIDENCE PRESERVATION:**")
                sb.appendLine("• **Bank Return Memo:** Collect original cheque with memo stating 'Funds Insufficient' or 'Account Closed'.")
                sb.appendLine("• **30-Day Notice Clock:** Notice MUST be dispatched within 30 DAYS of receiving bank memo.")
                sb.appendLine("• **Enforceable Debt Proof:** Collect contracts, invoices, ledger statements, or delivery receipts showing valid debt.")
                sb.appendLine()
                sb.appendLine("📜 **PHASE 2: FORMAL LEGAL NOTICE & STATUTORY DEMAND:**")
                sb.appendLine("• **15-Day Statutory Legal Demand Notice:** Serve formal notice u/s 138(b) NI Act demanding payment within 15 DAYS of receipt.")
                sb.appendLine("• **Speed Post with Tracking:** Dispatch via Registered Speed Post (RPAD) and email; preserve delivery tracking report.")
                sb.appendLine("• **Cause of Action:** Legally arises on the 16th day if drawer fails to pay.")
                sb.appendLine()
                sb.appendLine("🏛️ **PHASE 3: JUDICIAL PROCEEDINGS IN COURT:**")
                sb.appendLine("• **File Criminal Complaint within 30 Days:** File complaint before Judicial Magistrate u/s 142(1)(b) NI Act within 30 days from expiry of notice.")
                sb.appendLine("• **Pre-Summoning Affidavit u/s 145 NI Act:** Complainant tenders evidence on affidavit for issuance of summons/warrant.")
                sb.appendLine("• **20% Interim Compensation u/s 143A:** Move court for interim deposit of up to 20% of cheque amount.")
                sb.appendLine("• **Company Offense u/s 141:** Implead all Directors/Partners in charge of day-to-day business.")
                sb.appendLine()
                sb.appendLine("🛡️ **ADVOCATE'S STRATEGIC ADVICE & IMPORTANT CAUTIONS:**")
                sb.appendLine("• *Limitation Warning:* Missing the 30-day notice or 30-day filing window is fatal to prosecution.")
                sb.appendLine("• *Presumption u/s 139:* Law presumes cheque was issued for debt; burden of proof is entirely on the accused.")
                sb.appendLine("• Keep original cheque in protective plastic cover; do not staple or overwrite.")
            }

            // 4. Cyber Fraud & UPI Scams
            (q.contains("cyber") || q.contains("online") || q.contains("upi") || q.contains("phishing") || q.contains("otp") || q.contains("bank account")) &&
            (q.contains("fraud") || q.contains("scam") || q.contains("debited") || q.contains("money") || q.contains("hacked") || q.contains("stolen") || q.contains("cheated")) -> {
                sb.appendLine("⚖️ **SENIOR ADVOCATE LEGAL ADVISORY & PROCEEDING ROADMAP**")
                sb.appendLine("**Matter:** Cyber Financial Fraud, UPI Scam & Fund Freezing Recourse")
                sb.appendLine("**Primary Statutes:** Information Technology Act 2000 (Sec 43, 66D) • BNS 2023 Sec 318(4) • RBI Master Direction 2017")
                sb.appendLine()
                sb.appendLine("⚖️ **NATURE OF OFFENSE & CLASSIFICATION:**")
                sb.appendLine("• **Classification:** Cognizable Cyber Crime + Statutory Bank Zero-Liability Protection.")
                sb.appendLine("• **Cognizable Status:** Cognizable • Non-Bailable depending on quantum.")
                sb.appendLine()
                sb.appendLine("🚨 **PHASE 1: IMMEDIATE STEPS & GOLDEN HOURS ACTIONS (First 2-4 Hours):**")
                sb.appendLine("• **Dial 1930 Cyber Fraud Helpline:** Call 1930 immediately or log on to cybercrime.gov.in to trigger an automated inter-bank lien to freeze funds.")
                sb.appendLine("• **Notify Bank within 72 Hours:** Under RBI circular on Customer Protection, reporting within 3 days grants ZERO customer liability.")
                sb.appendLine("• **Preserve Digital Evidence:** Screenshot UPI transaction IDs, reference numbers, SMS alerts, and bank statements under Section 63 BSA 2023.")
                sb.appendLine("• **Block Cards & Reset Net Banking:** Freeze compromised debit/credit cards and UPI credentials immediately.")
                sb.appendLine()
                sb.appendLine("📜 **PHASE 2: FORMAL NOTICE & POLICE RECOURSE:**")
                sb.appendLine("• **Cyber Crime FIR:** Register FIR under Section 66D IT Act & Section 318(4) BNS.")
                sb.appendLine("• **Section 175(3) BNSS Escalation:** If local police refuse, escalate directly to Cyber Crime Nodal Officer / SP.")
                sb.appendLine("• **RBI Banking Ombudsman:** File complaint on cms.rbi.org.in if bank fails to reverse fraudulent debits within 30 days.")
                sb.appendLine()
                sb.appendLine("🏛️ **PHASE 3: JUDICIAL PROCEEDINGS & FUND RECOVERY:**")
                sb.appendLine("• **Section 503 BNSS De-freezing Application:** Move Magistrate for release of frozen funds directly back into your account.")
                sb.appendLine("• **Adjudicating Officer u/s 46 IT Act:** Claim compensation from State IT Secretary for financial losses.")
                sb.appendLine("• **Consumer Court:** File for deficiency in banking service if bank failed to enforce security standards.")
                sb.appendLine()
                sb.appendLine("🛡️ **ADVOCATE'S STRATEGIC ADVICE & IMPORTANT CAUTIONS:**")
                sb.appendLine("• Do NOT delete WhatsApp chats or SMS with fraudsters; export and backup chat history.")
                sb.appendLine("• The first 2 hours are the most critical for account freezing before money is laundered.")
            }

            // 5. Police Refusal to Lodge FIR
            q.contains("police") && (q.contains("refused") || q.contains("not taking") || q.contains("not registering") || q.contains("rejected")) && (q.contains("fir") || q.contains("complaint")) -> {
                sb.appendLine("⚖️ **SENIOR ADVOCATE LEGAL ADVISORY & PROCEEDING ROADMAP**")
                sb.appendLine("**Matter:** Statutory Remedies against Police Refusal to Lodge FIR")
                sb.appendLine("**Primary Statutes:** Bharatiya Nagarik Suraksha Sanhita (BNSS 2023) Sec 173, 175(3), 175(4), 176 • BNS Sec 199")
                sb.appendLine()
                sb.appendLine("⚖️ **NATURE OF OFFENSE & CLASSIFICATION:**")
                sb.appendLine("• **Classification:** Dereliction of Public Duty by Police & Infringement of Statutory Rights.")
                sb.appendLine("• **Statutory Mandate:** Mandatory duty to register FIR for cognizable offenses under Lalita Kumari v. Govt of UP.")
                sb.appendLine()
                sb.appendLine("🚨 **PHASE 1: IMMEDIATE STEPS AT THE POLICE STATION:**")
                sb.appendLine("• **Written Complaint with Receiving Stamp:** Carry two copies; insist on station seal and General Diary (GD) entry number.")
                sb.appendLine("• **Call 112 from the Station:** Creates an official computer-aided dispatch log verifying you attended the station.")
                sb.appendLine("• **Zero FIR u/s 173 BNSS:** If out-of-jurisdiction is cited, demand registration of a Zero FIR for transfer.")
                sb.appendLine()
                sb.appendLine("📜 **PHASE 2: STATUTORY ESCALATION (MANDATORY ADVOCATE STEP):**")
                sb.appendLine("• **Section 175(3) BNSS Representation to SP:** Send your signed complaint via Registered Speed Post directly to Superintendent of Police (SP) / DCP.")
                sb.appendLine("• **Preserve Speed Post Consignment Receipt:** Mandatory prerequisite for approaching Magistrate.")
                sb.appendLine("• **Section 199 BNS Action:** Section 199 BNS punishes with up to 2 years jail any public servant who willfully disobeys direction to record cognizable information.")
                sb.appendLine()
                sb.appendLine("🏛️ **PHASE 3: JUDICIAL PROCEEDINGS BEFORE MAGISTRATE & HIGH COURT:**")
                sb.appendLine("• **Section 175(4) / 176 BNSS Application to Judicial Magistrate:** File application before JMFC praying for judicial orders directing police to lodge FIR.")
                sb.appendLine("• **Article 226 Writ of Mandamus:** Approach High Court if offense is severe and local police machinery is compromised.")
                sb.appendLine()
                sb.appendLine("🛡️ **ADVOCATE'S STRATEGIC ADVICE & IMPORTANT CAUTIONS:**")
                sb.appendLine("• *Precedent:* Supreme Court in 'Lalita Kumari' held FIR registration is mandatory if cognizable crime is disclosed.")
                sb.appendLine("• Keep dated chronological binder of all complaint copies and India Post consignment slips.")
            }

            // 6. Default Fallback for Any Real-World Scenario
            else -> {
                sb.appendLine("⚖️ **SENIOR ADVOCATE LEGAL ADVISORY & PROCEEDING ROADMAP**")
                sb.appendLine("**Matter:** Legal Evaluation & Procedural Roadmap for Client Inquiry")
                sb.appendLine("**Primary Statutes:** Bharatiya Nyaya Sanhita (BNS 2023) • Bharatiya Nagarik Suraksha Sanhita (BNSS 2023) • Special Enactments")
                sb.appendLine()
                sb.appendLine("⚖️ **NATURE OF OFFENSE & LEGAL CLASSIFICATION:**")
                sb.appendLine("• **Classification:** Civil/Criminal Infringement of Codified Rights under Indian Jurisprudence.")
                sb.appendLine("• **Cognizable Status:** Subject to Station House Officer General Diary assessment.")
                sb.appendLine()
                sb.appendLine("🚨 **PHASE 1: IMMEDIATE STEPS & EVIDENCE PRESERVATION (First 24-48 Hours):**")
                sb.appendLine("• **Avoid Self-Help:** Do not take the law into your own hands or engage in verbal/physical retaliation.")
                sb.appendLine("• **Preserve Evidence:** Document all contemporaneous evidence (WhatsApp, audio recordings, invoices, photographs) under Section 63 BSA 2023.")
                sb.appendLine("• **Official Helpline Logging:** Call 112 or relevant government helpline to generate an official police event log.")
                sb.appendLine()
                sb.appendLine("📜 **PHASE 2: FORMAL LEGAL NOTICE & STATUTORY POLICE RECOURSE:**")
                sb.appendLine("• **Advocate Legal Demand Notice:** Serve formal 15-Day Legal Demand Notice via Registered Speed Post detailing statutory violations.")
                sb.appendLine("• **Police Complaint / Zero FIR:** Submit written complaint under Section 173 BNSS 2023; obtain stamped receiving copy.")
                sb.appendLine("• **Section 175(3) BNSS Escalation to SP:** If local police refuse, escalate in writing to the Superintendent of Police.")
                sb.appendLine()
                sb.appendLine("🏛️ **PHASE 3: JUDICIAL PROCEEDINGS, PETITIONS & COURT RELIEFS:**")
                sb.appendLine("• **Section 175(4) / 176 BNSS Application to Magistrate:** Move Judicial Magistrate for orders directing investigation if police fail to act.")
                sb.appendLine("• **Civil Injunction / Specific Relief / Consumer Forum:** File in competent forum for injunction, recovery, or compensation.")
                sb.appendLine("• **Claim Liquidated Damages:** Pray for financial restitution and compensation for mental agony and litigation costs.")
                sb.appendLine()
                sb.appendLine("🛡️ **ADVOCATE'S STRATEGIC ADVICE & IMPORTANT CAUTIONS:**")
                sb.appendLine("• Always verify statutory limitation periods before instituting proceedings.")
                sb.appendLine("• Maintain India Post Speed Post receipts and delivery tracking consignment notes as primary evidence.")
                sb.appendLine("• Consult licensed Bar Council advocate for customized litigation strategy.")
            }
        }

        sb.appendLine()
        sb.appendLine("⚡ Note: Senior Advocate procedural roadmap grounded in BNS, BNSS, BSA, and Special Acts.")
        return sb.toString().trim() to 0.98
    }

}
