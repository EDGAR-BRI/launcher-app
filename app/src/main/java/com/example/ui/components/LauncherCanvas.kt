package com.example.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.LauncherScreen
import com.example.ui.LauncherUiState
import com.example.ui.LauncherViewModel
import com.example.ui.drawer.AppDrawerScreen
import com.example.ui.home.HomeScreen
import kotlinx.coroutines.launch

/**
 * Interactive unified canvas providing 1:1 real-time gesture tracking between
 * HomeScreen and AppDrawerScreen. Swiping up on Home or down on Drawer follows
 * the user's finger with zero latency.
 */
@Composable
fun LauncherCanvas(
    viewModel: LauncherViewModel,
    uiState: LauncherUiState,
    appWidgetHost: AppWidgetHost? = null,
    appWidgetManager: AppWidgetManager? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val screenHeight = constraints.maxHeight.toFloat()
        val coroutineScope = rememberCoroutineScope()

        // 0f = App Drawer fully opened, screenHeight = App Drawer fully closed (showing Home)
        val initialOffset = if (uiState.currentScreen == LauncherScreen.DRAWER) 0f else screenHeight
        val drawerOffsetY = remember { Animatable(initialOffset) }

        // Sync with programmatic screen transitions (Back button, buttons, gestures, etc.)
        LaunchedEffect(uiState.currentScreen, screenHeight) {
            if (screenHeight > 0f) {
                if (uiState.currentScreen == LauncherScreen.DRAWER) {
                    if (drawerOffsetY.value > 0f) {
                        drawerOffsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                } else if (uiState.currentScreen == LauncherScreen.HOME) {
                    if (drawerOffsetY.value < screenHeight) {
                        drawerOffsetY.animateTo(
                            targetValue = screenHeight,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
            }
        }

        val isDrawerVisible = drawerOffsetY.value < screenHeight || uiState.currentScreen == LauncherScreen.DRAWER

        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Home Screen layer with fluid parallax depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (screenHeight > 0f) {
                            val progress = (drawerOffsetY.value / screenHeight).coerceIn(0f, 1f)
                            // Upward parallax displacement as drawer slides up
                            translationY = (1f - progress) * -screenHeight * 0.12f
                            // Subtle fade
                            alpha = 0.45f + 0.55f * progress
                        }
                    }
            ) {
                HomeScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    appWidgetHost = appWidgetHost,
                    appWidgetManager = appWidgetManager,
                    isDrawerPartiallyOpen = { drawerOffsetY.value < screenHeight - 2f },
                    onSwipeUpDrag = { deltaY ->
                        coroutineScope.launch {
                            val newOffset = (drawerOffsetY.value + deltaY).coerceIn(0f, screenHeight)
                            drawerOffsetY.snapTo(newOffset)
                        }
                    },
                    onSwipeUpRelease = { velocityY ->
                        coroutineScope.launch {
                            // If flicked up with momentum or pulled up past 28% of the screen
                            val shouldOpen = velocityY < -500f || drawerOffsetY.value < screenHeight * 0.72f
                            if (shouldOpen) {
                                drawerOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                viewModel.navigateTo(LauncherScreen.DRAWER)
                            } else {
                                drawerOffsetY.animateTo(
                                    targetValue = screenHeight,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                viewModel.navigateTo(LauncherScreen.HOME)
                            }
                        }
                    }
                )
            }

            // 2. App Drawer Screen layer sliding directly over Home
            if (isDrawerVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = drawerOffsetY.value
                        }
                ) {
                    AppDrawerScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onSwipeDownDrag = { deltaY ->
                            coroutineScope.launch {
                                val newOffset = (drawerOffsetY.value + deltaY).coerceIn(0f, screenHeight)
                                drawerOffsetY.snapTo(newOffset)
                            }
                        },
                        onSwipeDownRelease = { velocityY ->
                            coroutineScope.launch {
                                // If flicked down with momentum or pulled down past 25% of the screen
                                val shouldClose = velocityY > 500f || drawerOffsetY.value > screenHeight * 0.25f
                                if (shouldClose) {
                                    drawerOffsetY.animateTo(
                                        targetValue = screenHeight,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                    viewModel.navigateTo(LauncherScreen.HOME)
                                } else {
                                    drawerOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                    viewModel.navigateTo(LauncherScreen.DRAWER)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
