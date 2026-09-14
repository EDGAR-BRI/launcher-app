package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AmoledColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = DarkTextSecondary,
    onSecondary = Color.White,
    tertiary = DarkTextTertiary,
    background = PitchBlack,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

private val CharcoalColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = CharcoalDark,
    secondary = DarkTextSecondary,
    onSecondary = Color.White,
    tertiary = DarkTextTertiary,
    background = CharcoalDark,
    onBackground = DarkTextPrimary,
    surface = CharcoalSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = CharcoalBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = LightTextSecondary,
    onSecondary = Color.Black,
    tertiary = LightTextTertiary,
    background = PaperWhite,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

@Composable
fun MinimalLauncherTheme(
    themeMode: String = "amoled",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        "monochrome_light" -> LightColorScheme
        "monochrome_gray" -> CharcoalColorScheme
        else -> AmoledColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MinimalLauncherTheme(
        themeMode = if (darkTheme) "amoled" else "monochrome_light",
        content = content
    )
}
