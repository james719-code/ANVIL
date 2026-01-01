package com.james.anvil.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.james.anvil.data.Task
import com.james.anvil.ui.theme.DeepTeal
import com.james.anvil.ui.theme.MutedTeal
import java.util.Calendar
import java.util.Locale

@Composable
fun ConsistencyChart(completedTasks: List<Task>) {
    val chartData = remember(completedTasks) {
        val counts = IntArray(7) { 0 }
        val days = arrayOfNulls<String>(7)
        val calendar = Calendar.getInstance()
        
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        for (i in 6 downTo 0) {
             days[i] = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
             val start = calendar.timeInMillis
             val end = start + 24 * 60 * 60 * 1000
             
             counts[i] = completedTasks.count { 
                 (it.completedAt ?: 0L) in start until end
             }
             calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        Pair(counts, days)
    }

    val (counts, days) = chartData
    val maxCount = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
    val barColor = DeepTeal
    val trackColor = MutedTeal.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .height(180.dp)
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 24.dp)) {
                val barWidth = size.width / 14
                val spacing = barWidth
                val chartHeight = size.height
                val heightPerUnit = chartHeight / maxCount

                counts.forEachIndexed { index, count ->
                    val x = spacing + (index * 2 * barWidth)
                    val barHeight = count * heightPerUnit
                    
                    // Draw track (background bar)
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    
                    // Draw filled bar
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, chartHeight - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
            
            // Day labels
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                days.forEach { day ->
                    Text(
                        text = day ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
