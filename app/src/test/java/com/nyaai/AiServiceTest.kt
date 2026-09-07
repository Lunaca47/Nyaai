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
                terms.any { t -> ex.question.lowercase().contains(t) }
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

        assertTrue("Answer should contain fallback message", answer.contains("couldn't find information"))
        assertEquals("Empty context confidence should be 0.20", 0.20, confidence, 0.001)
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
        val (answer, _) = service.generateAnswer("throw_error query", AppLanguage.ENGLISH)

        assertNotNull("Answer must be returned despite dao exception", answer)
        assertTrue("Should fallback safely", answer.contains("couldn't find information"))
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
        assertEquals("Confidence for verified training example offline fallback should be 0.95", 0.95, confidence, 0.001)
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
        assertTrue("Should fallback safely when doc doesn't have substantive keywords", answer.contains("couldn't find information"))
        assertEquals(0.20, confidence, 0.001)
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
}
