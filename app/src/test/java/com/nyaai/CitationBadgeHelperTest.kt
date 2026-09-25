package com.nyaai

import com.nyaai.data.matter.ApplicableLaw
import com.nyaai.data.verification.CitationBadgeHelper
import com.nyaai.data.verification.CitationBadgeType
import org.junit.Assert.assertEquals
import org.junit.Test

class CitationBadgeHelperTest {

    @Test
    fun testVerifiedGoodLawPrecedentReturnsPassed() {
        val badge = CitationBadgeHelper.evaluatePrecedentBadge("Lalita Kumari v. Govt. of U.P., (2014) 2 SCC 1")
        assertEquals(CitationBadgeType.PASSED, badge.type)
        assertEquals("PASSED", badge.label)
    }

    @Test
    fun testSupersededPrecedentReturnsAnnotatedSupersededPrecedent() {
        val badge = CitationBadgeHelper.evaluatePrecedentBadge("Dr. Subhash Kashinath Mahajan v. State of Maharashtra, (2018) 6 SCC 454")
        assertEquals(CitationBadgeType.ANNOTATED_SUPERSEDED_PRECEDENT, badge.type)
        assertEquals("ANNOTATED_SUPERSEDED_PRECEDENT", badge.label)
    }

    @Test
    fun testFabricatedPrecedentReturnsRejectedUngrounded() {
        val badge = CitationBadgeHelper.evaluatePrecedentBadge("Fabricated Landlord Precedent v. Fake Respondent")
        assertEquals(CitationBadgeType.REJECTED_UNGROUNDED, badge.type)
        assertEquals("REJECTED_UNGROUNDED", badge.label)
    }

    @Test
    fun testInForceStatuteReturnsPassed() {
        val law = ApplicableLaw(
            act = "Bharatiya Nagarik Suraksha Sanhita, 2023",
            section = "Section 173",
            currentnessStatus = "in_force",
            asOf = "2026-09"
        )
        val badge = CitationBadgeHelper.evaluateStatuteBadge(law)
        assertEquals(CitationBadgeType.PASSED, badge.type)
        assertEquals("PASSED", badge.label)
    }

    @Test
    fun testRepealedStatuteReturnsAnnotatedRepealed() {
        val law = ApplicableLaw(
            act = "Code of Criminal Procedure, 1973",
            section = "Section 154",
            currentnessStatus = "repealed",
            asOf = "2024-07-01"
        )
        val badge = CitationBadgeHelper.evaluateStatuteBadge(law)
        assertEquals(CitationBadgeType.ANNOTATED_REPEALED, badge.type)
        assertEquals("ANNOTATED_REPEALED", badge.label)
    }

    @Test
    fun testUngroundedStatuteReturnsRejectedUngrounded() {
        val law = ApplicableLaw(
            act = "Unknown Statute",
            section = "Section 999",
            currentnessStatus = "ungrounded",
            asOf = "2026-09"
        )
        val badge = CitationBadgeHelper.evaluateStatuteBadge(law)
        assertEquals(CitationBadgeType.REJECTED_UNGROUNDED, badge.type)
        assertEquals("REJECTED_UNGROUNDED", badge.label)
    }
}
