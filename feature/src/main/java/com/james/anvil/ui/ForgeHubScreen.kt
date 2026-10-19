package com.james.anvil.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.ui.components.ForgeActionTile
import com.james.anvil.ui.components.PageHeader
import com.james.anvil.ui.components.ProgressRing
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
            contentPadding = PaddingValues(bottom = DesignTokens.SpacingLg),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLg)
        ) {
            item {
                PageHeader(
                    title = "The Forge",
                    subtitle = "Level up, earn rewards, sharpen skills"
                )
            }

            item {
                ForgeLevelCard(
                    level = currentLevel,
                    title = currentTitle,
                    xp = totalXp,
                    xpForNext = xpForNextLevel,
                    coins = coinBalance,
                    onClick = onNavigateToForgeProfile
                )
            }

            item {
                SectionTitle(title = "Progression & gear")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ForgeGridTile(
                            label = "Skills",
                            supporting = "Unlock new abilities and perks",
                            icon = Icons.Outlined.AccountTree,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = onNavigateToSkillTree
                        )
                        ForgeGridTile(
                            label = "Equipment",
                            supporting = "Manage your forge gear",
                            icon = Icons.Outlined.MilitaryTech,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = onNavigateToGearEquipment
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ForgeGridTile(
                            label = "Shop",
                            supporting = "Spend coins on upgrades",
                            icon = Icons.Outlined.Storefront,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = onNavigateToForgeShop
                        )
                        ForgeGridTile(
                            label = "Quests",
                            supporting = "Complete bonus objectives",
                            icon = Icons.Outlined.EmojiEvents,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = onNavigateToQuestLog
                        )
                    }
                }
            }

            item {
                SectionTitle(title = "Performance & focus")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

/**
 * Tile for the two-column grid. The icon sits above the text so the description
 * gets the full tile width instead of being cut off after a few words.
 */
@Composable
private fun ForgeGridTile(
    label: String,
    supporting: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(DesignTokens.RadiusLarge),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Level + XP + coins in one glanceable card; the whole card opens the profile. */
@Composable
private fun ForgeLevelCard(
    level: Int,
    title: String,
    xp: Int,
    xpForNext: Int,
    coins: Int,
    onClick: () -> Unit
) {
    val progress = if (xpForNext > 0) (xp.toFloat() / xpForNext).coerceIn(0f, 1f) else 0f
    SectionCard(onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProgressRing(progress = progress, size = 64.dp, strokeWidth = 6.dp) {
                Text(
                    text = "$level",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Level $level",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$xp / $xpForNext XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$coins",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "COINS",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
