package com.nyaai.data.local

import android.util.Log
import com.nyaai.data.model.BackendModelClient
import com.nyaai.data.model.GeminiModelClient
import com.nyaai.data.model.ModelClient
import com.nyaai.data.procedure.ProcedureEngine
import com.nyaai.data.retrieval.LegalRetrievalService
import com.nyaai.data.verification.CitationVerifier
import com.nyaai.ui.state.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiService(
    private val ragDao: RagDao, 
    private val apiKey: String = "",
    private val customApiKeyProvider: (() -> String?)? = null,
    private val procedureEngine: ProcedureEngine = ProcedureEngine(),
    private val backendBaseUrl: String? = null,
    private val retrievalService: LegalRetrievalService = LegalRetrievalService(ragDao, backendBaseUrl),
    private val modelClient: ModelClient = BackendModelClient(
        baseUrl = backendBaseUrl ?: "http://10.0.2.2:8000",
        fallbackClient = GeminiModelClient(
            apiKeyProvider = { customApiKeyProvider?.invoke()?.trim()?.takeIf { it.isNotBlank() } ?: apiKey.trim() }
        )
    ),
    private val citationVerifier: CitationVerifier = CitationVerifier()
) {


    companion object {
        private const val TAG = "AiService"
    }

    suspend fun generateAnswer(userQuery: String, responseLanguage: AppLanguage): Pair<String, Double> = withContext(Dispatchers.IO) {
        val langName = responseLanguage.displayName

        // 0. CONVERSATIONAL INTENT: Greetings, small talk, identity, and gratitude
        val conversationalGreeting = getConversationalResponse(userQuery, responseLanguage)
        if (conversationalGreeting != null) {
            return@withContext conversationalGreeting to 0.99
        }

        // 1. PROCEDURAL SCENARIOS: If offline or scenario query, route through structured ProcedureEngine
        val isScenario = procedureEngine.isScenarioQuery(userQuery)

        // 2. RETRIEVAL: Execute via LegalRetrievalService
        val retrievalResult = retrievalService.retrieve(userQuery)
        val uniqueTrainingMatches = retrievalResult.uniqueTrainingMatches
        val bestCandidate = retrievalResult.bestCandidate
        val bestCandidateScore = retrievalResult.bestCandidateScore
        val uniqueContexts = retrievalResult.documents
        val substantiveKeywords = retrievalResult.substantiveKeywords
        val numbersInQuery = retrievalResult.numbersInQuery

        // 3. CONFIDENCE SCORING
        var confidence = when {
            bestCandidateScore >= 20 -> 0.98
            bestCandidateScore >= 10 -> 0.95
            uniqueTrainingMatches.isNotEmpty() -> 0.92
            uniqueContexts.size >= 3 -> 0.88
            uniqueContexts.size == 2 -> 0.80
            uniqueContexts.size == 1 -> 0.70
            else -> 0.40
        }
        if (numbersInQuery.isNotEmpty() && (bestCandidateScore >= 15 || uniqueContexts.any { ctx -> numbersInQuery.any { num -> ctx.content.contains(num) } })) {
            confidence = (confidence + 0.02).coerceAtMost(0.99)
        }

        val contextData = uniqueContexts.joinToString("\n\n") { "[Source: ${it.sourcePath}]\n${it.content}" }
        val trainingData = uniqueTrainingMatches.joinToString("\n\n") { "[Verified Act: ${it.sourcePath} | Domain: ${it.legalDomain}]\nQ: ${it.question}\nA: ${it.answer}" }

        // 4. MODEL CALL via ModelClient abstraction
        val effectiveApiKey = customApiKeyProvider?.invoke()?.trim()?.takeIf { it.isNotBlank() } ?: apiKey.trim()
        if (effectiveApiKey.isNotBlank() && effectiveApiKey != "YOUR_NEW_API_KEY_HERE" && effectiveApiKey != "YOUR_GEMINI_API_KEY") {
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

            val modelResult = modelClient.generateContent(promptText)
            if (modelResult.isSuccess) {
                val answer = modelResult.getOrThrow()
                if (answer.isNotBlank()) {
                    // Hard Gate Citation Verification
                    val gated = citationVerifier.enforceHardGate(
                        responseText = answer,
                        retrievedDocuments = uniqueContexts,
                        retrievedExamples = uniqueTrainingMatches
                    )
                    if (gated.shouldFallback) {
                        Log.w(TAG, "CitationVerifier hard gate rejected ungrounded generation: ${gated.result.ungroundedCitations}")
                    } else {
                        return@withContext gated.gatedText to confidence
                    }
                }
            } else {
                Log.w(TAG, "ModelClient failed: ${modelResult.exceptionOrNull()?.message}")
            }
        }

        // 5. OFFLINE FALLBACK — Grounded in structured procedures, training examples, or document search
        return@withContext buildOfflineAnswer(
            userQuery = userQuery,
            queryKeywords = substantiveKeywords,
            contexts = uniqueContexts,
            trainingMatches = uniqueTrainingMatches,
            bestCandidate = bestCandidate,
            bestCandidateScore = bestCandidateScore,
            baseConfidence = confidence,
            language = responseLanguage
        )
    }

    fun isScenarioQuery(query: String): Boolean = procedureEngine.isScenarioQuery(query)

    fun isDetailedQuery(query: String): Boolean = procedureEngine.isDetailedQuery(query)

    fun buildAdvocateScenarioAnswer(query: String, language: AppLanguage): Pair<String, Double> =
        procedureEngine.buildProcedureAnswer(query, language)

    fun scoreTrainingCandidate(
        item: TrainingExampleEntity,
        query: String,
        terms: List<String>,
        numbers: List<String>
    ): Int = retrievalService.scoreTrainingCandidate(item, query, terms, numbers)

    private fun buildOfflineAnswer(
        userQuery: String,
        queryKeywords: List<String>,
        contexts: List<DocumentEntity>,
        trainingMatches: List<TrainingExampleEntity>,
        bestCandidate: TrainingExampleEntity?,
        bestCandidateScore: Int,
        baseConfidence: Double,
        language: AppLanguage = AppLanguage.ENGLISH
    ): Pair<String, Double> {

        if (procedureEngine.isScenarioQuery(userQuery)) {
            return procedureEngine.buildProcedureAnswer(userQuery, language)
        }

        // If strong matching verified training example exists, return it with high accuracy
        val targetCandidate = if (bestCandidate != null && bestCandidateScore >= 8) {
            bestCandidate
        } else {
            trainingMatches.firstOrNull()
        }

        if (targetCandidate != null) {
            val sb = StringBuilder()
            sb.appendLine("🏛️ **VERIFIED STATUTORY LEGAL PROVISION**\n")
            sb.appendLine(targetCandidate.answer.trim())
            sb.appendLine()
            sb.appendLine("📖 **Source:** ${targetCandidate.sourcePath}")
            if (targetCandidate.legalDomain.isNotBlank()) {
                sb.appendLine("⚖️ **Legal Domain:** ${targetCandidate.legalDomain}")
            }
            sb.append("\n⚡ Note: Verified statutory knowledge grounded in codified Indian Law.")
            val finalConf = if (bestCandidateScore >= 18) 0.98 else 0.95
            return sb.toString().trim() to finalConf
        }

        if (contexts.isEmpty()) {
            return "Based on Indian Statutory Law, information regarding \"$userQuery\" is governed under codified provisions of the Bharatiya Nyaya Sanhita (BNS 2023), Bharatiya Nagarik Suraksha Sanhita (BNSS 2023), Bharatiya Sakshya Adhiniyam (BSA 2023), or the Constitution of India. For immediate assistance, contact National Legal Aid (NALSA) at 15100 or Emergency Services at 112." to 0.75
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
            return "Based on Indian Statutory Law, legal procedures and rights regarding \"$userQuery\" are codified under the Bharatiya Nyaya Sanhita (BNS 2023), Bharatiya Nagarik Suraksha Sanhita (BNSS 2023), or the Constitution of India. For immediate legal aid or representation, call National Legal Aid (NALSA) at 15100 or Emergency Services at 112." to 0.75
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
