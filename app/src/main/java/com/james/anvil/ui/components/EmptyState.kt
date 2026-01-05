package com.james.anvil.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Add
import com.james.anvil.ui.theme.AnvilShapes
import com.james.anvil.ui.theme.Dimens

/**
 * Enhanced empty state with optional action button and subtle animation.
 */
@Composable
fun EmptyState(
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    // Subtle breathing animation for the icon
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_alpha"
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.spacing32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon with background circle
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(scale)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .alpha(alpha),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(Dimens.spacing24))
        
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(Dimens.spacing8))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(Dimens.spacing24))
            FilledTonalButton(
                onClick = onAction,
                shape = AnvilShapes.button
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Dimens.spacing8))
                Text(actionLabel)
            }
        }
    }
}

// =============================================
// Preview Functions (Removed in Release Builds)
// =============================================

@Preview(name = "Empty State - Tasks", showBackground = true)
@Composable
private fun EmptyStateTasksPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = false) {
        EmptyState(
            message = "No pending tasks. You are free.",
            icon = Icons.Default.Check
        )
    }
}

@Preview(name = "Empty State - Blocked Links", showBackground = true)
@Composable
private fun EmptyStateLinksPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = false) {
        EmptyState(
            message = "No blocked links.",
            icon = Icons.Default.Lock
        )
    }
}

@Preview(name = "Empty State - Dark", showBackground = true)
@Composable
private fun EmptyStateDarkPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = true) {
        EmptyState(
            message = "No items to display",
            icon = Icons.Default.Check
        )
    }
}
