package com.james.anvil.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.james.anvil.ui.theme.AnvilShapes
import com.james.anvil.ui.theme.Dimens

/**
 * Shimmer effect for loading states.
 * Creates an animated gradient that moves horizontally across the content.
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    widthOfShadowBrush: Int = 500,
    angleOfAxisY: Float = 270f,
    durationMillis: Int = 1000
): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = (durationMillis + widthOfShadowBrush).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnimation - widthOfShadowBrush, y = 0.0f),
        end = Offset(x = translateAnimation, y = angleOfAxisY),
    )
}

/**
 * A shimmer placeholder box for loading content.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 20.dp,
    shape: RoundedCornerShape = AnvilShapes.cardSmall
) {
    val brush = ShimmerEffect()
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(brush)
    )
}

/**
 * Shimmer loading state for a task list item.
 */
@Composable
fun ShimmerTaskItem(modifier: Modifier = Modifier) {
    val brush = ShimmerEffect()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AnvilShapes.card)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Dimens.cardPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.itemSpacing)
    ) {
        // Checkbox placeholder
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(brush)
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacing8)
        ) {
            // Title
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(18.dp)
                    .clip(AnvilShapes.cardSmall)
                    .background(brush)
            )
            // Subtitle
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp)
                    .clip(AnvilShapes.cardSmall)
                    .background(brush)
            )
        }
        
        // Action icons placeholder
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )
    }
}

/**
 * Shimmer loading state for a card.
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    val brush = ShimmerEffect()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(AnvilShapes.card)
            .background(brush)
    )
}

/**
 * Shimmer loading list with multiple items.
 */
@Composable
fun ShimmerTaskList(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.itemSpacing)
    ) {
        repeat(itemCount) {
            ShimmerTaskItem()
        }
    }
}
