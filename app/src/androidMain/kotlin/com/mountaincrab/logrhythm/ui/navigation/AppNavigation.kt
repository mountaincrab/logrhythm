package com.mountaincrab.logrhythm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mountaincrab.logrhythm.auth.AuthRepository
import com.mountaincrab.logrhythm.ui.addentry.AddFoodScreen
import com.mountaincrab.logrhythm.ui.addentry.AddNoteScreen
import com.mountaincrab.logrhythm.ui.addentry.AddPoopScreen
import com.mountaincrab.logrhythm.ui.auth.SignInScreen
import com.mountaincrab.logrhythm.ui.detail.EntryDetailScreen
import com.mountaincrab.logrhythm.ui.history.HistoryScreen
import com.mountaincrab.logrhythm.ui.home.HomeScreen
import com.mountaincrab.logrhythm.ui.profiles.ProfilesScreen
import com.mountaincrab.logrhythm.ui.settings.SettingsScreen
import org.koin.compose.koinInject

sealed class Screen(val route: String) {
    data object SignIn : Screen("signIn")
    data object Home : Screen("home")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object Profiles : Screen("profiles")

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
    val authRepo: AuthRepository = koinInject()
    val currentUser by authRepo.currentUser.collectAsStateWithLifecycle()

    // startDestination uses the synchronous currentUserId so there's no sign-in flash
    // for users who are already authenticated.
    val startDestination = remember {
        if (authRepo.currentUserId != null) Screen.Home.route else Screen.SignIn.route
    }

    // React to auth state changes: sign-in navigates to Home, sign-out returns to SignIn.
    LaunchedEffect(currentUser) {
        val route = navController.currentDestination?.route
        when {
            currentUser != null && route == Screen.SignIn.route ->
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.SignIn.route) { inclusive = true }
                }
            currentUser == null && route != Screen.SignIn.route && route != null ->
                navController.navigate(Screen.SignIn.route) {
                    popUpTo(0) { inclusive = true }
                }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.SignIn.route) {
            SignInScreen()
        }
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
                onOpenProfiles = { navController.navigate(Screen.Profiles.route) },
            )
        }
        composable(Screen.Profiles.route) {
            ProfilesScreen(onBack = { navController.popBackStack() })
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
