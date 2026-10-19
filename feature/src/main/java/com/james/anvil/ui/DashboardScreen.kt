package com.james.anvil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.data.Task
import com.james.anvil.feature.R
import com.james.anvil.ui.components.AnvilGlyph
import com.james.anvil.ui.components.AnvilListRow
import com.james.anvil.ui.components.AnvilLogo
import com.james.anvil.ui.components.ContributionGraph
import com.james.anvil.ui.components.rememberContributionData
import androidx.compose.runtime.getValue
import com.james.anvil.ui.components.GroupDivider
import com.james.anvil.ui.components.GroupedCard
import com.james.anvil.ui.components.HeaderIconButton
import com.james.anvil.ui.components.ProgressRing
import com.james.anvil.ui.components.QuickActionTile
import com.james.anvil.ui.components.SectionCard
import com.james.anvil.ui.components.SectionTitle
import com.james.anvil.ui.components.TopLevelPageScaffold
import com.james.anvil.ui.theme.ANVILTheme
import com.james.anvil.ui.theme.DesignTokens
import com.james.anvil.ui.theme.ForgeEmber
import com.james.anvil.ui.theme.ForgeInk
import com.james.anvil.ui.theme.ForgeInkRaised
import com.james.anvil.ui.theme.ForgePaper
import com.james.anvil.ui.theme.ForgePaperMuted
import com.james.anvil.ui.theme.LocalWindowInfo
import com.james.anvil.ui.viewmodel.ForgeCoinViewModel
import com.james.anvil.ui.viewmodel.StreakViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: TaskViewModel,
    budgetViewModel: BudgetViewModel = hiltViewModel(),
    streakViewModel: StreakViewModel = hiltViewModel(),
    forgeCoinViewModel: ForgeCoinViewModel = hiltViewModel(),
    onNavigateToTasks: () -> Unit = {},
    onNavigateToBonusTasks: () -> Unit = {},
    onNavigateToBudget: () -> Unit = {},
    onNavigateToBlocklist: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToForge: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToSavings: () -> Unit = {},
    onNavigateToShop: () -> Unit = {},
    onNavigateToQuests: () -> Unit = {},
    onNavigateToReport: () -> Unit = {}
) {
    val coinBalance by forgeCoinViewModel.coinBalance.collectAsState()
    val dailyProgress by viewModel.dailyProgress.collectAsState(initial = 0f)
    val totalPendingCount by viewModel.totalPendingCount.collectAsState(initial = 0)
    val pendingTasks by viewModel.tasks.collectAsState(initial = emptyList())
    val completedTasks by viewModel.completedTasks.collectAsState(initial = emptyList())
    val allTasks by viewModel.allTasks.collectAsState(initial = emptyList())
    val bonusTasks by viewModel.bonusTasks.collectAsState(initial = emptyList())
    val bonusTaskCount by viewModel.bonusTaskCount.collectAsState(initial = 0)
    val hasDailyTasks by viewModel.hasDailyTasks.collectAsState(initial = false)
    val totalBalance by budgetViewModel.totalBalance.collectAsState(initial = 0.0)
    val activeLoans by budgetViewModel.activeLoans.collectAsState(initial = emptyList())
    val habitContributions by streakViewModel.habitContributions.collectAsState(initial = emptyList())
    val currentStreak by streakViewModel.currentStreak.collectAsState(initial = 0)
    // Read once per bonus-count change, not on every recomposition
    val graceDays = remember(bonusTaskCount) { viewModel.getGraceDaysCount() }
    val bonusForGrace = remember { viewModel.getBonusTasksForGrace() }
    var showGraceExchangeDialog by remember { mutableStateOf(false) }

    val completedTodayCount = remember(completedTasks) {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val todayStart = now.timeInMillis
        val todayEnd = todayStart + 24 * 60 * 60 * 1000L
        completedTasks.count { (it.completedAt ?: 0L) in todayStart until todayEnd }
    }
    val contributionData by rememberContributionData(allTasks, bonusTasks, habitContributions)
    val upNext = remember(pendingTasks) { pendingTasks.sortedBy { it.deadline }.take(3) }

    val greetingText = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> stringResource(R.string.dashboard_greeting_morning)
        in 12..17 -> stringResource(R.string.dashboard_greeting_afternoon)
        else -> stringResource(R.string.dashboard_greeting_evening)
    }
    val dateText = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH")) }
    val windowInfo = LocalWindowInfo.current

    TopLevelPageScaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .then(
                    if (windowInfo.maxContentWidth != Dp.Unspecified)
                        Modifier.widthIn(max = windowInfo.maxContentWidth) else Modifier
                )
                .padding(horizontal = windowInfo.contentPadding),
            contentPadding = PaddingValues(top = DesignTokens.SpacingSm, bottom = DesignTokens.SpacingXl),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLg)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnvilLogo(size = 44.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greetingText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    HeaderIconButton(Icons.Outlined.Settings, "Settings", onNavigateToSettings)
                }
            }

            item {
                TodayHeroCard(
                    progress = dailyProgress,
                    pendingCount = totalPendingCount,
                    completedTodayCount = completedTodayCount,
                    streak = currentStreak,
                    hasDailyTasks = hasDailyTasks,
                    onAction = if (totalPendingCount > 0) onNavigateToFocus else onNavigateToTasks
                )
            }

            item {
                SectionTitle(
                    title = "Up next",
                    actionLabel = if (pendingTasks.size > upNext.size) "See all ${pendingTasks.size}" else null,
                    onAction = onNavigateToTasks
                )
                GroupedCard {
                    if (upNext.isEmpty()) {
                        AnvilListRow(
                            title = "Nothing on your list",
                            supporting = "Add a task and break it into steps.",
                            icon = Icons.Outlined.Add,
                            onClick = onNavigateToTasks
                        )
                    } else {
                        upNext.forEachIndexed { index, task ->
                            if (index > 0) GroupDivider()
                            UpNextRow(
                                task = task,
                                onComplete = { viewModel.completeTask(task) },
                                onOpen = onNavigateToTasks
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle(title = "Shortcuts")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile("Focus", Icons.Outlined.Timer, Modifier.weight(1f), onClick = onNavigateToFocus)
                    QuickActionTile("Quests", Icons.Outlined.EmojiEvents, Modifier.weight(1f), onClick = onNavigateToQuests)
                    QuickActionTile("Shop", Icons.Outlined.Storefront, Modifier.weight(1f), onClick = onNavigateToShop)
                    QuickActionTile("Report", Icons.Outlined.Assessment, Modifier.weight(1f), onClick = onNavigateToReport)
                }
            }

            item {
                SectionTitle(title = "Overview")
                GroupedCard {
                    AnvilListRow(
                        title = "Vault balance",
                        supporting = if (activeLoans.isNotEmpty())
                            "${activeLoans.size} active loan${if (activeLoans.size != 1) "s" else ""}"
                        else "All your wallets",
                        value = currencyFormat.format(totalBalance),
                        icon = Icons.Outlined.AccountBalanceWallet,
                        onClick = onNavigateToBudget
                    )
                    GroupDivider()
                    AnvilListRow(
                        title = "The Forge",
                        supporting = "Level up, earn rewards, sharpen skills",
                        value = "$coinBalance coins",
                        icon = Icons.Outlined.LocalFireDepartment,
                        onClick = onNavigateToForge
                    )
                    if (graceDays > 0 || bonusTaskCount > 0) {
                        GroupDivider()
                        val canExchange = bonusTaskCount >= bonusForGrace
                        AnvilListRow(
                            title = "Grace reserve",
                            supporting = if (canExchange) "Exchange bonus tasks for another grace day"
                            else "Complete ${bonusForGrace - bonusTaskCount} more bonus tasks",
                            value = "$graceDays available",
                            icon = Icons.Outlined.CheckCircle,
                            onClick = {
                                if (canExchange) showGraceExchangeDialog = true else onNavigateToBonusTasks()
                            }
                        )
                    }
                }
            }

            item {
                SectionTitle(
                    title = "Consistency",
                    actionLabel = if (bonusTaskCount > 0) "Bonus tasks" else null,
                    onAction = if (bonusTaskCount > 0) onNavigateToBonusTasks else null
                )
                SectionCard {
                    ContributionGraph(data = contributionData)
                }
            }
        }
    }

    if (showGraceExchangeDialog) {
        AlertDialog(
            onDismissRequest = { showGraceExchangeDialog = false },
            title = { Text("Exchange bonus tasks?") },
            text = { Text("Spend $bonusForGrace bonus tasks to gain 1 grace day for streak protection.") },
            confirmButton = { Button(onClick = { viewModel.tryExchangeBonusForGrace(); showGraceExchangeDialog = false }) { Text("Exchange") } },
            dismissButton = { TextButton(onClick = { showGraceExchangeDialog = false }) { Text("Cancel") } }
        )
    }
}

