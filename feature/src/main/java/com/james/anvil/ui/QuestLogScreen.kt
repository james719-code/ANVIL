package com.james.anvil.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.data.Quest
import com.james.anvil.data.QuestType
import com.james.anvil.ui.components.AnvilCard
import com.james.anvil.ui.components.EmptyState
import com.james.anvil.ui.components.PageHeader
import com.james.anvil.ui.components.RewardChip
import com.james.anvil.ui.components.TopLevelPageScaffold
import com.james.anvil.ui.theme.*
import com.james.anvil.ui.viewmodel.QuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestLogScreen(
    onBack: () -> Unit,
    viewModel: QuestViewModel = hiltViewModel()
) {
    val dailyQuests by viewModel.dailyQuests.collectAsState()
    val weeklyChain by viewModel.weeklyChain.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    val windowInfo = LocalWindowInfo.current

    TopLevelPageScaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .then(
                    if (windowInfo.maxContentWidth != androidx.compose.ui.unit.Dp.Unspecified) {
                        Modifier.widthIn(max = windowInfo.maxContentWidth)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = windowInfo.contentPadding)
        ) {
            PageHeader(
                eyebrow = "Quests",
                title = "Quest Log",
                subtitle = "Track daily contracts, weekly chains, XP, and coin rewards.",
                trailing = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            // Tab selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Daily") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Weekly") }
                )
            }

            when (selectedTab) {
                0 -> DailyQuestsTab(dailyQuests)
                1 -> WeeklyChainTab(weeklyChain)
            }
        }
    }
}

@Composable
private fun DailyQuestsTab(quests: List<Quest>) {
    if (quests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                message = "No active quests",
                subtitle = "New quests appear at midnight.",
                icon = Icons.Outlined.Flag
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(quests, key = { it.id }) { quest ->
                QuestCard(quest)
            }
        }
    }
}

@Composable
private fun WeeklyChainTab(quests: List<Quest>) {
    if (quests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                message = "No weekly chain active",
                subtitle = "New chains start on Monday.",
                icon = Icons.Outlined.Bolt
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Weekly Quest Chain",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(quests, key = { it.id }) { quest ->
                WeeklyStepCard(quest, quests)
            }
        }
    }
}

@Composable
private fun QuestCard(quest: Quest) {
    val progress = if (quest.targetValue > 0) quest.currentValue.toFloat() / quest.targetValue else 0f

    AnvilCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        quest.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (quest.isCompleted) SuccessGreen
                            else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        quest.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                if (quest.isCompleted) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Completed",
                        tint = SuccessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(XpBarTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (quest.isCompleted) SuccessGreen
                            else ElectricBlue
                        )
                )
            }

            Spacer(Modifier.height(8.dp))

            // Progress text and rewards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${quest.currentValue}/${quest.targetValue}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (quest.rewardCoins > 0) {
                        RewardChip(
                            label = "${quest.rewardCoins}",
                            icon = Icons.Outlined.Star,
                            tint = ForgedGold
                        )
                    }
                    if (quest.rewardXp > 0) {
                        RewardChip(
                            label = "+${quest.rewardXp} XP",
                            icon = Icons.Outlined.Bolt,
                            tint = XpGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyStepCard(quest: Quest, allSteps: List<Quest>) {
    val stepIndex = quest.weekChainStep
    val isBoss = quest.questType == QuestType.WEEKLY_BOSS

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            if (quest.isCompleted) {
                Icon(
                    Icons.Filled.CheckCircle,
                    null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(20.dp)
                )
            } else if (isBoss) {
                Icon(
                    Icons.Outlined.Bolt,
                    null,
                    tint = ErrorRed,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Outlined.RadioButtonUnchecked,
                    null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
            // Connecting line (except for last step)
            if (stepIndex < allSteps.size - 1) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(
                            if (quest.isCompleted) SuccessGreen.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Quest card content
        QuestCard(quest)
    }
}
