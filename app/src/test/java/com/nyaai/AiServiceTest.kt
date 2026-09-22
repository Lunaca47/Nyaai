package com.nyaai

import com.nyaai.data.local.*
import com.nyaai.ui.state.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FakeRagDao(
    private val sampleDocs: List<DocumentEntity> = emptyList(),
    private val sampleTraining: List<TrainingExampleEntity> = emptyList()
) : RagDao {
    override suspend fun search(query: String): List<DocumentEntity> {
        if (query.contains("throw_error")) {
            throw RuntimeException("Simulated FTS search failure")
        }
        return sampleDocs
    }

    override suspend fun insert(document: DocumentEntity) {}
    override suspend fun insertAll(documents: List<DocumentEntity>) {}
    override suspend fun getDocumentCount(): Int = sampleDocs.size
    override suspend fun createSession(session: ChatSessionEntity): Long = 1L
    override suspend fun getAllSessions(): List<ChatSessionEntity> = emptyList()
    override suspend fun insertMessage(message: MessageEntity): Long = 1L
    override suspend fun getMessagesForSession(sessionId: Long): List<MessageEntity> = emptyList()
    override suspend fun updateSessionTitle(sessionId: Long, newTitle: String) {}
    override suspend fun updateMessageFeedback(messageId: Long, feedback: String) {}
    override suspend fun clearAllHistory() {}
    override suspend fun deleteMessagesForSession(sessionId: Long) {}
    override suspend fun deleteSession(sessionId: Long) {}
    override suspend fun insertBookmark(bookmark: BookmarkEntity): Long = 1L
    override fun getAllBookmarksFlow(): Flow<List<BookmarkEntity>> = flowOf(emptyList())
    override suspend fun getAllBookmarks(): List<BookmarkEntity> = emptyList()
    override suspend fun deleteBookmark(id: Long) {}
    override suspend fun deleteBookmarkByContent(content: String) {}
    override suspend fun isBookmarked(content: String): Int = 0
    override suspend fun insertTrainingExample(example: TrainingExampleEntity) {}
    override suspend fun insertAllTrainingExamples(examples: List<TrainingExampleEntity>) {}
    override suspend fun searchTrainingExamples(query: String): List<TrainingExampleEntity> {
        if (sampleTraining.isNotEmpty()) {
            val terms = query.lowercase().split(" ").filter { it.length > 2 }
            return sampleTraining.filter { ex ->
                terms.any { t -> ex.question.lowercase().contains(t) || ex.sourcePath.lowercase().contains(t) || ex.answer.lowercase().contains(t) }
            }
        }
        return emptyList()
    }
    override suspend fun searchTrainingExamplesByNumber(num: String): List<TrainingExampleEntity> {
        if (sampleTraining.isNotEmpty()) {
            return sampleTraining.filter { ex ->
                ex.sourcePath.contains(num) || ex.question.contains(num)
            }
        }
        return emptyList()
    }
    override suspend fun getGoodExamples(): List<MessageEntity> = emptyList()
    override suspend fun getTrainingCount(): Int = sampleTraining.size
    override fun getTrainingCountFlow(): Flow<Int> = flowOf(sampleTraining.size)
    override suspend fun getPagedDocuments(limit: Int, offset: Int): List<DocumentEntity> = emptyList()
}

class AiServiceTest {

    @Test
    fun testEmptyContextReturnsFallback() = runTest {
        val fakeDao = FakeRagDao(emptyList())
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer("nonexistent topic", AppLanguage.ENGLISH)

        assertTrue("Answer should contain statutory guidance fallback message", answer.contains("Bharatiya Nyaya Sanhita") || answer.contains("15100"))
        assertTrue("Empty context confidence should be helpful and >= 0.70", confidence >= 0.70)
    }

    @Test
    fun testMatchingContextGeneratesOfflineAnswer() = runTest {
        val doc = DocumentEntity(
            sourcePath = "coi.pdf",
            content = "Page 15: Article 21. No person shall be deprived of his life or personal liberty except according to procedure established by law."
        ).apply { rowid = 1 }

        val fakeDao = FakeRagDao(listOf(doc))
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer("personal liberty rights", AppLanguage.ENGLISH)

        assertTrue("Should include source name", answer.contains("Constitution of India"))
        assertTrue("Confidence should be above fallback minimum", confidence >= 0.25)
    }

