package com.nyaai

import com.nyaai.data.local.AiService
import com.nyaai.data.local.DocumentEntity
import com.nyaai.data.local.TrainingExampleEntity
import com.nyaai.data.model.ModelClient
import com.nyaai.data.verification.CitationVerifier
import com.nyaai.data.verification.GateAction
import com.nyaai.ui.state.AppLanguage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CitationVerifierHardGateTest {

    private lateinit var citationVerifier: CitationVerifier

    @Before
    fun setUp() {
        citationVerifier = CitationVerifier()
    }

    @Test
    fun testCleanPassForGroundedStatutes() {
        val response = "Under Bharatiya Nyaya Sanhita Section 329, criminal trespass into private premises is an offense."
        val doc = DocumentEntity(sourcePath = "bns.pdf", content = "Section 329. Criminal trespass and punishment.")

        val gated = citationVerifier.enforceHardGate(
            responseText = response,
            retrievedDocuments = listOf(doc)
        )

        assertEquals(GateAction.PASSED, gated.action)
        assertFalse(gated.shouldFallback)
        assertEquals(response, gated.gatedText)
    }

    @Test
    fun testRepealedStatuteAutoAnnotationWithReplacements() {
        val legacyResponse = "The accused should be prosecuted under Section 420 IPC and Section 154 CrPC for the fraud committed."
        val doc = DocumentEntity(sourcePath = "penal.pdf", content = "Provisions for Section 420 IPC fraud and cheating, and Section 154 CrPC information in cognizable cases.")

        val gated = citationVerifier.enforceHardGate(
            responseText = legacyResponse,
            retrievedDocuments = listOf(doc)
        )

        assertEquals(GateAction.ANNOTATED_REPEALED, gated.action)
        assertFalse(gated.shouldFallback)
        assertTrue(gated.gatedText.contains("STATUTORY CURRENTNESS & REPEAL NOTICE"))
        assertTrue(gated.gatedText.contains("Section 318(4)"))
        assertTrue(gated.gatedText.contains("Section 173"))
    }

    @Test
    fun testUngroundedHallucinationRejectedTriggersFallback() {
        val hallucinatedResponse = "According to Section 9999 and Section 8888 of the Fantasy Legal Code, you have the right to claim ₹10 crore."
        val realDoc = DocumentEntity(sourcePath = "bns.pdf", content = "Section 318. Cheating definition and punishment.")

        val gated = citationVerifier.enforceHardGate(
            responseText = hallucinatedResponse,
            retrievedDocuments = listOf(realDoc)
        )

        assertEquals(GateAction.REJECTED_UNGROUNDED, gated.action)
        assertTrue("Severe hallucination must trigger fallback", gated.shouldFallback)
        assertEquals(2, gated.result.ungroundedCitations.size)
    }

    @Test
    fun testAiServiceFallsBackWhenHardGateRejectsHallucination() = runTest {
        // Model client returning completely fabricated citations
        val hallucinatingClient = object : ModelClient {
            override suspend fun generateContent(prompt: String): Result<String> {
                return Result.success("Under Section 7777 and Section 8888, the court will dismiss the entire case.")
            }
        }

        val fakeDao = FakeRagDao(
            sampleDocs = listOf(
                DocumentEntity(sourcePath = "bns.pdf", content = "Section 318. Cheating punishment.")
            ),
            sampleTraining = listOf(
                TrainingExampleEntity(
                    question = "What is the legal provision for cheating?",
                    answer = "Under BNS Section 318(4), cheating is punishable with up to 7 years imprisonment.",
                    sourcePath = "BNS 2023",
                    legalDomain = "Criminal Law",
                    reasoningQuality = 1
                )
            )
        )

        val service = AiService(fakeDao, apiKey = "test_active_key", modelClient = hallucinatingClient)
        val (answer, _) = service.generateAnswer("What is the legal provision for cheating?", AppLanguage.ENGLISH)

        // The answer must NOT contain the hallucinated sections 7777 or 8888; it should have fallen back to verified offline answer
        assertFalse("Hallucinated Section 7777 must not be returned to user", answer.contains("7777"))
        assertFalse("Hallucinated Section 8888 must not be returned to user", answer.contains("8888"))
        assertTrue("Must contain grounded statutory answer", answer.contains("318") || answer.contains("Cheating"))
    }

    @Test
    fun testMixedGroundedAndUngroundedCitationsAnnotatedWithoutCleanPass() {
        // Query output contains one grounded section (Section 329) and one hallucinated section (Section 9999)
        val mixedResponse = "Under Section 329 criminal trespass is penalized, and under Section 9999 the tenant is awarded punitive damages."
        val realDoc = DocumentEntity(sourcePath = "bns.pdf", content = "Section 329. Criminal trespass into private premises.")

        val gated = citationVerifier.enforceHardGate(
            responseText = mixedResponse,
            retrievedDocuments = listOf(realDoc)
        )

        // Loophole closed: Must NOT be GateAction.PASSED
        assertEquals(GateAction.ANNOTATED_UNGROUNDED, gated.action)
        assertFalse("Mixed responses with partial grounding do not trigger offline fallback but are annotated", gated.shouldFallback)
        assertFalse("Result must NOT report isGrounded=true when ungrounded citations exist", gated.result.isGrounded)
        assertEquals(listOf("Section 329"), gated.result.groundedCitations)
        assertEquals(listOf("Section 9999"), gated.result.ungroundedCitations)
        assertTrue("Must contain prominent ungrounded warning", gated.gatedText.contains("UNGROUNDED CITATION WARNING"))
        assertTrue("Must specifically flag Section 9999", gated.gatedText.contains("Section 9999"))
    }

    @Test
    fun testModelTenancyActStatutoryApplicabilityCaveatAppended() {
        val mtaResponse = "Under the Model Tenancy Act, 2021 Section 11, the landlord cannot demand more than 2 months rent as security deposit."
        val realDoc = DocumentEntity(sourcePath = "mta.pdf", content = "Section 11. Security deposit cap and refund upon taking vacant possession.")

        val gated = citationVerifier.enforceHardGate(
            responseText = mtaResponse,
            retrievedDocuments = listOf(realDoc)
        )

        assertEquals(GateAction.ANNOTATED_MODEL_LAW, gated.action)
        assertFalse(gated.shouldFallback)
        assertTrue(gated.result.isGrounded)
        assertTrue("Must contain statutory applicability caveat", gated.gatedText.contains("STATUTORY APPLICABILITY CAVEAT (MODEL LAW)"))
        assertTrue("Must explain State legislative adoption requirement", gated.gatedText.contains("Entry 18"))
        assertTrue("Must mention State rent control legislation", gated.gatedText.contains("Delhi Rent Control Act 1958"))
    }

    @Test
    fun testModelTenancyActAlwaysAnnotatedWithCaveat() {
        val rawResponse = "Under Section 11 of the Model Tenancy Act, 2021, the maximum security deposit is capped at two months rent."
        val example = TrainingExampleEntity(
            question = "What is the security deposit limit?",
            answer = "Under Model Tenancy Act Section 11, it is 2 months rent.",
            sourcePath = "MTA 2021",
            legalDomain = "Tenancy",
            reasoningQuality = 1
        )
        val gated = citationVerifier.enforceHardGate(
            responseText = rawResponse,
            retrievedExamples = listOf(example)
        )

        assertEquals(GateAction.ANNOTATED_MODEL_LAW, gated.action)
        assertFalse(gated.shouldFallback)
        assertTrue("Must contain statutory applicability caveat", gated.gatedText.contains("STATUTORY APPLICABILITY CAVEAT (MODEL LAW)"))
        assertTrue("Must reference Entry 18 of State List", gated.gatedText.contains("Entry 18"))
    }

    @Test
    fun testFabricatedModelTenancyActRejectedAsUngrounded() {
        val hallucinatedMta = "Under Section 99 of the Model Tenancy Act, 2021, the tenant can withhold rent indefinitely."
        val gated = citationVerifier.enforceHardGate(
            responseText = hallucinatedMta,
            retrievedDocuments = emptyList()
        )

        assertEquals("Invented MTA citation with zero grounded sources must be rejected as ungrounded", GateAction.REJECTED_UNGROUNDED, gated.action)
        assertTrue("Severe hallucination must trigger fallback", gated.shouldFallback)
    }

    @Test
    fun testFabricatedRepealedStatuteRejectedAsUngrounded() {
        val hallucinatedRepealed = "File a petition under Section 9999 of the Indian Penal Code."
        val gated = citationVerifier.enforceHardGate(
            responseText = hallucinatedRepealed,
            retrievedDocuments = emptyList()
        )

        assertEquals("Invented repealed citation with zero grounded sources must be rejected as ungrounded", GateAction.REJECTED_UNGROUNDED, gated.action)
        assertTrue("Severe hallucination must trigger fallback", gated.shouldFallback)
    }

    @Test
    fun testPositiveProofGroundedRepealedStatuteAnnotatedRepealed() {
        val legacyResponse = "The accused should be prosecuted under Section 302 IPC for the offense of murder."
        val doc = DocumentEntity(sourcePath = "ipc.pdf", content = "Section 302 IPC / BNS Section 103(1): Punishment for murder with death or imprisonment for life.")

        val gated = citationVerifier.enforceHardGate(
            responseText = legacyResponse,
            retrievedDocuments = listOf(doc)
        )

        assertEquals(GateAction.ANNOTATED_REPEALED, gated.action)
        assertFalse(gated.shouldFallback)
        assertTrue(gated.result.isGrounded)
        assertFalse(gated.result.repealedCitations.isEmpty())
        assertTrue("Must contain statutory currentness notice", gated.gatedText.contains("STATUTORY CURRENTNESS & REPEAL NOTICE"))
        assertTrue("Must suggest BNS Section 103(1)", gated.gatedText.contains("Section 103(1)"))
    }

    @Test
    fun testGroundedNonMtaWithUngroundedMtaAnnotatedUngroundedNotModelLaw() {
        val mixedResponse = "Under Section 318 BNS cheating is penalized, while Section 99 of the Model Tenancy Act governs eviction."
        val doc = DocumentEntity(sourcePath = "bns.pdf", content = "Section 318 BNS: Cheating and dishonestly inducing delivery of property.")

        val gated = citationVerifier.enforceHardGate(
            responseText = mixedResponse,
            retrievedDocuments = listOf(doc)
        )

        assertEquals("Grounded non-MTA with ungrounded MTA must be ANNOTATED_UNGROUNDED", GateAction.ANNOTATED_UNGROUNDED, gated.action)
        assertNotEquals("Must not be ANNOTATED_MODEL_LAW", GateAction.ANNOTATED_MODEL_LAW, gated.action)
        assertFalse(gated.shouldFallback)
    }

    @Test
    fun testIncidentalMtaMentionWithGroundedNonMtaCitationPassedNotModelLaw() {
        val response = "Unlike the Model Tenancy Act which applies to leases, under Section 318 BNS cheating is penalized."
        val doc = DocumentEntity(sourcePath = "bns.pdf", content = "Section 318 BNS: Cheating and dishonestly inducing delivery of property.")

        val gated = citationVerifier.enforceHardGate(
            responseText = response,
            retrievedDocuments = listOf(doc)
        )

        assertEquals("Incidental MTA mention with grounded non-MTA citation must be PASSED", GateAction.PASSED, gated.action)
        assertNotEquals("Must not be ANNOTATED_MODEL_LAW", GateAction.ANNOTATED_MODEL_LAW, gated.action)
    }

    @Test
    fun testFabricatedCasePrecedentRejectedUngrounded() {
        val fabricatedResponse = "As held in Ramesh Sharma v. State of Narnia, (2024) 99 SCC 999, all loan defaults are non-actionable."
        val realDoc = DocumentEntity(sourcePath = "bns.pdf", content = "Section 318 BNS: Cheating definition and punishment.")

        val gated = citationVerifier.enforceHardGate(
            responseText = fabricatedResponse,
            retrievedDocuments = listOf(realDoc)
        )

        assertEquals("Fabricated case law citation must be REJECTED_UNGROUNDED", GateAction.REJECTED_UNGROUNDED, gated.action)
        assertTrue("Fabricated case must trigger fallback", gated.shouldFallback)
    }

    @Test
    fun testSupersededPrecedentSubhashMahajanAnnotated() {
        val mahajanResponse = "According to Dr. Subhash Kashinath Mahajan v. State of Maharashtra, (2018) 6 SCC 454, no arrest can be made under SC/ST Act without prior approval."
        val doc = DocumentEntity(
            sourcePath = "case_law_corpus.json",
            content = "Dr. Subhash Kashinath Mahajan v. State of Maharashtra. Citation: (2018) 6 SCC 454. Precedent Status: SUPERSEDED_BY_STATUTE. Superseded by Parliament through Scheduled Castes and the Scheduled Tribes (Prevention of Atrocities) Amendment Act, 2018 inserting Section 18A."
        )

        val gated = citationVerifier.enforceHardGate(
            responseText = mahajanResponse,
            retrievedDocuments = listOf(doc)
        )

        assertEquals("Grounded superseded precedent must produce ANNOTATED_SUPERSEDED_PRECEDENT", GateAction.ANNOTATED_SUPERSEDED_PRECEDENT, gated.action)
        assertFalse(gated.shouldFallback)
        assertTrue("Must contain superseded notice", gated.gatedText.contains("SUPERSEDED PRECEDENT"))
        assertTrue("Must mention Section 18A", gated.gatedText.contains("Section 18A"))
    }

    @Test
    fun testGroundedGoodLawPrecedentPassed() {
        val goodLawResponse = "In Lalita Kumari v. Govt. of U.P., (2014) 2 SCC 1, the Supreme Court mandated FIR registration."
        val doc = DocumentEntity(
            sourcePath = "case_law_corpus.json",
            content = "Lalita Kumari v. Govt. of U.P. Citation: (2014) 2 SCC 1. Precedent Status: GOOD_LAW. Registration of FIR is mandatory."
        )

        val gated = citationVerifier.enforceHardGate(
            responseText = goodLawResponse,
            retrievedDocuments = listOf(doc)
        )

        assertEquals("Grounded good law precedent must pass", GateAction.PASSED, gated.action)
        assertFalse(gated.shouldFallback)
        assertEquals(goodLawResponse, gated.gatedText)
    }
}


