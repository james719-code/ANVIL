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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.data.HabitContribution
import com.james.anvil.feature.R
import com.james.anvil.ui.components.ActionTile
import com.james.anvil.ui.components.ContributionGraph
import com.james.anvil.ui.components.MetricPill
import com.james.anvil.ui.components.PageHeader
import com.james.anvil.ui.components.SectionCard
import com.james.anvil.ui.components.SectionTitle
import com.james.anvil.ui.components.TopLevelPageScaffold
import com.james.anvil.ui.theme.DesignTokens
import com.james.anvil.ui.theme.ElectricBlue
import com.james.anvil.ui.theme.ForgedGold
import com.james.anvil.ui.theme.InfoBlue
import com.james.anvil.ui.theme.LocalWindowInfo
import com.james.anvil.ui.theme.SuccessGreen
import com.james.anvil.ui.theme.WarningOrange
import com.james.anvil.ui.viewmodel.ForgeCoinViewModel
import com.james.anvil.ui.viewmodel.StreakViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

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

    val completedTodayCount = remember(completedTasks) {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = now.timeInMillis
        val todayEnd = todayStart + 24 * 60 * 60 * 1000L
        completedTasks.count { task ->
            val completedAt = task.completedAt ?: 0L
            completedAt in todayStart until todayEnd
        }
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
    val horizontalPadding = windowInfo.contentPadding
    val maxWidth = windowInfo.maxContentWidth

    TopLevelPageScaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .then(
                    if (maxWidth != androidx.compose.ui.unit.Dp.Unspecified) {
                        Modifier.widthIn(max = maxWidth)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = horizontalPadding),
            contentPadding = PaddingValues(
                top = DesignTokens.SpacingSm,
                bottom = DesignTokens.SpacingLg
            ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXl)
        ) {
            item {
                PageHeader(
                    eyebrow = "Daily Command",
                    title = greetingText,
                    subtitle = "What matters most is visible first so you can act quickly.",
                    trailing = {
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    RoundedCornerShape(18.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }

            item {
                TodayHeroCard(
                    progress = dailyProgress,
                    pendingCount = totalPendingCount,
                    completedTodayCount = completedTodayCount,
                    streak = currentStreak,
                    quote = quote
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        MetricPill(
                            label = "Forge Coins",
                            value = "$coinBalance",
                            icon = Icons.Outlined.Star,
                            tint = ForgedGold
                        )
                    }
                    item {
                        MetricPill(
                            label = "Balance",
                            value = currencyFormat.format(totalBalance),
                            icon = Icons.Outlined.AccountBalanceWallet,
                            tint = InfoBlue
                        )
                    }
                    item {
                        MetricPill(
                            label = "Blocked",
                            value = "${blockedApps.size + blockedLinks.size}",
                            icon = Icons.Outlined.Shield,
                            tint = SuccessGreen
                        )
                    }
                    if (hasDailyTasks) {
                        item {
                            MetricPill(
                                label = "Streak",
                                value = "$currentStreak days",
                                icon = Icons.Outlined.Bolt,
                                tint = WarningOrange
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle(title = "Quick actions")
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardActionRow(
                        primary = {
                            ActionTile(
                                label = "Open tasks",
                                supporting = "$totalPendingCount waiting for completion",
                                icon = Icons.Outlined.Schedule,
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = onNavigateToTasks
                            )
                        },
                        secondary = {
                            ActionTile(
                                label = "Start focus",
                                supporting = "Launch a distraction-free session",
                                icon = Icons.Outlined.Timer,
                                tint = ElectricBlue,
                                onClick = onNavigateToFocus
                            )
                        }
                    )
                    DashboardActionRow(
                        primary = {
                            ActionTile(
                                label = "Forge profile",
                                supporting = "Check level, achievements, and progression",
                                icon = Icons.Outlined.Star,
                                tint = ForgedGold,
                                onClick = onNavigateToForge
                            )
                        },
                        secondary = {
                            ActionTile(
                                label = "Forge report",
                                supporting = "Review productivity and output trends",
                                icon = Icons.Outlined.Assessment,
                                tint = InfoBlue,
                                onClick = onNavigateToReport
                            )
                        }
                    )
                }
            }

            item {
                SectionTitle(title = "Operational overview")
            }

            item {
                DashboardActionRow(
                    primary = {
                        SummaryPanel(
                            label = "Budget",
                            value = currencyFormat.format(totalBalance),
                            supporting = if (activeLoans.isEmpty()) {
                                "Healthy snapshot across wallets"
                            } else {
                                "${activeLoans.size} active loans, ${currencyFormat.format(totalActiveLoanedAmount)} outstanding"
                            },
                            tint = InfoBlue,
                            onClick = onNavigateToBudget
                        )
                    },
                    secondary = {
                        SummaryPanel(
                            label = "Blocklist",
                            value = "${blockedApps.size} apps",
                            supporting = "${blockedLinks.size} blocked links ready to enforce",
                            tint = SuccessGreen,
                            onClick = onNavigateToBlocklist
                        )
                    }
                )
            }

            if (graceDays > 0 || bonusTaskCount > 0) {
                item {
                    SectionCard(
                        onClick = {
                            if (bonusTaskCount >= viewModel.getBonusTasksForGrace()) {
                                showGraceExchangeDialog = true
                            } else {
                                onNavigateToBonusTasks()
                            }
                        },
                        accentColor = ForgedGold
                    ) {
                        Text(
                            text = "Grace reserve",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$graceDays ice available",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (bonusTaskCount >= viewModel.getBonusTasksForGrace()) {
                                "You have enough bonus tasks to exchange for another grace day."
                            } else {
                                "Complete ${viewModel.getBonusTasksForGrace() - bonusTaskCount} more bonus tasks to earn another grace day."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                SectionTitle(title = "Keep momentum")
            }

            item {
                DashboardActionRow(
                    primary = {
                        SummaryPanel(
                            label = "Quests",
                            value = "${bonusTasks.size} bonus logged",
                            supporting = "Open your quest log and continue the streak of extra effort.",
                            tint = WarningOrange,
                            onClick = onNavigateToQuests
                        )
                    },
                    secondary = {
                        SummaryPanel(
                            label = "Savings",
                            value = "$coinBalance coins",
                            supporting = "Convert effort into long-term rewards and upgrades.",
                            tint = ForgedGold,
                            onClick = onNavigateToSavings
                        )
                    }
                )
            }

            item {
                SectionCard(accentColor = InfoBlue) {
                    SectionTitle(
                        title = "Consistency",
                        actionLabel = if (bonusTaskCount > 0) "Bonus tasks" else null,
                        onAction = if (bonusTaskCount > 0) onNavigateToBonusTasks else null
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ContributionGraph(
                        allTasks = allTasks,
                        bonusTasks = bonusTasks,
                        habitContributions = habitContributions
                    )
                }
            }

            item {
                DashboardActionRow(
                    primary = {
                        ActionTile(
                            label = "Forge shop",
                            supporting = "Spend coins on upgrades and equipment",
                            icon = Icons.Outlined.Storefront,
                            tint = ForgedGold,
                            onClick = onNavigateToShop
                        )
                    },
                    secondary = {
                        ActionTile(
                            label = "Bonus tasks",
                            supporting = "Track extra wins and exchange them for grace",
                            icon = Icons.Outlined.CheckCircle,
                            tint = WarningOrange,
                            onClick = onNavigateToBonusTasks
                        )
                    }
                )
            }
        }
    }

    if (showGraceExchangeDialog) {
        AlertDialog(
            onDismissRequest = { showGraceExchangeDialog = false },
            title = { Text("Exchange bonus tasks?") },
            text = {
                Text("Spend ${viewModel.getBonusTasksForGrace()} bonus tasks to gain 1 grace day for streak protection.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.tryExchangeBonusForGrace()
                        showGraceExchangeDialog = false
                    }
                ) {
                    Text("Exchange")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGraceExchangeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TodayHeroCard(
    progress: Float,
    pendingCount: Int,
    completedTodayCount: Int,
    streak: Int,
    quote: String
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "hero_progress"
    )

    SectionCard(
        accentColor = MaterialTheme.colorScheme.primary,
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TODAY'S FORGE STATUS",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (progress >= 1f) "Objectives Cleared" else "Forging Objectives",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Beautiful premium progress gauge
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    Canvas(modifier = Modifier.size(64.dp)) {
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.08f),
                            style = Stroke(width = 6.dp.toPx())
                        )
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    primaryColor,
                                    secondaryColor,
                                    primaryColor
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Beautiful quote block with side accent line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                        RoundedCornerShape(DesignTokens.RadiusMedium)
                    )
                    .border(
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(DesignTokens.RadiusMedium)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "\"$quote\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeroMetric("Pending", "$pendingCount", InfoBlue, Modifier.weight(1f))
                HeroMetric("Completed", "$completedTodayCount", SuccessGreen, Modifier.weight(1f))
                HeroMetric("Streak", "$streak Days", WarningOrange, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
                RoundedCornerShape(16.dp)
            )
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

@Composable
private fun SummaryPanel(
    label: String,
    value: String,
    supporting: String,
    tint: Color,
    onClick: () -> Unit
) {
    SectionCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        accentColor = tint
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = tint
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DashboardActionRow(
    primary: @Composable () -> Unit,
    secondary: @Composable () -> Unit
) {
    val windowInfo = LocalWindowInfo.current
    if (windowInfo.shouldShowTwoPane) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) { primary() }
            Box(modifier = Modifier.weight(1f)) { secondary() }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            primary()
            secondary()
        }
    }
}
