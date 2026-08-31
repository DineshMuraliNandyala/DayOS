package com.lifeos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppTheme { DARK, AMOLED, LIGHT }
enum class AccentDomain { FOCUS, ENERGY, CALM }

/**
 * Extended colour tokens that Material 3 doesn't have slots for.
 * Accessed via LocalLifeOSColors.current inside composables.
 */
data class LifeOSColors(
    val success: Color,
    val successDim: Color,
    val warning: Color,
    val warningDim: Color,
    val surface1: Color,
    val surface2: Color,
    val surfaceHover: Color,
    val textFaint: Color,
    val accentDim: Color,
)

val LocalLifeOSColors = staticCompositionLocalOf {
    LifeOSColors(
        success = SemanticSuccess,
        successDim = Color(0x226DC99A),
        warning = SemanticWarning,
        warningDim = Color(0x22F7C462),
        surface1 = DarkSurface1,
        surface2 = DarkSurface2,
        surfaceHover = DarkSurfaceHover,
        textFaint = DarkTextFaint,
        accentDim = FocusDim,
    )
}

// ─── Color scheme builders ─────────────────────────────────────────────────────

private fun darkScheme(primary: Color, container: Color, onContainer: Color, onPrimary: Color): ColorScheme =
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondary = CalmPrimary,
        onSecondary = CalmOnPrimary,
        secondaryContainer = CalmContainer,
        onSecondaryContainer = CalmOnContainer,
        tertiary = SemanticSuccess,
        onTertiary = Color(0xFF003820),
        tertiaryContainer = Color(0xFF0D3525),
        onTertiaryContainer = Color(0xFFA8F0C8),
        error = ErrorPrimary,
        onError = OnError,
        errorContainer = ErrorContainer,
        onErrorContainer = OnErrorContainer,
        background = DarkBg,
        onBackground = DarkText,
        surface = DarkBg,
        onSurface = DarkText,
        surfaceVariant = DarkSurface2,
        onSurfaceVariant = DarkTextMuted,
        surfaceTint = primary,
        inverseSurface = DarkText,
        inverseOnSurface = DarkBg,
        inversePrimary = container,
        outline = DarkTextFaint,
        outlineVariant = DarkBorder,
        scrim = Color.Black,
    )

private fun amoledScheme(primary: Color, container: Color, onContainer: Color, onPrimary: Color): ColorScheme =
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondary = CalmPrimary,
        onSecondary = CalmOnPrimary,
        secondaryContainer = CalmContainer,
        onSecondaryContainer = CalmOnContainer,
        tertiary = SemanticSuccess,
        onTertiary = Color(0xFF003820),
        tertiaryContainer = Color(0xFF0D3525),
        onTertiaryContainer = Color(0xFFA8F0C8),
        error = ErrorPrimary,
        onError = OnError,
        errorContainer = ErrorContainer,
        onErrorContainer = OnErrorContainer,
        background = AmoledBg,
        onBackground = DarkText,
        surface = AmoledBg,
        onSurface = DarkText,
        surfaceVariant = AmoledSurface2,
        onSurfaceVariant = AmoledTextMuted,
        surfaceTint = primary,
        inverseSurface = DarkText,
        inverseOnSurface = AmoledBg,
        inversePrimary = container,
        outline = AmoledTextFaint,
        outlineVariant = AmoledBorder,
        scrim = Color.Black,
    )

private fun lightScheme(primary: Color, container: Color, onContainer: Color, onPrimary: Color): ColorScheme =
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondary = CalmPrimary,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFECE4FF),
        onSecondaryContainer = Color(0xFF150030),
        tertiary = SemanticSuccess,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFB8F5D8),
        onTertiaryContainer = Color(0xFF003820),
        error = SemanticDanger,
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = LightBg,
        onBackground = LightText,
        surface = LightBg,
        onSurface = LightText,
        surfaceVariant = LightSurface2,
        onSurfaceVariant = LightTextMuted,
        surfaceTint = primary,
        inverseSurface = LightText,
        inverseOnSurface = LightBg,
        inversePrimary = FocusContainer,
        outline = LightTextMuted,
        outlineVariant = LightBorder,
        scrim = Color.Black,
    )

// ─── Per-accent schemes ────────────────────────────────────────────────────────

