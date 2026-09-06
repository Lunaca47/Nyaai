package com.nyaai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nyaai.data.local.RagDao
import com.nyaai.ui.screens.*

object AppRoutes {
    const val SPLASH   = "splash"
    const val WELCOME  = "welcome"
    const val LOGIN    = "login"
    const val HOME     = "home"
    const val ABOUT    = "about"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(ragDao: RagDao? = null) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AppRoutes.SPLASH) {

        composable(AppRoutes.SPLASH) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(AppRoutes.WELCOME) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.WELCOME) {
            WelcomeScreen(
                onNext = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.LOGIN) {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.HOME) {
            MainScreen(navController = navController, ragDao = ragDao)
        }
    }
}