    @Test
    fun testArticleNumberMatchBoostsConfidence() = runTest {
        val doc = DocumentEntity(
            sourcePath = "coi.pdf",
            content = "Page 15: Article 21. Protection of life and personal liberty."
        ).apply { rowid = 1 }

        val fakeDao = FakeRagDao(listOf(doc))
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer("what is article 21", AppLanguage.ENGLISH)

        assertTrue("Should mention source act", answer.contains("Constitution of India"))
        assertTrue("Confidence should be calculated and boosted", confidence > 0.40)
    }

    @Test
    fun testFtsSearchFailureDoesNotCrashService() = runTest {
        val fakeDao = FakeRagDao(emptyList())
        val service = AiService(fakeDao, apiKey = "")

        // Query that could trigger search failure
        val (answer, confidence) = service.generateAnswer("throw_error query", AppLanguage.ENGLISH)

        assertNotNull("Answer must be returned despite dao exception", answer)
        assertTrue("Should fallback safely to statutory guidance", answer.contains("Bharatiya Nyaya Sanhita") || answer.contains("15100"))
        assertTrue("Confidence should be helpful", confidence >= 0.70)
    }

    @Test
    fun testConsumerRightsQueryResolvedViaTrainingExamples() = runTest {
        val trainingExample = TrainingExampleEntity(
            question = "What is a consumer rights overview under the Consumer Protection Act, 2019?",
            answer = "Under the Consumer Protection Act, 2019, consumers have statutory rights including the Right to Safety, Right to be Informed, Right to Choose, Right to be Heard, Right to seek Redressal, and Right to Consumer Education (Sections 2(9), 10, 28, 35).",
            sourcePath = "Consumer Protection Act, 2019",
            legalDomain = "Consumer Law",
            reasoningQuality = 1
        )
        val irrelevantDoc = DocumentEntity(
            sourcePath = "coi.pdf",
            content = "Page 6: Article 6. Rights of citizenship of certain persons who have migrated to India from Pakistan."
        ).apply { rowid = 6 }

        val fakeDao = FakeRagDao(
            sampleDocs = listOf(irrelevantDoc),
            sampleTraining = listOf(trainingExample)
        )
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer("consumer right overview", AppLanguage.ENGLISH)

        assertTrue("Answer should contain Consumer Protection Act provisions", answer.contains("Consumer Protection Act, 2019"))
        assertTrue("Answer should mention consumer rights like Right to Safety or Redressal", answer.contains("Right to Safety") || answer.contains("Right to be Informed"))
        assertFalse("Answer must NOT contain irrelevant Pakistan migration citizenship text", answer.contains("Pakistan"))
        assertTrue("Confidence for verified training example offline fallback should be >= 0.95 (actual: $confidence)", confidence >= 0.95)
    }

    @Test
    fun testIrrelevantConstitutionMatchesFilteredOutWhenNoSubstantiveKeywords() = runTest {
        val irrelevantDoc = DocumentEntity(
            sourcePath = "coi.pdf",
            content = "Page 6: Article 6. Rights of citizenship of certain persons who have migrated to India from Pakistan."
        ).apply { rowid = 6 }

        val fakeDao = FakeRagDao(sampleDocs = listOf(irrelevantDoc), sampleTraining = emptyList())
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer("consumer rights overview", AppLanguage.ENGLISH)

        assertFalse("Should filter out Article 6 Pakistan migration doc", answer.contains("Pakistan"))
        assertTrue("Should fallback safely to statutory guidance when doc doesn't have substantive keywords", answer.contains("Bharatiya Nyaya Sanhita") || answer.contains("15100"))
        assertTrue("Fallback confidence should be >= 0.70", confidence >= 0.70)
    }

