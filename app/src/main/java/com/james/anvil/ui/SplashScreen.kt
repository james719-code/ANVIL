package com.james.anvil.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.james.anvil.R
import com.james.anvil.ui.theme.DesignTokens
import kotlinx.coroutines.delay

/**
 * Splash screen composable that displays while the app preloads content.
 * Shows the app logo with a horizontal progress bar.
 * 
 * @param progress Current loading progress (0f to 1f)
 * @param isLoading Whether the app is still loading
 * @param onSplashComplete Callback when splash animation is complete and loading is done
 */
@Composable
fun SplashScreen(
    progress: Float,
    isLoading: Boolean,
    onSplashComplete: () -> Unit
) {
    var showSplash by remember { mutableStateOf(true) }
    var hasMinTimeElapsed by remember { mutableStateOf(false) }
    
    // Ensure minimum splash screen time for branding visibility
    LaunchedEffect(Unit) {
        delay(1500) // Minimum 1.5s splash time
        hasMinTimeElapsed = true
    }
    
    // Complete splash when loading is done AND minimum time has elapsed
    LaunchedEffect(isLoading, hasMinTimeElapsed) {
        if (!isLoading && hasMinTimeElapsed) {
            delay(300) // Brief pause before exit animation
            showSplash = false
            delay(DesignTokens.AnimDurationSlow.toLong())
            onSplashComplete()
        }
    }
    
    AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(DesignTokens.AnimDurationSlow)) + 
               slideOutVertically(
                   targetOffsetY = { -it / 2 },
                   animationSpec = tween(DesignTokens.AnimDurationSlow)
               )
    ) {
        SplashContent(progress = progress)
    }
}

@Composable
private fun SplashContent(progress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "splashPulse")
    
    // Subtle pulse animation for the logo
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Animate progress for smooth transition
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(DesignTokens.PaddingScreen)
        ) {
            // Logo Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(pulseScale)
            ) {
                // Official App Icon Logo Container (Shadow removed)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(DesignTokens.RadiusXLarge)),
                    contentAlignment = Alignment.Center
                ) {
                    // Layered Adaptive Icon
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_background),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "ANVIL Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXxl))
            
            // App Name
            Text(
                text = "ANVIL",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            
            // Tagline
            Text(
                text = "Forge Your Productivity",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXxxl))
            
            // Progress Bar Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.SpacingXxl)
            ) {
                // Horizontal Progress Bar with rounded corners
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(DesignTokens.RadiusFull)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round
                )
                
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                
                // Loading text
                Text(
                    text = "Loading ${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
