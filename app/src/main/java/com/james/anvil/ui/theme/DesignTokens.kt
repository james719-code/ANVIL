package com.james.anvil.ui.theme

import androidx.compose.ui.unit.dp

/**
 * ANVIL Design Tokens - Single Source of Truth
 * 
 * All spacing, sizing, and styling values should come from this object.
 * Using these tokens ensures visual consistency across the app.
 * 
 * Usage: DesignTokens.SpacingLg or import with alias
 */
object DesignTokens {
    // ============================================
    // SPACING SCALE (based on 4dp grid)
    // Use for padding, margin, gaps
    // ============================================
    val SpacingXxs = 2.dp      // Minimal spacing
    val SpacingXs = 4.dp       // Tight spacing (icon-text)
    val SpacingSm = 8.dp       // Small spacing (list items)
    val SpacingMd = 12.dp      // Medium spacing (card content)
    val SpacingLg = 16.dp      // Large spacing (sections)
    val SpacingXl = 20.dp      // Screen padding
    val SpacingXxl = 24.dp     // Between major sections
    val SpacingXxxl = 32.dp    // Large section gaps
    val Spacing40 = 40.dp      // Hero spacing
    val Spacing48 = 48.dp      // Maximum spacing
    val Spacing56 = 56.dp      // Extra large
    val Spacing64 = 64.dp      // Maximum
    
    // ============================================
    // SEMANTIC SPACING
    // Named values for specific use cases
    // ============================================
    val PaddingScreen = SpacingXl        // 20dp - Default screen padding
    val PaddingCard = SpacingLg          // 16dp - Inside cards
    val PaddingCardLarge = SpacingXxl    // 24dp - Large card content
    val PaddingList = SpacingMd          // 12dp - List item padding
    val PaddingButton = SpacingMd        // 12dp - Button content padding
    val SectionSpacing = SpacingXxl      // 24dp - Between sections
    val ItemSpacing = SpacingMd          // 12dp - Between list items
    val IconTextSpacing = SpacingSm      // 8dp - Icon to text gap
    
    // ============================================
    // CORNER RADIUS
    // ============================================
    val RadiusNone = 0.dp
    val RadiusSmall = 8.dp     // Chips, small buttons
    val RadiusMedium = 12.dp   // Cards, inputs
    val RadiusLarge = 16.dp    // Dialogs, large cards
    val RadiusXLarge = 24.dp   // Bottom sheets
    val RadiusRound = 28.dp    // FAB, rounded buttons
    val RadiusFull = 100.dp    // Pills, circles
    
    // ============================================
    // ELEVATION
    // ============================================
    val ElevationNone = 0.dp
    val ElevationLow = 1.dp    // Subtle lift
    val ElevationMedium = 4.dp // Cards
    val ElevationHigh = 8.dp   // Dialogs, popovers
    val ElevationHighest = 16.dp // Modal sheets
    
    // ============================================
    // ICON SIZES
    // ============================================
    val IconSizeXs = 14.dp     // Inline with small text
    val IconSizeSmall = 16.dp  // Inline icons
    val IconSizeMedium = 24.dp // Default icons
    val IconSizeLarge = 32.dp  // Feature icons
    val IconSizeXLarge = 48.dp // Empty state icons
    val IconSizeHero = 64.dp   // Hero/splash icons
    
    // ============================================
    // COMPONENT HEIGHTS
    // ============================================
    val ButtonHeightSmall = 36.dp
    val ButtonHeightMedium = 44.dp
    val ButtonHeightLarge = 56.dp
    val BottomNavHeight = 80.dp
    val AppBarHeight = 56.dp
    val ListItemMinHeight = 56.dp
    val CardMinHeight = 80.dp
    
    // ============================================
    // WIDTHS
    // ============================================
    val DialogMinWidth = 280.dp
    val DialogMaxWidth = 560.dp
    val DialogMaxHeight = 600.dp
    val BottomSheetPeekHeight = 400.dp
    
    // ============================================
    // AVATAR SIZES
    // ============================================
    val AvatarSizeSmall = 32.dp
    val AvatarSizeMedium = 40.dp
    val AvatarSizeLarge = 56.dp
    
    // ============================================
    // BORDER
    // ============================================
    val BorderWidth = 1.dp
    val BorderWidthThick = 2.dp
    
    // ============================================
    // TOUCH TARGETS (Accessibility)
    // Minimum touch target size per Material guidelines
    // ============================================
    val TouchTargetMin = 48.dp
    
    // ============================================
    // ANIMATION DURATIONS (in milliseconds)
    // ============================================
    const val AnimDurationInstant = 100
    const val AnimDurationFast = 200
    const val AnimDurationMedium = 300
    const val AnimDurationSlow = 400
    const val AnimDurationSlower = 500
    
    // ============================================
    // ALPHA VALUES
    // ============================================
    const val AlphaDisabled = 0.38f
    const val AlphaHover = 0.08f
    const val AlphaPressed = 0.12f
    const val AlphaFocus = 0.12f
    const val AlphaSelected = 0.12f
    const val AlphaHighlight = 0.15f
    const val AlphaMuted = 0.6f
    const val AlphaSubtle = 0.7f
}