    @Test
    fun testGreetingQueriesReturnWarmGreeting() = runTest {
        val fakeDao = FakeRagDao(emptyList())
        val service = AiService(fakeDao, apiKey = "")

        val greetings = listOf("hi", "hello", "good morning", "how are you", "who are you", "thank you")
        for (g in greetings) {
            val (answer, confidence) = service.generateAnswer(g, AppLanguage.ENGLISH)
            assertFalse("Greeting '$g' should NOT return legal search fallback error", answer.contains("couldn't find information"))
            assertTrue("Greeting '$g' should contain friendly introduction or pleasantry", answer.contains("Nyaai") || answer.contains("welcome") || answer.contains("great"))
            assertEquals("Greeting confidence should be high (0.99)", 0.99, confidence, 0.001)
        }
    }

    @Test
    fun testHindiGreetingReturnsHindiGreeting() = runTest {
        val fakeDao = FakeRagDao(emptyList())
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer("namaste", AppLanguage.HINDI)
        assertTrue("Hindi greeting should contain नमस्ते or न्यायAI", answer.contains("नमस्ते") || answer.contains("न्यायAI"))
        assertEquals(0.99, confidence, 0.001)
    }

    @Test
    fun testLandlordLockedFlatScenarioReturnsAdvocateRoadmap() = runTest {
        val fakeDao = FakeRagDao(emptyList())
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer(
            "My landlord locked my flat and threw out my belongings, what should I do?",
            AppLanguage.ENGLISH
        )

        assertTrue("Answer should feature Senior Advocate Advisory", answer.contains("SENIOR ADVOCATE LEGAL ADVISORY"))
        assertTrue("Answer should address unlawful eviction / lockout", answer.contains("Unlawful Eviction") || answer.contains("Flat Lockout"))
        assertTrue("Answer should cite Transfer of Property Act or BNS", answer.contains("Transfer of Property Act") || answer.contains("BNS 2023"))
        assertTrue("Answer should advise Section 175(3) BNSS or police complaint", answer.contains("Section 175(3) BNSS") || answer.contains("173 BNSS"))
        assertTrue("Answer should advise Order 39 CPC or Specific Relief Act", answer.contains("Order 39") || answer.contains("Specific Relief"))
        assertEquals("Advocate scenario confidence should be 0.98", 0.98, confidence, 0.001)
    }

    @Test
    fun testHitAndRunAccidentScenarioReturnsAdvocateRoadmap() = runTest {
        val fakeDao = FakeRagDao(emptyList())
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer(
            "Hit and run accident happened with my car, what is the procedure?",
            AppLanguage.ENGLISH
        )

        assertTrue("Answer should feature Senior Advocate Advisory", answer.contains("SENIOR ADVOCATE LEGAL ADVISORY"))
        assertTrue("Answer should address hit and run accident", answer.contains("Hit-and-Run Motor Vehicle Accident"))
        assertTrue("Answer should mention Medico-Legal Certificate (MLC)", answer.contains("Medico-Legal Certificate"))
        assertTrue("Answer should mention MACT claim under Motor Vehicles Act", answer.contains("MACT Claim") || answer.contains("Motor Vehicles Act"))
        assertEquals(0.98, confidence, 0.001)
    }

    @Test
    fun testChequeBounceScenarioReturnsAdvocateRoadmap() = runTest {
        val fakeDao = FakeRagDao(emptyList())
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer(
            "My client gave me a cheque that bounced due to insufficient funds, how do I proceed?",
            AppLanguage.ENGLISH
        )

        assertTrue("Answer should feature Senior Advocate Advisory", answer.contains("SENIOR ADVOCATE LEGAL ADVISORY"))
        assertTrue("Answer should cite Section 138 Negotiable Instruments Act", answer.contains("Negotiable Instruments Act") || answer.contains("138"))
        assertTrue("Answer should mention 15-Day Statutory Legal Demand Notice", answer.contains("15-Day Statutory Legal Demand Notice") || answer.contains("15-Day"))
        assertTrue("Answer should mention 20% Interim Compensation u/s 143A", answer.contains("143A"))
        assertEquals(0.98, confidence, 0.001)
    }