/**
 * The "furnace": always dark so the ember ring glows in both themes.
 * Qualitative message + ring on top, the three numbers that matter in the middle, one CTA.
 */
@Composable
private fun TodayHeroCard(
    progress: Float,
    pendingCount: Int,
    completedTodayCount: Int,
    streak: Int,
    hasDailyTasks: Boolean,
    onAction: () -> Unit
) {
    val total = pendingCount + completedTodayCount
    val ringProgress = when {
        hasDailyTasks -> progress.coerceIn(0f, 1f)
        total > 0 -> completedTodayCount.toFloat() / total
        else -> 0f
    }
    val showPercent = hasDailyTasks || total > 0

    val headline = when {
        pendingCount == 0 && completedTodayCount == 0 -> "Light the forge"
        pendingCount == 0 -> "Forge cleared"
        completedTodayCount > 0 -> "Keep it burning"
        else -> "Strike while it's hot"
    }
    val supporting = when {
        pendingCount == 0 && completedTodayCount == 0 -> "Add a task and get moving."
        pendingCount == 0 -> "Everything for today is done."
        else -> "$pendingCount left on your list."
    }

    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ForgeInk)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(ForgeEmber.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width * 0.92f, 0f),
                        radius = size.width * 0.75f
                    )
                )
            }
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ProgressRing(
                progress = ringProgress,
                size = 92.dp,
                strokeWidth = 9.dp,
                color = ForgeEmber,
                trackColor = ForgePaper.copy(alpha = 0.12f)
            ) {
                if (showPercent) {
                    Text(
                        text = "${(ringProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForgePaper
                    )
                } else {
                    AnvilGlyph(size = 52.dp, tint = ForgeEmber)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "TODAY'S FORGE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                    color = ForgeEmber
                )
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ForgePaper
                )
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForgePaperMuted
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ForgeInkRaised)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeroMetric("To do", pendingCount.toString(), Modifier.weight(1f))
            HeroDivider()
            HeroMetric("Done today", completedTodayCount.toString(), Modifier.weight(1f))
            HeroDivider()
            HeroMetric("Day streak", streak.toString(), Modifier.weight(1f))
        }

        Button(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForgeEmber, contentColor = ForgeInk)
        ) {
            Icon(
                if (pendingCount > 0) Icons.Outlined.Timer else Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (pendingCount > 0) "Start focusing" else "Plan your next task",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ForgePaper
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ForgePaperMuted,
            maxLines = 1
        )
    }
}

