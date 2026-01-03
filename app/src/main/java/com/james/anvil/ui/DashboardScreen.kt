package com.james.anvil.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.james.anvil.ui.components.ConsistencyChart
import com.james.anvil.ui.components.MotivationCard
import com.james.anvil.ui.theme.DeepTeal
import com.james.anvil.ui.theme.MutedTeal
import androidx.compose.ui.tooling.preview.Preview

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
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
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
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$pending",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Pending",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Divider(
                modifier = Modifier
                    .height(80.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = DeepTeal,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$completed",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepTeal
                )
                Text(
                    text = "Completed Today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BlockedStatsCard(blockedAppsCount: Int, blockedLinksCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Shield Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$blockedAppsCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Blocked Apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$blockedLinksCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MutedTeal
                    )
                    Text(
                        text = "Blocked Links",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// =============================================
// Preview Functions (Removed in Release Builds)
// =============================================

@Preview(name = "Task Overview - Light", showBackground = true)
@Composable
private fun TaskOverviewCardPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = false) {
        TaskOverviewCard(pending = 5, completed = 3)
    }
}

@Preview(name = "Task Overview - Dark", showBackground = true)
@Composable
private fun TaskOverviewCardDarkPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = true) {
        TaskOverviewCard(pending = 2, completed = 8)
    }
}

@Preview(name = "Blocked Stats - Light", showBackground = true)
@Composable
private fun BlockedStatsCardPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = false) {
        BlockedStatsCard(blockedAppsCount = 12, blockedLinksCount = 5)
    }
}

@Preview(name = "Blocked Stats - Dark", showBackground = true)
@Composable
private fun BlockedStatsCardDarkPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = true) {
        BlockedStatsCard(blockedAppsCount = 8, blockedLinksCount = 3)
    }
}