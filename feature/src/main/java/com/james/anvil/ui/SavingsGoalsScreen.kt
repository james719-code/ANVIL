package com.james.anvil.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.data.BalanceType
import com.james.anvil.data.SavingsGoal
import com.james.anvil.ui.components.AnvilCard
import com.james.anvil.ui.theme.*
import com.james.anvil.ui.viewmodel.SavingsViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    onBack: () -> Unit,
    viewModel: SavingsViewModel = hiltViewModel()
) {
    val allGoals by viewModel.allGoals.collectAsState()
    val goalCompleted by viewModel.goalCompletedEvent.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showContributeDialog by remember { mutableStateOf<SavingsGoal?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<SavingsGoal?>(null) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH")) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Goal completed snackbar
    LaunchedEffect(goalCompleted) {
        goalCompleted?.let {
            viewModel.clearGoalCompletedEvent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Goals", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = ForgedGold,
                contentColor = Color.White
            ) {
                Icon(Icons.Outlined.Add, "Create Goal")
            }
        }
    ) { paddingValues ->
        if (allGoals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Savings,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextSecondaryDark
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No savings goals yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "Tap + to set your first treasure goal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allGoals, key = { it.id }) { goal ->
                    TreasureChestCard(
                        goal = goal,
                        progress = viewModel.getProgressPercent(goal),
                        currencyFormat = currencyFormat,
                        onContribute = { showContributeDialog = goal },
                        onDelete = { showDeleteConfirm = goal }
                    )
                }
            }
        }
    }

    // Create Goal Dialog
    if (showCreateDialog) {
        CreateGoalDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, amount, balanceType, emoji ->
                viewModel.createGoal(name, amount, balanceType, emoji)
                showCreateDialog = false
            }
        )
    }

    // Contribute Dialog
    showContributeDialog?.let { goal ->
        ContributeDialog(
            goal = goal,
            currencyFormat = currencyFormat,
            onDismiss = { showContributeDialog = null },
            onContribute = { amount, note ->
                viewModel.addContribution(goal.id, amount, note)
                showContributeDialog = null
            }
        )
    }

    // Delete Confirmation
    showDeleteConfirm?.let { goal ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Goal") },
            text = { Text("Delete \"${goal.name}\"? This will remove all contributions.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGoal(goal.id)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TreasureChestCard(
    goal: SavingsGoal,
    progress: Float,
    currencyFormat: NumberFormat,
    onContribute: () -> Unit,
    onDelete: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600),
        label = "progress"
    )

    AnvilCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goal.iconEmoji,
                        fontSize = 28.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (goal.balanceType == BalanceType.CASH) "Cash" else "GCash",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (goal.balanceType == BalanceType.CASH) ElectricTeal else GcashBlue
                        )
                    }
                }

                if (goal.isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SuccessGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "COMPLETE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = SuccessGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Treasure chest progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(XpBarTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = if (goal.isCompleted)
                                    listOf(ForgedGold, XpGold)
                                else
                                    listOf(ForgedGoldDark, ForgedGold)
                            )
                        )
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(8.dp))

            // Amount info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currencyFormat.format(goal.currentAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ForgedGold
                )
                Text(
                    text = "/ ${currencyFormat.format(goal.targetAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!goal.isCompleted) {
                    Button(
                        onClick = onContribute,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ForgedGold)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Savings")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        "Delete",
                        tint = ErrorRed.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Double, BalanceType, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var selectedBalanceType by remember { mutableStateOf(BalanceType.CASH) }
    var selectedEmoji by remember { mutableStateOf("\uD83D\uDCB0") }

    val emojis = listOf("\uD83D\uDCB0", "\uD83C\uDFAF", "\uD83D\uDCF1", "\u2708\uFE0F", "\uD83C\uDFE0", "\uD83D\uDE97", "\uD83C\uDF93", "\uD83D\uDC8E")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Savings Goal", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    placeholder = { Text("e.g., New Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Target Amount (PHP)") },
                    placeholder = { Text("e.g., 15000") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // Balance type selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedBalanceType == BalanceType.CASH,
                        onClick = { selectedBalanceType = BalanceType.CASH },
                        label = { Text("Cash") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricTeal.copy(alpha = 0.2f)
                        )
                    )
                    FilterChip(
                        selected = selectedBalanceType == BalanceType.GCASH,
                        onClick = { selectedBalanceType = BalanceType.GCASH },
                        label = { Text("GCash") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GcashBlue.copy(alpha = 0.2f)
                        )
                    )
                }

                // Emoji picker
                Text("Icon", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    emojis.forEach { emoji ->
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { selectedEmoji = emoji },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedEmoji == emoji)
                                ForgedGold.copy(alpha = 0.2f)
                            else
                                Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = targetAmount.toDoubleOrNull()
                    if (name.isNotBlank() && amount != null && amount > 0) {
                        onCreate(name, amount, selectedBalanceType, selectedEmoji)
                    }
                },
                enabled = name.isNotBlank() && (targetAmount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Create", color = ForgedGold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ContributeDialog(
    goal: SavingsGoal,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit,
    onContribute: (Double, String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val remaining = goal.targetAmount - goal.currentAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to \"${goal.name}\"", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Remaining: ${currencyFormat.format(remaining)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForgedGold
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (PHP)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onContribute(amt, note.ifBlank { null })
                    }
                },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Save", color = ForgedGold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