    @Test
    fun testCyberFraudScenarioReturnsAdvocateRoadmap() = runTest {
        val fakeDao = FakeRagDao(emptyList())
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer(
            "Someone did a UPI fraud and debited money from my bank account, please help",
            AppLanguage.ENGLISH
        )

        assertTrue("Answer should feature Senior Advocate Advisory", answer.contains("SENIOR ADVOCATE LEGAL ADVISORY"))
        assertTrue("Answer should mention 1930 Cyber Fraud Helpline", answer.contains("1930 Cyber Fraud Helpline") || answer.contains("1930"))
        assertTrue("Answer should mention Section 66D IT Act", answer.contains("66D IT Act") || answer.contains("Information Technology Act"))
        assertTrue("Answer should mention Magistrate de-freezing application u/s 503 BNSS", answer.contains("503 BNSS"))
        assertEquals(0.98, confidence, 0.001)
    }

    @Test
    fun testPoliceRefusedFirScenarioReturnsAdvocateRoadmap() = runTest {
        val fakeDao = FakeRagDao(emptyList())
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer(
            "Police refused to register my fir for a cognizable complaint, what can I do?",
            AppLanguage.ENGLISH
        )

        assertTrue("Answer should feature Senior Advocate Advisory", answer.contains("SENIOR ADVOCATE LEGAL ADVISORY"))
        assertTrue("Answer should cite Section 175(3) BNSS Representation to SP", answer.contains("175(3) BNSS"))
        assertTrue("Answer should cite Landmark Lalita Kumari ruling", answer.contains("Lalita Kumari"))
        assertTrue("Answer should cite Section 175(4) / 176 BNSS Application to Magistrate", answer.contains("175(4)") || answer.contains("176 BNSS"))
        assertEquals(0.98, confidence, 0.001)
    }

    @Test
    fun testNovelScenarioQueryReturnsAdvocateProceduralFramework() = runTest {
        val fakeDao = FakeRagDao(emptyList())
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer(
            "Someone is threatening me and harassing me over disputed invoices, what are my legal options?",
            AppLanguage.ENGLISH
        )

        assertTrue("Answer should feature Senior Advocate Advisory", answer.contains("SENIOR ADVOCATE LEGAL ADVISORY"))
        assertTrue("Answer should structure into Phase 1 Immediate Steps", answer.contains("PHASE 1"))
        assertTrue("Answer should structure into Phase 2 Formal Legal Notice / Police Recourse", answer.contains("PHASE 2"))
        assertTrue("Answer should structure into Phase 3 Judicial Proceedings", answer.contains("PHASE 3"))
        assertTrue("Answer should include Advocate's Strategic Advice", answer.contains("ADVOCATE'S STRATEGIC ADVICE"))
        assertEquals(0.98, confidence, 0.001)
    }

    @Test
    fun testDetailedQueryReturnsMoreThanSevenPointsForLandlord() = runTest {
        val fakeDao = FakeRagDao(emptyList())
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer(
            "My landlord locked my flat and threw out my belongings, explain in detail step by step each and everything",
            AppLanguage.ENGLISH
        )

        assertTrue("Answer should feature Comprehensive Brief", answer.contains("COMPREHENSIVE LEGAL BRIEF"))
        // Check for 9 sequentially numbered points
        for (i in 1..9) {
            assertTrue("Answer must contain point $i", answer.contains("$i."))
        }
        val pointMatches = Regex("(?m)^\\d+\\.").findAll(answer).count()
        assertTrue("Detailed answer must contain more than 7 points (actual: $pointMatches)", pointMatches >= 8)
        assertTrue("Must cite Section 106 TP Act", answer.contains("Transfer of Property Act"))
        assertTrue("Must cite Section 175(3) BNSS", answer.contains("175(3) BNSS"))
        assertTrue("Must cite Order 39 CPC", answer.contains("Order 39"))
        assertEquals(0.99, confidence, 0.001)
    }

    @Test
    fun testDetailedQueryReturnsMoreThanSevenPointsForChequeBounce() = runTest {
        val fakeDao = FakeRagDao(emptyList())
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer(
            "Cheque bounced due to insufficient funds, provide full detailed explanation with all points",
            AppLanguage.ENGLISH
        )

        assertTrue("Answer should feature Comprehensive Brief", answer.contains("COMPREHENSIVE LEGAL BRIEF"))
        val pointMatches = Regex("(?m)^\\d+\\.").findAll(answer).count()
        assertTrue("Detailed cheque answer must contain more than 7 points (actual: $pointMatches)", pointMatches >= 8)
        assertTrue("Must mention Section 138", answer.contains("138"))
        assertTrue("Must mention Section 143A interim compensation", answer.contains("143A"))
        assertTrue("Must mention Section 141 corporate liability", answer.contains("141"))
        assertEquals(0.99, confidence, 0.001)
    }

