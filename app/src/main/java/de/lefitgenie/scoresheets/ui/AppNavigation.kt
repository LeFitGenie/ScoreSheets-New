package de.lefitgenie.scoresheets.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.lefitgenie.scoresheets.game.GameType

private const val HOME = "home"
private const val NEW_GAME = "new/{type}"
private const val SHEET = "sheet/{sessionId}"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HOME) {
        composable(HOME) {
            HomeScreen(
                onNewGame = { type -> navController.navigate("new/${type.id}") },
                onOpenSession = { id -> navController.navigate("sheet/$id") },
            )
        }
        composable(
            route = NEW_GAME,
            arguments = listOf(navArgument("type") { type = NavType.StringType }),
        ) { entry ->
            val type = GameType.fromId(entry.arguments?.getString("type").orEmpty())
            NewGameScreen(
                type = type,
                onBack = { navController.popBackStack() },
                onStarted = { id ->
                    navController.navigate("sheet/$id") {
                        popUpTo(HOME)
                    }
                },
            )
        }
        composable(
            route = SHEET,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
        ) {
            SheetScreen(onBack = { navController.popBackStack() })
        }
    }
}
