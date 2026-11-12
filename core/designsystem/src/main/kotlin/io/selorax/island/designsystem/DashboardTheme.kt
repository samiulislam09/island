package io.selorax.island.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.selorax.island.model.AccentGlowColor

public data class DashboardColors(
    public val isLight: Boolean,
    public val background: Color,
    public val cardBackground: Color,
    public val cardBorder: Color,
    public val textPrimary: Color,
    public val textSecondary: Color,
    public val textMuted: Color,
    public val divider: Color,
    public val surfaceVariant: Color,
    public val surfaceVariantBorder: Color,
    public val navBackground: Color,
    public val navBorder: Color
)

public val LightDashboardColors: DashboardColors = DashboardColors(
    isLight = true,
    background = Color(0xFFF8FAFC),
    cardBackground = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF94A3B8),
    divider = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFFF1F5F9),
    surfaceVariantBorder = Color(0xFFCBD5E1),
    navBackground = Color(0xFFFFFFFF),
    navBorder = Color(0xFFE2E8F0)
)

public val DarkDashboardColors: DashboardColors = DashboardColors(
    isLight = false,
    background = Color(0xFF101012),
    cardBackground = Color(0xFF141417),
    cardBorder = Color(0xFF222228),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF8E8E93),
    textMuted = Color(0xFF666666),
    divider = Color(0xFF222228),
    surfaceVariant = Color(0xFF1C1C22),
    surfaceVariantBorder = Color(0xFF2E2E36),
    navBackground = Color(0xFF121215),
    navBorder = Color(0xFF222228)
)

public val LocalDashboardColors: androidx.compose.runtime.ProvidableCompositionLocal<DashboardColors> =
    staticCompositionLocalOf { LightDashboardColors }

public object IslandTheme {
    public val colors: DashboardColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDashboardColors.current

    public fun getAccent(accentColor: AccentGlowColor, isLight: Boolean): Color {
        return if (isLight) {
            when (accentColor) {
                AccentGlowColor.PEARL -> Color(0xFF0F172A)
                AccentGlowColor.CYAN -> Color(0xFF0284C7)
                AccentGlowColor.PURPLE -> Color(0xFF7C3AED)
                AccentGlowColor.GREEN -> Color(0xFF059669)
                AccentGlowColor.AMBER -> Color(0xFFD97706)
                AccentGlowColor.ROSE -> Color(0xFFE11D48)
            }
        } else {
            Color(accentColor.hexColor)
        }
    }
}
