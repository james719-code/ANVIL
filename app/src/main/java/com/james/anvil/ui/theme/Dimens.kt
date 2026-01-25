package com.james.anvil.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Legacy spacing aliases for backward compatibility.
 * 
 * @deprecated Use DesignTokens directly for new code.
 * This object provides aliases that map to DesignTokens values.
 */
object Dimens {
    // Base spacing scale - aliases to DesignTokens
    val spacing2 = DesignTokens.SpacingXxs
    val spacing4 = DesignTokens.SpacingXs
    val spacing8 = DesignTokens.SpacingSm
    val spacing12 = DesignTokens.SpacingMd
    val spacing16 = DesignTokens.SpacingLg
    val spacing20 = DesignTokens.SpacingXl
    val spacing24 = DesignTokens.SpacingXxl
    val spacing32 = DesignTokens.SpacingXxxl
    val spacing40 = DesignTokens.Spacing40
    val spacing48 = DesignTokens.Spacing48
    val spacing56 = DesignTokens.Spacing56
    val spacing64 = DesignTokens.Spacing64
    
    // Semantic spacing
    val screenPadding = DesignTokens.PaddingScreen
    val cardPadding = DesignTokens.PaddingCard
    val listItemPadding = DesignTokens.PaddingList
    val sectionSpacing = DesignTokens.SectionSpacing
    val itemSpacing = DesignTokens.ItemSpacing
    val iconTextSpacing = DesignTokens.IconTextSpacing
    val buttonPadding = DesignTokens.PaddingButton
    
    // Component sizes
    val iconSizeSmall = DesignTokens.IconSizeSmall
    val iconSizeMedium = DesignTokens.IconSizeMedium
    val iconSizeLarge = DesignTokens.IconSizeLarge
    val iconSizeXLarge = DesignTokens.IconSizeXLarge
    
    val avatarSizeSmall = DesignTokens.AvatarSizeSmall
    val avatarSizeMedium = DesignTokens.AvatarSizeMedium
    val avatarSizeLarge = DesignTokens.AvatarSizeLarge
    
    val buttonHeight = DesignTokens.ButtonHeightLarge
    val buttonHeightSmall = DesignTokens.ButtonHeightSmall
    
    val cardMinHeight = DesignTokens.CardMinHeight
    val listItemMinHeight = DesignTokens.ListItemMinHeight
    
    // Border & stroke
    val borderWidth = DesignTokens.BorderWidth
    val borderWidthThick = DesignTokens.BorderWidthThick
    
    // Elevation
    val elevationNone = DesignTokens.ElevationNone
    val elevationLow = DesignTokens.ElevationLow
    val elevationMedium = DesignTokens.ElevationMedium
    val elevationHigh = DesignTokens.ElevationHigh
}
