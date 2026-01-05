package com.james.anvil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.james.anvil.ui.components.AnvilCard
import com.james.anvil.ui.components.AnvilHeader
import com.james.anvil.ui.components.ContributionGraph
import com.james.anvil.ui.components.MotivationCard
import com.james.anvil.ui.navigation.BudgetRoute
import com.james.anvil.ui.navigation.LoansRoute
import com.james.anvil.ui.theme.ElectricTeal
import com.james.anvil.ui.theme.InfoBlue
import com.james.anvil.ui.theme.WarningOrange
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: TaskViewModel, navController: NavController? = null) {
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp), // Increased horizontal padding
            verticalArrangement = Arrangement.spacedBy(20.dp), // Increased spacing
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp) // Bottom padding for nav bar
        ) {
            // Header Section
            item {
                AnvilHeader(
                    title = greeting,
                    subtitle = "Let's make today count."
                )
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
                        icon = Icons.Outlined.Schedule,
                        label = "Pending",
                        value = "$totalPendingCount",
                        color = InfoBlue
                    )
                    // Completed
                    StatChip(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.CheckCircle,
                        label = "Done",
                        value = "$completedTodayCount",
                        color = ElectricTeal
                    )
                    // Bonus
                    if (bonusTaskCount > 0) {
                        StatChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Star,
                            label = "Bonus",
                            value = "$bonusTaskCount",
                            color = WarningOrange
                        )
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
                        onClick = { navController?.navigate(BudgetRoute) }
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
                        modifier = Modifier.weight(1f)
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
                            Text("Protected", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha=0.3f), // Warning tint
                        onClick = { navController?.navigate(LoansRoute) }
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
                                    Icon(Icons.Filled.Warning, null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Active Loans",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = WarningOrange
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    currencyFormat.format(totalActiveLoanedAmount),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${activeLoans.size} borrowers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Outlined.People,
                                null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha=0.5f), RoundedCornerShape(16.dp))
                                    .padding(10.dp)
                            )
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
}

@Composable
fun StatChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    AnvilCard(modifier = modifier) {
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