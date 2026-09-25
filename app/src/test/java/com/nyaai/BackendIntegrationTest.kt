package com.nyaai

import com.nyaai.data.local.AiService
import com.nyaai.data.local.DocumentEntity
import com.nyaai.data.model.BackendModelClient
import com.nyaai.data.model.FakeModelClient
import com.nyaai.data.retrieval.LegalRetrievalService
import com.nyaai.ui.state.AppLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BackendIntegrationTest {

    @Test
    fun testBackendModelClientFallbackOnUnreachableHost() = runBlocking {
        val fallback = FakeModelClient(response = "Fallback procedure response")
        // Non-existent port/host will fail connection quickly
        val client = BackendModelClient(
            baseUrl = "http://127.0.0.1:59999",
            fallbackClient = fallback,
            timeoutMs = 1000
        )

        val result = client.generateContent("Test query")
        assertTrue("Client should succeed using fallback", result.isSuccess)
        assertEquals("Fallback procedure response", result.getOrNull())
    }

    @Test
    fun testBackendModelClientFailsWithoutFallbackWhenOffline() = runBlocking {
        val client = BackendModelClient(
            baseUrl = "http://127.0.0.1:59999",
            fallbackClient = null,
            timeoutMs = 1000
        )

        val result = client.generateContent("Test query")
        assertTrue("Client should fail if backend offline and no fallback", result.isFailure)
    }

    @Test
    fun testLegalRetrievalServiceFallsBackToRoomFTSWhenBackendOffline() = runBlocking {
        val doc = DocumentEntity(
            sourcePath = "bns.pdf",
            content = "Section 281 Rash driving",
            jurisdiction = "central",
            act = "Bharatiya Nyaya Sanhita, 2023",
            section = "Section 281",
            status = "in_force"
        ).apply { rowid = 1 }

        val dao = FakeRagDao(sampleDocs = listOf(doc))
        val retrievalService = LegalRetrievalService(
            ragDao = dao,
            backendBaseUrl = "http://127.0.0.1:59999"
        )

        val result = retrievalService.retrieve("Rash driving Section 281")
        assertNotNull(result)
        // Should fall back to local Room DAO search
        assertTrue("Should return local document from fallback", result.documents.isNotEmpty())
        assertEquals("Section 281", result.documents.first().section)
    }

    @Test
    fun testAiServiceOperatesWithNoApiKeyViaBackendClient() = runBlocking {
        val doc = DocumentEntity(
            sourcePath = "ni.pdf",
            content = "Section 138 Cheque bounce",
            jurisdiction = "central",
            act = "Negotiable Instruments Act, 1881",
            section = "Section 138",
            status = "in_force"
        ).apply { rowid = 1 }

        val dao = FakeRagDao(sampleDocs = listOf(doc))
        val aiService = AiService(
            ragDao = dao,
            apiKey = "", // Empty API key!
            backendBaseUrl = "http://127.0.0.1:59999",
            modelClient = FakeModelClient(response = "Simulated backend response")
        )

        val (answer, conf) = aiService.generateAnswer("Cheque bounce 138 notice", AppLanguage.ENGLISH)
        assertNotNull(answer)
        assertTrue(conf > 0.0)
    }
}
