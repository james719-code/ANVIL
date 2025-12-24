package com.james.anvil.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.james.anvil.data.Task
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
        val todayStart = calendar.timeInMillis

        
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .height(200.dp)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = size.width / 14 
            val spacing = barWidth
            val heightPerUnit = size.height / maxCount

            counts.forEachIndexed { index, count ->
                val x = spacing + (index * 2 * barWidth)
                val barHeight = count * heightPerUnit
                
                
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
            }
        }
        
        
    }
    
    
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp).fillMaxSize(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceAround
    ) {
        days.forEach { day ->
             Text(text = day ?: "", style = MaterialTheme.typography.labelSmall)
        }
    }
}
