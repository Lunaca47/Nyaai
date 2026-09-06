package com.nyaai.ui.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = true) {
        val auth = FirebaseAuth.getInstance()
        val prefs = context.getSharedPreferences("nyaai_preferences", Context.MODE_PRIVATE)
        val isGuest = prefs.getBoolean("guest_mode", false)
        if (auth.currentUser != null || isGuest) {
            onNavigateToHome()
        } else {
            onNavigateToOnboarding()
        }
    }
}

