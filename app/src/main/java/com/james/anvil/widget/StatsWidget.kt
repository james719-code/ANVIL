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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider
import com.james.anvil.MainActivity
import com.james.anvil.ui.theme.*
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.runtime.remember

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
        primary = PrimaryAccent,
        onPrimary = Color.White,
        primaryContainer = PrimaryAccent.copy(alpha = 0.14f),
        onPrimaryContainer = PrimaryAccentDark,
        secondary = PrimaryAccentLight,
        onSecondary = Color.White,
        secondaryContainer = SurfaceElevatedLight,
        onSecondaryContainer = TextPrimaryLight,
        tertiary = PrimaryAccentDark,
        onTertiary = Color.White,
        tertiaryContainer = PrimaryAccentDark.copy(alpha = 0.14f),
        onTertiaryContainer = PrimaryAccent,
        background = BackgroundLight,
        onBackground = TextPrimaryLight,
        surface = SurfaceLight,
        onSurface = TextPrimaryLight,
        surfaceVariant = SurfaceElevatedLight,
        onSurfaceVariant = TextSecondaryLight,
        outline = BorderLight,
        error = ErrorRed,
        onError = Color.White
    ),
    dark = darkColorScheme(
        primary = PrimaryAccent,
        onPrimary = Color.White,
        primaryContainer = PrimaryAccent.copy(alpha = 0.18f),
        onPrimaryContainer = PrimaryAccentLight,
        secondary = PrimaryAccentLight,
        onSecondary = Color.White,
        secondaryContainer = SurfaceElevatedDark,
        onSecondaryContainer = TextPrimaryDark,
        tertiary = PrimaryAccentDark,
        onTertiary = Color.White,
        tertiaryContainer = PrimaryAccentDark.copy(alpha = 0.18f),
        onTertiaryContainer = PrimaryAccentLight,
        background = BackgroundDark,
        onBackground = TextPrimaryDark,
        surface = SurfaceDark,
        onSurface = TextPrimaryDark,
        surfaceVariant = SurfaceElevatedDark,
        onSurfaceVariant = TextSecondaryDark,
        outline = BorderDark,
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
                .padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // ─── Header Section ───────────────────────────
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.Start
                ) {
                    // Accent bar
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
                            text = "Lv.${stats.currentLevel} ${stats.levelTitle}",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    // Status pill
                    Box(
                        modifier = GlanceModifier
                            .cornerRadius(12.dp)
                            .background(
                                when {
                                    stats.currentStreak >= 3 -> GlanceTheme.colors.error
                                    stats.pendingTasks > 0 -> GlanceTheme.colors.tertiary
                                    else -> GlanceTheme.colors.secondary
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = when {
                                stats.currentStreak >= 3 -> "Streak: ${stats.currentStreak}d"
                                stats.pendingTasks > 0 -> "Active"
                                else -> "Clear"
                            },
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // ─── XP Progress Bar ──────────────────────────
                XpProgressBar(stats)
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // ─── Thin separator ───────────────────────────
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GlanceTheme.colors.surfaceVariant),
                content = {}
            )

            Spacer(modifier = GlanceModifier.height(10.dp))

            // ─── Activity & Stats Section ─────────────────
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                // ─── Weekly Activity Chart ────────────────────
                WeeklyActivityChart(stats.weeklyCompletionHistory)

                Spacer(modifier = GlanceModifier.height(8.dp))

                // ─── Quick Stats Summary Row ──────────────────
                QuickStatsRow(stats)

                Spacer(modifier = GlanceModifier.height(8.dp))

                // ─── Bottom Summary Row ──────────────────────
                Row(
                    modifier = GlanceModifier.fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Blocked items: ${stats.activeBlocks}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "${(stats.dailyProgress * 100).toInt()}% weekly",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

/**
 * XP progress bar built from nested Box composables.
 * Shows current XP, level progress, and XP needed for next level.
 */
@Composable
private fun XpProgressBar(stats: WidgetStats) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        // Label row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stats.totalXp} XP",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "${(stats.xpProgress * 100).toInt()}%",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        // Progress bar track
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(6.dp)
                .cornerRadius(3.dp)
                .background(GlanceTheme.colors.surfaceVariant)
        ) {
            // Progress bar fill — we use a Row trick to simulate percentage width
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                if (stats.xpProgress > 0f) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(6.dp)
                            .cornerRadius(3.dp)
                            .background(GlanceTheme.colors.primary),
                        content = {}
                    )
                }
                if (stats.xpProgress < 1f) {
                    // "Empty" portion — use a proportional weight
                    val emptyWeight = ((1f - stats.xpProgress) / stats.xpProgress.coerceAtLeast(0.01f))
                    // Glance doesn't support dynamic float weights, so we approximate
                    // by using conditional spacer sizing
                    Spacer(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(6.dp)
                    )
                }
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
        isSuccess -> ColorProvider(SuccessGreen)
        isHighlighted -> GlanceTheme.colors.tertiary
        else -> GlanceTheme.colors.onSurface
    }

    Box(
        modifier = modifier
            .cornerRadius(14.dp)
            .background(backgroundColor)
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (emoji.isNotEmpty()) {
                Text(
                    text = emoji,
                    style = TextStyle(fontSize = 14.sp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
            }
            Column {
                Text(
                    text = value,
                    style = TextStyle(
                        color = valueColor,
                        fontSize = 18.sp,
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
}

@Composable
private fun WeeklyActivityChart(history: List<Int>) {
    val maxVal = history.maxOrNull()?.coerceAtLeast(1) ?: 1
    
    val dayLabels = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("E", Locale.getDefault())
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            list.add(sdf.format(cal.time).first().toString().uppercase(Locale.ROOT))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "WEEKLY COMPLETIONS",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(52.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            history.forEachIndexed { index, count ->
                val heightFraction = count.toFloat() / maxVal
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    Box(
                        modifier = GlanceModifier
                            .width(18.dp)
                            .height(36.dp)
                            .cornerRadius(4.dp)
                            .background(GlanceTheme.colors.surfaceVariant),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (heightFraction > 0f) {
                            val fillHeight = (36 * heightFraction).toInt().coerceIn(4, 36).dp
                            Box(
                                modifier = GlanceModifier
                                    .width(18.dp)
                                    .height(fillHeight)
                                    .cornerRadius(4.dp)
                                    .background(GlanceTheme.colors.primary),
                                content = {}
                            )
                        }
                    }
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = dayLabels.getOrElse(index) { "" },
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatsRow(stats: WidgetStats) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(12.dp)
            .background(GlanceTheme.colors.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(
                text = stats.pendingTasks.toString(),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Pending",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Medium)
            )
        }
        
        // Vertical divider
        Box(
            modifier = GlanceModifier.width(1.dp).height(14.dp).background(GlanceTheme.colors.outline),
            content = {}
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(
                text = stats.completedToday.toString(),
                style = TextStyle(color = ColorProvider(SuccessGreen), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Completed",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Medium)
            )
        }

        // Vertical divider
        Box(
            modifier = GlanceModifier.width(1.dp).height(14.dp).background(GlanceTheme.colors.outline),
            content = {}
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(
                text = "${stats.currentStreak}d",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Streak",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Medium)
            )
        }

        // Vertical divider
        Box(
            modifier = GlanceModifier.width(1.dp).height(14.dp).background(GlanceTheme.colors.outline),
            content = {}
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(
                text = "${stats.focusMinutesToday}m",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Focus",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Medium)
            )
        }
    }
}
