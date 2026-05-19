package com.james.anvil.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ANVIL Design System - Unified Color Palette
 * 
 * The ANVIL color system is built around the "forge" metaphor with
 * Electric Blue as the primary action color and Forged Gold for
 * premium/financial highlights.
 * 
 * Usage Guidelines:
 * - Primary (ElectricBlue): Main actions, navigation, links
 * - Secondary (ElectricTeal): Secondary actions, success states
 * - Accent (ForgedGold): Financial elements, rewards, premium features
 * - Semantic colors: Success, Error, Warning for feedback states
 */

// ============================================
// PRIMARY BRAND COLORS
// Core brand identity colors used throughout the app
// ============================================
val EmberOrange = Color(0xFFF25C19)       // Primary actions and emphasis (vibrant forge ember)
val EmberSoft = Color(0xFFFFA27A)         // Warm highlight for hero elements
val SteelBlue = Color(0xFF5F758E)         // Cool structural accent
val ElectricBlue = Color(0xFF2563EB)      // Info states and secondary emphasis (vibrant electric blue)
val ElectricTeal = Color(0xFFBD7447)      // Warm bronze secondary accent
val ForgedGold = Color(0xFFE5A93C)        // Financial highlights, rewards, premium

// ============================================
// EXTENDED BRAND PALETTE
// Variations for subtle backgrounds and states
// ============================================
val ForgedGoldDark = Color(0xFFC28725)    // Pressed/active gold states
val ForgedGoldLight = Color(0xFFF3C775)   // Gold highlights, badges

// ============================================
// SEMANTIC COLORS
// Universal feedback colors
// ============================================
val SuccessGreen = Color(0xFF10B981)      // Success, completion, positive (emerald green)
val ErrorRed = Color(0xFFEF4444)          // Errors, deletion, negative (vibrant red)
val WarningOrange = Color(0xFFF97316)     // Warnings, attention needed (vibrant orange)
val InfoBlue = Color(0xFF3B82F6)          // Information, tips (vibrant blue)

// ============================================
// FINANCIAL SEMANTIC COLORS
// Standardized colors for financial UI elements
// ============================================
val GcashBlue = Color(0xFF007DFE)         // GCash wallet indicator
val CashTeal = ElectricTeal               // Cash wallet indicator (alias)
val LiabilityRed = Color(0xFFFFB3B3)      // Outstanding debts, negative balance
val HighPriority = Color(0xFFE53935)      // High priority tasks/items

// ============================================
// DARK THEME SURFACE COLORS
// Slate-based palette for dark mode
// ============================================
val BackgroundDark = Color(0xFF111315)    // Main background
val SurfaceDark = Color(0xFF191C20)       // Card/container surfaces
val SurfaceElevatedDark = Color(0xFF23282E) // Elevated surfaces
val TextPrimaryDark = Color(0xFFF3F2EF)   // Primary text
val TextSecondaryDark = Color(0xFFA19D95) // Secondary text
val BorderDark = Color(0xFF2C3138)        // Borders/dividers

// ============================================
// LIGHT THEME SURFACE COLORS
// Clean, professional light mode palette
// ============================================
val BackgroundLight = Color(0xFFF4F1EB)   // Main background
val SurfaceLight = Color(0xFFFFFCF7)      // Card/container surfaces
val SurfaceElevatedLight = Color(0xFFEDE7DE) // Elevated surfaces
val TextPrimaryLight = Color(0xFF201D19)  // Primary text
val TextSecondaryLight = Color(0xFF6C665D) // Secondary text
val BorderLight = Color(0xFFDCD2C4)       // Borders/dividers

// ============================================
// GRADIENT COLORS
// For special visual effects
// ============================================
val GradientStart = Color(0xFF34251D)     // Warm graphite
val GradientEnd = Color(0xFF1E2228)       // Steel slate
val ProgressTrackLight = Color(0x80FFFFFF)

// ============================================
// SPECIAL PURPOSE COLORS
// Lock screen and blocking UI
// ============================================
val CautionAmber = Color(0xFFFFB300)      // Warning in lock screen
val DarkGray = Color(0xFF121212)          // Lock screen background
val BorderGray = Color(0xFF333333)        // Lock screen borders

// ============================================
// XP & LEVELING COLORS
// Forge-themed progression UI
// ============================================
val XpGold = Color(0xFFFFD700)            // XP amount text, level badge glow
val XpBarFill = Color(0xFFD4A853)         // XP progress bar fill (matches ForgedGold)
val XpBarTrack = Color(0xFF2A2A3E)        // XP progress bar background track
val LevelBadgeBg = Color(0xFF1A1A2E)      // Level badge circle background

// ============================================
// LEGACY ALIASES (Deprecated - use above instead)
// Kept for backward compatibility during migration
// ============================================
@Deprecated("Use TextPrimaryDark instead", ReplaceWith("TextPrimaryDark"))
val TextWhite = TextPrimaryDark
@Deprecated("Use TextPrimaryLight instead", ReplaceWith("TextPrimaryLight"))
val TextBlack = TextPrimaryLight
@Deprecated("Use ElectricTeal or SuccessGreen instead")
val DeepTeal = Color(0xFF00897B)
@Deprecated("Use ElectricTeal.copy(alpha) instead")
val MutedTeal = Color(0xFF4DB6AC)
@Deprecated("Use ElectricTeal.copy(alpha) instead")
val LightTeal = Color(0xFF80CBC4)
@Deprecated("Use SurfaceLight or BackgroundLight instead")
val PaleTeal = Color(0xFFE0F2F1)
@Deprecated("Use ElectricBlue instead")
val DeepBlue = Color(0xFF1565C0)
@Deprecated("Use ElectricBlue instead")
val OceanBlue = Color(0xFF0288D1)
@Deprecated("Use ElectricBlue instead")  
val SkyBlue = Color(0xFF03A9F4)
@Deprecated("Use SurfaceLight or BackgroundLight instead")
val PaleBlue = Color(0xFFE1F5FE)
@Deprecated("Use BackgroundDark instead")
val Charcoal = Color(0xFF0D1117)
@Deprecated("Use BackgroundLight instead")
val PaperWhite = Color(0xFFF6F8FA)
@Deprecated("Use SteelBlue instead")
val SteelBlueDark = Color(0xFF3A7286)
@Deprecated("Use SteelBlue instead")
val SteelBlueLight = Color(0xFF5FADC4)
@Deprecated("Use SurfaceDark instead")
val IndustrialGrey = Color(0xFF1A1A1A)
@Deprecated("Use SurfaceElevatedDark instead")
val IndustrialGreyLight = Color(0xFF2A2A2A)
@Deprecated("Use BackgroundDark instead")
val IndustrialGreyDark = Color(0xFF0F0F0F)
@Deprecated("Use BorderDark instead")
val IndustrialBorder = Color(0xFF333333)
