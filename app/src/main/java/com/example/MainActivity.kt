package com.example

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.os.Bundle
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
                    when (uiState.currentScreen) {
                        LauncherScreen.SETTINGS -> viewModel.navigateTo(LauncherScreen.HOME)
                        LauncherScreen.DRAWER -> viewModel.navigateTo(LauncherScreen.HOME)
                        LauncherScreen.HOME -> {
                            // Do nothing: keep launcher pinned as Home
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = uiState.currentScreen,
                        transitionSpec = {
                            when {
                                // Home -> Drawer: Drawer slides up fluidly from bottom
                                initialState == LauncherScreen.HOME && targetState == LauncherScreen.DRAWER -> {
                                    (slideInVertically(
                                        initialOffsetY = { height -> height },
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) + fadeIn(animationSpec = tween(120)))
                                        .togetherWith(
                                            slideOutVertically(
                                                targetOffsetY = { height -> -height / 6 },
                                                animationSpec = tween(180)
                                            ) + fadeOut(animationSpec = tween(120))
                                        )
                                }
                                // Drawer -> Home: Drawer slides down fluidly back to home
                                initialState == LauncherScreen.DRAWER && targetState == LauncherScreen.HOME -> {
                                    (slideInVertically(
                                        initialOffsetY = { height -> -height / 6 },
                                        animationSpec = tween(180)
                                    ) + fadeIn(animationSpec = tween(150)))
                                        .togetherWith(
                                            slideOutVertically(
                                                targetOffsetY = { height -> height },
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            ) + fadeOut(animationSpec = tween(150))
                                        )
                                }
                                // Any -> Settings: Slide in horizontally from right
                                targetState == LauncherScreen.SETTINGS -> {
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
                                }
                                // Settings -> Any: Slide out horizontally to right
                                initialState == LauncherScreen.SETTINGS -> {
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
                                else -> {
                                    fadeIn(animationSpec = tween(150))
                                        .togetherWith(fadeOut(animationSpec = tween(150)))
                                }
                            }
                        },
                        label = "screen_fluid_transition"
                    ) { screen ->
                        when (screen) {
                            LauncherScreen.HOME -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    appWidgetHost = appWidgetHost,
                                    appWidgetManager = appWidgetManager
                                )
                            }
                            LauncherScreen.DRAWER -> {
                                AppDrawerScreen(
                                    viewModel = viewModel,
                                    uiState = uiState
                                )
                            }
                            LauncherScreen.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    appWidgetHost = appWidgetHost,
                                    appWidgetManager = appWidgetManager
                                )
                            }
                        }
                    }
                }
            }
        }
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
