package com.james.anvil.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Design tokens for consistent spacing, sizing, and styling across the app.
 * Using a centralized object makes it easy to maintain visual consistency.
 */
object DesignTokens {
    // ============================================
    // SPACING
    // ============================================
    val SpacingXxs = 2.dp
    val SpacingXs = 4.dp
    val SpacingSm = 8.dp
    val SpacingMd = 12.dp
    val SpacingLg = 16.dp
    val SpacingXl = 24.dp
    val SpacingXxl = 32.dp
    val SpacingXxxl = 48.dp
    
    // ============================================
    // PADDING
    // ============================================
    val PaddingScreen = 16.dp
    val PaddingCard = 16.dp
    val PaddingCardLarge = 24.dp
    val PaddingList = 12.dp
    val PaddingButton = 12.dp
    
    // ============================================
    // CORNER RADIUS
    // ============================================
    val RadiusSmall = 8.dp
    val RadiusMedium = 12.dp
    val RadiusLarge = 16.dp
    val RadiusXLarge = 24.dp
    val RadiusRound = 28.dp
    val RadiusFull = 50.dp
    
    // ============================================
    // ELEVATION
    // ============================================
    val ElevationNone = 0.dp
    val ElevationLow = 2.dp
    val ElevationMedium = 4.dp
    val ElevationHigh = 8.dp
    val ElevationHighest = 16.dp
    
    // ============================================
    // ICON SIZES
    // ============================================
    val IconSizeSmall = 16.dp
    val IconSizeMedium = 24.dp
    val IconSizeLarge = 32.dp
    val IconSizeXLarge = 48.dp
    
    // ============================================
    // COMPONENT HEIGHTS
    // ============================================
    val ButtonHeightSmall = 36.dp
    val ButtonHeightMedium = 44.dp
    val ButtonHeightLarge = 56.dp
    val BottomNavHeight = 80.dp
    val AppBarHeight = 56.dp
    val DialogMaxHeight = 600.dp
    
    // ============================================
    // WIDTHS
    // ============================================
    val DialogMinWidth = 280.dp
    val DialogMaxWidth = 560.dp
    val BottomSheetPeekHeight = 400.dp
    
    // ============================================
    // ANIMATION DURATIONS (in milliseconds)
    // ============================================
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
}
