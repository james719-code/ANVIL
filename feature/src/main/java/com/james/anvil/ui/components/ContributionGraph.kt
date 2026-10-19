package com.james.anvil.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.data.BonusTask
import com.james.anvil.data.HabitContribution
import com.james.anvil.data.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val CELL_DP = 12f
private const val GAP_DP = 2f
private const val PITCH_DP = CELL_DP + GAP_DP
private const val MONTH_GAP_DP = 6f
private const val GRID_HEIGHT_DP = 7 * CELL_DP + 6 * GAP_DP

@Immutable
data class ContributionDay(
    val date: Long,
    val count: Int,
    val dayOfWeek: Int, // 0 = Sunday, 6 = Saturday
    val isFuture: Boolean = false
)

/** One column of the grid. [days] is indexed by day of week; null cells are month-edge padding. */
@Immutable
class ContributionWeek(val days: Array<ContributionDay?>)

@Immutable
class ContributionMonth(val weeks: List<ContributionWeek>) {
    /** Width in dp of this month's block, excluding the trailing month gap. */
    val widthDp: Float get() = weeks.size * PITCH_DP - GAP_DP
}

@Immutable
class YearContributionData(
    val year: Int,
    val months: List<ContributionMonth>,
    /** Every week with its x offset in dp, flattened so drawing and hit-testing are a single loop. */
    val placedWeeks: List<PlacedWeek>,
    val totalWidthDp: Float,
    val todayXDp: Float
)

@Immutable
class PlacedWeek(val xDp: Float, val week: ContributionWeek)

private val MONTH_LABELS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/**
 * Year-long consistency heatmap.
 *
 * Perf notes: the grid is one Canvas (not ~365 composed, clickable boxes) and the data is built
 * off the main thread with a sweep over tasks instead of a per-day scan of every task.
 */
/**
 * Computes the grid off the main thread. Call this at screen level, not inside a lazy item:
 * a lazy item drops its state when scrolled away, which would redo the work on every scroll back.
 */
@Composable
fun rememberContributionData(
    allTasks: List<Task>,
    bonusTasks: List<BonusTask>,
    habitContributions: List<HabitContribution>
): State<YearContributionData?> {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    return produceState<YearContributionData?>(null, allTasks, bonusTasks, habitContributions, currentYear) {
        value = withContext(Dispatchers.Default) {
            calculateYearContributionData(allTasks, bonusTasks, habitContributions, currentYear)
        }
    }
}

