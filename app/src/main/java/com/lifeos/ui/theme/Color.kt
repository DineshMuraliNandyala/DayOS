package com.lifeos.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Accent primaries (from globals.css) ─────────────────────────────────────
// These three are the user-selectable accent colours.
val FocusPrimary = Color(0xFF7C9DFF)    // --accent-focus  (periwinkle)
val EnergyPrimary = Color(0xFFFF9478)   // --accent-energy (coral)
val CalmPrimary = Color(0xFFC4B0FF)     // --accent-calm   (violet)

// ─── Semantic tokens (theme-invariant) ───────────────────────────────────────
val SemanticSuccess = Color(0xFF6DC99A) // --success
val SemanticWarning = Color(0xFFF7C462) // --warning
val SemanticDanger = Color(0xFFF47070)  // --danger

// ─── Dark theme surfaces ──────────────────────────────────────────────────────
val DarkBg = Color(0xFF09090F)             // --bg
val DarkSurface1 = Color(0xFF13141A)       // --surface-1
val DarkSurface2 = Color(0xFF1A1C24)       // --surface-2
val DarkSurfaceHover = Color(0xFF20232E)   // --surface-hover
val DarkText = Color(0xFFF0F1F5)           // --text
val DarkTextMuted = Color(0xFF8A91A0)      // --text-muted
val DarkTextFaint = Color(0xFF4E5568)      // --text-faint
val DarkBorder = Color(0xFF1E2130)         // ~rgba(255,255,255,0.08) on dark bg

// ─── AMOLED surfaces (true black) ─────────────────────────────────────────────
val AmoledBg = Color(0xFF000000)
val AmoledSurface1 = Color(0xFF0A0A0A)
val AmoledSurface2 = Color(0xFF111111)
val AmoledSurfaceHover = Color(0xFF1A1A1A)
val AmoledTextMuted = Color(0xFF828999)
val AmoledTextFaint = Color(0xFF404550)
val AmoledBorder = Color(0xFF141416)

// ─── Light theme surfaces ─────────────────────────────────────────────────────
val LightBg = Color(0xFFF2F1EF)
val LightSurface1 = Color(0xFFFAF9F7)
val LightSurface2 = Color(0xFFFFFFFF)
val LightText = Color(0xFF1A1917)
val LightTextMuted = Color(0xFF6B6860)
val LightTextFaint = Color(0xFFA8A49E)
val LightBorder = Color(0xFFE5E3DF)

// ─── Focus accent — derived containers ────────────────────────────────────────
val FocusDim = Color(0x267C9DFF)           // 15% opacity focus primary
val FocusContainer = Color(0xFF1B2550)     // dark background container
val FocusOnContainer = Color(0xFFC8D8FF)
val FocusOnPrimary = Color(0xFF000C2E)

// ─── Energy accent — derived containers ───────────────────────────────────────
val EnergyDim = Color(0x26FF9478)
val EnergyContainer = Color(0xFF4A1A00)
val EnergyOnContainer = Color(0xFFFFD8C8)
val EnergyOnPrimary = Color(0xFF2E0A00)

// ─── Calm accent — derived containers ─────────────────────────────────────────
val CalmDim = Color(0x26C4B0FF)
val CalmContainer = Color(0xFF2A1A50)
val CalmOnContainer = Color(0xFFE8D8FF)
val CalmOnPrimary = Color(0xFF100020)

// ─── Shared error ─────────────────────────────────────────────────────────────
val ErrorPrimary = Color(0xFFF47070)
val ErrorContainer = Color(0xFF500000)
val OnError = Color(0xFF2E0000)
val OnErrorContainer = Color(0xFFFFB0B0)
