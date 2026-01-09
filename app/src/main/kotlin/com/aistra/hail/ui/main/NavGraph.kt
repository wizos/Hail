package com.aistra.hail.ui.main

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.about.AboutScreen
import com.aistra.hail.ui.apps.AppsScreen
import com.aistra.hail.ui.auth.AuthScreen
import com.aistra.hail.ui.home.HomeScreen
import com.aistra.hail.ui.settings.SettingsScreen

object AppDestinations {
    const val AUTH = "auth"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val APPS = "apps"
    const val ABOUT = "about"
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (HailData.biometricLogin) AppDestinations.AUTH else AppDestinations.HOME

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppDestinations.AUTH) {
            AuthScreen(onAuthenticated = {
                navController.navigate(AppDestinations.HOME) {
                    popUpTo(AppDestinations.AUTH) { inclusive = true }
                }
            })
        }
        composable(AppDestinations.HOME) {
            HomeScreen(navController = navController)
        }
        composable(AppDestinations.SETTINGS) {
            SettingsScreen(navController = navController)
        }
        composable(AppDestinations.APPS) {
            AppsScreen()
        }
        composable(AppDestinations.ABOUT) {
            AboutScreen()
        }
    }
}
