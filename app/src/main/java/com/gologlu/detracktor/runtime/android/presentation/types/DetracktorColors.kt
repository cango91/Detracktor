package com.gologlu.detracktor.runtime.android.presentation.types

import androidx.compose.ui.graphics.Color

/**
 * Centralized color definitions for the Detracktor app.
 * Provides consistent color usage across all components with light/dark mode variants.
 */
data class DetracktorColors(
    // Primary brand colors
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    
    // Secondary colors
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    
    // Surface colors
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    
    // Background colors
    val background: Color,
    val onBackground: Color,
    
    // Error colors
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    
    // Outline colors
    val outline: Color,
    val outlineVariant: Color,
    
    // Detracktor-specific semantic colors
    val sensitiveContent: Color,
    val sensitiveContentContainer: Color,
    val onSensitiveContent: Color,
    val onSensitiveContentContainer: Color,
    
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    
    // URL component colors
    val urlScheme: Color,
    val urlHost: Color,
    val urlPath: Color,
    val urlQuery: Color,
    val urlFragment: Color,
    val urlCredentials: Color,
    
    // Blur and masking colors
    val blurOverlay: Color,
    val maskText: Color,
    val revealedText: Color,
    
    // Interactive element colors
    val chipBackground: Color,
    val chipBorder: Color,
    val chipText: Color,
    val chipSelectedBackground: Color,
    val chipSelectedText: Color,
    
    // Rule editor colors
    val ruleItemBackground: Color,
    val ruleItemBorder: Color,
    val ruleActiveIndicator: Color,
    val ruleInactiveIndicator: Color
)

/**
 * Light theme color definitions
 */
val LightDetracktorColors = DetracktorColors(
    // Primary brand colors - using a blue-based theme for security/privacy feel
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF0D47A1),
    
    // Secondary colors - complementary teal
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2F1),
    onSecondaryContainer = Color(0xFF004D40),
    
    // Surface colors
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    
    // Background colors
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    
    // Error colors
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    // Outline colors
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    
    // Detracktor-specific semantic colors
    sensitiveContent = Color(0xFFD32F2F),
    sensitiveContentContainer = Color(0xFFFFEBEE),
    onSensitiveContent = Color.White,
    onSensitiveContentContainer = Color(0xFFB71C1C),
    
    warning = Color(0xFFFF9800),
    onWarning = Color.Black,
    warningContainer = Color(0xFFFFF3E0),
    onWarningContainer = Color(0xFFE65100),
    
    success = Color(0xFF4CAF50),
    onSuccess = Color.White,
    successContainer = Color(0xFFE8F5E8),
    onSuccessContainer = Color(0xFF1B5E20),
    
    // URL component colors
    urlScheme = Color(0xFF1976D2),
    urlHost = Color(0xFF1976D2),
    urlPath = Color(0xFF1976D2),
    urlQuery = Color(0xFFFF9800),
    urlFragment = Color(0xFFE91E63),
    urlCredentials = Color(0xFFD32F2F),
    
    // Blur and masking colors
    blurOverlay = Color(0x80FFFFFF),
    maskText = Color(0xFF757575),
    revealedText = Color(0xFF1C1B1F),
    
    // Interactive element colors
    chipBackground = Color(0xFFE0E0E0),
    chipBorder = Color(0xFFBDBDBD),
    chipText = Color(0xFF424242),
    chipSelectedBackground = Color(0xFF1976D2),
    chipSelectedText = Color.White,
    
    // Rule editor colors
    ruleItemBackground = Color(0xFFF5F5F5),
    ruleItemBorder = Color(0xFFE0E0E0),
    ruleActiveIndicator = Color(0xFF4CAF50),
    ruleInactiveIndicator = Color(0xFF9E9E9E)
)

/**
 * Dark theme color definitions
 */
val DarkDetracktorColors = DetracktorColors(
    // Primary brand colors
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFE3F2FD),
    
    // Secondary colors
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF004D40),
    secondaryContainer = Color(0xFF00695C),
    onSecondaryContainer = Color(0xFFE0F2F1),
    
    // Surface colors
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
    
    // Background colors
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    
    // Error colors
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    // Outline colors
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    
    // Detracktor-specific semantic colors
    sensitiveContent = Color(0xFFEF5350),
    sensitiveContentContainer = Color(0xFFB71C1C),
    onSensitiveContent = Color.White,
    onSensitiveContentContainer = Color(0xFFFFCDD2),
    
    warning = Color(0xFFFFB74D),
    onWarning = Color(0xFF3E2723),
    warningContainer = Color(0xFFE65100),
    onWarningContainer = Color(0xFFFFF3E0),
    
    success = Color(0xFF81C784),
    onSuccess = Color(0xFF1B5E20),
    successContainer = Color(0xFF2E7D32),
    onSuccessContainer = Color(0xFFC8E6C9),
    
    // URL component colors
    urlScheme = Color(0xFFE6E0E9),
    urlHost = Color(0xFFE6E0E9),
    urlPath = Color(0xFFE6E0E9),
    urlQuery = Color(0xFFFFB74D),
    urlFragment = Color(0xFFF06292),
    urlCredentials = Color(0xFFEF5350),
    
    // Blur and masking colors
    blurOverlay = Color(0x80000000),
    maskText = Color(0xFFBDBDBD),
    revealedText = Color(0xFFE6E0E9),
    
    // Interactive element colors
    chipBackground = Color(0xFF424242),
    chipBorder = Color(0xFF616161),
    chipText = Color(0xFFE0E0E0),
    chipSelectedBackground = Color(0xFF1976D2),
    chipSelectedText = Color.White,
    
    // Rule editor colors
    ruleItemBackground = Color(0xFF2C2C2C),
    ruleItemBorder = Color(0xFF424242),
    ruleActiveIndicator = Color(0xFF66BB6A),
    ruleInactiveIndicator = Color(0xFF757575)
)
