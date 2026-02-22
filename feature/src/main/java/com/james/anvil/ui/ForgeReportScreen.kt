package com.james.anvil.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.data.XpSource
import com.james.anvil.ui.components.AnvilCard
import com.james.anvil.ui.theme.*
import com.james.anvil.ui.viewmodel.DailyStat
import com.james.anvil.ui.viewmodel.ForgeReportState
import com.james.anvil.ui.viewmodel.ForgeReportViewModel
import com.james.anvil.ui.viewmodel.ReportRange
import com.james.anvil.ui.viewmodel.XpBySource
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgeReportScreen(
    onBack: () -> Unit,
    viewModel: ForgeReportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forge Report", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ForgedGold)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Time Range Selector ──
                item {
                    RangeSelector(
                        selected = state.range,
                        onSelect = { viewModel.selectRange(it) }
                    )
                }

                // ── Productivity Score ──
                item {
                    ProductivityScoreCard(score = state.productivityScore)
                }

                // ── Key Metrics Row ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.CheckCircle,
                            label = "Tasks Done",
                            value = "${state.tasksCompleted}",
                            color = ElectricTeal,
                            delta = state.tasksDelta
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Timer,
                            label = "Focus",
                            value = formatMinutes(state.totalFocusMinutes),
                            color = ElectricBlue,
                            delta = state.focusDelta?.let { if (it != 0) it else null }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Star,
                            label = "XP Earned",
                            value = "${state.totalXpEarned}",
                            color = ForgedGold,
                            delta = state.xpDelta?.let { if (it != 0) it else null }
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.AddTask,
                            label = "Bonus Tasks",
                            value = "${state.bonusTasksCompleted}",
                            color = WarningOrange
                        )
                    }
                }

                // ── Task Completion Chart ──
                if (state.dailyStats.isNotEmpty() && state.range != ReportRange.ALL_TIME) {
                    item {
                        ChartCard(
                            title = "Tasks Completed",
                            icon = Icons.Outlined.CheckCircle,
                            iconTint = ElectricTeal
                        ) {
                            BarChart(
                                data = state.dailyStats.map { it.tasksCompleted.toFloat() },
                                labels = state.dailyStats.map { formatShortDate(it.dayStartMillis) },
                                barColor = ElectricTeal,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            )
                        }
                    }
                }

                // ── Focus Time Chart ──
                if (state.dailyStats.isNotEmpty() && state.range != ReportRange.ALL_TIME) {
                    item {
                        ChartCard(
                            title = "Focus Minutes",
                            icon = Icons.Outlined.Timer,
                            iconTint = ElectricBlue
                        ) {
                            BarChart(
                                data = state.dailyStats.map { it.focusMinutes.toFloat() },
                                labels = state.dailyStats.map { formatShortDate(it.dayStartMillis) },
                                barColor = ElectricBlue,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            )
                        }
                    }
                }

                // ── Budget Breakdown ──
                if (state.totalSpending > 0 || state.totalIncome > 0) {
                    item {
                        BudgetBreakdownCard(state = state, currencyFormat = currencyFormat)
                    }
                }

                // ── XP by Source ──
                if (state.xpBySource.isNotEmpty()) {
                    item {
                        XpBreakdownCard(xpBySource = state.xpBySource, totalXp = state.totalXpEarned)
                    }
                }

                // ── Combat & Quests ──
                if (state.monstersDefeated > 0 || state.questsCompleted > 0) {
                    item {
                        CombatQuestCard(state = state)
                    }
                }

                // ── Consistency ──
                item {
                    ConsistencyCard(
                        contributionDays = state.contributionDays,
                        totalDays = if (state.range == ReportRange.ALL_TIME) state.contributionDays else state.range.days,
                        focusSessions = state.focusSessionCount
                    )
                }

                // ── Top Categories ──
                if (state.topCategories.isNotEmpty()) {
                    item {
                        TopCategoriesCard(categories = state.topCategories)
                    }
                }

                // Bottom spacer
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ──────────────────────────────────────────────────
// Composable Components
// ──────────────────────────────────────────────────

