package com.james.anvil.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.james.anvil.ui.theme.DesignTokens
import com.james.anvil.ui.theme.ElectricTeal

/**
 * Interactive Onboarding System for ANVIL
 * 
 * Provides an overlay-based tutorial with tooltips that highlight specific
 * UI elements and guide users through the app's features.
 * 
 * Usage:
 * ```
 * OnboardingOverlay(
 *     isVisible = showOnboarding,
 *     steps = listOf(
 *         OnboardingStep(
 *             title = "Welcome to Dashboard",
 *             description = "Your daily progress at a glance",
 *             targetBounds = dashboardBounds
 *         ),
 *         // More steps...
 *     ),
 *     onComplete = { showOnboarding = false }
 * )
 * ```
 */

data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector? = null,
    val targetPosition: Offset = Offset.Zero,
    val targetSize: IntSize = IntSize.Zero,
    val tooltipPosition: TooltipPosition = TooltipPosition.BELOW
)

enum class TooltipPosition {
    ABOVE,
    BELOW,
    LEFT,
    RIGHT
}

/**
 * Full-screen onboarding overlay with spotlight effect
 */
@Composable
fun OnboardingOverlay(
    isVisible: Boolean,
    steps: List<OnboardingStep>,
    currentStep: Int,
    onStepComplete: () -> Unit,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && steps.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(DesignTokens.AnimDurationMedium)),
        exit = fadeOut(animationSpec = tween(DesignTokens.AnimDurationMedium)),
        modifier = modifier
    ) {
        val step = steps.getOrNull(currentStep) ?: return@AnimatedVisibility
        val isLastStep = currentStep == steps.lastIndex
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume clicks */ }
        ) {
            // Skip button in top-right
            IconButton(
                onClick = onSkip,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(DesignTokens.SpacingMd)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Skip tutorial",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
            
            // Tooltip card
            TooltipCard(
                step = step,
                currentStep = currentStep + 1,
                totalSteps = steps.size,
                isLastStep = isLastStep,
                onNext = {
                    if (isLastStep) {
                        onComplete()
                    } else {
                        onStepComplete()
                    }
                },
                modifier = Modifier.align(Alignment.Center)
            )
            
            // Step indicator dots at bottom
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = DesignTokens.SpacingXl),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXs)
            ) {
                steps.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentStep) ElectricTeal
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun TooltipCard(
    step: OnboardingStep,
    currentStep: Int,
    totalSteps: Int,
    isLastStep: Boolean,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = DesignTokens.SpacingLg)
            .fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.RadiusLarge),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = DesignTokens.ElevationHigh
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.SpacingLg)
        ) {
            // Step counter
            Text(
                text = "Step $currentStep of $totalSteps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            
            // Title with optional icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                step.icon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ElectricTeal,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                }
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            
            // Description
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
            
            // Next/Complete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = onNext,
                    shape = RoundedCornerShape(DesignTokens.RadiusFull),
                    color = ElectricTeal,
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = DesignTokens.SpacingLg,
                            vertical = DesignTokens.SpacingSm
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isLastStep) "Get Started" else "Next",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(DesignTokens.SpacingXs))
                        Icon(
                            imageVector = if (isLastStep) Icons.Default.Check 
                                         else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modifier extension to capture element bounds for onboarding targeting
 */
fun Modifier.onboardingTarget(
    onBoundsChange: (Offset, IntSize) -> Unit
): Modifier = this.onGloballyPositioned { coordinates ->
    val bounds = coordinates.boundsInRoot()
    onBoundsChange(
        Offset(bounds.left, bounds.top),
        IntSize(bounds.width.toInt(), bounds.height.toInt())
    )
}

/**
 * Pre-defined onboarding steps for ANVIL
 */
object AnvilOnboardingSteps {
    
    fun getDashboardSteps(): List<OnboardingStep> = listOf(
        OnboardingStep(
            title = "Welcome to ANVIL! 🔨",
            description = "Your personal productivity forge. Let's take a quick tour of the main features.",
            icon = null
        ),
        OnboardingStep(
            title = "Daily Progress",
            description = "Track your daily task completion with the motivation card. It shows your progress and a daily quote to keep you inspired.",
            icon = null
        ),
        OnboardingStep(
            title = "Quick Stats",
            description = "See your pending tasks, completed tasks, and bonus tasks at a glance. Tap any stat to jump to that section.",
            icon = null
        ),
        OnboardingStep(
            title = "Budget & Shield",
            description = "Monitor your finances and app blocklist from the dashboard. Quick access to your most important tools.",
            icon = null
        )
    )
    
    fun getTasksSteps(): List<OnboardingStep> = listOf(
        OnboardingStep(
            title = "Task Management",
            description = "Create daily tasks and bonus tasks. Daily tasks are your main priorities, while bonus tasks earn rewards.",
            icon = null
        ),
        OnboardingStep(
            title = "Complete Tasks",
            description = "Tap a task to mark it complete. Complete bonus tasks to earn grace days that can protect your streak.",
            icon = null
        )
    )
    
    fun getBudgetSteps(): List<OnboardingStep> = listOf(
        OnboardingStep(
            title = "Track Your Money",
            description = "Log income and expenses across Cash and GCash accounts. See your total balance at the top.",
            icon = null
        ),
        OnboardingStep(
            title = "Manage Loans",
            description = "Track money you've lent to others. Add repayments as they're made to keep everything organized.",
            icon = null
        )
    )
    
    fun getBlocklistSteps(): List<OnboardingStep> = listOf(
        OnboardingStep(
            title = "Focus Mode",
            description = "Block distracting apps and websites to stay focused. Enable the Accessibility Service for blocking to work.",
            icon = null
        ),
        OnboardingStep(
            title = "Schedule Blocks",
            description = "Set up blocking schedules to automatically enable focus mode during work hours.",
            icon = null
        )
    )
    
    fun getFullOnboarding(): List<OnboardingStep> = listOf(
        OnboardingStep(
            title = "Welcome to ANVIL! 🔨",
            description = "Your personal productivity forge. ANVIL helps you manage tasks, track finances, and stay focused by blocking distractions.",
            icon = null
        ),
        OnboardingStep(
            title = "Navigation",
            description = "Swipe left and right to navigate between screens, or tap the icons in the bottom bar. It's that simple!",
            icon = null
        ),
        OnboardingStep(
            title = "Dashboard",
            description = "Your command center. See today's progress, quick stats, and access all features from one place.",
            icon = null
        ),
        OnboardingStep(
            title = "Tasks",
            description = "Create daily tasks for your main priorities. Complete bonus tasks to earn grace days that protect your streak.",
            icon = null
        ),
        OnboardingStep(
            title = "Budget",
            description = "Track your money across Cash and GCash. Log income, expenses, and loans to stay on top of your finances.",
            icon = null
        ),
        OnboardingStep(
            title = "Blocklist",
            description = "Stay focused by blocking distracting apps and websites. Set up schedules for automatic focus mode.",
            icon = null
        ),
        OnboardingStep(
            title = "Ready to Forge Ahead!",
            description = "You're all set! Start by adding your first task or exploring the app. Access Settings anytime from the Dashboard.",
            icon = null
        )
    )
}