@Composable
private fun HeroDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(28.dp)
            .background(ForgePaper.copy(alpha = 0.12f))
    )
}

@Composable
private fun UpNextRow(task: Task, onComplete: () -> Unit, onOpen: () -> Unit) {
    val overdue = !task.isDaily && task.deadline < System.currentTimeMillis()
    val timeFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val meta = buildString {
        append(task.category)
        append(" · ")
        append(if (task.isDaily) "Daily" else timeFormat.format(Date(task.deadline)))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onComplete)
                .semantics { contentDescription = "Complete ${task.title}" },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {}
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (overdue) "Overdue · $meta" else meta,
                style = MaterialTheme.typography.bodySmall,
                color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(name = "Today hero - light", showBackground = true, widthDp = 360)
@Composable
private fun TodayHeroPreview() {
    ANVILTheme(darkTheme = false) {
        Column(
            Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TodayHeroCard(0.4f, 3, 2, 7, true, {})
            TodayHeroCard(0f, 0, 0, 0, false, {})
        }
    }
}

@Preview(name = "Today hero - dark, large text", showBackground = true, widthDp = 360, fontScale = 1.5f)
@Composable
private fun TodayHeroDarkPreview() {
    ANVILTheme(darkTheme = true) {
        Column(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            TodayHeroCard(0.6f, 2, 3, 12, false, {})
        }
    }
}
