package com.nyaai.ui.state

import androidx.compose.runtime.compositionLocalOf
import com.nyaai.ui.strings.AppStrings
import com.nyaai.ui.strings.englishStrings
import com.nyaai.data.local.RagDao

// ── Theme ───────────────────────────────────────────────────────────────────
enum class AppTheme { LIGHT, SYSTEM, DARK }

// ── Language ─────────────────────────────────────────────────────────────────
enum class AppLanguage(val displayName: String) {
    ENGLISH("English"),
    HINDI("हिंदी (Hindi)"),
    BENGALI("বাংলা (Bengali)"),
    TELUGU("తెలుగు (Telugu)"),
    TAMIL("தமிழ் (Tamil)")
}

// ── CompositionLocals ────────────────────────────────────────────────────────
val LocalAppTheme             = compositionLocalOf { AppTheme.SYSTEM }
val LocalAppLanguage          = compositionLocalOf { AppLanguage.ENGLISH }
val LocalAiResponseLanguage   = compositionLocalOf { AppLanguage.ENGLISH }
val LocalThemeUpdater         = compositionLocalOf<(AppTheme) -> Unit> { {} }
val LocalLanguageUpdater      = compositionLocalOf<(AppLanguage) -> Unit> { {} }
val LocalAiLanguageUpdater    = compositionLocalOf<(AppLanguage) -> Unit> { {} }
val LocalStrings              = compositionLocalOf<AppStrings> { englishStrings }
val LocalAiService            = compositionLocalOf<com.nyaai.data.local.AiService?> { null }
val LocalRagDao               = compositionLocalOf<RagDao?> { null }
val LocalNotificationEnabled  = compositionLocalOf { true }
val LocalNotificationUpdater  = compositionLocalOf<(Boolean) -> Unit> { {} }
val LocalAuthState            = compositionLocalOf { true }
val LocalAuthUpdater           = compositionLocalOf<(Boolean) -> Unit> { {} }
val LocalIsGuest              = compositionLocalOf { false }
val LocalLogOutAction         = compositionLocalOf<() -> Unit> { {} }
val LocalOpenLoginAction      = compositionLocalOf<() -> Unit> { {} }
