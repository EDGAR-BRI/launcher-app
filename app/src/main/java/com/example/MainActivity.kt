package com.example

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LauncherScreen
import com.example.ui.LauncherViewModel
import com.example.ui.components.LauncherCanvas
import com.example.ui.components.RecentAppsSheet
import com.example.ui.drawer.AppDrawerScreen
import com.example.ui.home.HomeScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MinimalLauncherTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private val APPWIDGET_HOST_ID = 2048
    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetHost = AppWidgetHost(applicationContext, APPWIDGET_HOST_ID)
        appWidgetManager = AppWidgetManager.getInstance(applicationContext)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.hideStatusBar) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                if (uiState.hideStatusBar) {
                    insetsController.hide(WindowInsetsCompat.Type.statusBars())
                    insetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    insetsController.show(WindowInsetsCompat.Type.statusBars())
                }
            }

            MinimalLauncherTheme(themeMode = uiState.themeMode) {
                // Launcher Back Button Discipline:
                // If in Drawer or Settings, back returns to Home.
                // If in Home, back is safely consumed so launcher does not exit.
                BackHandler(enabled = true) {
                    if (uiState.showRecentAppsSheet) {
                        viewModel.hideRecentsSheet()
                    } else {
                        when (uiState.currentScreen) {
                            LauncherScreen.SETTINGS -> viewModel.navigateTo(LauncherScreen.HOME)
                            LauncherScreen.DRAWER -> viewModel.navigateTo(LauncherScreen.HOME)
                            LauncherScreen.HOME -> {
                                // Do nothing: keep launcher pinned as Home
                            }
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    val isSettings = uiState.currentScreen == LauncherScreen.SETTINGS
                    AnimatedContent(
                        targetState = isSettings,
                        transitionSpec = {
                            if (targetState) {
                                // Home/Drawer -> Settings: Slide in horizontally from right
                                (slideInHorizontally(
                                    initialOffsetX = { width -> width },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + fadeIn(animationSpec = tween(120)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            targetOffsetX = { width -> -width / 4 },
                                            animationSpec = tween(180)
                                        ) + fadeOut(animationSpec = tween(120))
                                    )
                            } else {
                                // Settings -> Home/Drawer: Slide out horizontally to right
                                (slideInHorizontally(
                                    initialOffsetX = { width -> -width / 4 },
                                    animationSpec = tween(180)
                                ) + fadeIn(animationSpec = tween(150)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            targetOffsetX = { width -> width },
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) + fadeOut(animationSpec = tween(150))
                                    )
                            }
                        },
                        label = "settings_fluid_transition"
                    ) { showSettings ->
                        if (showSettings) {
                            SettingsScreen(
                                viewModel = viewModel,
                                uiState = uiState,
                                appWidgetHost = appWidgetHost,
                                appWidgetManager = appWidgetManager
                            )
                        } else {
                            LauncherCanvas(
                                viewModel = viewModel,
                                uiState = uiState,
                                appWidgetHost = appWidgetHost,
                                appWidgetManager = appWidgetManager
                            )
                        }
                    }
                }

                if (uiState.showRecentAppsSheet) {
                    RecentAppsSheet(
                        recentApps = uiState.recentApps,
                        iconStyle = uiState.iconStyle,
                        repository = viewModel.repository,
                        onDismiss = { viewModel.hideRecentsSheet() },
                        onOpenApp = { app -> viewModel.launchApp(app) },
                        onOpenSystemRecents = { viewModel.openRecents() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Ensure returning to home dismisses recent apps sheet and returns to HOME screen
        viewModel.hideRecentsSheet()
        viewModel.navigateTo(LauncherScreen.HOME)
    }

    override fun onResume() {
        super.onResume()
        // When coming back from external apps or home gestures, keep recents sheet closed
        viewModel.hideRecentsSheet()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_MENU || event.keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            if (event.action == KeyEvent.ACTION_UP) {
                viewModel.toggleRecentsSheet()
            }
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_HOME) {
            if (event.action == KeyEvent.ACTION_UP) {
                viewModel.hideRecentsSheet()
                viewModel.navigateTo(LauncherScreen.HOME)
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            viewModel.toggleRecentsSheet()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onStart() {
        super.onStart()
        try {
            appWidgetHost.startListening()
        } catch (_: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try {
            appWidgetHost.stopListening()
        } catch (_: Exception) {}
    }
}
