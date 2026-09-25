package com.nyaai

import com.nyaai.data.local.DocumentEntity
import com.nyaai.data.local.TrainingExampleEntity
import com.nyaai.data.research.ResearchPlanner
import com.nyaai.data.research.ResearchSubIssueType
import com.nyaai.data.retrieval.LegalRetrievalService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ResearchPlannerTest {

    private lateinit var retrievalService: LegalRetrievalService
    private lateinit var researchPlanner: ResearchPlanner

    @Before
    fun setUp() {
        val fakeDao = FakeRagDao(
            sampleDocs = listOf(
                DocumentEntity(sourcePath = "bns.pdf", content = "Section 329. Criminal trespass into house property."),
                DocumentEntity(sourcePath = "cpa.pdf", content = "Section 35. Filing of complaint with District Commission.")
            ),
            sampleTraining = listOf(
                TrainingExampleEntity(
                    question = "What is the procedure for unlawful eviction?",
                    answer = "Under Section 6 Specific Relief Act and Section 329 BNS, unlawful eviction is actionable.",
                    sourcePath = "Specific Relief Act",
                    legalDomain = "Property Law",
                    reasoningQuality = 1
                )
            )
        )
        retrievalService = LegalRetrievalService(fakeDao)
        researchPlanner = ResearchPlanner(retrievalService)
    }

    @Test
    fun testQueryDecompositionTenancy() {
        val subIssues = researchPlanner.decomposeQuery("Landlord locked out flat and withheld deposit", "tenancy_deposit")

        assertEquals(5, subIssues.size)
        val types = subIssues.map { it.issueType }
        assertTrue(types.contains(ResearchSubIssueType.SUBSTANTIVE_ELEMENTS))
        assertTrue(types.contains(ResearchSubIssueType.PROCEDURAL_FORUM))
        assertTrue(types.contains(ResearchSubIssueType.LIMITATION_PERIOD))
        assertTrue(types.contains(ResearchSubIssueType.PRE_LITIGATION_DEMAND))
        assertTrue(types.contains(ResearchSubIssueType.INTERIM_RELIEF))

        val substantive = subIssues.first { it.issueType == ResearchSubIssueType.SUBSTANTIVE_ELEMENTS }
        assertTrue(substantive.targetStatutes.any { it.contains("Bharatiya Nyaya Sanhita") })
        assertTrue("Substantive elements must include supporting precedent", substantive.supportingPrecedents.any { it.contains("Bhajan Lal") })
    }

    @Test
    fun testQueryDecompositionChequeBounce() {
        val subIssues = researchPlanner.decomposeQuery("Cheque bounced with return memo funds insufficient", "cheque_bounce")

        assertEquals(5, subIssues.size)
        val substantive = subIssues.first { it.issueType == ResearchSubIssueType.SUBSTANTIVE_ELEMENTS }
        assertTrue("Cheque bounce must include Meters and Instruments precedent", substantive.supportingPrecedents.any { it.contains("Meters and Instruments") })

        val interim = subIssues.first { it.issueType == ResearchSubIssueType.INTERIM_RELIEF }
        assertTrue(interim.subQuery.contains("143A"))
        assertTrue(interim.targetStatutes.contains("Negotiable Instruments Act 1881"))
    }

    @Test
    fun testQueryDecompositionConsumer() {
        val subIssues = researchPlanner.decomposeQuery("Defective washing machine Amazon refusing replacement", "consumer_complaint")

        assertEquals(5, subIssues.size)
        val substantive = subIssues.first { it.issueType == ResearchSubIssueType.SUBSTANTIVE_ELEMENTS }
        assertTrue("Consumer dispute must include Arjun Khotkar electronic evidence precedent", substantive.supportingPrecedents.any { it.contains("Arjun Panditrao Khotkar") })

        val forum = subIssues.first { it.issueType == ResearchSubIssueType.PROCEDURAL_FORUM }
        assertTrue(forum.subQuery.contains("Section 35"))
        assertTrue(forum.targetStatutes.contains("Consumer Protection Act 2019"))
    }

    @Test
    fun testExecuteResearchSynthesizesAssessmentMemo() = runTest {
        val report = researchPlanner.executeResearch(
            matterTitle = "Illegal Lockout in Indiranagar",
            query = "Landlord replaced door locks without notice",
            domain = "tenancy_deposit",
            jurisdictionState = "Karnataka"
        )

        assertNotNull(report)
        assertEquals("Illegal Lockout in Indiranagar", report.matterTitle)
        assertEquals("Karnataka", report.jurisdiction)
        assertEquals(5, report.findings.size)
        assertTrue("Synthesized memo must contain header", report.synthesizedAssessmentMemo.contains("SENIOR ADVOCATE MULTI-HOP LEGAL ASSESSMENT MEMO"))
        assertTrue("Synthesized memo must contain analysis sections", report.synthesizedAssessmentMemo.contains("MULTI-DIMENSIONAL LEGAL ANALYSIS"))
        assertTrue("Must include verified statutes", report.verifiedStatutes.isNotEmpty())
        assertTrue("Must weave supporting precedents into assessment report", report.citedPrecedents.isNotEmpty())
        assertTrue("Synthesized memo must contain Supporting Precedents section", report.synthesizedAssessmentMemo.contains("Supporting Precedents"))
        assertTrue("Synthesized memo must explicitly cite Bhajan Lal", report.synthesizedAssessmentMemo.contains("Bhajan Lal"))
    }
}
