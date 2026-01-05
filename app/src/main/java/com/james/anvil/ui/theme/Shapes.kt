package com.james.anvil.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Design system shape tokens for consistent corner radii.
 */
object AnvilShapes {
    // Corner radii scale
    val radiusNone = 0.dp
    val radiusSmall = 8.dp
    val radiusMedium = 12.dp
    val radiusLarge = 16.dp
    val radiusXLarge = 24.dp
    val radiusFull = 100.dp
    
    // Semantic shapes
    val card = RoundedCornerShape(radiusMedium)
    val cardSmall = RoundedCornerShape(radiusSmall)
    val cardLarge = RoundedCornerShape(radiusLarge)
    
    val button = RoundedCornerShape(radiusMedium)
    val buttonSmall = RoundedCornerShape(radiusSmall)
    val buttonPill = RoundedCornerShape(radiusFull)
    
    val chip = RoundedCornerShape(radiusSmall)
    val badge = RoundedCornerShape(radiusFull)
    
    val bottomSheet = RoundedCornerShape(
        topStart = radiusXLarge,
        topEnd = radiusXLarge,
        bottomStart = radiusNone,
        bottomEnd = radiusNone
    )
    
    val dialog = RoundedCornerShape(radiusXLarge)
    
    val iconContainer = RoundedCornerShape(radiusMedium)
    val iconContainerSmall = RoundedCornerShape(radiusSmall)
}

/**
 * Material3 Shapes configuration for the app theme.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
