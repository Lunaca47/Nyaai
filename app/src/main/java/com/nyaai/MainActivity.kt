package com.nyaai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.nyaai.data.local.AiService

import com.nyaai.theme.NyaaiTheme
import com.nyaai.ui.navigation.AppNavigation
import com.nyaai.ui.state.*
import com.nyaai.ui.strings.stringsFor
import androidx.room.Room
import androidx.lifecycle.lifecycleScope
import com.nyaai.data.local.RagDatabase
import com.nyaai.data.local.PdfExtractorService
import com.nyaai.data.local.MIGRATION_7_8

import android.content.Context
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        FirebaseApp.initializeApp(this)
        val auth = FirebaseAuth.getInstance()
        
        val apiKey = BuildConfig.GEMINI_API_KEY

        val prefs = applicationContext.getSharedPreferences("nyaai_preferences", Context.MODE_PRIVATE)

        val db = Room.databaseBuilder(
            applicationContext,
            RagDatabase::class.java, "nyaai_v9.db"
        )
        .createFromAsset("database/nyaai_preloaded.db")
        .addMigrations(MIGRATION_7_8)
        .fallbackToDestructiveMigration()
        .build()

        val pdfExtractor = PdfExtractorService(applicationContext, db.ragDao())
        val aiService = AiService(
            db.ragDao(), 
            apiKey,
            customApiKeyProvider = { prefs.getString("custom_gemini_api_key", null) }
        )
        lifecycleScope.launch {
            pdfExtractor.initializeDatabaseFromAssets()
        }

        val initialThemeName = prefs.getString("pref_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        val initialTheme = try { AppTheme.valueOf(initialThemeName) } catch (_: Exception) { AppTheme.SYSTEM }

        val initialLangName = prefs.getString("pref_lang", AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name
        val initialLang = try { AppLanguage.valueOf(initialLangName) } catch (_: Exception) { AppLanguage.ENGLISH }

        val initialAiLangName = prefs.getString("pref_ai_lang", AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name
        val initialAiLang = try { AppLanguage.valueOf(initialAiLangName) } catch (_: Exception) { AppLanguage.ENGLISH }

        val initialNotifs = prefs.getBoolean("pref_notifs", true)

        setContent {
            var currentTheme    by remember { mutableStateOf(initialTheme) }
            var currentLanguage by remember { mutableStateOf(initialLang) }
            var aiLanguage      by remember { mutableStateOf(initialAiLang) }
            var notificationsEnabled by remember { mutableStateOf(initialNotifs) }
            var isGuest by remember { mutableStateOf(prefs.getBoolean("guest_mode", false)) }
            var isLoggedIn by remember { mutableStateOf(auth.currentUser != null || isGuest) }

            DisposableEffect(auth) {
                val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    val currentlyGuest = prefs.getBoolean("guest_mode", false)
                    isGuest = currentlyGuest
                    isLoggedIn = firebaseAuth.currentUser != null || currentlyGuest
                }
                auth.addAuthStateListener(listener)
                onDispose { auth.removeAuthStateListener(listener) }
            }

            val systemDark = isSystemInDarkTheme()
            val isDark = when (currentTheme) {
                AppTheme.LIGHT  -> false
                AppTheme.DARK   -> true
                AppTheme.SYSTEM -> systemDark
            }

            CompositionLocalProvider(
                LocalAppTheme           provides currentTheme,
                LocalAppLanguage        provides currentLanguage,
                LocalAiResponseLanguage provides aiLanguage,
                LocalThemeUpdater       provides { 
                    currentTheme = it
                    prefs.edit().putString("pref_theme", it.name).apply()
                },
                LocalLanguageUpdater    provides { 
                    currentLanguage = it
                    prefs.edit().putString("pref_lang", it.name).apply()
                },
                LocalAiLanguageUpdater  provides { 
                    aiLanguage = it
                    prefs.edit().putString("pref_ai_lang", it.name).apply()
                },
                LocalStrings            provides stringsFor(currentLanguage),
                LocalNotificationEnabled provides notificationsEnabled,
                LocalNotificationUpdater provides { 
                    notificationsEnabled = it
                    prefs.edit().putBoolean("pref_notifs", it).apply()
                },
                LocalAuthState          provides (auth.currentUser != null),
                LocalIsGuest            provides isGuest,
                LocalAuthUpdater        provides { 
                    isLoggedIn = it
                    isGuest = prefs.getBoolean("guest_mode", false)
                },
                LocalAiService          provides aiService,
                LocalRagDao             provides db.ragDao()
            ) {
                NyaaiTheme(darkTheme = isDark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color    = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(db.ragDao())
                    }
                }
            }
        }
    }
}
