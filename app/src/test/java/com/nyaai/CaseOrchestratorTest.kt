package com.nyaai

import com.nyaai.data.intake.CaseOrchestrator
import com.nyaai.data.intake.IntakeEngine
import com.nyaai.data.intake.OrchestratorStage
import com.nyaai.data.local.TrainingExampleEntity
import com.nyaai.data.matter.CaseStateManager
import com.nyaai.data.matter.Matter
import com.nyaai.data.procedure.ProcedureEngine
import com.nyaai.data.retrieval.LegalRetrievalService
import com.nyaai.data.verification.CitationVerifier
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CaseOrchestratorTest {

    private val fakeDao = FakeRagDao()
    private val caseStateManager = CaseStateManager(fakeDao)
    private val orchestrator = CaseOrchestrator(
        intakeEngine = IntakeEngine(),
        procedureEngine = ProcedureEngine(),
        retrievalService = LegalRetrievalService(fakeDao),
        citationVerifier = CitationVerifier(),
        caseStateManager = caseStateManager
    )

    @Test
    fun testScenarioQueryRoutesToProceduralGuidance() = runTest {
        val matter = Matter(title = "Lockout Issue", domain = "tenancy_deposit")
        caseStateManager.createMatter(matter)

        val response = orchestrator.processTurn(
            currentMatter = matter,
            userInput = "My landlord locked my flat and threw my belongings"
        )

        assertEquals(OrchestratorStage.PROCEDURAL_GUIDANCE, response.stage)
        assertTrue(response.isComplete)
        assertTrue(response.text.contains("SENIOR ADVOCATE LEGAL ADVISORY"))
        assertEquals("guidance_delivered", response.updatedMatter.proceduralStage)
    }

    @Test
    fun testIntakeAsksMissingJurisdictionQuestion() = runTest {
        val matter = Matter(title = "Deposit Dispute", domain = "")
        caseStateManager.createMatter(matter)

        val response = orchestrator.processTurn(
            currentMatter = matter,
            userInput = "My landlord is withholding my security deposit"
        )

        assertEquals(OrchestratorStage.INTAKE, response.stage)
        assertFalse(response.isComplete)
        assertTrue(response.text.contains("state or city", ignoreCase = true))
    }

    @Test
    fun testGroundedStatutoryAnswerDeliveredWhenMatchExists() = runTest {
        val training = listOf(
            TrainingExampleEntity(
                id = 482L,
                question = "Can I apply for Anticipatory Bail under Section 482 BNSS?",
                answer = "Yes, Anticipatory Bail can be sought under Section 482 BNSS.",
                sourcePath = "Section 482, BNSS, 2023",
                legalDomain = "Criminal Procedure (BNSS)",
                reasoningQuality = 1
            )
        )
        val daoWithTraining = FakeRagDao(sampleTraining = training)
        val orchestratorWithTraining = CaseOrchestrator(
            intakeEngine = IntakeEngine(),
            procedureEngine = ProcedureEngine(),
            retrievalService = LegalRetrievalService(daoWithTraining),
            citationVerifier = CitationVerifier()
        )

        val matter = Matter(title = "Bail Question", domain = "")
        val response = orchestratorWithTraining.processTurn(
            currentMatter = matter,
            userInput = "Section 482 BNSS Bail procedure"
        )

        assertEquals(OrchestratorStage.GROUNDED_ANSWER, response.stage)
        assertTrue(response.text.contains("VERIFIED STATUTORY LEGAL PROVISION"))
        assertTrue(response.text.contains("482"))
    }
}
