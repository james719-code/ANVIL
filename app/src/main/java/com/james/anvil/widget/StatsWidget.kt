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
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.james.anvil.MainActivity

class StatsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = WidgetRepository(context)
        val stats = repository.getStats()

        provideContent {
            GlanceTheme {
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

@Composable
private fun WidgetContent(context: Context, stats: WidgetStats) {
    // FIX: Use Compose Color (0xAARRGGBB) instead of android.graphics.Color.parseColor
    val backgroundColor = ColorProvider(
        day = Color(0xFFFFFFFF),
        night = Color(0xFF161B22)
    )
    val primaryTextColor = ColorProvider(
        day = Color(0xFF1F2328),
        night = Color(0xFFE6EDF3)
    )
    val secondaryTextColor = ColorProvider(
        day = Color(0xFF656D76),
        night = Color(0xFF8B949E)
    )
    val accentBlue = ColorProvider(
        day = Color(0xFF1565C0),
        night = Color(0xFF0288D1)
    )
    val accentTeal = ColorProvider(
        day = Color(0xFF00897B),
        night = Color(0xFF4DB6AC)
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(12.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "ANVIL Stats",
                style = TextStyle(
                    color = accentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                StatItem(
                    value = stats.pendingTasks.toString(),
                    label = "Pending",
                    valueColor = primaryTextColor,
                    labelColor = secondaryTextColor,
                    modifier = GlanceModifier.defaultWeight()
                )
                
                StatItem(
                    value = stats.completedToday.toString(),
                    label = "Done Today",
                    valueColor = accentTeal,
                    labelColor = secondaryTextColor,
                    modifier = GlanceModifier.defaultWeight()
                )
            }
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                StatItem(
                    value = "${(stats.dailyProgress * 100).toInt()}%",
                    label = "Progress",
                    valueColor = accentBlue,
                    labelColor = secondaryTextColor,
                    modifier = GlanceModifier.defaultWeight()
                )
                
                StatItem(
                    value = stats.activeBlocks.toString(),
                    label = "Blocked",
                    valueColor = primaryTextColor,
                    labelColor = secondaryTextColor,
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    valueColor: ColorProvider,
    labelColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = value,
            style = TextStyle(
                color = valueColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = label,
            style = TextStyle(
                color = labelColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        )
    }
}
