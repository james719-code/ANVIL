package com.james.anvil.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.feature.R
import com.james.anvil.ui.components.ContributionGraph
import com.james.anvil.ui.components.ForgeActionTile
import com.james.anvil.ui.components.PageHeader
import com.james.anvil.ui.components.SectionCard
import com.james.anvil.ui.components.SectionTitle
import com.james.anvil.ui.components.TopLevelPageScaffold
import com.james.anvil.ui.theme.DesignTokens
import com.james.anvil.ui.theme.LocalWindowInfo
import com.james.anvil.ui.viewmodel.ForgeCoinViewModel
import com.james.anvil.ui.viewmodel.StreakViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lightbulb

@Composable
fun DashboardScreen(
    viewModel: TaskViewModel,
    budgetViewModel: BudgetViewModel = hiltViewModel(),
    blocklistViewModel: BlocklistViewModel = hiltViewModel(),
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
    val dailyQuote by viewModel.dailyQuote.collectAsState(initial = "")
    val completedTasks by viewModel.completedTasks.collectAsState(initial = emptyList())
    val allTasks by viewModel.allTasks.collectAsState(initial = emptyList())
    val bonusTasks by viewModel.bonusTasks.collectAsState(initial = emptyList())
    val bonusTaskCount by viewModel.bonusTaskCount.collectAsState(initial = 0)
    val hasDailyTasks by viewModel.hasDailyTasks.collectAsState(initial = false)
    val cashBalance by budgetViewModel.cashBalance.collectAsState(initial = 0.0)
    val gcashBalance by budgetViewModel.gcashBalance.collectAsState(initial = 0.0)
    val totalActiveLoanedAmount by budgetViewModel.totalActiveLoanedAmount.collectAsState(initial = 0.0)
    val activeLoans by budgetViewModel.activeLoans.collectAsState(initial = emptyList())
    val blockedApps by blocklistViewModel.blockedApps.collectAsState(initial = emptyList())
    val blockedLinks by blocklistViewModel.blockedLinks.collectAsState(initial = emptyList())
    val habitContributions by streakViewModel.habitContributions.collectAsState(initial = emptyList())
    val currentStreak by streakViewModel.currentStreak.collectAsState(initial = 0)
    val graceDays = viewModel.getGraceDaysCount()
    var showGraceExchangeDialog by remember { mutableStateOf(false) }
    var isSuggestionDismissed by remember { mutableStateOf(false) }

    val completedTodayCount = remember(completedTasks) {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val todayStart = now.timeInMillis
        val todayEnd = todayStart + 24 * 60 * 60 * 1000L
        completedTasks.count { (it.completedAt ?: 0L) in todayStart until todayEnd }
    }

    val greetingText = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> stringResource(R.string.dashboard_greeting_morning)
        in 12..17 -> stringResource(R.string.dashboard_greeting_afternoon)
        else -> stringResource(R.string.dashboard_greeting_evening)
    }

    val quote = dailyQuote.ifBlank { "Prioritize the next right action, then make it easy to do." }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH")) }
    val totalBalance = cashBalance + gcashBalance
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
            contentPadding = PaddingValues(top = DesignTokens.SpacingSm, bottom = DesignTokens.SpacingLg),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXl)
        ) {
            item {
                PageHeader(
                    eyebrow = "Daily Command",
                    title = greetingText,
                    subtitle = "What matters most is visible first.",
                    trailing = {
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        ) {
                            Icon(Icons.Filled.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                )
            }

            item { HeroStatusCard(dailyProgress, totalPendingCount, completedTodayCount, currentStreak, quote) }

            if (!isSuggestionDismissed) {
                item {
                    SmartSuggestionsCard(
                        pendingCount = totalPendingCount,
                        dailyProgress = dailyProgress,
                        streak = currentStreak,
                        onDismiss = { isSuggestionDismissed = true },
                        onNavigateToTasks = onNavigateToTasks,
                        onNavigateToFocus = onNavigateToFocus,
                        onNavigateToForge = onNavigateToForge
                    )
                }
            }

            item {
                SectionTitle("Quick actions")
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        ForgeActionTile("Tasks", "$totalPendingCount waiting", Icons.Outlined.Schedule, onClick = onNavigateToTasks)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ForgeActionTile("Focus", "Deep work session", Icons.Outlined.Timer, onClick = onNavigateToFocus)
                    }
                }
            }

            item {
                SectionCard(onClick = onNavigateToBudget) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vault balance", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currencyFormat.format(totalBalance), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            if (activeLoans.isNotEmpty()) {
                                Text("${activeLoans.size} active loan${if (activeLoans.size != 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(onClick = onNavigateToForge) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("The Forge", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$coinBalance coins", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Level up, earn rewards, sharpen skills", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (graceDays > 0 || bonusTaskCount > 0) {
                item {
                    SectionCard(
                        onClick = {
                            if (bonusTaskCount >= viewModel.getBonusTasksForGrace()) showGraceExchangeDialog = true
                            else onNavigateToBonusTasks()
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Grace reserve", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$graceDays ice available", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    if (bonusTaskCount >= viewModel.getBonusTasksForGrace()) "Exchange bonus tasks for another grace day"
                                    else "Complete ${viewModel.getBonusTasksForGrace() - bonusTaskCount} more bonus tasks",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle("Consistency", actionLabel = if (bonusTaskCount > 0) "Bonus tasks" else null, onAction = if (bonusTaskCount > 0) onNavigateToBonusTasks else null)
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                SectionCard {
                    ContributionGraph(allTasks = allTasks, bonusTasks = bonusTasks, habitContributions = habitContributions)
                }
            }
        }
    }

    if (showGraceExchangeDialog) {
        AlertDialog(
            onDismissRequest = { showGraceExchangeDialog = false },
            title = { Text("Exchange bonus tasks?") },
            text = { Text("Spend ${viewModel.getBonusTasksForGrace()} bonus tasks to gain 1 grace day for streak protection.") },
            confirmButton = { Button(onClick = { viewModel.tryExchangeBonusForGrace(); showGraceExchangeDialog = false }) { Text("Exchange") } },
            dismissButton = { TextButton(onClick = { showGraceExchangeDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HeroStatusCard(progress: Float, pendingCount: Int, completedTodayCount: Int, streak: Int, quote: String) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000), label = "hero")

    SectionCard(contentPadding = PaddingValues(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TODAY", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(if (progress >= 1f) "All clear" else "In progress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    val arcColor = MaterialTheme.colorScheme.primary
                    val trackColor = arcColor.copy(alpha = 0.10f)
                    Canvas(modifier = Modifier.size(64.dp)) {
                        drawCircle(color = trackColor, style = Stroke(width = 6.dp.toPx()))
                        drawArc(color = arcColor, startAngle = -90f, sweepAngle = animatedProgress * 360f, useCenter = false, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(DesignTokens.RadiusMedium))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), RoundedCornerShape(DesignTokens.RadiusMedium))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(4.dp).height(36.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(12.dp))
                Text(quote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat("Pending", "$pendingCount", Modifier.weight(1f))
                MiniStat("Done", "$completedTodayCount", Modifier.weight(1f))
                MiniStat("Streak", "$streak days", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

private data class DashboardSuggestion(
    val title: String,
    val description: String,
    val actionText: String,
    val actionClick: () -> Unit
)

@Composable
private fun SmartSuggestionsCard(
    pendingCount: Int,
    dailyProgress: Float,
    streak: Int,
    onDismiss: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToForge: () -> Unit
) {
    val suggestion = when {
        pendingCount > 0 -> DashboardSuggestion(
            title = "Tasks awaiting focus",
            description = "You have $pendingCount task${if (pendingCount != 1) "s" else ""} waiting. Start a focus session to power through them efficiently.",
            actionText = "Start Focus",
            actionClick = onNavigateToFocus
        )
        dailyProgress >= 1f -> DashboardSuggestion(
            title = "Daily goal reached!",
            description = "Awesome job! All daily objectives complete. Check out the Forge for shop items and quests.",
            actionText = "Visit Forge",
            actionClick = onNavigateToForge
        )
        streak == 0 -> DashboardSuggestion(
            title = "Start your streak",
            description = "Complete at least one task today to ignite your daily consistency streak.",
            actionText = "View Tasks",
            actionClick = onNavigateToTasks
        )
        else -> DashboardSuggestion(
            title = "Keep the momentum",
            description = "Stay consistent! Track your tasks and maintain your active $streak-day streak.",
            actionText = "View Tasks",
            actionClick = onNavigateToTasks
        )
    }

    SectionCard(
        accentColor = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = "Suggestion",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suggestion.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = suggestion.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = suggestion.actionClick,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = suggestion.actionText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss suggestion",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
