package com.james.anvil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.data.Task
import com.james.anvil.ui.theme.DeepTeal
import java.text.SimpleDateFormat
import java.util.*

data class ContributionDay(
    val date: Long,
    val count: Int
)

@Composable
fun ContributionGraph(
    completedTasks: List<Task>,
    modifier: Modifier = Modifier
) {
    var monthOffset by remember { mutableIntStateOf(0) }
    
    val contributionData = remember(completedTasks, monthOffset) {
        calculateContributionData(completedTasks, monthOffset)
    }
    
    val dateRange = remember(monthOffset) {
        getDateRangeLabel(monthOffset)
    }

    var selectedDay by remember { mutableStateOf<ContributionDay?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Less",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ContributionLegend()
                        Text(
                            text = "More",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                // Month/Year navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { monthOffset += 1 },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = dateRange,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    IconButton(
                        onClick = { if (monthOffset > 0) monthOffset -= 1 },
                        enabled = monthOffset > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            tint = if (monthOffset > 0) MaterialTheme.colorScheme.onSurfaceVariant 
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Contribution grid (7 rows x 12 columns = ~84 days / 3 months)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(12),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.height(((7 * 14) + (6 * 3)).dp)
                ) {
                    items(contributionData) { day ->
                        ContributionCell(
                            day = day,
                            isSelected = selectedDay?.date == day.date,
                            onClick = { selectedDay = if (selectedDay?.date == day.date) null else day }
                        )
                    }
                }

                // Tooltip for selected day
                selectedDay?.let { day ->
                    Spacer(modifier = Modifier.height(8.dp))
                    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    Text(
                        text = "${day.count} task${if (day.count != 1) "s" else ""} completed on ${dateFormat.format(Date(day.date))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}


@Composable
private fun ContributionCell(
    day: ContributionDay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = getContributionColor(day.count)
    
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ContributionLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(0, 1, 3, 5).forEach { count ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(getContributionColor(count))
            )
        }
    }
}

@Composable
private fun getContributionColor(count: Int): Color {
    return when {
        count == 0 -> MaterialTheme.colorScheme.surfaceVariant
        count <= 2 -> DeepTeal.copy(alpha = 0.3f)
        count <= 4 -> DeepTeal.copy(alpha = 0.6f)
        else -> DeepTeal
    }
}

private fun calculateContributionData(completedTasks: List<Task>, monthOffset: Int = 0): List<ContributionDay> {
    val calendar = Calendar.getInstance()
    val result = mutableListOf<ContributionDay>()
    
    // Calculate base offset (each month offset is ~30 days, showing 84 days at a time)
    val daysOffset = monthOffset * 84
    
    // Generate 84 days (12 weeks) of data
    for (i in 83 downTo 0) {
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.DAY_OF_YEAR, -(i + daysOffset))
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis
        
        val count = completedTasks.count { task ->
            task.completedAt != null && task.completedAt >= startOfDay && task.completedAt < endOfDay
        }
        
        result.add(ContributionDay(startOfDay, count))
    }
    
    return result
}

private fun getDateRangeLabel(monthOffset: Int): String {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    
    // End date (most recent in this view)
    calendar.add(Calendar.DAY_OF_YEAR, -(monthOffset * 84))
    val endDate = dateFormat.format(calendar.time)
    
    // Start date (84 days before end)
    calendar.add(Calendar.DAY_OF_YEAR, -83)
    val startDate = dateFormat.format(calendar.time)
    
    return if (startDate == endDate) endDate else "$startDate - $endDate"
}

