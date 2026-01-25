package com.james.anvil.ui.theme

import android.app.Activity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adaptive Layout Utilities for ANVIL
 * 
 * Provides responsive design support for phones, tablets, foldables, and desktop.
 * Uses Material3 WindowSizeClass to determine appropriate layouts.
 * 
 * Usage:
 * ```
 * val windowInfo = LocalWindowInfo.current
 * when (windowInfo.screenWidthType) {
 *     WindowInfo.WindowType.Compact -> PhoneLayout()
 *     WindowInfo.WindowType.Medium -> TabletLayout()
 *     WindowInfo.WindowType.Expanded -> DesktopLayout()
 * }
 * ```
 */

/**
 * Local composition providing window information throughout the app
 */
val LocalWindowInfo = compositionLocalOf<WindowInfo> { 
    error("No WindowInfo provided. Wrap your composable with ProvideWindowInfo.") 
}

/**
 * Information about the current window size and type
 */
data class WindowInfo(
    val screenWidthType: WindowType,
    val screenHeightType: WindowType,
    val screenWidth: Dp,
    val screenHeight: Dp,
    val isLandscape: Boolean
) {
    enum class WindowType {
        /** Phone in portrait: < 600dp width */
        Compact,
        /** Tablet portrait / Phone landscape / Foldable unfolded: 600-840dp width */
        Medium,
        /** Tablet landscape / Desktop: > 840dp width */
        Expanded
    }
    
    /**
     * Whether the layout should use a two-pane/split approach
     * True for medium/expanded widths in landscape, or expanded widths in portrait
     */
    val shouldShowTwoPane: Boolean
        get() = screenWidthType == WindowType.Expanded || 
                (screenWidthType == WindowType.Medium && isLandscape)
    
    /**
     * Whether the layout should use a rail navigation instead of bottom nav
     * True for expanded width screens
     */
    val shouldShowNavRail: Boolean
        get() = screenWidthType == WindowType.Expanded
    
    /**
     * Suggested number of columns for grid layouts
     */
    val suggestedGridColumns: Int
        get() = when (screenWidthType) {
            WindowType.Compact -> 2
            WindowType.Medium -> 3
            WindowType.Expanded -> 4
        }
    
    /**
     * Whether content padding should be increased for larger screens
     */
    val contentPadding: Dp
        get() = when (screenWidthType) {
            WindowType.Compact -> DesignTokens.SpacingMd
            WindowType.Medium -> DesignTokens.SpacingLg
            WindowType.Expanded -> DesignTokens.SpacingXl
        }
    
    /**
     * Maximum content width to prevent overly wide content on large screens
     */
    val maxContentWidth: Dp
        get() = when (screenWidthType) {
            WindowType.Compact -> Dp.Unspecified
            WindowType.Medium -> 840.dp
            WindowType.Expanded -> 1200.dp
        }
}

/**
 * Calculate WindowInfo from Material3 WindowSizeClass
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun calculateWindowInfo(activity: Activity): WindowInfo {
    val windowSizeClass = calculateWindowSizeClass(activity)
    val configuration = LocalConfiguration.current
    
    return remember(windowSizeClass, configuration) {
        WindowInfo(
            screenWidthType = when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> WindowInfo.WindowType.Compact
                WindowWidthSizeClass.Medium -> WindowInfo.WindowType.Medium
                WindowWidthSizeClass.Expanded -> WindowInfo.WindowType.Expanded
                else -> WindowInfo.WindowType.Compact
            },
            screenHeightType = when (windowSizeClass.heightSizeClass) {
                WindowHeightSizeClass.Compact -> WindowInfo.WindowType.Compact
                WindowHeightSizeClass.Medium -> WindowInfo.WindowType.Medium
                WindowHeightSizeClass.Expanded -> WindowInfo.WindowType.Expanded
                else -> WindowInfo.WindowType.Compact
            },
            screenWidth = configuration.screenWidthDp.dp,
            screenHeight = configuration.screenHeightDp.dp,
            isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
        )
    }
}

/**
 * Provides WindowInfo to child composables via LocalWindowInfo
 */
@Composable
fun ProvideWindowInfo(
    activity: Activity,
    content: @Composable () -> Unit
) {
    val windowInfo = calculateWindowInfo(activity)
    CompositionLocalProvider(LocalWindowInfo provides windowInfo) {
        content()
    }
}

/**
 * Convenience composable that adapts based on window width
 */
@Composable
fun AdaptiveLayout(
    compactContent: @Composable () -> Unit,
    mediumContent: @Composable (() -> Unit)? = null,
    expandedContent: @Composable (() -> Unit)? = null
) {
    val windowInfo = LocalWindowInfo.current
    
    when (windowInfo.screenWidthType) {
        WindowInfo.WindowType.Compact -> compactContent()
        WindowInfo.WindowType.Medium -> (mediumContent ?: compactContent)()
        WindowInfo.WindowType.Expanded -> (expandedContent ?: mediumContent ?: compactContent)()
    }
}

/**
 * Extension to get appropriate horizontal padding based on screen size
 * Centers content on large screens
 */
@Composable
fun adaptiveHorizontalPadding(): Dp {
    val windowInfo = LocalWindowInfo.current
    return windowInfo.contentPadding
}

/**
 * Calculates the start padding needed to center content with max width on large screens
 */
@Composable
fun calculateCenteringPadding(maxWidth: Dp = 1200.dp): Dp {
    val windowInfo = LocalWindowInfo.current
    return if (windowInfo.screenWidth > maxWidth) {
        (windowInfo.screenWidth - maxWidth) / 2
    } else {
        0.dp
    }
}
