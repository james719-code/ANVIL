package com.james.anvil.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// PRIMARY ACCENT  (ember on graphite)
// ============================================
val PrimaryAccent = Color(0xFFC8431A)       // ember: 4.9:1 on white, safe for text and fills in light theme
val PrimaryAccentLight = Color(0xFFFF8A57)  // glowing ember: primary in dark theme
val PrimaryAccentDark = Color(0xFF8F2D0E)

// ============================================
// SEMANTIC COLORS
// ============================================
val SuccessGreen = Color(0xFF10B981)
val ErrorRed = Color(0xFFEF4444)
val WarningOrange = Color(0xFFF97316)

// ============================================
// DARK THEME SURFACE COLORS  (graphite, a touch cool so ember pops)
// ============================================
val BackgroundDark = Color(0xFF0F1113)
val SurfaceDark = Color(0xFF181B1E)
val SurfaceElevatedDark = Color(0xFF22262A)
val TextPrimaryDark = Color(0xFFF2F1ED)
val TextSecondaryDark = Color(0xFFA7ABB0)
val BorderDark = Color(0xFF2C3136)

// ============================================
// LIGHT THEME SURFACE COLORS  (warm stone)
// ============================================
val BackgroundLight = Color(0xFFF4F3EF)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceElevatedLight = Color(0xFFEAE8E2)
val TextPrimaryLight = Color(0xFF17191B)
val TextSecondaryLight = Color(0xFF5B5F64)
val BorderLight = Color(0xFFE0DED7)

// ============================================
// HERO / FORGE SURFACE  (same in both themes: the glowing "furnace" card)
// ============================================
val ForgeInk = Color(0xFF1A1D21)
val ForgeInkRaised = Color(0xFF262A2F)
val ForgePaper = Color(0xFFF7F4EC)
val ForgePaperMuted = Color(0xFFB9BDC2)
val ForgeEmber = Color(0xFFFF8A57)

// ============================================
// LOCK SCREEN COLORS
// ============================================
val LockScreenBg = Color(0xFF0A0A0A)
val LockScreenBorder = Color(0xFF3B403A)
val LockScreenWarning = Color(0xFFFFB300)

// ============================================
// LEGACY ALIASES — all map to PrimaryAccent
// ============================================
val EmberOrange = PrimaryAccent
val EmberSoft = PrimaryAccentLight
val SteelBlue = PrimaryAccent
val ElectricBlue = PrimaryAccent
val ElectricTeal = PrimaryAccentLight
val ForgedGold = PrimaryAccent
val ForgedGoldDark = PrimaryAccentDark
val ForgedGoldLight = PrimaryAccentLight
val XpGold = PrimaryAccent
val XpBarFill = PrimaryAccent
val XpBarTrack = SurfaceElevatedDark
val LevelBadgeBg = SurfaceDark
val GcashBlue = PrimaryAccent
val CashTeal = PrimaryAccent
val LiabilityRed = ErrorRed
val HighPriority = ErrorRed
val InfoBlue = PrimaryAccent
val GradientStart = SurfaceDark
val GradientEnd = SurfaceElevatedDark
val ProgressTrackLight = SurfaceElevatedDark
val CautionAmber = LockScreenWarning
val DarkGray = LockScreenBg
val BorderGray = LockScreenBorder
val DeepTeal = PrimaryAccent
val MutedTeal = PrimaryAccentLight
val IndustrialGrey = SurfaceDark
val IndustrialGreyLight = SurfaceElevatedDark
val IndustrialBorder = BorderDark
val SteelBlueDark = PrimaryAccentDark
val SteelBlueLight = PrimaryAccentLight
