package com.nyaai.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    LaunchedEffect(key1 = true) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            onNavigateToHome()
        } else {
            onNavigateToOnboarding()
        }
    }
}
