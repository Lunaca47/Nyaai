package com.nyaai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nyaai.data.local.RagDao
import com.nyaai.ui.screens.*

import androidx.compose.runtime.CompositionLocalProvider
import com.nyaai.ui.state.LocalLogOutAction
import com.nyaai.ui.state.LocalOpenLoginAction

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

    val openLogin: () -> Unit = {
        navController.navigate(AppRoutes.LOGIN)
    }

    val logOut: () -> Unit = {
        navController.navigate(AppRoutes.LOGIN) {
            popUpTo(AppRoutes.HOME) { inclusive = true }
        }
    }

    CompositionLocalProvider(
        LocalOpenLoginAction provides openLogin,
        LocalLogOutAction provides logOut
    ) {
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
                    onBack = { 
                        if (!navController.popBackStack()) {
                            navController.navigate(AppRoutes.HOME) {
                                popUpTo(AppRoutes.LOGIN) { inclusive = true }
                            }
                        }
                    },
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
}
