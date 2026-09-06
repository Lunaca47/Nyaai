package com.nyaai

import com.nyaai.data.local.*
import com.nyaai.ui.state.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FakeRagDao(private val sampleDocs: List<DocumentEntity> = emptyList()) : RagDao {
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
    override suspend fun searchTrainingExamples(query: String): List<TrainingExampleEntity> = emptyList()
    override suspend fun getGoodExamples(): List<MessageEntity> = emptyList()
    override suspend fun getTrainingCount(): Int = 0
    override fun getTrainingCountFlow(): Flow<Int> = flowOf(0)
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
}
