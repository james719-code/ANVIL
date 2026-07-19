package com.james.anvil.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.james.anvil.ui.theme.DesignTokens

@Composable
fun Modifier.staggeredItemAnimation(index: Int): Modifier {
    var target by remember { mutableFloatStateOf(0f) }
    val animatedAlpha by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = DesignTokens.AnimDurationMedium,
            delayMillis = index * 50
        ),
        label = "staggered_alpha"
    )
    LaunchedEffect(Unit) { target = 1f }
    return this.alpha(animatedAlpha)
}

@Composable
fun AnimatedContentState(
    visible: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(DesignTokens.AnimDurationFast)) +
                slideInVertically(tween(DesignTokens.AnimDurationFast)) { it / 2 },
        exit = fadeOut(tween(DesignTokens.AnimDurationFast)) +
                slideOutVertically(tween(DesignTokens.AnimDurationFast)) { it / 2 },
        content = content
    )
}

@Composable
fun animateCounterAsState(target: Int, duration: Int = 800): Int {
    val value by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = duration),
        label = "counter_animation"
    )
    return value
}

@Composable
fun animateFloatCounterAsState(target: Float, duration: Int = 800): Float {
    val value by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = duration),
        label = "float_counter_animation"
    )
    return value
}
