package com.james.anvil.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import com.james.anvil.ui.theme.DesignTokens
import kotlinx.coroutines.delay

/**
 * Haptic Feedback & Micro-Interactions for ANVIL
 * 
 * Provides tactile feedback and smooth animations to enhance the user experience.
 * All haptic patterns are designed to be subtle and purposeful.
 * 
 * Usage:
 * ```
 * // In a composable
 * val haptics = rememberHapticFeedback()
 * 
 * Button(
 *     onClick = {
 *         haptics.performClick()
 *         // ... action
 *     },
 *     modifier = Modifier.bounceClick()
 * )
 * ```
 */

// ============================================================================
// HAPTIC FEEDBACK
// ============================================================================

/**
 * Haptic feedback types for different interactions
 */
enum class HapticType {
    /** Light tap for standard button presses */
    CLICK,
    /** Heavier feedback for important actions */
    HEAVY_CLICK,
    /** Double pulse for success confirmation */
    SUCCESS,
    /** Single strong pulse for errors */
    ERROR,
    /** Very light tick for selections/toggles */
    TICK,
    /** Keyboard-like feedback */
    KEYBOARD
}

/**
 * Haptic feedback controller that handles vibration on supported devices
 */
class HapticFeedback(private val view: View, private val context: Context) {
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    /**
     * Perform haptic feedback for the specified type
     */
    fun perform(type: HapticType) {
        when (type) {
            HapticType.CLICK -> performClick()
            HapticType.HEAVY_CLICK -> performHeavyClick()
            HapticType.SUCCESS -> performSuccess()
            HapticType.ERROR -> performError()
            HapticType.TICK -> performTick()
            HapticType.KEYBOARD -> performKeyboard()
        }
    }
    
    /**
     * Light click feedback - for standard button presses
     */
    fun performClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
    
    /**
     * Heavy click feedback - for important confirmations
     */
    fun performHeavyClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            vibrator?.let { vib ->
                if (vib.hasVibrator()) {
                    vib.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
    
    /**
     * Success feedback - double pulse
     */
    fun performSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.let { vib ->
                if (vib.hasVibrator()) {
                    vib.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 30, 50, 30),
                            intArrayOf(0, 100, 0, 150),
                            -1
                        )
                    )
                }
            }
        } else {
            performClick()
        }
    }
    
    /**
     * Error feedback - single strong pulse
     */
    fun performError() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.let { vib ->
                if (vib.hasVibrator()) {
                    vib.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
    
    /**
     * Light tick feedback - for toggles and selections
     */
    fun performTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
    
    /**
     * Keyboard feedback - very light
     */
    fun performKeyboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}

/**
 * Remember a HapticFeedback instance in a composable
 */
@Composable
fun rememberHapticFeedback(): HapticFeedback {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, context) { HapticFeedback(view, context) }
}

// ============================================================================
// MICRO-INTERACTIONS - MODIFIERS
// ============================================================================

/**
 * Bouncy click animation that scales down on press
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.95f
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounce_scale"
    )
    
    this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    try {
                        awaitRelease()
                    } finally {
                        isPressed = false
                    }
                }
            )
        }
}

/**
 * Subtle press-down animation with opacity change
 */
fun Modifier.pressIndicator(
    pressedAlpha: Float = 0.8f
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) pressedAlpha else 1f,
        animationSpec = tween(DesignTokens.AnimDurationFast),
        label = "press_alpha"
    )
    
    this
        .graphicsLayer { this.alpha = alpha }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    try {
                        awaitRelease()
                    } finally {
                        isPressed = false
                    }
                }
            )
        }
}

/**
 * Shake animation for error states or invalid input
 */
fun Modifier.shakeOnError(
    trigger: Boolean
): Modifier = composed {
    val shakeOffset by animateFloatAsState(
        targetValue = if (trigger) 1f else 0f,
        animationSpec = if (trigger) {
            keyframes {
                durationMillis = 400
                0f at 0
                -8f at 50
                8f at 100
                -6f at 150
                6f at 200
                -4f at 250
                4f at 300
                0f at 400
            }
        } else {
            tween(0)
        },
        label = "shake"
    )
    
    this.graphicsLayer {
        translationX = shakeOffset * 3
    }
}

/**
 * Pulse animation for highlighting new/updated content
 */
fun Modifier.pulseAnimation(
    enabled: Boolean = true,
    scale: Float = 1.05f
): Modifier = composed {
    if (!enabled) return@composed this
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = scale,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    this.scale(pulseScale)
}

// ============================================================================
// CELEBRATION EFFECTS
// ============================================================================

/**
 * State holder for celebration animations
 */
@Composable
fun rememberCelebrationState(): CelebrationState {
    return remember { CelebrationState() }
}

class CelebrationState {
    var isShowing by mutableStateOf(false)
        private set
    
    var message by mutableStateOf("")
        private set
    
    fun show(message: String = "Great job! 🎉") {
        this.message = message
        this.isShowing = true
    }
    
    fun hide() {
        isShowing = false
    }
}

/**
 * Compact celebration overlay that can be shown briefly for task completion
 */
@Composable
fun CelebrationOverlay(
    state: CelebrationState,
    durationMs: Int = 2000
) {
    LaunchedEffect(state.isShowing) {
        if (state.isShowing) {
            delay(durationMs.toLong())
            state.hide()
        }
    }
    
    AnimatedVisibility(
        visible = state.isShowing,
        enter = scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = fadeOut(
            animationSpec = tween(300)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(DesignTokens.RadiusLarge),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = DesignTokens.ElevationHigh
            ) {
                Text(
                    text = state.message,
                    modifier = Modifier.padding(
                        horizontal = DesignTokens.SpacingLg,
                        vertical = DesignTokens.SpacingMd
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
