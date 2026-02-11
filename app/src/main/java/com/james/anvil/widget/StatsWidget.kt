package com.james.anvil.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.material3.ColorProviders
import androidx.glance.Image
import androidx.glance.ColorFilter
import com.james.anvil.MainActivity
import com.james.anvil.R
import com.james.anvil.ui.theme.*
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

class StatsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = WidgetRepository(context)
        val stats = repository.getStats()

        provideContent {
            GlanceTheme(colors = AnvilGlanceColors) {
                WidgetContent(context, stats)
            }
        }
    }

    companion object {
        suspend fun refreshAll(context: Context) {
            StatsWidget().updateAll(context)
        }
    }
}

/**
 * Material You color scheme for Glance widgets.
 * Falls back to ANVIL brand colors on devices without dynamic colors.
 */
private val AnvilGlanceColors = ColorProviders(
    light = lightColorScheme(
        primary = ElectricBlue,
        onPrimary = Color.White,
        primaryContainer = ElectricBlue.copy(alpha = 0.12f),
        onPrimaryContainer = ElectricBlue,
        secondary = ElectricTeal,
        onSecondary = Color.White,
        secondaryContainer = ElectricTeal.copy(alpha = 0.12f),
        onSecondaryContainer = ElectricTeal,
        background = BackgroundLight,
        onBackground = TextPrimaryLight,
        surface = SurfaceLight,
        onSurface = TextPrimaryLight,
        surfaceVariant = SurfaceElevatedLight,
        onSurfaceVariant = TextSecondaryLight,
        tertiary = WarningOrange,
        onTertiary = Color.White,
        error = ErrorRed,
        onError = Color.White
    ),
    dark = darkColorScheme(
        primary = ElectricBlue,
        onPrimary = Color.White,
        primaryContainer = ElectricBlue.copy(alpha = 0.24f),
        onPrimaryContainer = ElectricBlue,
        secondary = ElectricTeal,
        onSecondary = Color.White,
        secondaryContainer = ElectricTeal.copy(alpha = 0.24f),
        onSecondaryContainer = ElectricTeal,
        background = BackgroundDark,
        onBackground = TextPrimaryDark,
        surface = SurfaceDark,
        onSurface = TextPrimaryDark,
        surfaceVariant = SurfaceElevatedDark,
        onSurfaceVariant = TextSecondaryDark,
        tertiary = WarningOrange,
        onTertiary = Color.White,
        error = ErrorRed,
        onError = Color.White
    )
)

@Composable
private fun WidgetContent(context: Context, stats: WidgetStats) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.surface)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // ─── Header ────────────────────────────────────
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                // Gradient accent bar (brand identity)
                Box(
                    modifier = GlanceModifier
                        .width(3.dp)
                        .height(18.dp)
                        .cornerRadius(2.dp)
                        .background(GlanceTheme.colors.primary),
                    content = {}
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Column {
                    Text(
                        text = "ANVIL",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Productivity",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                // Live status pill
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(12.dp)
                        .background(
                            if (stats.pendingTasks > 0)
                                GlanceTheme.colors.tertiary
                            else
                                GlanceTheme.colors.secondary
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (stats.pendingTasks > 0) "⚡ Active" else "✓ Clear",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            // ─── Thin separator ────────────────────────────
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GlanceTheme.colors.surfaceVariant),
                content = {}
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            // ─── Stats Row 1: Pending + Completed ─────────
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                StatCard(
                    value = stats.pendingTasks.toString(),
                    label = "Pending",
                    emoji = "📋",
                    isHighlighted = stats.pendingTasks > 0,
                    modifier = GlanceModifier.defaultWeight()
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                StatCard(
                    value = stats.completedToday.toString(),
                    label = "Done Today",
                    emoji = "✅",
                    isSuccess = true,
                    modifier = GlanceModifier.defaultWeight()
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // ─── Stats Row 2: Progress + Blocks ───────────
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Progress card — hero-style with primary accent
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.primaryContainer)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "${(stats.dailyProgress * 100).toInt()}",
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "%",
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "Weekly Progress",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                StatCard(
                    value = stats.activeBlocks.toString(),
                    label = "Blocked",
                    emoji = "🛡️",
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    emoji: String = "",
    modifier: GlanceModifier = GlanceModifier,
    isHighlighted: Boolean = false,
    isSuccess: Boolean = false
) {
    val backgroundColor = when {
        isSuccess -> GlanceTheme.colors.secondaryContainer
        isHighlighted -> GlanceTheme.colors.tertiaryContainer
        else -> GlanceTheme.colors.surfaceVariant
    }

    val valueColor = when {
        isSuccess -> GlanceTheme.colors.secondary
        isHighlighted -> GlanceTheme.colors.tertiary
        else -> GlanceTheme.colors.onSurface
    }

    Box(
        modifier = modifier
            .cornerRadius(16.dp)
            .background(backgroundColor)
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            if (emoji.isNotEmpty()) {
                Text(
                    text = emoji,
                    style = TextStyle(fontSize = 12.sp)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
            }
            Text(
                text = value,
                style = TextStyle(
                    color = valueColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = label,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
