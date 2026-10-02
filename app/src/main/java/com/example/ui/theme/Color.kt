package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Vault Dark Luxury Palette
val VaultDarkBg = Color(0xFF090D12)
val VaultSurface = Color(0xFF111822)
val VaultSurfaceElevated = Color(0xFF192330)
val VaultSurfaceHighlight = Color(0xFF222F40)
val VaultBorder = Color(0xFF253344)

// Primary Emerald
val EmeraldPrimary = Color(0xFF10B981)
val EmeraldLight = Color(0xFF34D399)
val EmeraldDark = Color(0xFF059669)
val EmeraldGlow = Color(0x3310B981)
val EmeraldContainer = Color(0xFF064E3B)
val OnEmeraldContainer = Color(0xFFA7F3D0)

// Accent Cyan / Teal
val CyanAccent = Color(0xFF06B6D4)
val TealAccent = Color(0xFF14B8A6)

// Alerts & States
val VaultRed = Color(0xFFEF4444)
val VaultRedContainer = Color(0xFF450A0A)
val VaultAmber = Color(0xFFF59E0B)
val VaultAmberContainer = Color(0xFF451A03)
val VaultBlue = Color(0xFF3B82F6)

// Text & Content
val TextPrimary = Color(0xFFF9FAFB)
val TextSecondary = Color(0xFF9CA3AF)
val TextTertiary = Color(0xFF6B7280)
val TextMuted = Color(0xFF4B5563)

// Theme Palette Definitions
enum class AppThemeStyle(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val accentColor: Color,
    val bgColor: Color,
    val surfaceColor: Color,
    val surfaceElevatedColor: Color,
    val borderColor: Color,
    val iconDescription: String
) {
    EMERALD(
        id = "EMERALD",
        displayName = "Emerald Aurora",
        primaryColor = Color(0xFF10B981),
        accentColor = Color(0xFF06B6D4),
        bgColor = Color(0xFF090D12),
        surfaceColor = Color(0xFF111822),
        surfaceElevatedColor = Color(0xFF192330),
        borderColor = Color(0xFF253344),
        iconDescription = "Cyber Tech Emerald"
    ),
    VIOLET(
        id = "VIOLET",
        displayName = "Cyber Violet",
        primaryColor = Color(0xFFA855F7),
        accentColor = Color(0xFFEC4899),
        bgColor = Color(0xFF090614),
        surfaceColor = Color(0xFF140D26),
        surfaceElevatedColor = Color(0xFF20163B),
        borderColor = Color(0xFF35255F),
        iconDescription = "Neon Amethyst Glow"
    ),
    SUNSET(
        id = "SUNSET",
        displayName = "Sunset Rose Gold",
        primaryColor = Color(0xFFF43F5E),
        accentColor = Color(0xFFF59E0B),
        bgColor = Color(0xFF12080D),
        surfaceColor = Color(0xFF221019),
        surfaceElevatedColor = Color(0xFF351927),
        borderColor = Color(0xFF52263C),
        iconDescription = "Warm Coral & Gold"
    ),
    SAPPHIRE(
        id = "SAPPHIRE",
        displayName = "Ocean Sapphire",
        primaryColor = Color(0xFF3B82F6),
        accentColor = Color(0xFF38BDF8),
        bgColor = Color(0xFF070E1A),
        surfaceColor = Color(0xFF0F1C33),
        surfaceElevatedColor = Color(0xFF182B4D),
        borderColor = Color(0xFF244175),
        iconDescription = "Electric Royal Blue"
    ),
    MIDNIGHT(
        id = "MIDNIGHT",
        displayName = "Midnight OLED",
        primaryColor = Color(0xFF84CC16),
        accentColor = Color(0xFFEAB308),
        bgColor = Color(0xFF000000),
        surfaceColor = Color(0xFF0E0E0E),
        surfaceElevatedColor = Color(0xFF1A1A1A),
        borderColor = Color(0xFF2C2C2C),
        iconDescription = "Pure Pitch Black"
    );

    companion object {
        fun fromId(id: String?): AppThemeStyle {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: EMERALD
        }
    }
}

// Hidden Vault Palette (Highest Security Tier - Obsidian & Radiant Violet)
val HiddenVaultDarkBg = Color(0xFF08060F)
val HiddenVaultSurface = Color(0xFF110D1F)
val HiddenVaultSurfaceElevated = Color(0xFF191330)
val HiddenVaultSurfaceHighlight = Color(0xFF231B42)
val HiddenVaultBorder = Color(0xFF2D2352)
val HiddenBorder = HiddenVaultBorder
val HiddenPrimary = Color(0xFF8B5CF6)
val HiddenPrimaryLight = Color(0xFFA78BFA)
val HiddenPrimaryDark = Color(0xFF6D28D9)
val HiddenPrimaryContainer = Color(0xFF2E1065)
val OnHiddenContainer = Color(0xFFDDD6FE)
val HiddenGlow = Color(0x338B5CF6)
val HiddenGold = Color(0xFFFBBF24)
val HiddenIndigo = Color(0xFF6366F1)
