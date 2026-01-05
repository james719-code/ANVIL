package com.james.anvil.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import com.james.anvil.MainActivity
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
        onBackground = TextBlack,
        surface = SurfaceLight,
        onSurface = TextBlack,
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
        onBackground = TextWhite,
        surface = SurfaceDark,
        onSurface = TextWhite,
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
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.surface)
            .padding(16.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            // Header with app name
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚒️",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "ANVIL",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            Spacer(modifier = GlanceModifier.height(12.dp))
            
            // Stats Grid - Row 1
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                StatCard(
                    value = stats.pendingTasks.toString(),
                    label = "Pending",
                    isHighlighted = stats.pendingTasks > 0,
                    modifier = GlanceModifier.defaultWeight()
                )
                
                Spacer(modifier = GlanceModifier.width(8.dp))
                
                StatCard(
                    value = stats.completedToday.toString(),
                    label = "Done Today",
                    isSuccess = true,
                    modifier = GlanceModifier.defaultWeight()
                )
            }
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            // Stats Grid - Row 2
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Progress indicator
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .cornerRadius(12.dp)
                        .background(GlanceTheme.colors.primaryContainer)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "${(stats.dailyProgress * 100).toInt()}%",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Progress",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }
                
                Spacer(modifier = GlanceModifier.width(8.dp))
                
                StatCard(
                    value = stats.activeBlocks.toString(),
                    label = "Blocked",
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
            .cornerRadius(12.dp)
            .background(backgroundColor)
            .padding(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}
