package com.james.anvil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.james.anvil.ui.components.AddBonusTaskBottomSheet
import com.james.anvil.ui.components.AnvilCard
import com.james.anvil.ui.components.AnvilHeader
import com.james.anvil.ui.components.ContributionGraph
import com.james.anvil.ui.components.MotivationCard
import com.james.anvil.ui.theme.ElectricTeal
import com.james.anvil.ui.theme.ForgedGold
import com.james.anvil.ui.theme.InfoBlue
import com.james.anvil.ui.theme.WarningOrange
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TaskViewModel,
    onNavigateToPage: ((Int) -> Unit)? = null
) {
    val dailyProgress by viewModel.dailyProgress.collectAsState(initial = 0f)
    val totalPendingCount by viewModel.totalPendingCount.collectAsState(initial = 0)
    val dailyQuote by viewModel.dailyQuote.collectAsState(initial = "")
    val completedTasks by viewModel.completedTasks.collectAsState(initial = emptyList())
    val blockedApps by viewModel.blockedApps.collectAsState(initial = emptyList())
    val blockedLinks by viewModel.blockedLinks.collectAsState(initial = emptyList())
    val bonusTaskCount by viewModel.bonusTaskCount.collectAsState(initial = 0)
    val cashBalance by viewModel.cashBalance.collectAsState(initial = 0.0)
    val gcashBalance by viewModel.gcashBalance.collectAsState(initial = 0.0)
    val totalActiveLoanedAmount by viewModel.totalActiveLoanedAmount.collectAsState(initial = 0.0)
    val activeLoans by viewModel.activeLoans.collectAsState(initial = emptyList())
    
    val graceDays = viewModel.getGraceDaysCount() // Note: This is a function, not a flow in ViewModel. Consider making it reactive if needed, but for now we'll trust the viewmodel update flow.
    // Actually, viewModel.bonusTasks triggers updates, but grace days calculation depends on bonusManager which uses SharedPreferences.
    // It's better if we just assume the value is updated when bonusTaskCount changes or screen recomposes.
    
    var showGraceExchangeDialog by remember { mutableStateOf(false) }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    val completedTodayCount = completedTasks.count {
        val calendar = java.util.Calendar.getInstance()
        val todayYear = calendar.get(java.util.Calendar.YEAR)
        val todayDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        calendar.timeInMillis = it.completedAt ?: 0L
        calendar.get(java.util.Calendar.YEAR) == todayYear && calendar.get(java.util.Calendar.DAY_OF_YEAR) == todayDay
    }

    // Time-based greeting
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {


            // Header Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    AnvilHeader(
                        title = greeting,
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
                            contentDescription = "Settings",
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
                    quote = dailyQuote
                )
            }

            // Quick Stats Row
            item {
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
                    // Bonus
                    StatChip(
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToPage?.invoke(4) },
                        icon = Icons.Outlined.Star,
                        label = "Bonus",
                        value = "$bonusTaskCount",
                        color = WarningOrange
                    )

                }
            }
            
            // Grace Period & Exchange
            if (graceDays > 0 || bonusTaskCount >= 3) {
                  item {
                    AnvilCard(
                        onClick = {
                            if (bonusTaskCount >= 3) {
                                showGraceExchangeDialog = true
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Star, 
                                        null, 
                                        tint = ForgedGold, 
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "REWARD SYSTEM",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = ForgedGold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Grace Days: $graceDays",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    if (bonusTaskCount >= 3) "Can exchange bonus tasks now" 
                                    else "Need ${3 - bonusTaskCount} more bonus tasks for a grace day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (bonusTaskCount >= 3) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(ForgedGold.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Warning, "Exchange", tint = ForgedGold, modifier = Modifier.size(20.dp))
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
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(
                                Icons.Outlined.AccountBalanceWallet,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Budget", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                currencyFormat.format(cashBalance + gcashBalance),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Shield,
                                null,
                                tint = ElectricTeal,
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(ElectricTeal.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Blocked", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${blockedApps.size} apps",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Warning, 
                                        null, 
                                        tint = ForgedGold, 
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "ACTIVE DEBT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = ForgedGold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    currencyFormat.format(totalActiveLoanedAmount),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${activeLoans.size} borrowers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(ForgedGold.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
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
               Column {
                   Text(
                       text = "Habit Consistency",
                       style = MaterialTheme.typography.titleLarge,
                       fontWeight = FontWeight.Bold,
                       color = MaterialTheme.colorScheme.onBackground,
                       modifier = Modifier.padding(bottom = 12.dp)
                   )
                   AnvilCard {
                       Box(modifier = Modifier.padding(16.dp)) {
                           ContributionGraph(completedTasks = completedTasks)
                       }
                   }
               }
            }
        }
    }
    
    if (showGraceExchangeDialog) {

        AlertDialog(
            onDismissRequest = { showGraceExchangeDialog = false },
            title = { Text("Redeem Grace Day?") },
            text = { Text("Exchange 3 bonus tasks for 1 grace day? This will protect you from blocking penalties for one day.") },
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

        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
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
        }
    }
}