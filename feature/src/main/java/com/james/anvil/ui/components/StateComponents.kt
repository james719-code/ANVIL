package com.james.anvil.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.james.anvil.ui.theme.DesignTokens
import com.james.anvil.ui.theme.ElectricTeal
import com.james.anvil.ui.theme.ErrorRed

/**
 * State Components for ANVIL
 * 
 * Provides consistent UI states across the app:
 * - LoadingState: Animated loading indicators
 * - ErrorState: Error displays with retry options
 * - SuccessState: Celebratory completion feedback
 * 
 * These components follow Material 3 guidelines and use ANVIL's design tokens.
 */

// ============================================================================
// LOADING STATES
// ============================================================================

/**
 * Full-screen loading indicator with optional message
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(DesignTokens.SpacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pulsing animation for the indicator
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = ElectricTeal,
                strokeWidth = 3.dp
            )
        }
        
        if (message != null) {
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Compact inline loading indicator for buttons or small areas
 */
@Composable
fun InlineLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Int = 20,
    color: Color = ElectricTeal
) {
    CircularProgressIndicator(
        modifier = modifier.size(size.dp),
        color = color,
        strokeWidth = 2.dp
    )
}

/**
 * Skeleton loading placeholder for content that's still loading
 */
@Composable
fun SkeletonLoader(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(DesignTokens.RadiusSmall))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
    )
}

/**
 * Skeleton card placeholder
 */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.RadiusMedium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.SpacingMd)
        ) {
            SkeletonLoader(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp)
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            SkeletonLoader(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
            SkeletonLoader(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(14.dp)
            )
        }
    }
}

// ============================================================================
// ERROR STATES
// ============================================================================

/**
 * Error type for different error scenarios
 */
enum class ErrorType {
    GENERIC,
    NETWORK,
    SERVER,
    EMPTY
}

/**
 * Full error state with retry option
 */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    errorType: ErrorType = ErrorType.GENERIC,
    onRetry: (() -> Unit)? = null
) {
    val icon = when (errorType) {
        ErrorType.GENERIC -> Icons.Outlined.ErrorOutline
        ErrorType.NETWORK -> Icons.Outlined.WifiOff
        ErrorType.SERVER -> Icons.Outlined.CloudOff
        ErrorType.EMPTY -> Icons.Default.Warning
    }
    
    val iconColor = when (errorType) {
        ErrorType.GENERIC -> ErrorRed
        ErrorType.NETWORK -> MaterialTheme.colorScheme.error
        ErrorType.SERVER -> MaterialTheme.colorScheme.error
        ErrorType.EMPTY -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(DesignTokens.SpacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Error icon with subtle animation
        val infiniteTransition = rememberInfiniteTransition(label = "error")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = iconColor.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Error",
                modifier = Modifier
                    .size(40.dp)
                    .alpha(alpha),
                tint = iconColor.copy(alpha = 0.8f)
            )
        }
        
        Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
            
            FilledTonalButton(
                onClick = onRetry,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(DesignTokens.SpacingXs))
                Text("Try Again")
            }
        }
    }
}

/**
 * Compact inline error message
 */
@Composable
fun InlineError(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = ErrorRed.copy(alpha = 0.1f),
                shape = RoundedCornerShape(DesignTokens.RadiusSmall)
            )
            .padding(DesignTokens.SpacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = ErrorRed,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = ErrorRed
        )
    }
}

// ============================================================================
// SUCCESS STATE
// ============================================================================

/**
 * Success celebration state with animation
 */
@Composable
fun SuccessState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Refresh,
    onContinue: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "success")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(DesignTokens.SpacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .background(
                    color = ElectricTeal.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Success",
                modifier = Modifier.size(40.dp),
                tint = ElectricTeal
            )
        }
        
        Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
        
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
        
        if (onContinue != null) {
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
            
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricTeal
                )
            ) {
                Text("Continue")
            }
        }
    }
}

// ============================================================================
// CONTENT STATE WRAPPER
// ============================================================================

/**
 * Wrapper composable that handles loading, error, empty, and success states
 * automatically based on the provided state.
 */
sealed class ContentState<out T> {
    data object Loading : ContentState<Nothing>()
    data class Error(val message: String, val type: ErrorType = ErrorType.GENERIC) : ContentState<Nothing>()
    data class Success<T>(val data: T) : ContentState<T>()
    data object Empty : ContentState<Nothing>()
}

@Composable
fun <T> ContentStateHandler(
    state: ContentState<T>,
    onRetry: (() -> Unit)? = null,
    emptyMessage: String = "No items found",
    emptyIcon: ImageVector = Icons.Default.Warning,
    loadingMessage: String? = null,
    content: @Composable (T) -> Unit
) {
    when (state) {
        is ContentState.Loading -> {
            LoadingState(message = loadingMessage)
        }
        is ContentState.Error -> {
            ErrorState(
                message = state.message,
                errorType = state.type,
                onRetry = onRetry
            )
        }
        is ContentState.Empty -> {
            EmptyState(
                message = emptyMessage,
                icon = emptyIcon
            )
        }
        is ContentState.Success -> {
            content(state.data)
        }
    }
}
