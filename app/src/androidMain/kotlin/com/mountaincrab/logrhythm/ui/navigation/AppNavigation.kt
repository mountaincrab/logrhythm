package com.mountaincrab.logrhythm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mountaincrab.logrhythm.ui.addentry.AddFoodScreen
import com.mountaincrab.logrhythm.ui.addentry.AddNoteScreen
import com.mountaincrab.logrhythm.ui.addentry.AddPoopScreen
import com.mountaincrab.logrhythm.ui.detail.EntryDetailScreen
import com.mountaincrab.logrhythm.ui.history.HistoryScreen
import com.mountaincrab.logrhythm.ui.home.HomeScreen
import com.mountaincrab.logrhythm.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object History : Screen("history")
    data object Settings : Screen("settings")

    /** kind: "poop" | "food" | "note", optional editId. */
    data object AddPoop : Screen("addPoop?editId={editId}") {
        fun route(editId: String? = null) =
            if (editId == null) "addPoop?editId=" else "addPoop?editId=$editId"
    }
    data object AddFood : Screen("addFood?editId={editId}") {
        fun route(editId: String? = null) =
            if (editId == null) "addFood?editId=" else "addFood?editId=$editId"
    }
    data object AddNote : Screen("addNote?editId={editId}") {
        fun route(editId: String? = null) =
            if (editId == null) "addNote?editId=" else "addNote?editId=$editId"
    }

    /** kind: "poop" | "food" | "note". */
    data object EntryDetail : Screen("entry/{kind}/{id}") {
        fun route(kind: String, id: String) = "entry/$kind/$id"
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenAddPoop = { navController.navigate(Screen.AddPoop.route()) },
                onOpenAddFood = { navController.navigate(Screen.AddFood.route()) },
                onOpenAddNote = { navController.navigate(Screen.AddNote.route()) },
                onOpenEntry = { kind, id ->
                    navController.navigate(Screen.EntryDetail.route(kind, id))
                },
                onTabSelect = { tab ->
                    if (tab != Screen.Home.route) {
                        navController.navigate(tab) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onTabSelect = { tab ->
                    if (tab != Screen.History.route) {
                        navController.navigate(tab) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                onOpenEntry = { kind, id ->
                    navController.navigate(Screen.EntryDetail.route(kind, id))
                },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onTabSelect = { tab ->
                    if (tab != Screen.Settings.route) {
                        navController.navigate(tab) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
        composable(
            route = Screen.AddPoop.route,
            arguments = listOf(navArgument("editId") {
                type = NavType.StringType; defaultValue = ""; nullable = false
            }),
        ) { backStackEntry ->
            val editId = backStackEntry.arguments?.getString("editId")?.takeIf { it.isNotBlank() }
            AddPoopScreen(editId = editId, onDismiss = { navController.popBackStack() })
        }
        composable(
            route = Screen.AddFood.route,
            arguments = listOf(navArgument("editId") {
                type = NavType.StringType; defaultValue = ""; nullable = false
            }),
        ) { backStackEntry ->
            val editId = backStackEntry.arguments?.getString("editId")?.takeIf { it.isNotBlank() }
            AddFoodScreen(editId = editId, onDismiss = { navController.popBackStack() })
        }
        composable(
            route = Screen.AddNote.route,
            arguments = listOf(navArgument("editId") {
                type = NavType.StringType; defaultValue = ""; nullable = false
            }),
        ) { backStackEntry ->
            val editId = backStackEntry.arguments?.getString("editId")?.takeIf { it.isNotBlank() }
            AddNoteScreen(editId = editId, onDismiss = { navController.popBackStack() })
        }
        composable(
            route = Screen.EntryDetail.route,
            arguments = listOf(
                navArgument("kind") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val kind = backStackEntry.arguments?.getString("kind") ?: "poop"
            val id = backStackEntry.arguments?.getString("id") ?: ""
            EntryDetailScreen(
                kind = kind,
                id = id,
                onBack = { navController.popBackStack() },
                onEdit = { editKind, editId ->
                    val target = when (editKind) {
                        "poop" -> Screen.AddPoop.route(editId)
                        "food" -> Screen.AddFood.route(editId)
                        else -> Screen.AddNote.route(editId)
                    }
                    navController.navigate(target)
                },
            )
        }
    }
}