    @Test
    fun testDetailedQueryReturnsMoreThanSevenPointsForPoliceRefusal() = runTest {
        val fakeDao = FakeRagDao(emptyList())
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer(
            "Police refused to register my FIR, elaborate and explain properly in full detail",
            AppLanguage.ENGLISH
        )

        assertTrue("Answer should feature Comprehensive Brief", answer.contains("COMPREHENSIVE LEGAL BRIEF"))
        val pointMatches = Regex("(?m)^\\d+\\.").findAll(answer).count()
        assertTrue("Detailed police refusal answer must contain more than 7 points (actual: $pointMatches)", pointMatches >= 8)
        assertTrue("Must mention Section 175(3) BNSS", answer.contains("175(3) BNSS"))
        assertTrue("Must mention Lalita Kumari", answer.contains("Lalita Kumari"))
        assertTrue("Must mention Article 226", answer.contains("Article 226"))
        assertEquals(0.99, confidence, 0.001)
    }

    @Test
    fun testSection482BNSSBailGroundedResponse() = runTest {
        val training = listOf(
            TrainingExampleEntity(
                id = 482L,
                question = "What are the rules for: I have apprehension of arrest. Can I apply for Anticipatory Bail?",
                answer = "Yes, you can apply for Anticipatory Bail under Section 482 BNSS before the Sessions Court or High Court.",
                sourcePath = "Section 482, Bharatiya Nagarik Suraksha Sanhita, 2023",
                legalDomain = "Criminal Procedure (BNSS)",
                reasoningQuality = 1
            )
        )
        val fakeDao = FakeRagDao(sampleTraining = training)
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer("Section 482 BNSS Bail", AppLanguage.ENGLISH)

        assertTrue("Answer should contain verified section 482 info", answer.contains("Anticipatory Bail") || answer.contains("482"))
        assertTrue("Source should cite BNSS 482", answer.contains("482"))
        assertTrue("Confidence should be high (>= 0.95)", confidence >= 0.95)
    }

    @Test
    fun testArticle21GroundedResponse() = runTest {
        val training = listOf(
            TrainingExampleEntity(
                id = 21L,
                question = "What rights are guaranteed under Article 21 of the Constitution?",
                answer = "Under Article 21, Constitution of India, no person shall be deprived of life or personal liberty except by procedure established by law. This includes right to privacy (Puttaswamy judgment).",
                sourcePath = "Article 21, Constitution of India",
                legalDomain = "Constitutional Law",
                reasoningQuality = 1
            )
        )
        val fakeDao = FakeRagDao(sampleTraining = training)
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer("Article 21 Privacy Rights", AppLanguage.ENGLISH)

        assertTrue("Answer should contain Article 21 info", answer.contains("Article 21") || answer.contains("privacy"))
        assertTrue("Confidence should be high (>= 0.95)", confidence >= 0.95)
    }

    @Test
    fun testSection103BNSGroundedResponse() = runTest {
        val training = listOf(
            TrainingExampleEntity(
                id = 103L,
                question = "What is the penalty for mob lynching and murder under BNS Section 103?",
                answer = "Under Section 103(2) BNS, murder committed by a group of five or more persons on grounds of race, caste, or religion is punishable with death or life imprisonment.",
                sourcePath = "Bharatiya Nyaya Sanhita, 2023, Section 103(2)",
                legalDomain = "Criminal Law (BNS)",
                reasoningQuality = 1
            )
        )
        val fakeDao = FakeRagDao(sampleTraining = training)
        val service = AiService(fakeDao, apiKey = "")

        val (answer, confidence) = service.generateAnswer("Section 103 BNS Murder", AppLanguage.ENGLISH)

        assertTrue("Answer should contain Section 103 murder provision", answer.contains("103") || answer.contains("murder"))
        assertTrue("Confidence should be high (>= 0.95)", confidence >= 0.95)
    }
}

