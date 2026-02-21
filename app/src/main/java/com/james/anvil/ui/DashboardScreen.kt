package com.james.anvil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.james.anvil.R
import com.james.anvil.ui.components.AddBonusTaskBottomSheet
import com.james.anvil.ui.components.AnvilCard
import com.james.anvil.ui.components.AnvilHeader
import com.james.anvil.ui.components.ContributionGraph
import com.james.anvil.ui.components.MotivationCard
import com.james.anvil.ui.components.StreakCard
import com.james.anvil.data.HabitContribution
import com.james.anvil.ui.theme.DesignTokens
import com.james.anvil.ui.theme.ElectricBlue
import com.james.anvil.ui.theme.ElectricTeal
import com.james.anvil.ui.theme.ForgedGold
import com.james.anvil.ui.theme.InfoBlue
import com.james.anvil.ui.theme.LocalWindowInfo
import com.james.anvil.ui.theme.WarningOrange
import com.james.anvil.ui.theme.WindowInfo
import com.james.anvil.ui.viewmodel.ForgeCoinViewModel
import com.james.anvil.ui.viewmodel.StreakViewModel
import java.text.NumberFormat
import java.util.*
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TaskViewModel,
    budgetViewModel: BudgetViewModel = hiltViewModel(),
    blocklistViewModel: BlocklistViewModel = hiltViewModel(),
    streakViewModel: StreakViewModel = hiltViewModel(),
    forgeCoinViewModel: ForgeCoinViewModel = hiltViewModel(),
    onNavigateToPage: ((Int) -> Unit)? = null,
    onNavigateToForge: (() -> Unit)? = null,
    onNavigateToFocus: (() -> Unit)? = null,
    onNavigateToSavings: (() -> Unit)? = null,
    onNavigateToShop: (() -> Unit)? = null,
    onNavigateToQuests: (() -> Unit)? = null,
    onNavigateToReport: (() -> Unit)? = null
) {
    val coinBalance by forgeCoinViewModel.coinBalance.collectAsState()
    val dailyProgress by viewModel.dailyProgress.collectAsState(initial = 0f)
    val totalPendingCount by viewModel.totalPendingCount.collectAsState(initial = 0)
    val dailyQuote by viewModel.dailyQuote.collectAsState(initial = "")
    val completedTasks by viewModel.completedTasks.collectAsState(initial = emptyList())
    val allCompletedTasks by viewModel.allCompletedTasks.collectAsState(initial = emptyList())
    val allTasks by viewModel.allTasks.collectAsState(initial = emptyList())
    val bonusTasks by viewModel.bonusTasks.collectAsState(initial = emptyList())
    val blockedApps by blocklistViewModel.blockedApps.collectAsState(initial = emptyList())
    val blockedLinks by blocklistViewModel.blockedLinks.collectAsState(initial = emptyList())
    val bonusTaskCount by viewModel.bonusTaskCount.collectAsState(initial = 0)
    val cashBalance by budgetViewModel.cashBalance.collectAsState(initial = 0.0)
    val gcashBalance by budgetViewModel.gcashBalance.collectAsState(initial = 0.0)
    val totalActiveLoanedAmount by budgetViewModel.totalActiveLoanedAmount.collectAsState(initial = 0.0)
    val activeLoans by budgetViewModel.activeLoans.collectAsState(initial = emptyList())
    val habitContributions by streakViewModel.habitContributions.collectAsState(initial = emptyList())
    val currentStreak by streakViewModel.currentStreak.collectAsState(initial = 0)
    val hasDailyTasks by viewModel.hasDailyTasks.collectAsState(initial = false)
    
    val graceDays = viewModel.getGraceDaysCount() // Note: This is a function, not a flow in ViewModel. Consider making it reactive if needed, but for now we'll trust the viewmodel update flow.
    // Actually, viewModel.bonusTasks triggers updates, but grace days calculation depends on bonusManager which uses SharedPreferences.
    // It's better if we just assume the value is updated when bonusTaskCount changes or screen recomposes.
    
    var showGraceExchangeDialog by remember { mutableStateOf(false) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH")) }
    val calendar = remember { java.util.Calendar.getInstance() }

    val completedTodayCount = remember(completedTasks) {
        val now = java.util.Calendar.getInstance()
        now.set(java.util.Calendar.HOUR_OF_DAY, 0)
        now.set(java.util.Calendar.MINUTE, 0)
        now.set(java.util.Calendar.SECOND, 0)
        now.set(java.util.Calendar.MILLISECOND, 0)
        val todayStart = now.timeInMillis
        val todayEnd = todayStart + 24 * 60 * 60 * 1000L

        completedTasks.count {
            val completedAt = it.completedAt ?: 0L
            completedAt in todayStart until todayEnd
        }
    }

    // Time-based greeting
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "morning"
            in 12..17 -> "afternoon"
            else -> "evening"
        }
    }
    
    val greetingText = when(greeting) {
        "morning" -> stringResource(R.string.dashboard_greeting_morning)
        "afternoon" -> stringResource(R.string.dashboard_greeting_afternoon)
        else -> stringResource(R.string.dashboard_greeting_evening)
    }

    // Responsive layout support
    val windowInfo = LocalWindowInfo.current
    val horizontalPadding = windowInfo.contentPadding
    val maxWidth = windowInfo.maxContentWidth
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(
                    if (maxWidth != androidx.compose.ui.unit.Dp.Unspecified) {
                        Modifier.widthIn(max = maxWidth)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp), // Increased spacing for a breathable modern feel
            contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp)
        ) {


            // Header Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    AnvilHeader(
                        title = greetingText,
                        subtitle = "Let's make today count.",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onNavigateToPage?.invoke(5) },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings_button),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                }
            }


            // Hero Section: Motivation
            item {
                MotivationCard(
                    dailyProgress = dailyProgress,
                    pendingCount = totalPendingCount,
                    quote = dailyQuote,
                    streak = currentStreak
                )
            }
            
            // Streak Section
            if (hasDailyTasks) {
                item {
                    StreakCard(streak = currentStreak)
                }
            }

            // Forge Level Card
            item {
                ForgeLevelCard(
                    onClick = { onNavigateToForge?.invoke() }
                )
            }

            // Forge Economy Card - Coins + Savings + Shop
            item {
                AnvilCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp), // Increased padding
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coin balance
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(ForgedGold.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("\uD83D\uDCB0", fontSize = 24.sp)
                            }
                            Spacer(Modifier.width(16.dp)) // Increased spacer
                            Column {
                                Text(
                                    "$coinBalance",
                                    style = MaterialTheme.typography.headlineMedium, // Larger typography
                                    fontWeight = FontWeight.Black, // Heavier weight
                                    color = ForgedGold
                                )
                                Text(
                                    "Forge Coins",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        // Quick action buttons
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                            FilledTonalButton(
                                onClick = { onNavigateToSavings?.invoke() },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = ForgedGold.copy(alpha = 0.1f),
                                    contentColor = ForgedGold
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Savings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            FilledTonalButton(
                                onClick = { onNavigateToShop?.invoke() },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = ElectricBlue.copy(alpha = 0.1f),
                                    contentColor = ElectricBlue
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Shop", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Active Quests Card (Moved Up optionally, keeping originally ordering but styled)
            item {
                AnvilCard(
                    onClick = { onNavigateToReport?.invoke() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp), // Increased padding
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(ForgedGold.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("\uD83D\uDCCA", fontSize = 24.sp)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Forge Report",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "View productivity trends",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Icon(
                            Icons.Outlined.Assessment,
                            contentDescription = null,
                            tint = ForgedGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Active Quests Card (Actual Quests)
            item {
                AnvilCard(
                    onClick = { onNavigateToQuests?.invoke() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp), // Matched padding with Report Card
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(ElectricBlue.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("\u2694\uFE0F", fontSize = 24.sp)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Active Quests",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Tap to view quest log",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Quick Stats Grid - 2x2 layout
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Pending
                        StatChip(
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToPage?.invoke(1) },
                            icon = Icons.Outlined.Schedule,
                            label = "Pending",
                            value = "$totalPendingCount",
                            color = InfoBlue
                        )
                        // Completed
                        StatChip(
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToPage?.invoke(1) },
                            icon = Icons.Outlined.CheckCircle,
                            label = "Done",
                            value = "$completedTodayCount",
                            color = ElectricTeal
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Bonus
                        StatChip(
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToPage?.invoke(4) },
                            icon = Icons.Outlined.Star,
                            label = "Bonus",
                            value = "$bonusTaskCount",
                            color = WarningOrange
                        )
                        // Focus
                        StatChip(
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToFocus?.invoke() },
                            icon = Icons.Outlined.Timer,
                            label = "Focus",
                            value = "▶",
                            color = ElectricBlue
                        )
                    }
                }
            }
            
            // Grace Period & Exchange
            if (graceDays > 0 || bonusTaskCount >= 5) {
                item {
                    AnvilCard(
                        onClick = {
                            if (bonusTaskCount >= 5) {
                                showGraceExchangeDialog = true
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Star, 
                                        null, 
                                        tint = ForgedGold, 
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "REWARD SYSTEM",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.2.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = ForgedGold
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Ice Available: $graceDays 🧊",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (bonusTaskCount >= 5) "Exchange 5 bonus tasks for 1 Ice" 
                                    else "Need ${5 - bonusTaskCount} more bonus tasks for an Ice",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (bonusTaskCount >= 5) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(ForgedGold.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Warning, "Exchange", tint = ForgedGold, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }


            // Finance & Shield Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Budget Card
                    AnvilCard(
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToPage?.invoke(2) }
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Outlined.AccountBalanceWallet,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                    .padding(12.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Budget", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                currencyFormat.format(cashBalance + gcashBalance),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Shield Card
                    AnvilCard(
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToPage?.invoke(3) }
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Shield,
                                null,
                                tint = ElectricTeal,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(ElectricTeal.copy(alpha = 0.1f), CircleShape)
                                    .padding(12.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Blocked", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${blockedApps.size} apps",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Active Loans
            if (activeLoans.isNotEmpty()) {
                item {
                    AnvilCard(
                        onClick = { onNavigateToPage?.invoke(2) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp), // Matched padding
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Warning, 
                                        null, 
                                        tint = ForgedGold, 
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "ACTIVE DEBT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.2.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = ForgedGold
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    currencyFormat.format(totalActiveLoanedAmount),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${activeLoans.size} borrowers",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(ForgedGold.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.People,
                                    null,
                                    tint = ForgedGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }


            // Consistency Graph
            item {
               Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Consistency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    AnvilCard {
                        Box(modifier = Modifier.padding(24.dp)) {
                           ContributionGraph(
                                allTasks = allTasks,
                                bonusTasks = bonusTasks,
                                habitContributions = habitContributions
                            )
                        }
                    }
               }
            }
        }
    }
    
    if (showGraceExchangeDialog) {

        AlertDialog(
            onDismissRequest = { showGraceExchangeDialog = false },
            title = { Text("Redeem Ice?") },
            text = { Text("Exchange 5 bonus tasks for 1 Ice (Streak Freeze)? This will protect your streak if you miss a day.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.tryExchangeBonusForGrace()
                        showGraceExchangeDialog = false
                    }
                ) {
                    Text("Redeem")
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
fun StatChip(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    AnvilCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}