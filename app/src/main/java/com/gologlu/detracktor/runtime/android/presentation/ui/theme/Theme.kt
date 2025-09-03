package com.gologlu.detracktor.runtime.android.presentation.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.gologlu.detracktor.runtime.android.presentation.types.DetracktorColors
import com.gologlu.detracktor.runtime.android.presentation.types.DarkDetracktorColors
import com.gologlu.detracktor.runtime.android.presentation.types.LightDetracktorColors

/**
 * CompositionLocal for accessing Detracktor-specific colors throughout the app
 */
val LocalDetracktorColors = staticCompositionLocalOf<DetracktorColors> {
    error("No DetracktorColors provided")
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkDetracktorColors.primary,
    onPrimary = DarkDetracktorColors.onPrimary,
    primaryContainer = DarkDetracktorColors.primaryContainer,
    onPrimaryContainer = DarkDetracktorColors.onPrimaryContainer,
    secondary = DarkDetracktorColors.secondary,
    onSecondary = DarkDetracktorColors.onSecondary,
    secondaryContainer = DarkDetracktorColors.secondaryContainer,
    onSecondaryContainer = DarkDetracktorColors.onSecondaryContainer,
    surface = DarkDetracktorColors.surface,
    onSurface = DarkDetracktorColors.onSurface,
    surfaceVariant = DarkDetracktorColors.surfaceVariant,
    onSurfaceVariant = DarkDetracktorColors.onSurfaceVariant,
    background = DarkDetracktorColors.background,
    onBackground = DarkDetracktorColors.onBackground,
    error = DarkDetracktorColors.error,
    onError = DarkDetracktorColors.onError,
    errorContainer = DarkDetracktorColors.errorContainer,
    onErrorContainer = DarkDetracktorColors.onErrorContainer,
    outline = DarkDetracktorColors.outline,
    outlineVariant = DarkDetracktorColors.outlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = LightDetracktorColors.primary,
    onPrimary = LightDetracktorColors.onPrimary,
    primaryContainer = LightDetracktorColors.primaryContainer,
    onPrimaryContainer = LightDetracktorColors.onPrimaryContainer,
    secondary = LightDetracktorColors.secondary,
    onSecondary = LightDetracktorColors.onSecondary,
    secondaryContainer = LightDetracktorColors.secondaryContainer,
    onSecondaryContainer = LightDetracktorColors.onSecondaryContainer,
    surface = LightDetracktorColors.surface,
    onSurface = LightDetracktorColors.onSurface,
    surfaceVariant = LightDetracktorColors.surfaceVariant,
    onSurfaceVariant = LightDetracktorColors.onSurfaceVariant,
    background = LightDetracktorColors.background,
    onBackground = LightDetracktorColors.onBackground,
    error = LightDetracktorColors.error,
    onError = LightDetracktorColors.onError,
    errorContainer = LightDetracktorColors.errorContainer,
    onErrorContainer = LightDetracktorColors.onErrorContainer,
    outline = LightDetracktorColors.outline,
    outlineVariant = LightDetracktorColors.outlineVariant
)

@Composable
fun DetracktorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val detracktorColors = if (darkTheme) DarkDetracktorColors else LightDetracktorColors

    CompositionLocalProvider(LocalDetracktorColors provides detracktorColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Enhanced DetracktorTheme that supports explicit theme mode control
 */
@Composable
fun DetracktorTheme(
    themeMode: com.gologlu.detracktor.runtime.android.presentation.types.ThemeMode = com.gologlu.detracktor.runtime.android.presentation.types.ThemeMode.SYSTEM,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        com.gologlu.detracktor.runtime.android.presentation.types.ThemeMode.LIGHT -> false
        com.gologlu.detracktor.runtime.android.presentation.types.ThemeMode.DARK -> true
        com.gologlu.detracktor.runtime.android.presentation.types.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    DetracktorTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}

/**
 * Access Detracktor-specific colors from any composable within DetracktorTheme
 */
object DetracktorTheme {
    val colors: DetracktorColors
        @Composable
        get() = LocalDetracktorColors.current
}
