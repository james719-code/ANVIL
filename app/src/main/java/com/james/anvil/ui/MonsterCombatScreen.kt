package com.james.anvil.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.core.DamageSource
import com.james.anvil.data.GearRarity
import com.james.anvil.data.MonsterType
import com.james.anvil.ui.components.AnvilCard
import com.james.anvil.ui.theme.*
import com.james.anvil.ui.viewmodel.CombatViewModel
import com.james.anvil.ui.viewmodel.LootReveal
import kotlinx.coroutines.flow.collectLatest
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonsterCombatScreen(
    monsterId: Long,
    onBack: () -> Unit,
    viewModel: CombatViewModel = hiltViewModel()
) {
    val monster by viewModel.currentMonster.collectAsState()
    val combatLog by viewModel.combatLog.collectAsState()
    val lootReveal by viewModel.lootReveal.collectAsState()

    // Quiz state
    var showQuiz by remember { mutableStateOf(false) }
    var quizQuestion by remember { mutableStateOf<QuizQuestion?>(null) }

    LaunchedEffect(monsterId) {
        viewModel.setMonster(monsterId)
    }

    // Animate damage flash
    var damageFlash by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.damageEvents.collectLatest {
            damageFlash = true
            kotlinx.coroutines.delay(300)
            damageFlash = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monster Combat", fontWeight = FontWeight.Bold) },
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
        val currentMonster = monster
        if (currentMonster == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Monster Display
            MonsterDisplay(
                monster = currentMonster,
                damageFlash = damageFlash
            )

            // HP Bar
            MonsterHpBar(
                currentHp = currentMonster.currentHp,
                maxHp = currentMonster.maxHp,
                isDefeated = currentMonster.isDefeated
            )

            // Action Buttons
            if (!currentMonster.isDefeated) {
                CombatActions(
                    onTaskDamage = { viewModel.dealTaskDamage(2) },
                    onFocusDamage = { viewModel.dealFocusDamage(25) },
                    onQuizDamage = {
                        quizQuestion = generateQuizQuestion()
                        showQuiz = true
                    }
                )
            } else {
                // Defeated message
                AnvilCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("\u2694\uFE0F", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Monster Defeated!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Text(
                            "The path is now clear.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Combat Log
            CombatLogSection(combatLog)
        }

        // Loot Reveal Dialog
        if (lootReveal != null) {
            LootRevealDialog(
                loot = lootReveal!!,
                onDismiss = { viewModel.dismissLoot() }
            )
        }

        // Quiz Dialog
        if (showQuiz && quizQuestion != null) {
            QuizDialog(
                question = quizQuestion!!,
                onCorrect = {
                    viewModel.dealQuizDamage()
                    showQuiz = false
                },
                onDismiss = { showQuiz = false }
            )
        }
    }
}

@Composable
private fun MonsterDisplay(
    monster: com.james.anvil.data.Monster,
    damageFlash: Boolean
) {
    val bgColor by animateColorAsState(
        targetValue = if (damageFlash) ErrorRed.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(300),
        label = "damageFlash"
    )

    AnvilCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Monster emoji/icon
            Text(
                text = when {
                    monster.isDefeated -> "\uD83D\uDC80"
                    monster.monsterType == MonsterType.BOSS -> "\uD83D\uDC32"
                    monster.difficulty >= 3 -> "\uD83D\uDC79"
                    monster.difficulty >= 2 -> "\uD83D\uDC7E"
                    else -> "\uD83D\uDC7B"
                },
                fontSize = 64.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = monster.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (monster.monsterType == MonsterType.BOSS) {
                    Surface(
                        color = ErrorRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "BOSS",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                    }
                }
                Text(
                    "Difficulty: ${"★".repeat(monster.difficulty)}${"☆".repeat((5 - monster.difficulty).coerceAtLeast(0))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun MonsterHpBar(currentHp: Int, maxHp: Int, isDefeated: Boolean) {
    val hpFraction = if (maxHp > 0) currentHp.toFloat() / maxHp else 0f
    val animatedHp by animateFloatAsState(
        targetValue = hpFraction,
        animationSpec = tween(500),
        label = "hpBar"
    )

    val barColor = when {
        isDefeated -> Color.Gray
        hpFraction > 0.5f -> SuccessGreen
        hpFraction > 0.25f -> WarningOrange
        else -> ErrorRed
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "HP",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "$currentHp / $maxHp",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }

        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(XpBarTrack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedHp.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(6.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun CombatActions(
    onTaskDamage: () -> Unit,
    onFocusDamage: () -> Unit,
    onQuizDamage: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Attack!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onTaskDamage,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Task Hit", fontSize = 12.sp)
            }

            Button(
                onClick = onFocusDamage,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
            ) {
                Icon(Icons.Outlined.Bolt, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Focus Hit", fontSize = 12.sp)
            }

            Button(
                onClick = onQuizDamage,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ForgedGold)
            ) {
                Icon(Icons.Outlined.Quiz, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Challenge", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CombatLogSection(log: List<String>) {
    if (log.isEmpty()) return

    val listState = rememberLazyListState()
    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.size - 1)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Combat Log",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        AnvilCard(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 150.dp)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(log) { entry ->
                    Text(
                        entry,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LootRevealDialog(loot: LootReveal, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("\uD83C\uDF81", fontSize = 40.sp)
                Spacer(Modifier.height(4.dp))
                Text("Loot Dropped!", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Defeated ${loot.monsterName}", style = MaterialTheme.typography.bodyMedium)

                if (loot.coins > 0) {
                    Text(
                        "\uD83D\uDCB0 +${loot.coins} Forge Coins",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ForgedGold
                    )
                }

                if (loot.gearItem != null) {
                    val rarityColor = when (loot.gearItem.rarity) {
                        GearRarity.COMMON -> Color.Gray
                        GearRarity.RARE -> ElectricBlue
                        GearRarity.EPIC -> Color(0xFF9C27B0)
                        GearRarity.LEGENDARY -> ForgedGold
                    }

                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = rarityColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                loot.gearItem.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = rarityColor
                            )
                            Text(
                                loot.gearItem.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                "Slot: ${loot.gearItem.slot.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Collect!")
            }
        }
    )
}

// ============================================
// Quiz System
// ============================================

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

private fun generateQuizQuestion(): QuizQuestion {
    val questions = listOf(
        QuizQuestion("What is the capital of the Philippines?", listOf("Manila", "Cebu", "Davao", "Quezon City"), 0),
        QuizQuestion("Which planet is closest to the Sun?", listOf("Venus", "Mercury", "Mars", "Earth"), 1),
        QuizQuestion("What is 15 x 8?", listOf("100", "110", "120", "130"), 2),
        QuizQuestion("How many minutes in 2 hours?", listOf("100", "110", "120", "130"), 2),
        QuizQuestion("What does 'CPU' stand for?", listOf("Central Power Unit", "Central Processing Unit", "Computer Personal Unit", "Central Program Utility"), 1),
        QuizQuestion("Which is the largest ocean?", listOf("Atlantic", "Indian", "Arctic", "Pacific"), 3),
        QuizQuestion("What is the square root of 144?", listOf("10", "11", "12", "14"), 2),
        QuizQuestion("How many days in a leap year?", listOf("364", "365", "366", "367"), 2),
        QuizQuestion("What gas do plants absorb?", listOf("Oxygen", "Nitrogen", "Carbon Dioxide", "Hydrogen"), 2),
        QuizQuestion("What is 7 x 9?", listOf("56", "63", "72", "81"), 1),
        QuizQuestion("Which animal is the fastest?", listOf("Lion", "Cheetah", "Horse", "Eagle"), 1),
        QuizQuestion("What year did WW2 end?", listOf("1943", "1944", "1945", "1946"), 2),
        QuizQuestion("What is the chemical symbol for water?", listOf("O2", "H2O", "CO2", "NaCl"), 1),
        QuizQuestion("How many continents are there?", listOf("5", "6", "7", "8"), 2),
        QuizQuestion("What color do you get mixing blue and yellow?", listOf("Orange", "Purple", "Green", "Brown"), 2),
    )
    return questions.random()
}

@Composable
private fun QuizDialog(
    question: QuizQuestion,
    onCorrect: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf(-1) }
    var answered by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("\uD83E\uDDE0", fontSize = 32.sp)
                Spacer(Modifier.height(4.dp))
                Text("Challenge!", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(question.question, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))

                question.options.forEachIndexed { index, option ->
                    val bgColor = when {
                        !answered -> {
                            if (selectedIndex == index) ElectricBlue.copy(alpha = 0.2f)
                            else Color.Transparent
                        }
                        index == question.correctIndex -> SuccessGreen.copy(alpha = 0.2f)
                        index == selectedIndex -> ErrorRed.copy(alpha = 0.2f)
                        else -> Color.Transparent
                    }

                    Surface(
                        onClick = {
                            if (!answered) {
                                selectedIndex = index
                            }
                        },
                        color = bgColor,
                        shape = RoundedCornerShape(8.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                    ) {
                        Text(
                            option,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (answered) {
                    Text(
                        if (isCorrect) "Correct! Dealing 15 damage!" else "Wrong! No damage dealt.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrect) SuccessGreen else ErrorRed
                    )
                }
            }
        },
        confirmButton = {
            if (!answered) {
                Button(
                    onClick = {
                        if (selectedIndex >= 0) {
                            answered = true
                            isCorrect = selectedIndex == question.correctIndex
                        }
                    },
                    enabled = selectedIndex >= 0
                ) {
                    Text("Submit")
                }
            } else {
                Button(onClick = {
                    if (isCorrect) onCorrect() else onDismiss()
                }) {
                    Text(if (isCorrect) "Deal Damage!" else "Close")
                }
            }
        },
        dismissButton = {
            if (!answered) {
                TextButton(onClick = onDismiss) {
                    Text("Skip")
                }
            }
        }
    )
}