@Composable
fun ContributionGraph(
    data: YearContributionData?,
    modifier: Modifier = Modifier
) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    var selectedDay by remember { mutableStateOf<ContributionDay?>(null) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // Bring the current month into view once the data exists
    LaunchedEffect(data?.todayXDp) {
        val x = data?.todayXDp ?: return@LaunchedEffect
        scrollState.scrollTo(with(density) { (x - 160f).coerceAtLeast(0f).dp.roundToPx() })
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentYear.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ContributionLegend()
                Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(end = 6.dp, top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(GAP_DP.dp)
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                    Box(modifier = Modifier.size(CELL_DP.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
            ) {
                val grid = data
                if (grid == null) {
                    // Reserve the final height so nothing jumps when the data arrives
                    Box(modifier = Modifier.width(300.dp).height((20 + GRID_HEIGHT_DP).dp))
                } else {
                    Row(modifier = Modifier.padding(bottom = 4.dp)) {
                        grid.months.forEachIndexed { index, month ->
                            val gap = if (index < grid.months.lastIndex) MONTH_GAP_DP else 0f
                            Box(modifier = Modifier.width((month.widthDp + gap).dp)) {
                                Text(
                                    text = MONTH_LABELS[index],
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    ContributionCanvas(
                        grid = grid,
                        selectedDate = selectedDay?.date,
                        onDayTapped = { day ->
                            selectedDay = if (selectedDay?.date == day.date) null else day
                        }
                    )
                }
            }
        }

        selectedDay?.let { day ->
            Spacer(modifier = Modifier.height(12.dp))
            val dateText = remember(day.date) {
                SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(day.date))
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = if (day.count > 0) "Clean day on $dateText" else "Overdue task on $dateText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ContributionCanvas(
    grid: YearContributionData,
    selectedDate: Long?,
    onDayTapped: (ContributionDay) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val palette = ContributionPalette(
        empty = MaterialTheme.colorScheme.surfaceVariant,
        future = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        low = primary.copy(alpha = 0.35f),
        mid = primary.copy(alpha = 0.6f),
        high = primary
    )

    Canvas(
        modifier = Modifier
            .width(grid.totalWidthDp.dp)
            .height(GRID_HEIGHT_DP.dp)
            .pointerInput(grid) {
                detectTapGestures { tap ->
                    val xDp = tap.x / density
                    val yDp = tap.y / density
                    val row = (yDp / PITCH_DP).toInt()
                    if (row !in 0..6 || yDp - row * PITCH_DP > CELL_DP) return@detectTapGestures
                    val placed = grid.placedWeeks.firstOrNull { xDp >= it.xDp && xDp <= it.xDp + CELL_DP }
                        ?: return@detectTapGestures
                    val day = placed.week.days[row] ?: return@detectTapGestures
                    if (!day.isFuture) onDayTapped(day)
                }
            }
    ) {
        val cell = CELL_DP.dp.toPx()
        val pitch = PITCH_DP.dp.toPx()
        val corner = CornerRadius(2.dp.toPx())
        val cellSize = Size(cell, cell)
        for (placed in grid.placedWeeks) {
            val x = placed.xDp.dp.toPx()
            for (row in 0..6) {
                val day = placed.week.days[row] ?: continue
                val color = when {
                    day.isFuture -> palette.future
                    else -> palette.forCount(day.count)
                }
                drawRoundRect(color, Offset(x, row * pitch), cellSize, corner)
                if (day.date == selectedDate) {
                    drawRoundRect(primary, Offset(x, row * pitch), cellSize, corner, style = Stroke(1.5.dp.toPx()))
                }
            }
        }
    }
}

private class ContributionPalette(
    val empty: Color,
    val future: Color,
    val low: Color,
    val mid: Color,
    val high: Color
) {
    fun forCount(count: Int): Color = when {
        count == 0 -> empty
        count <= 2 -> low
        count <= 4 -> mid
        else -> high
    }
}

@Composable
private fun ContributionLegend() {
    val primary = MaterialTheme.colorScheme.primary
    val colors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        primary.copy(alpha = 0.35f),
        primary.copy(alpha = 0.6f),
        primary
    )
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

/** Index of the first element in [arr] that is strictly greater than [value], or arr.size. */
private fun firstGreater(arr: LongArray, value: Long): Int {
    var lo = 0
    var hi = arr.size
    while (lo < hi) {
        val mid = (lo + hi) ushr 1
        if (arr[mid] > value) hi = mid else lo = mid + 1
    }
    return lo
}

/**
 * Builds the year grid in O(tasks * log 366 + 366).
 *
 * A day is "clean" (count 1) unless a task was overdue at the end of it: deadline before the end
 * of the day and not completed until after that point. A persisted [HabitContribution] wins over
 * that calculation. Every 3 bonus tasks completed that day add 1.
 */
private fun calculateYearContributionData(
    allTasks: List<Task>,
    bonusTasks: List<BonusTask>,
    habitContributions: List<HabitContribution>,
    year: Int
): YearContributionData {
    val cal = Calendar.getInstance()
    cal.set(year, Calendar.JANUARY, 1, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val startList = ArrayList<Long>(366)
    val dowList = ArrayList<Int>(366)
    val monthList = ArrayList<Int>(366)
    while (cal.get(Calendar.YEAR) == year) {
        startList.add(cal.timeInMillis)
        dowList.add(cal.get(Calendar.DAY_OF_WEEK) - 1)
        monthList.add(cal.get(Calendar.MONTH))
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    val n = startList.size
    val starts = startList.toLongArray()
    // ends[i] is the start of the following day (the exclusive end of day i)
    val ends = LongArray(n) { if (it + 1 < n) starts[it + 1] else cal.timeInMillis }

    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val todayStart = todayCal.timeInMillis

    // Overdue days via a difference array: each task covers days where deadline < end <= completedAt
    val diff = IntArray(n + 1)
    for (task in allTasks) {
        val first = firstGreater(ends, task.deadline)
        val completedAt = task.completedAt
        val last = if (completedAt == null) n - 1 else firstGreater(ends, completedAt) - 1
        if (first <= last) {
            diff[first]++
            diff[last + 1]--
        }
    }

    fun dayIndexOf(time: Long): Int {
        val idx = firstGreater(ends, time)
        return if (idx < n && time >= starts[idx]) idx else -1
    }

    val persisted = arrayOfNulls<Int>(n)
    for (c in habitContributions) {
        val idx = dayIndexOf(c.date)
        if (idx >= 0) persisted[idx] = c.contributionValue
    }

    val bonusPerDay = IntArray(n)
    for (b in bonusTasks) {
        val idx = dayIndexOf(b.completedAt)
        if (idx >= 0) bonusPerDay[idx]++
    }

    val days = arrayOfNulls<ContributionDay>(n)
    var overdueRunning = 0
    for (i in 0 until n) {
        overdueRunning += diff[i]
        val isFuture = starts[i] > todayStart
        val base = when {
            isFuture -> 0
            persisted[i] != null -> persisted[i]!!
            overdueRunning > 0 -> 0
            else -> 1
        }
        val bonus = if (isFuture) 0 else bonusPerDay[i] / 3
        days[i] = ContributionDay(starts[i], base + bonus, dowList[i], isFuture)
    }

    // Lay out: a new column starts every Sunday and at every month boundary
    val months = ArrayList<ContributionMonth>(12)
    val placed = ArrayList<PlacedWeek>(60)
    var x = 0f
    var todayX = 0f
    var i = 0
    for (month in 0..11) {
        val weeks = ArrayList<ContributionWeek>(6)
        var current = arrayOfNulls<ContributionDay>(7)
        var currentHasDays = false
        while (i < n && monthList[i] == month) {
            val day = days[i]!!
            if (day.dayOfWeek == 0 && currentHasDays) {
                weeks.add(ContributionWeek(current))
                current = arrayOfNulls(7)
                currentHasDays = false
            }
            current[day.dayOfWeek] = day
            currentHasDays = true
            i++
        }
        if (currentHasDays) weeks.add(ContributionWeek(current))

        val monthBlock = ContributionMonth(weeks)
        months.add(monthBlock)
        weeks.forEachIndexed { w, week ->
            val wx = x + w * PITCH_DP
            placed.add(PlacedWeek(wx, week))
            if (week.days.any { it != null && it.date == todayStart }) todayX = wx
        }
        x += monthBlock.widthDp + MONTH_GAP_DP
    }

    return YearContributionData(year, months, placed, x - MONTH_GAP_DP, todayX)
}
