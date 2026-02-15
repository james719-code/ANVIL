package com.james.anvil.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.core.AchievementManager
import com.james.anvil.core.LevelManager
import com.james.anvil.data.Achievement
import com.james.anvil.data.XpSource
import com.james.anvil.ui.components.AnvilCard
import com.james.anvil.ui.theme.DesignTokens
import com.james.anvil.ui.theme.ElectricBlue
import com.james.anvil.ui.theme.ElectricTeal
import com.james.anvil.ui.theme.ForgedGold
import com.james.anvil.ui.theme.ForgedGoldLight
import com.james.anvil.ui.theme.LevelBadgeBg
import com.james.anvil.ui.theme.SuccessGreen
import com.james.anvil.ui.theme.XpBarFill
import com.james.anvil.ui.theme.XpBarTrack
import com.james.anvil.ui.theme.XpGold
import com.james.anvil.ui.viewmodel.LevelViewModel
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgeProfileScreen(
    levelViewModel: LevelViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val totalXp by levelViewModel.totalXp.collectAsState()
    val currentLevel by levelViewModel.currentLevel.collectAsState()
    val currentTitle by levelViewModel.currentTitle.collectAsState()
    val xpProgress by levelViewModel.xpProgress.collectAsState()
    val xpForNextLevel by levelViewModel.xpForNextLevel.collectAsState()
    val xpForCurrentLevel by levelViewModel.xpForCurrentLevel.collectAsState()
    val recentEntries by levelViewModel.recentXpEntries.collectAsState(initial = emptyList())

    // Achievements
    val context = LocalContext.current
    val achievementManager = remember { AchievementManager(context) }
    val achievements = remember(totalXp, currentLevel) {
        achievementManager.evaluateAchievements(
            completedTaskCount = 0, // Will be filled from actual stats later
            currentStreak = 0,
            longestStreak = 0,
            currentLevel = currentLevel,
            budgetEntryCount = 0,
            focusSessionCount = 0,
            totalFocusMinutes = 0,
            bonusTaskCount = 0,
            iceCount = 0,
            loansCleared = 0
        )
    }
    val unlockedCount = achievements.count { it.isUnlocked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The Forge", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLg),
            contentPadding = PaddingValues(
                start = DesignTokens.SpacingMd,
                end = DesignTokens.SpacingMd,
                top = DesignTokens.SpacingMd,
                bottom = DesignTokens.SpacingXl
            )
        ) {
            // Level Badge Section
            item {
                LevelHeroCard(
                    level = currentLevel,
                    title = currentTitle,
                    totalXp = totalXp,
                    xpProgress = xpProgress,
                    xpForCurrentLevel = xpForCurrentLevel,
                    xpForNextLevel = xpForNextLevel
                )
            }

            // Level Roadmap
            item {
                LevelRoadmapCard(currentLevel = currentLevel)
            }

            // Achievements Gallery
            item {
                AchievementsGallery(
                    achievements = achievements,
                    unlockedCount = unlockedCount
                )
            }

            // Activity Feed Header
            item {
                Text(
                    text = "Recent Forging Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = DesignTokens.SpacingSm)
                )
            }

            // Activity Feed
            if (recentEntries.isEmpty()) {
                item {
                    AnvilCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(DesignTokens.SpacingXl),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = ForgedGold.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                                Text(
                                    text = "Complete tasks to start earning XP!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(recentEntries, key = { it.id }) { entry ->
                    XpActivityItem(
                        xpAmount = entry.xpAmount,
                        source = entry.source,
                        label = entry.sourceLabel,
                        timestamp = entry.timestamp
                    )
                }
            }
        }
    }
}

// ============================================
// LEVEL HERO CARD
// ============================================

