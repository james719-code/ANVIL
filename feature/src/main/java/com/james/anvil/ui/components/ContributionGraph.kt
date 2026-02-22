package com.james.anvil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.data.BonusTask
import com.james.anvil.data.HabitContribution
import com.james.anvil.data.Task
import com.james.anvil.ui.theme.DeepTeal
import java.text.SimpleDateFormat
import java.util.*

data class ContributionDay(
    val date: Long,
    val count: Int,
    val dayOfWeek: Int, // 0 = Sunday, 6 = Saturday
    val isCurrentYear: Boolean = true,
    val isFuture: Boolean = false
)

@Composable
fun ContributionGraph(
    allTasks: List<Task>,
    bonusTasks: List<BonusTask>,
    habitContributions: List<HabitContribution> = emptyList(),
    modifier: Modifier = Modifier
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    
    val contributionData = remember(allTasks, bonusTasks, habitContributions, currentYear) {
        calculateYearContributionData(allTasks, bonusTasks, habitContributions, currentYear)
    }
    
    val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    var selectedDay by remember { mutableStateOf<ContributionDay?>(null) }
    val scrollState = rememberScrollState(Int.MAX_VALUE)

    Column(modifier = modifier.fillMaxWidth()) {
        // Year header with legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentYear.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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

        Spacer(modifier = Modifier.height(12.dp))

        // Calendar grid
        Row(modifier = Modifier.fillMaxWidth()) {
            // Day of week labels
            Column(
                modifier = Modifier.padding(end = 6.dp, top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
                dayLabels.forEach { label ->
                    Box(
                        modifier = Modifier
                            .height(12.dp)
                            .width(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Scrollable calendar grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
            ) {
                // Month labels row
                Row(
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    contributionData.months.forEachIndexed { index, monthData ->
                        val weekCount = monthData.weeks.size
                        val monthWidth = (weekCount * 14).dp // 12dp cell + 2dp gap
                        
                        Box(
                            modifier = Modifier.width(monthWidth),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = monthLabels[index],
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }
                
                // Contribution grid organized by months
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    contributionData.months.forEachIndexed { monthIndex, monthData ->
                        // Month container
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(end = if (monthIndex < 11) 6.dp else 0.dp)
                        ) {
                            monthData.weeks.forEach { week ->
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    for (dayIndex in 0..6) {
                                        val day = week.find { it.dayOfWeek == dayIndex }
                                        if (day != null && day.isCurrentYear) {
                                            ContributionCell(
                                                day = day,
                                                isSelected = selectedDay?.date == day.date,
                                                onClick = { 
                                                    if (!day.isFuture) {
                                                        selectedDay = if (selectedDay?.date == day.date) null else day 
                                                    }
                                                }
                                            )
                                        } else {
                                            // Empty/placeholder cell
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color.Transparent)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tooltip for selected day
        selectedDay?.let { day ->
            if (!day.isFuture) {
                Spacer(modifier = Modifier.height(12.dp))
                val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                val message = if (day.count > 0) {
                    "Clean day on ${dateFormat.format(Date(day.date))}"
                } else {
                    "Overdue task on ${dateFormat.format(Date(day.date))}"
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
    val color = when {
        day.isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else -> getContributionColor(day.count)
    }
    
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
            .then(
                if (isSelected && !day.isFuture) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                } else {
                    Modifier
                }
            )
            .clickable(enabled = !day.isFuture, onClick = onClick)
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
        count <= 2 -> DeepTeal.copy(alpha = 0.35f)
        count <= 4 -> DeepTeal.copy(alpha = 0.6f)
        else -> DeepTeal
    }
}

data class YearContributionData(
    val year: Int,
    val months: List<MonthContributionData>
)

data class MonthContributionData(
    val month: Int,
    val weeks: List<List<ContributionDay>>
)

/**
 * Calculates contribution data for an entire year, organized by months.
 * Each month contains weeks, and each week contains days.
 * A day is green (count = 1) by default UNLESS there was an overdue task on that day.
 * An overdue task is a task with a deadline on or before that day that wasn't completed by end of that day.
 * Bonus tasks add extra contribution (1 per 3 bonus tasks).
 */
private fun calculateYearContributionData(
    allTasks: List<Task>,
    bonusTasks: List<BonusTask>,
    habitContributions: List<HabitContribution>,
    year: Int
): YearContributionData {
    val calendar = Calendar.getInstance()
    val today = Calendar.getInstance()
    // Normalize today to start of day for accurate comparison
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)
    
    // Create lookup map for contributions
    // Ensure keys are start of day timestamps
    val contributionMap = habitContributions.associateBy { 
        val c = Calendar.getInstance()
        c.timeInMillis = it.date
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    }

    val months = mutableListOf<MonthContributionData>()
    
    for (month in 0..11) {
        val monthWeeks = mutableListOf<List<ContributionDay>>()
        
        // Set to first day of the month
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        var currentWeek = mutableListOf<ContributionDay>()
        
        // Process each day in the month
        for (day in 1..daysInMonth) {
            calendar.set(year, month, day)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
            
            // Start a new week on Sunday
            if (dayOfWeek == 0 && currentWeek.isNotEmpty()) {
                monthWeeks.add(currentWeek.toList())
                currentWeek = mutableListOf()
            }
            
            val startOfDay = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val endOfDay = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -1) // Reset
            
            val isFuture = calendar.timeInMillis > today.timeInMillis
            
            // LOGIC FIX: Use persisted contribution if available, else dynamic calculation
            val persistedContribution = contributionMap[startOfDay]
            
            val baseCount = if (isFuture) {
                0
            } else if (persistedContribution != null) {
                // We have a record! It's a verified Green Day.
                persistedContribution.contributionValue
            } else {
                // Fallback: Dynamic Calculation
                // Check if there were any overdue tasks at the end of this day
                // An overdue task is one with deadline <= endOfDay that wasn't completed by endOfDay
                val hadOverdueTask = allTasks.any { task ->
                    // Task deadline was on or before end of this day
                    task.deadline < endOfDay &&
                    // AND the task was either:
                    // - never completed, OR
                    // - completed AFTER the end of this day
                    run { val ca = task.completedAt; ca == null || ca >= endOfDay }
                }
                
                if (hadOverdueTask) 0 else 1
            }
            
            val bonusCount = if (isFuture) 0 else bonusTasks.count { bonus ->
                bonus.completedAt >= startOfDay && bonus.completedAt < endOfDay
            }
            
            // Total count: base (clean day) + bonus tasks (1 per 3)
            val totalCount = baseCount + (bonusCount / 3)
            
            currentWeek.add(ContributionDay(
                date = startOfDay,
                count = totalCount,
                dayOfWeek = dayOfWeek,
                isCurrentYear = true,
                isFuture = isFuture
            ))
        }
        
        // Add the last week if not empty
        if (currentWeek.isNotEmpty()) {
            monthWeeks.add(currentWeek.toList())
        }
        
        months.add(MonthContributionData(month, monthWeeks))
    }
    
    return YearContributionData(year, months)
}

