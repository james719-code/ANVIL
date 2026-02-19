package com.james.anvil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.core.SkillDefinition
import com.james.anvil.core.SkillTreeManager
import com.james.anvil.data.SkillBranch
import com.james.anvil.data.SkillNode
import com.james.anvil.ui.components.AnvilCard
import com.james.anvil.ui.theme.*
import com.james.anvil.ui.viewmodel.SkillTreeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillTreeScreen(
    onBack: () -> Unit,
    viewModel: SkillTreeViewModel = hiltViewModel()
) {
    val allNodes by viewModel.allNodes.collectAsState()
    val availablePoints by viewModel.availablePoints.collectAsState()
    val unlockResult by viewModel.unlockResult.collectAsState()

    var selectedBranch by remember { mutableIntStateOf(0) }
    val branches = SkillBranch.entries.toList()

    LaunchedEffect(Unit) { viewModel.refreshPoints() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skill Tree", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Available Points Header
            AnvilCard(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Available Skill Points",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            "Level ${viewModel.getCurrentLevel()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Surface(
                        color = if (availablePoints > 0) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ) {
                        Text(
                            "$availablePoints",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (availablePoints > 0) Color.White
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Branch Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedBranch,
                containerColor = MaterialTheme.colorScheme.background,
                edgePadding = 16.dp
            ) {
                branches.forEachIndexed { index, branch ->
                    Tab(
                        selected = selectedBranch == index,
                        onClick = { selectedBranch = index },
                        text = {
                            Text(
                                branchDisplayName(branch),
                                fontWeight = if (selectedBranch == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Text(branchEmoji(branch), fontSize = 16.sp)
                        }
                    )
                }
            }

            // Skill Nodes
            val currentBranch = branches[selectedBranch]
            val skills = viewModel.getSkillsForBranch(currentBranch)
            val nodeMap = allNodes.associateBy { it.skillId }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        branchDescription(currentBranch),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(skills, key = { it.skillId }) { skillDef ->
                    val node = nodeMap[skillDef.skillId]
                    val isUnlocked = node?.isUnlocked == true

                    SkillNodeCard(
                        skillDef = skillDef,
                        isUnlocked = isUnlocked,
                        canUnlock = availablePoints > 0 && !isUnlocked,
                        branchColor = branchColor(currentBranch),
                        onUnlock = { viewModel.unlockSkill(skillDef.skillId) }
                    )
                }
            }
        }

        // Unlock result snackbar
        if (unlockResult != null) {
            LaunchedEffect(unlockResult) {
                kotlinx.coroutines.delay(2000)
                viewModel.dismissUnlockResult()
            }
        }
    }
}

@Composable
private fun SkillNodeCard(
    skillDef: SkillDefinition,
    isUnlocked: Boolean,
    canUnlock: Boolean,
    branchColor: Color,
    onUnlock: () -> Unit
) {
    val borderColor = when {
        isUnlocked -> branchColor
        canUnlock -> branchColor.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    }

    AnvilCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (canUnlock) Modifier.clickable { onUnlock() }
                else Modifier
            )
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tier indicator
            Surface(
                color = if (isUnlocked) branchColor else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (isUnlocked) {
                        Icon(
                            Icons.Outlined.Stars,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            "T${skillDef.tier}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (canUnlock) 0.8f else 0.3f
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    skillDef.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) branchColor
                    else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (canUnlock) 1f else 0.4f
                    )
                )
                Text(
                    skillDef.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (isUnlocked) 0.7f else 0.4f
                    )
                )
            }

            if (canUnlock) {
                Icon(
                    Icons.Outlined.LockOpen,
                    "Unlock available",
                    tint = branchColor,
                    modifier = Modifier.size(20.dp)
                )
            } else if (!isUnlocked) {
                Icon(
                    Icons.Outlined.Lock,
                    "Locked",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun branchDisplayName(branch: SkillBranch) = when (branch) {
    SkillBranch.DISCIPLINE -> "Discipline"
    SkillBranch.WEALTH -> "Wealth"
    SkillBranch.FOCUS -> "Focus"
    SkillBranch.GUARDIAN -> "Guardian"
}

private fun branchEmoji(branch: SkillBranch) = when (branch) {
    SkillBranch.DISCIPLINE -> "\u2694\uFE0F"
    SkillBranch.WEALTH -> "\uD83D\uDCB0"
    SkillBranch.FOCUS -> "\uD83E\uDDD8"
    SkillBranch.GUARDIAN -> "\uD83D\uDEE1\uFE0F"
}

private fun branchDescription(branch: SkillBranch) = when (branch) {
    SkillBranch.DISCIPLINE -> "Master the art of discipline. Boost XP from tasks and quests."
    SkillBranch.WEALTH -> "Grow your fortune. Increase savings bonuses and coin earnings."
    SkillBranch.FOCUS -> "Sharpen your focus. Enhance focus session XP and quest rewards."
    SkillBranch.GUARDIAN -> "Become a guardian. Deal more damage and weaken monsters."
}

private fun branchColor(branch: SkillBranch) = when (branch) {
    SkillBranch.DISCIPLINE -> ElectricBlue
    SkillBranch.WEALTH -> ForgedGold
    SkillBranch.FOCUS -> ElectricTeal
    SkillBranch.GUARDIAN -> ErrorRed
}
