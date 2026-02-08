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
val ElectricBlue = Color(0xFF3B82F6)      // Primary actions, buttons, links
val ElectricTeal = Color(0xFF14B8A6)      // Secondary accent, success indicators
val ForgedGold = Color(0xFFD4A853)        // Financial highlights, rewards, premium

// ============================================
// EXTENDED BRAND PALETTE
// Variations for subtle backgrounds and states
// ============================================
val ForgedGoldDark = Color(0xFFB8923A)    // Pressed/active gold states
val ForgedGoldLight = Color(0xFFE8C878)   // Gold highlights, badges
val SteelBlue = Color(0xFF4A90A4)         // Alternative accent for variety

// ============================================
// SEMANTIC COLORS
// Universal feedback colors
// ============================================
val SuccessGreen = Color(0xFF10B981)      // Success, completion, positive
val ErrorRed = Color(0xFFEF4444)          // Errors, deletion, negative
val WarningOrange = Color(0xFFF59E0B)     // Warnings, attention needed
val InfoBlue = Color(0xFF3B82F6)          // Information, tips (same as primary)

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
val BackgroundDark = Color(0xFF0F172A)    // Main background (Slate 900)
val SurfaceDark = Color(0xFF1E293B)       // Card/container surfaces (Slate 800)
val SurfaceElevatedDark = Color(0xFF334155) // Elevated surfaces (Slate 700)
val TextPrimaryDark = Color(0xFFF1F5F9)   // Primary text (Slate 100)
val TextSecondaryDark = Color(0xFF94A3B8) // Secondary text (Slate 400)
val BorderDark = Color(0xFF334155)        // Borders/dividers (Slate 700)

// ============================================
// LIGHT THEME SURFACE COLORS
// Clean, professional light mode palette
// ============================================
val BackgroundLight = Color(0xFFF8FAFC)   // Main background (Slate 50)
val SurfaceLight = Color(0xFFFFFFFF)      // Card/container surfaces (White)
val SurfaceElevatedLight = Color(0xFFF1F5F9) // Elevated surfaces (Slate 100)
val TextPrimaryLight = Color(0xFF0F172A)  // Primary text (Slate 900)
val TextSecondaryLight = Color(0xFF64748B) // Secondary text (Slate 500)
val BorderLight = Color(0xFFE2E8F0)       // Borders/dividers (Slate 200)

// ============================================
// GRADIENT COLORS
// For special visual effects
// ============================================
val GradientStart = Color(0xFF2563EB)     // Blue 600
val GradientEnd = Color(0xFF0D9488)       // Teal 600
val ProgressTrackLight = Color(0x80FFFFFF)

// ============================================
// SPECIAL PURPOSE COLORS
// Lock screen and blocking UI
// ============================================
val CautionAmber = Color(0xFFFFB300)      // Warning in lock screen
val DarkGray = Color(0xFF121212)          // Lock screen background
val BorderGray = Color(0xFF333333)        // Lock screen borders

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