private val FocusDarkScheme = darkScheme(FocusPrimary, FocusContainer, FocusOnContainer, FocusOnPrimary)
private val EnergyDarkScheme = darkScheme(EnergyPrimary, EnergyContainer, EnergyOnContainer, EnergyOnPrimary)
private val CalmDarkScheme = darkScheme(CalmPrimary, CalmContainer, CalmOnContainer, CalmOnPrimary)

private val FocusAmoledScheme = amoledScheme(FocusPrimary, FocusContainer, FocusOnContainer, FocusOnPrimary)
private val EnergyAmoledScheme = amoledScheme(EnergyPrimary, EnergyContainer, EnergyOnContainer, EnergyOnPrimary)
private val CalmAmoledScheme = amoledScheme(CalmPrimary, CalmContainer, CalmOnContainer, CalmOnPrimary)

private val FocusLightScheme = lightScheme(FocusPrimary, Color(0xFFDDE4FF), Color(0xFF001258), Color.White)
private val EnergyLightScheme = lightScheme(EnergyPrimary, Color(0xFFFFDBCE), Color(0xFF3A0900), Color.White)
private val CalmLightScheme = lightScheme(CalmPrimary, Color(0xFFECE4FF), Color(0xFF22005D), Color.White)

// ─── LifeOSTheme ──────────────────────────────────────────────────────────────

@Composable
fun LifeOSTheme(
    appTheme: AppTheme = if (isSystemInDarkTheme()) AppTheme.DARK else AppTheme.LIGHT,
    accentDomain: AccentDomain = AccentDomain.FOCUS,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (appTheme) {
        AppTheme.DARK -> when (accentDomain) {
            AccentDomain.FOCUS -> FocusDarkScheme
            AccentDomain.ENERGY -> EnergyDarkScheme
            AccentDomain.CALM -> CalmDarkScheme
        }
        AppTheme.AMOLED -> when (accentDomain) {
            AccentDomain.FOCUS -> FocusAmoledScheme
            AccentDomain.ENERGY -> EnergyAmoledScheme
            AccentDomain.CALM -> CalmAmoledScheme
        }
        AppTheme.LIGHT -> when (accentDomain) {
            AccentDomain.FOCUS -> FocusLightScheme
            AccentDomain.ENERGY -> EnergyLightScheme
            AccentDomain.CALM -> CalmLightScheme
        }
    }

    val extendedColors = when (appTheme) {
        AppTheme.LIGHT -> LifeOSColors(
            success = SemanticSuccess,
            successDim = Color(0x226DC99A),
            warning = SemanticWarning,
            warningDim = Color(0x22F7C462),
            surface1 = LightSurface1,
            surface2 = LightSurface2,
            surfaceHover = LightSurface2,
            textFaint = LightTextFaint,
            accentDim = when (accentDomain) {
                AccentDomain.FOCUS -> FocusDim
                AccentDomain.ENERGY -> EnergyDim
                AccentDomain.CALM -> CalmDim
            },
        )
        AppTheme.AMOLED -> LifeOSColors(
            success = SemanticSuccess,
            successDim = Color(0x226DC99A),
            warning = SemanticWarning,
            warningDim = Color(0x22F7C462),
            surface1 = AmoledSurface1,
            surface2 = AmoledSurface2,
            surfaceHover = AmoledSurfaceHover,
            textFaint = AmoledTextFaint,
            accentDim = when (accentDomain) {
                AccentDomain.FOCUS -> FocusDim
                AccentDomain.ENERGY -> EnergyDim
                AccentDomain.CALM -> CalmDim
            },
        )
        else -> LifeOSColors(
            success = SemanticSuccess,
            successDim = Color(0x226DC99A),
            warning = SemanticWarning,
            warningDim = Color(0x22F7C462),
            surface1 = DarkSurface1,
            surface2 = DarkSurface2,
            surfaceHover = DarkSurfaceHover,
            textFaint = DarkTextFaint,
            accentDim = when (accentDomain) {
                AccentDomain.FOCUS -> FocusDim
                AccentDomain.ENERGY -> EnergyDim
                AccentDomain.CALM -> CalmDim
            },
        )
    }

    CompositionLocalProvider(LocalLifeOSColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LifeOSTypography,
            content = content,
        )
    }
}
