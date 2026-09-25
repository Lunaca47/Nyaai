package com.nyaai

import com.nyaai.data.local.DocumentEntity
import com.nyaai.data.local.TrainingExampleEntity
import com.nyaai.data.retrieval.LegalRetrievalService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LegalRetrievalServiceTest {

    @Test
    fun testCandidateScoringWithNumbersAndTerms() {
        val service = LegalRetrievalService(FakeRagDao())
        val item = TrainingExampleEntity(
            id = 138L,
            question = "What is the procedure under Section 138 Negotiable Instruments Act?",
            answer = "Section 138 governs dishonour of cheque for insufficient funds.",
            sourcePath = "Section 138, Negotiable Instruments Act, 1881",
            legalDomain = "Commercial Law",
            reasoningQuality = 1
        )

        val scoreExactNumber = service.scoreTrainingCandidate(
            item = item,
            query = "section 138 cheque bounce",
            terms = listOf("cheque", "bounce"),
            numbers = listOf("138")
        )

        assertTrue("Candidate with exact number and matching terms should have high score", scoreExactNumber >= 30)
    }

    @Test
    fun testRepealedDocumentsAreFilteredOut() = runTest {
        val activeDoc = DocumentEntity(
            sourcePath = "bns.pdf",
            content = "Section 103: Punishment for murder.",
            status = "in_force",
            jurisdiction = "central"
        ).apply { rowid = 1 }

        val repealedDoc = DocumentEntity(
            sourcePath = "ipc.pdf",
            content = "Section 302: Punishment for murder under IPC.",
            status = "repealed",
            jurisdiction = "central"
        ).apply { rowid = 2 }

        val fakeDao = FakeRagDao(sampleDocs = listOf(activeDoc, repealedDoc))
        val service = LegalRetrievalService(fakeDao)

        val result = service.retrieve("punishment for murder")
        assertTrue("Active document should be retrieved", result.documents.any { it.sourcePath == "bns.pdf" })
        assertFalse("Repealed document must be filtered out", result.documents.any { it.status == "repealed" })
    }

    @Test
    fun testJurisdictionBoostPrefersTargetState() = runTest {
        val centralDoc = DocumentEntity(
            sourcePath = "tp_act.pdf",
            content = "Transfer of Property Act provisions.",
            status = "in_force",
            jurisdiction = "central"
        ).apply { rowid = 1 }

        val maharashtraDoc = DocumentEntity(
            sourcePath = "mrca.pdf",
            content = "Maharashtra Rent Control Act provisions.",
            status = "in_force",
            jurisdiction = "Maharashtra"
        ).apply { rowid = 2 }

        val fakeDao = FakeRagDao(sampleDocs = listOf(centralDoc, maharashtraDoc))
        val service = LegalRetrievalService(fakeDao)

        val result = service.retrieve("rent control provisions", targetJurisdiction = "Maharashtra")
        assertEquals("Maharashtra document should be ranked first when Maharashtra is target jurisdiction",
            "Maharashtra", result.documents.firstOrNull()?.jurisdiction)
    }
}
