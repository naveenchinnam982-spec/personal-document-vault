package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun getThemeColorScheme(themeStyle: AppThemeStyle, darkTheme: Boolean): androidx.compose.material3.ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = themeStyle.primaryColor,
            onPrimary = Color.White,
            primaryContainer = themeStyle.primaryColor.copy(alpha = 0.25f),
            onPrimaryContainer = Color.White,
            secondary = themeStyle.accentColor,
            onSecondary = Color.White,
            secondaryContainer = themeStyle.accentColor.copy(alpha = 0.25f),
            onSecondaryContainer = Color.White,
            tertiary = CyanAccent,
            onTertiary = Color.White,
            background = themeStyle.bgColor,
            onBackground = TextPrimary,
            surface = themeStyle.surfaceColor,
            onSurface = TextPrimary,
            surfaceVariant = themeStyle.surfaceElevatedColor,
            onSurfaceVariant = TextSecondary,
            outline = themeStyle.borderColor,
            outlineVariant = themeStyle.borderColor.copy(alpha = 0.5f),
            error = VaultRed,
            onError = Color.White,
            errorContainer = VaultRedContainer,
            onErrorContainer = Color(0xFFFCA5A5)
        )
    } else {
        lightColorScheme(
            primary = themeStyle.primaryColor,
            onPrimary = Color.White,
            primaryContainer = themeStyle.primaryColor.copy(alpha = 0.15f),
            onPrimaryContainer = Color(0xFF065F46),
            secondary = themeStyle.accentColor,
            onSecondary = Color.White,
            secondaryContainer = themeStyle.accentColor.copy(alpha = 0.15f),
            onSecondaryContainer = Color(0xFF115E59),
            tertiary = CyanAccent,
            onTertiary = Color.White,
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF0F172A),
            surface = Color.White,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFFCBD5E1),
            outlineVariant = Color(0xFFE2E8F0),
            error = VaultRed,
            onError = Color.White,
            errorContainer = Color(0xFFFEE2E2),
            onErrorContainer = Color(0xFF991B1B)
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.EMERALD,
    darkTheme: Boolean = true, // Default to dark privacy theme
    content: @Composable () -> Unit
) {
    val colorScheme = getThemeColorScheme(themeStyle, darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

