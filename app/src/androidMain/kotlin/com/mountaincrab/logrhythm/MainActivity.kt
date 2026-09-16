package com.mountaincrab.logrhythm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.mountaincrab.logrhythm.preferences.UserPreferencesRepository
import com.mountaincrab.logrhythm.ui.navigation.AppNavigation
import com.mountaincrab.logrhythm.ui.theme.LogRhythmTheme
import com.mountaincrab.logrhythm.ui.theme.ThemeViewModel
import com.mountaincrab.logrhythm.widget.QuickAddNotificationPermission
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    private val preferences: UserPreferencesRepository by inject()

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* either answer is fine */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = koinViewModel()
            val appTheme by themeViewModel.appTheme.collectAsStateWithLifecycle()
            LogRhythmTheme(appTheme = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController = navController)
                }
            }
        }

        askForWidgetToastPermissionOnce()
    }

    /**
     * The route for someone who placed a quick-add tile before this app ever asked — they would
     * otherwise only meet the dialog by reconfiguring one. It asks nothing of a user with no
     * tile on their launcher, and nothing twice; see [QuickAddNotificationPermission].
     */
    private fun askForWidgetToastPermissionOnce() {
        if (!QuickAddNotificationPermission.isNeeded(this)) return
        if (!QuickAddNotificationPermission.hasPlacedWidgets(this)) return
        lifecycleScope.launch {
            if (preferences.isNotificationPromptShown()) return@launch
            preferences.setNotificationPromptShown()
            requestNotificationPermission.launch(QuickAddNotificationPermission.PERMISSION)
        }
    }
}
