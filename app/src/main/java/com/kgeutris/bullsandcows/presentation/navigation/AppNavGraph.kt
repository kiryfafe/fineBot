@file:OptIn(ExperimentalMaterial3Api::class)

package com.kgeutris.bullsandcows.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kgeutris.bullsandcows.presentation.screens.AboutScreen
import com.kgeutris.bullsandcows.presentation.screens.AiGuessingScreen
import com.kgeutris.bullsandcows.presentation.screens.GameScreen
import com.kgeutris.bullsandcows.presentation.screens.HistoryScreen
import com.kgeutris.bullsandcows.presentation.screens.MainScreen
import com.kgeutris.bullsandcows.presentation.screens.MultiplayerScreen
import com.kgeutris.bullsandcows.presentation.screens.SettingsScreen

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Game : Screen("game")
    object AiGuessing : Screen("ai")
    object Multiplayer : Screen("multiplayer")
    object History : Screen("history")
    object Settings : Screen("settings")
    object About : Screen("about")
}

@Composable
fun AppNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        // 🏠 Главный экран
        composable(
            route = Screen.Main.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            MainScreen(
                onNavigateToGame = { navController.navigate(Screen.Game.route) },
                onNavigateToAi = { navController.navigate(Screen.AiGuessing.route) },
                onNavigateToMultiplayer = { navController.navigate(Screen.Multiplayer.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }

        // 🎮 Одиночная игра
        composable(
            route = Screen.Game.route,
            enterTransition = {
                slideInHorizontally(
                    animationSpec = tween(400),
                    initialOffsetX = { it }
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(300),
                    targetOffsetX = { -it }
                ) + fadeOut(animationSpec = tween(200))
            },
            popEnterTransition = {
                slideInHorizontally(
                    animationSpec = tween(400),
                    initialOffsetX = { -it }
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(300),
                    targetOffsetX = { it }
                ) + fadeOut(animationSpec = tween(200))
            }
        ) {
            GameScreen(onBack = { navController.popBackStack() })
        }

        // 🤖 AI угадывает
        composable(
            route = Screen.AiGuessing.route,
            enterTransition = { slideInHorizontally(tween(300)) + fadeIn(tween(200)) },
            exitTransition = { slideOutHorizontally(tween(200)) + fadeOut(tween(150)) }
        ) {
            AiGuessingScreen(onBack = { navController.popBackStack() })
        }

        // 👥 Мультиплеер
        composable(
            route = Screen.Multiplayer.route,
            enterTransition = { slideInHorizontally(tween(300)) + fadeIn(tween(200)) },
            exitTransition = { slideOutHorizontally(tween(200)) + fadeOut(tween(150)) }
        ) {
            MultiplayerScreen(onBack = { navController.popBackStack() })
        }

        // 📜 История
        composable(
            route = Screen.History.route,
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(200)) }
        ) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }

        // ⚙️ Настройки
        composable(
            route = Screen.Settings.route,
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(200)) }
        ) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        // 🎯 Об игре
        composable(
            route = Screen.About.route,
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(200)) }
        ) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}