package com.nyaai

import com.nyaai.ui.state.AppLanguage
import com.nyaai.ui.strings.stringsFor
import org.junit.Assert.*
import org.junit.Test

class AppStringsTest {

    @Test
    fun testAllLanguagesHaveNonEmptyStrings() {
        for (lang in AppLanguage.values()) {
            val s = stringsFor(lang)
            assertNotNull("Strings for  should not be null", s)
            assertTrue("whatCanIHelpWith in  must not be blank", s.whatCanIHelpWith.isNotBlank())
            assertTrue("chatInputHint in  must not be blank", s.chatInputHint.isNotBlank())
            assertTrue("settingsTitle in  must not be blank", s.settingsTitle.isNotBlank())
            assertTrue("aboutTitle in  must not be blank", s.aboutTitle.isNotBlank())
            assertTrue("legalSosTitle in  must not be blank", s.legalSosTitle.isNotBlank())
            assertTrue("privacyPolicy in  must not be blank", s.privacyPolicy.isNotBlank())
            assertTrue("bookmarksTitle in $lang must not be blank", s.bookmarksTitle.isNotBlank())
            assertTrue("scanningDocument in $lang must not be blank", s.scanningDocument.isNotBlank())
            // Phase 3 & 4 Matter Workspace strings
            assertTrue("myMattersTitle in $lang must not be blank", s.myMattersTitle.isNotBlank())
            assertTrue("searchMattersHint in $lang must not be blank", s.searchMattersHint.isNotBlank())
            assertTrue("whatHappenedAction in $lang must not be blank", s.whatHappenedAction.isNotBlank())
            assertTrue("startCaseIntake in $lang must not be blank", s.startCaseIntake.isNotBlank())
            assertTrue("noMattersYet in $lang must not be blank", s.noMattersYet.isNotBlank())
            assertTrue("noMattersDesc in $lang must not be blank", s.noMattersDesc.isNotBlank())
            assertTrue("syncedBadge in $lang must not be blank", s.syncedBadge.isNotBlank())
            assertTrue("deleteMatterAction in $lang must not be blank", s.deleteMatterAction.isNotBlank())
            assertTrue("tabOverview in $lang must not be blank", s.tabOverview.isNotBlank())
            assertTrue("tabTimeline in $lang must not be blank", s.tabTimeline.isNotBlank())
            assertTrue("tabActionPlan in $lang must not be blank", s.tabActionPlan.isNotBlank())
            assertTrue("tabEvidence in $lang must not be blank", s.tabEvidence.isNotBlank())
            assertTrue("tabCitations in $lang must not be blank", s.tabCitations.isNotBlank())
            assertTrue("tabQuestions in $lang must not be blank", s.tabQuestions.isNotBlank())
            // Verification Badge strings
            assertTrue("badgeSeniorAdvocate in $lang must not be blank", s.badgeSeniorAdvocate.isNotBlank())
            assertTrue("badgeVerifiedIndianLaw in $lang must not be blank", s.badgeVerifiedIndianLaw.isNotBlank())
            assertTrue("badgeCodifiedExcerpts in $lang must not be blank", s.badgeCodifiedExcerpts.isNotBlank())
            assertTrue("badgeVerifiedStatutes in $lang must not be blank", s.badgeVerifiedStatutes.isNotBlank())
            assertTrue("badgeStatutoryGuide in $lang must not be blank", s.badgeStatutoryGuide.isNotBlank())
            assertTrue("badgePreliminaryGuidance in $lang must not be blank", s.badgePreliminaryGuidance.isNotBlank())
        }
    }

    @Test
    fun testDistinctDisplayNames() {
        val names = AppLanguage.values().map { it.displayName }
        assertEquals("Each language should have a distinct displayName", names.toSet().size, names.size)
    }
}
