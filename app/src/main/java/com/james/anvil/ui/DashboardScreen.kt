package com.james.anvil.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.ui.components.ConsistencyChart
import com.james.anvil.ui.components.MotivationCard

@Composable
fun DashboardScreen(viewModel: TaskViewModel) {
    val dailyProgress by viewModel.dailyProgress.collectAsState(initial = 0f)

    val totalPendingCount by viewModel.totalPendingCount.collectAsState(initial = 0)

    val dailyQuote by viewModel.dailyQuote.collectAsState(initial = "")
    val completedTasks by viewModel.completedTasks.collectAsState(initial = emptyList())
    val blockedApps by viewModel.blockedApps.collectAsState(initial = emptyList())
    val blockedLinks by viewModel.blockedLinks.collectAsState(initial = emptyList())

    
    val completedTodayCount = completedTasks.count {
        val calendar = java.util.Calendar.getInstance()
        val todayYear = calendar.get(java.util.Calendar.YEAR)
        val todayDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        calendar.timeInMillis = it.completedAt ?: 0L
        calendar.get(java.util.Calendar.YEAR) == todayYear && calendar.get(java.util.Calendar.DAY_OF_YEAR) == todayDay
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Dashboard",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            MotivationCard(
                dailyProgress = dailyProgress,
                pendingCount = totalPendingCount, 
                quote = dailyQuote
            )
        }

        item {
            TaskOverviewCard(totalPendingCount, completedTodayCount)
        }

        item {
            BlockedStatsCard(blockedApps.size, blockedLinks.size)
        }

        item {
            Text(
                text = "Consistency",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            ConsistencyChart(completedTasks = completedTasks)
        }
    }
}

@Composable
fun TaskOverviewCard(pending: Int, completed: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(text = "$pending", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(text = "Pending", style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(text = "$completed", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = "Completed Today", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun BlockedStatsCard(blockedAppsCount: Int, blockedLinksCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Shield Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Blocked Apps: $blockedAppsCount")
                Text("Blocked Links: $blockedLinksCount")
            }
        }
    }
}