@Composable
private fun RangeSelector(
    selected: ReportRange,
    onSelect: (ReportRange) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportRange.entries.forEach { range ->
            val isSelected = range == selected
            FilledTonalButton(
                onClick = { onSelect(range) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isSelected)
                        ForgedGold.copy(alpha = 0.2f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = if (isSelected) ForgedGold
                    else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    range.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ProductivityScoreCard(score: Int) {
    val animatedScore by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "score"
    )
    val scoreColor = when {
        score >= 80 -> ElectricTeal
        score >= 50 -> ForgedGold
        score >= 25 -> WarningOrange
        else -> Color(0xFFEF4444)
    }
    val scoreLabel = when {
        score >= 80 -> "Legendary"
        score >= 60 -> "Strong"
        score >= 40 -> "Steady"
        score >= 20 -> "Building"
        else -> "Starting"
    }

    AnvilCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "PRODUCTIVITY SCORE",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(16.dp))

            // Circular progress gauge
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.size(140.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    val arcSize = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    // Progress arc
                    drawArc(
                        color = scoreColor,
                        startAngle = 135f,
                        sweepAngle = 270f * animatedScore,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = scoreLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    delta: Int? = null
) {
    AnvilCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (delta != null) {
                Spacer(Modifier.height(4.dp))
                DeltaChip(delta = delta)
            }
        }
    }
}

@Composable
private fun DeltaChip(delta: Int) {
    val isPositive = delta > 0
    val chipColor = if (isPositive) ElectricTeal else Color(0xFFEF4444)
    val icon = if (isPositive) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown
    val prefix = if (isPositive) "+" else ""

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(chipColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(icon, null, tint = chipColor, modifier = Modifier.size(14.dp))
        Text(
            text = "$prefix$delta",
            style = MaterialTheme.typography.labelSmall,
            color = chipColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChartCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    AnvilCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun BarChart(
    data: List<Float>,
    labels: List<String>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val maxVal = data.max().coerceAtLeast(1f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val barBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    // Show at most 14 bars to avoid overcrowding; if more, show last 14
    val visibleCount = 14
    val visibleData = if (data.size > visibleCount) data.takeLast(visibleCount) else data
    val visibleLabels = if (labels.size > visibleCount) labels.takeLast(visibleCount) else labels
    // Only show every Nth label to avoid overlap
    val labelStep = when {
        visibleData.size > 10 -> 3
        visibleData.size > 6 -> 2
        else -> 1
    }

    Column(modifier = modifier) {
        // Bars area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            visibleData.forEachIndexed { _, value ->
                val fraction = value / maxVal
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = tween(600),
                    label = "bar"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Background bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(barBgColor)
                    )
                    // Value bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedFraction.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(barColor)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            visibleLabels.forEachIndexed { index, label ->
                Text(
                    text = if (index % labelStep == 0) label else "",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = labelColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BudgetBreakdownCard(state: ForgeReportState, currencyFormat: NumberFormat) {
    AnvilCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AccountBalanceWallet, null, tint = ForgedGold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Budget Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))

            // Income vs Spending bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        currencyFormat.format(state.totalIncome),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ElectricTeal
                    )
                    Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        currencyFormat.format(state.totalSpending),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                    Text("Spending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.spendingDelta != null && state.spendingDelta != 0.0) {
                        Spacer(Modifier.height(4.dp))
                        val isUp = state.spendingDelta > 0
                        // For spending, up = bad, down = good (invert colors)
                        val chipColor = if (isUp) Color(0xFFEF4444) else ElectricTeal
                        val icon = if (isUp) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(chipColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(icon, null, tint = chipColor, modifier = Modifier.size(14.dp))
                            Text(
                                currencyFormat.format(kotlin.math.abs(state.spendingDelta)),
                                style = MaterialTheme.typography.labelSmall,
                                color = chipColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Necessity vs Leisure breakdown
            if (state.necessitySpending > 0 || state.leisureSpending > 0) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(Modifier.height(12.dp))

                val total = state.necessitySpending + state.leisureSpending
                val necessityFraction = if (total > 0) (state.necessitySpending / total).toFloat() else 0.5f

                // Stacked bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    if (necessityFraction > 0f) {
                        Box(
                            Modifier
                                .weight(necessityFraction.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(ElectricBlue)
                        )
                    }
                    if (necessityFraction < 1f) {
                        Box(
                            Modifier
                                .weight((1f - necessityFraction).coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(WarningOrange)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(ElectricBlue, CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Necessity ${currencyFormat.format(state.necessitySpending)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(WarningOrange, CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Leisure ${currencyFormat.format(state.leisureSpending)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun XpBreakdownCard(xpBySource: List<XpBySource>, totalXp: Int) {
    val sourceColors = mapOf(
        XpSource.TASK to ElectricTeal,
        XpSource.BONUS to WarningOrange,
        XpSource.STREAK to ForgedGold,
        XpSource.BUDGET to Color(0xFF8B5CF6),
        XpSource.LOAN to Color(0xFFEC4899),
        XpSource.FOCUS to ElectricBlue,
        XpSource.SAVINGS to Color(0xFF06B6D4),
        XpSource.QUEST to Color(0xFFF59E0B),
        XpSource.COMBAT to Color(0xFFEF4444)
    )

    AnvilCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Star, null, tint = ForgedGold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("XP Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))

            xpBySource.take(5).forEach { entry ->
                val color = sourceColors[entry.source] ?: ElectricBlue
                val fraction = if (totalXp > 0) entry.total.toFloat() / totalXp else 0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).background(color, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        entry.source.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(60.dp)
                    )
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val animatedFraction by animateFloatAsState(
                            targetValue = fraction,
                            animationSpec = tween(600),
                            label = "xp_bar"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFraction.coerceAtLeast(0.01f))
                                .background(color, RoundedCornerShape(3.dp))
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${entry.total}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun CombatQuestCard(state: ForgeReportState) {
    AnvilCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\u2694\uFE0F", fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text("Combat & Quests", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${state.monstersDefeated}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                    Text("Monsters", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${state.questsCompleted}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ElectricTeal
                    )
                    Text("Quests Done", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${state.questsExpired}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WarningOrange
                    )
                    Text("Expired", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ConsistencyCard(contributionDays: Int, totalDays: Int, focusSessions: Int) {
    val percentage = if (totalDays > 0) (contributionDays * 100 / totalDays) else 0

    AnvilCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, tint = ElectricTeal, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Consistency", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$contributionDays",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ElectricTeal
                    )
                    Text("Active Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$percentage%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForgedGold
                    )
                    Text("Hit Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$focusSessions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue
                    )
                    Text("Sessions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TopCategoriesCard(categories: List<Pair<String, Int>>) {
    val maxCount = categories.maxOfOrNull { it.second } ?: 1
    val categoryColors = listOf(ElectricTeal, ElectricBlue, ForgedGold, WarningOrange, Color(0xFF8B5CF6))

    AnvilCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Category, null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Top Categories", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))

            categories.forEachIndexed { index, (category, count) ->
                val color = categoryColors[index % categoryColors.size]
                val fraction = count.toFloat() / maxCount

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        category,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(90.dp),
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val animatedFraction by animateFloatAsState(
                            targetValue = fraction,
                            animationSpec = tween(600),
                            label = "cat_bar"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFraction)
                                .background(color, RoundedCornerShape(3.dp))
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$count",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────────

private fun formatMinutes(minutes: Int): String = when {
    minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
    else -> "${minutes}m"
}

private fun formatShortDate(millis: Long): String {
    val sdf = SimpleDateFormat("d", Locale.getDefault())
    return sdf.format(Date(millis))
}
