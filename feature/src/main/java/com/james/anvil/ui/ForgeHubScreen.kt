package com.james.anvil.ui

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
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.ui.components.ForgeActionTile
import com.james.anvil.ui.components.PageHeader
import com.james.anvil.ui.components.SectionCard
import com.james.anvil.ui.components.SectionTitle
import com.james.anvil.ui.components.TopLevelPageScaffold
import com.james.anvil.ui.theme.DesignTokens
import com.james.anvil.ui.theme.LocalWindowInfo
import com.james.anvil.ui.viewmodel.ForgeCoinViewModel
import com.james.anvil.ui.viewmodel.LevelViewModel

@Composable
fun ForgeHubScreen(
    onNavigateToForgeProfile: () -> Unit = {},
    onNavigateToSkillTree: () -> Unit = {},
    onNavigateToGearEquipment: () -> Unit = {},
    onNavigateToForgeReport: () -> Unit = {},
    onNavigateToForgeShop: () -> Unit = {},
    onNavigateToQuestLog: () -> Unit = {},
    onNavigateToFocusSession: () -> Unit = {},
    onNavigateToSavingsGoals: () -> Unit = {}
) {
    val levelViewModel: LevelViewModel = hiltViewModel()
    val forgeCoinViewModel: ForgeCoinViewModel = hiltViewModel()

    val currentLevel by levelViewModel.currentLevel.collectAsState(initial = 1)
    val totalXp by levelViewModel.totalXp.collectAsState(initial = 0)
    val xpForNextLevel by levelViewModel.xpForNextLevel.collectAsState(initial = 100)
    val currentTitle by levelViewModel.currentTitle.collectAsState(initial = "Apprentice")
    val coinBalance by forgeCoinViewModel.coinBalance.collectAsState(initial = 0)

    val windowInfo = LocalWindowInfo.current

    TopLevelPageScaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .then(
                    if (windowInfo.maxContentWidth != androidx.compose.ui.unit.Dp.Unspecified) {
                        Modifier.widthIn(max = windowInfo.maxContentWidth)
                    } else Modifier
                )
                .padding(horizontal = windowInfo.contentPadding),
            contentPadding = PaddingValues(top = DesignTokens.SpacingSm, bottom = DesignTokens.SpacingLg),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXl)
        ) {
            item {
                PageHeader(
                    eyebrow = "Progression",
                    title = "The Forge",
                    subtitle = "Level up, earn rewards, and sharpen your skills."
                )
            }

            item {
                SectionCard(onClick = onNavigateToForgeProfile) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { totalXp.toFloat() / xpForNextLevel.toFloat() },
                                modifier = Modifier.size(56.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 4.dp
                            )
                            Text(
                                text = "$currentLevel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Level $currentLevel — $currentTitle",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalXp / $xpForNextLevel XP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$coinBalance",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "COINS",
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle(title = "Progression & gear")
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ForgeActionTile(
                                label = "Skills",
                                supporting = "Unlock new abilities and perks",
                                icon = Icons.Outlined.AccountTree,
                                onClick = onNavigateToSkillTree
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            ForgeActionTile(
                                label = "Equipment",
                                supporting = "Manage your forge gear",
                                icon = Icons.Outlined.MilitaryTech,
                                onClick = onNavigateToGearEquipment
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ForgeActionTile(
                                label = "Shop",
                                supporting = "Spend coins on upgrades",
                                icon = Icons.Outlined.Storefront,
                                onClick = onNavigateToForgeShop
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            ForgeActionTile(
                                label = "Quests",
                                supporting = "Complete bonus objectives",
                                icon = Icons.Outlined.EmojiEvents,
                                onClick = onNavigateToQuestLog
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle(title = "Performance & focus")
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ForgeActionTile(
                        label = "Forge report",
                        supporting = "Analyze productivity trends and XP breakdowns",
                        icon = Icons.Outlined.Assessment,
                        onClick = onNavigateToForgeReport
                    )
                    ForgeActionTile(
                        label = "Focus session",
                        supporting = "Launch a timer block for deep work",
                        icon = Icons.Outlined.Timer,
                        onClick = onNavigateToFocusSession
                    )
                    ForgeActionTile(
                        label = "Savings goals",
                        supporting = "Set targets and track long-term rewards",
                        icon = Icons.Outlined.Savings,
                        onClick = onNavigateToSavingsGoals
                    )
                }
            }
        }
    }
}
