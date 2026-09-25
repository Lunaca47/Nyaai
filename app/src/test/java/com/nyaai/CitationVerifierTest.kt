package com.nyaai

import com.nyaai.data.local.DocumentEntity
import com.nyaai.data.verification.CitationVerifier
import org.junit.Assert.*
import org.junit.Test

class CitationVerifierTest {

    private val verifier = CitationVerifier()

    @Test
    fun testGroundedCitationPasses() {
        val doc = DocumentEntity(
            sourcePath = "bnss.pdf",
            content = "Section 173: Information in cognizable cases and Zero FIR registration.",
            status = "in_force"
        ).apply { rowid = 1 }

        val response = "Under Section 173 BNSS, the police are required to register your complaint."
        val result = verifier.verify(response, retrievedDocuments = listOf(doc))

        assertTrue("Citation should be marked grounded", result.isGrounded)
        assertTrue("Section 173 should be in grounded citations", result.groundedCitations.contains("Section 173"))
        assertTrue("No ungrounded citations should be present", result.ungroundedCitations.isEmpty())
    }

    @Test
    fun testRepealedStatuteTriggersWarning() {
        val response = "You can file a complaint under Section 420 of the Indian Penal Code."
        val result = verifier.verify(response)

        assertFalse("Repealed citation list should not be empty", result.repealedCitations.isEmpty())
        assertTrue("Warnings should mention repeal of IPC", result.warnings.any { it.contains("repealed") })
    }

    @Test
    fun testRepealedStatuteWithClarificationPassesWithoutWarning() {
        val response = "Under Section 318 BNS 2023 (which replaces the repealed IPC 420), cheating is an offense."
        val result = verifier.verify(response)

        assertTrue("When response explicitly references BNS, no repeal warning should trigger", result.warnings.isEmpty())
    }
}
