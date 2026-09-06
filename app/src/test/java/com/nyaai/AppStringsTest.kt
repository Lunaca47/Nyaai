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
            assertTrue("bookmarksTitle in  must not be blank", s.bookmarksTitle.isNotBlank())
            assertTrue("scanningDocument in  must not be blank", s.scanningDocument.isNotBlank())
        }
    }

    @Test
    fun testDistinctDisplayNames() {
        val names = AppLanguage.values().map { it.displayName }
        assertEquals("Each language should have a distinct displayName", names.toSet().size, names.size)
    }
}