@Composable
private fun LevelHeroCard(
    level: Int,
    title: String,
    totalXp: Int,
    xpProgress: Float,
    xpForCurrentLevel: Int,
    xpForNextLevel: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = xpProgress,
        animationSpec = tween(durationMillis = 800),
        label = "xp_progress"
    )

    // Subtle glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    AnvilCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.SpacingLg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Level Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .shadow(12.dp, CircleShape, ambientColor = XpGold.copy(alpha = glowAlpha))
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                LevelBadgeBg,
                                LevelBadgeBg.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .border(2.dp, ForgedGold.copy(alpha = 0.6f), CircleShape)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$level",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = XpGold,
                        fontSize = 36.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ForgedGold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Total XP
            Text(
                text = "$totalXp XP Total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            // XP Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (xpForNextLevel > 0) "Level ${level + 1}" else "MAX LEVEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (xpForNextLevel > 0) "$totalXp / $xpForNextLevel XP" else "✦ Maxed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = XpGold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom XP bar with gradient
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    // Track
                    drawRoundRect(
                        color = XpBarTrack,
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                    // Fill
                    if (animatedProgress > 0f) {
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(ForgedGold, XpBarFill, ForgedGoldLight),
                                start = Offset.Zero,
                                end = Offset(size.width * animatedProgress, 0f)
                            ),
                            size = size.copy(width = size.width * animatedProgress),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// LEVEL ROADMAP
// ============================================

@Composable
private fun LevelRoadmapCard(currentLevel: Int) {
    AnvilCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.SpacingMd)
        ) {
            Text(
                text = "Forge Roadmap",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            LevelManager.TITLES.forEachIndexed { index, title ->
                val level = index + 1
                val isUnlocked = currentLevel >= level
                val isCurrent = currentLevel == level
                val themeUnlock = LevelManager.THEME_UNLOCKS[index]
                val xpNeeded = LevelManager.LEVEL_THRESHOLDS[index]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isCurrent) {
                                Modifier
                                    .background(
                                        ForgedGold.copy(alpha = 0.08f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, ForgedGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            } else Modifier
                        )
                        .padding(horizontal = DesignTokens.SpacingSm, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Level indicator
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isUnlocked) ForgedGold.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        if (isUnlocked) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = XpGold,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "$level",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        if (themeUnlock != null) {
                            Text(
                                text = "🎨 Unlocks: $themeUnlock theme",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUnlocked) ForgedGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }

                    Text(
                        text = "${xpNeeded} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnlocked) ForgedGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }

                if (index < LevelManager.TITLES.lastIndex) {
                    // Connector line
                    Box(
                        modifier = Modifier
                            .padding(start = 13.dp) // Center under the circle
                            .width(2.dp)
                            .height(4.dp)
                            .background(
                                if (isUnlocked) ForgedGold.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                    )
                }
            }
        }
    }
}
// ============================================
// ACHIEVEMENTS GALLERY
// ============================================

@Composable
private fun AchievementsGallery(
    achievements: List<Achievement>,
    unlockedCount: Int
) {
    AnvilCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.SpacingMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$unlockedCount / ${achievements.size}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ForgedGold
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            // Grid of achievement badges
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(
                    // Calculate height based on rows needed
                    ((achievements.size + 3) / 4 * 96).dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(achievements) { achievement ->
                    AchievementBadge(achievement)
                }
            }
        }
    }
}

@Composable
private fun AchievementBadge(achievement: Achievement) {
    Column(
        modifier = Modifier
            .background(
                if (achievement.isUnlocked)
                    ForgedGold.copy(alpha = 0.08f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (achievement.isUnlocked) achievement.icon else "🔒",
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = achievement.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (achievement.isUnlocked)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

// ============================================
// XP ACTIVITY ITEM
// ============================================

@Composable
private fun XpActivityItem(
    xpAmount: Int,
    source: XpSource,
    label: String,
    timestamp: Long
) {
    val (icon, iconColor) = when (source) {
        XpSource.TASK -> Icons.Outlined.CheckCircle to ElectricBlue
        XpSource.BONUS -> Icons.Outlined.Bolt to ElectricTeal
        XpSource.STREAK -> Icons.Outlined.LocalFireDepartment to ForgedGold
        XpSource.BUDGET -> Icons.Outlined.Receipt to SuccessGreen
        XpSource.LOAN -> Icons.Outlined.Payments to ForgedGold
        XpSource.FOCUS -> Icons.Outlined.CheckCircle to ElectricTeal
    }

    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val timeText = dateFormat.format(Date(timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp)
            )
            .padding(DesignTokens.SpacingMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f))
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "+$xpAmount XP",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = XpGold
        )
    }
}

// ============================================
// COMPACT FORGE CARD (for Dashboard)
// ============================================

@Composable
fun ForgeLevelCard(
    levelViewModel: LevelViewModel = hiltViewModel(),
    onClick: () -> Unit = {}
) {
    val totalXp by levelViewModel.totalXp.collectAsState()
    val currentLevel by levelViewModel.currentLevel.collectAsState()
    val currentTitle by levelViewModel.currentTitle.collectAsState()
    val xpProgress by levelViewModel.xpProgress.collectAsState()
    val xpForNextLevel by levelViewModel.xpForNextLevel.collectAsState()

    val animatedProgress by animateFloatAsState(
        targetValue = xpProgress,
        animationSpec = tween(durationMillis = 600),
        label = "dashboard_xp"
    )

    AnvilCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.SpacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini level badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(LevelBadgeBg, LevelBadgeBg.copy(alpha = 0.8f))
                        )
                    )
                    .border(1.5.dp, ForgedGold.copy(alpha = 0.5f), CircleShape)
            ) {
                Text(
                    text = "$currentLevel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = XpGold
                )
            }

            Spacer(modifier = Modifier.width(DesignTokens.SpacingMd))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ForgedGold
                    )
                    Text(
                        text = "$totalXp XP",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Mini XP bar
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                ) {
                    drawRoundRect(
                        color = XpBarTrack,
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                    )
                    if (animatedProgress > 0f) {
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(ForgedGold, XpBarFill),
                                start = Offset.Zero,
                                end = Offset(size.width * animatedProgress, 0f)
                            ),
                            size = size.copy(width = size.width * animatedProgress),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                if (xpForNextLevel > 0) {
                    Text(
                        text = "${xpForNextLevel - totalXp} XP to next level",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        text = "✦ Maximum level reached",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForgedGold.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
