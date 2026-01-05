package com.james.anvil.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.james.anvil.data.BalanceType
import com.james.anvil.data.BudgetEntry
import com.james.anvil.data.BudgetType
import com.james.anvil.data.Loan
import com.james.anvil.data.LoanStatus
import com.james.anvil.ui.components.AnvilCard
import com.james.anvil.ui.theme.ElectricTeal
import com.james.anvil.ui.theme.ErrorRed
import com.james.anvil.ui.theme.InfoBlue
import com.james.anvil.ui.theme.WarningOrange
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Filter types for budget entries
enum class BudgetFilter {
    ALL, INCOME, EXPENSES, LOANS
}

// Helper data class for 4 values
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: TaskViewModel
) {
    val budgetEntries by viewModel.budgetEntries.collectAsState(initial = emptyList())
    val activeLoans by viewModel.activeLoans.collectAsState(initial = emptyList())
    val cashBalance by viewModel.cashBalance.collectAsState(initial = 0.0)
    val gcashBalance by viewModel.gcashBalance.collectAsState(initial = 0.0)
    val totalCashLoaned by viewModel.totalCashLoaned.collectAsState(initial = 0.0)
    val totalGcashLoaned by viewModel.totalGcashLoaned.collectAsState(initial = 0.0)

    var selectedFilter by remember { mutableStateOf(BudgetFilter.ALL) }
    var showAddEntrySheet by remember { mutableStateOf(false) }
    var showAddLoanSheet by remember { mutableStateOf(false) }
    var showEditEntrySheet by remember { mutableStateOf<BudgetEntry?>(null) }
    var showRepaymentSheet by remember { mutableStateOf<Loan?>(null) }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (selectedFilter == BudgetFilter.LOANS) {
                        showAddLoanSheet = true
                    } else {
                        showAddEntrySheet = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Budget",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }

            // Balance Cards
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BalanceCard(
                        title = "Cash",
                        balance = cashBalance - totalCashLoaned,
                        loaned = totalCashLoaned,
                        currencyFormat = currencyFormat,
                        modifier = Modifier.weight(1f),
                        color = ElectricTeal
                    )
                    BalanceCard(
                        title = "GCash",
                        balance = gcashBalance - totalGcashLoaned,
                        loaned = totalGcashLoaned,
                        currencyFormat = currencyFormat,
                        modifier = Modifier.weight(1f),
                        color = InfoBlue
                    )
                }
            }

            // Filter Chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == BudgetFilter.ALL,
                            onClick = { selectedFilter = BudgetFilter.ALL },
                            label = { Text("All") },
                            leadingIcon = if (selectedFilter == BudgetFilter.ALL) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == BudgetFilter.INCOME,
                            onClick = { selectedFilter = BudgetFilter.INCOME },
                            label = { Text("Income") },
                            leadingIcon = if (selectedFilter == BudgetFilter.INCOME) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricTeal.copy(alpha = 0.1f),
                                selectedLabelColor = ElectricTeal,
                                selectedLeadingIconColor = ElectricTeal
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == BudgetFilter.EXPENSES,
                            onClick = { selectedFilter = BudgetFilter.EXPENSES },
                            label = { Text("Expenses") },
                            leadingIcon = if (selectedFilter == BudgetFilter.EXPENSES) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ErrorRed.copy(alpha = 0.1f),
                                selectedLabelColor = ErrorRed,
                                selectedLeadingIconColor = ErrorRed
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == BudgetFilter.LOANS,
                            onClick = { selectedFilter = BudgetFilter.LOANS },
                            label = { 
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Loans")
                                    if (activeLoans.isNotEmpty()) {
                                        Badge(
                                            containerColor = WarningOrange
                                        ) {
                                            Text("${activeLoans.size}")
                                        }
                                    }
                                }
                            },
                            leadingIcon = if (selectedFilter == BudgetFilter.LOANS) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WarningOrange.copy(alpha = 0.1f),
                                selectedLabelColor = WarningOrange,
                                selectedLeadingIconColor = WarningOrange
                            )
                        )
                    }
                }
            }

            // Content based on filter
            when (selectedFilter) {
                BudgetFilter.LOANS -> {
                    // Loans Section
                    if (activeLoans.isEmpty()) {
                        item {
                            EmptyStateContent(
                                message = "No active loans",
                                icon = Icons.Outlined.Person
                            )
                        }
                    } else {
                        items(activeLoans, key = { it.id }) { loan ->
                            LoanItem(
                                loan = loan,
                                currencyFormat = currencyFormat,
                                onRepayClick = { showRepaymentSheet = loan },
                                onDeleteClick = { viewModel.deleteLoan(loan) },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                else -> {
                    // Budget Entries
                    val filteredEntries = when (selectedFilter) {
                        BudgetFilter.INCOME -> budgetEntries.filter { it.type == BudgetType.INCOME }
                        BudgetFilter.EXPENSES -> budgetEntries.filter { it.type == BudgetType.EXPENSE }
                        else -> budgetEntries
                    }.sortedByDescending { it.timestamp }

                    if (filteredEntries.isEmpty()) {
                        item {
                            EmptyStateContent(
                                message = "No entries found",
                                icon = Icons.Outlined.AccountBalanceWallet
                            )
                        }
                    } else {
                        items(filteredEntries, key = { it.id }) { entry ->
                            BudgetEntryItem(
                                entry = entry,
                                currencyFormat = currencyFormat,
                                onEdit = { showEditEntrySheet = entry },
                                onDelete = { viewModel.deleteBudgetEntry(entry) },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheets
    if (showAddEntrySheet) {
        AddBudgetEntrySheet(
            onDismiss = { showAddEntrySheet = false },
            onSave = { type, balanceType, amount, description, category ->
                viewModel.addBudgetEntry(type, balanceType, amount, description, category)
            }
        )
    }

    if (showAddLoanSheet) {
        AddLoanSheet(
            onDismiss = { showAddLoanSheet = false },
            onSave = { borrowerName, amount, balanceType, description ->
                viewModel.createLoan(borrowerName, amount, balanceType, description, null)
            }
        )
    }

    showEditEntrySheet?.let { entry ->
        EditBudgetEntrySheet(
            entry = entry,
            onDismiss = { showEditEntrySheet = null },
            onSave = { type, balanceType, amount, description ->
                viewModel.updateBudgetEntry(entry.copy(
                    type = type,
                    balanceType = balanceType,
                    amount = amount,
                    description = description
                ))
            },
            onDelete = {
                viewModel.deleteBudgetEntry(entry)
                showEditEntrySheet = null
            }
        )
    }

    showRepaymentSheet?.let { loan ->
        AddRepaymentSheet(
            loan = loan,
            currencyFormat = currencyFormat,
            onDismiss = { showRepaymentSheet = null },
            onSave = { amount, note ->
                viewModel.addLoanRepayment(loan, amount, note)
            }
        )
    }
}

@Composable
private fun EmptyStateContent(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BalanceCard(
    title: String,
    balance: Double,
    loaned: Double,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier,
    color: Color
) {
    AnvilCard(
        modifier = modifier,
        containerColor = color
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = currencyFormat.format(balance),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (loaned > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Loaned: ${currencyFormat.format(loaned)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun BudgetEntryItem(
    entry: BudgetEntry,
    currencyFormat: NumberFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = entry.type == BudgetType.INCOME
    val isLoanOut = entry.type == BudgetType.LOAN_OUT
    val isLoanRepayment = entry.type == BudgetType.LOAN_REPAYMENT
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    var showMenu by remember { mutableStateOf(false) }
    
    // Determine colors and icons based on type
    val (bgColor, iconColor, icon, prefix) = when (entry.type) {
        BudgetType.INCOME -> Quadruple(ElectricTeal.copy(alpha = 0.1f), ElectricTeal, Icons.Outlined.ArrowDownward, "+")
        BudgetType.EXPENSE -> Quadruple(ErrorRed.copy(alpha = 0.1f), ErrorRed, Icons.Outlined.ArrowUpward, "-")
        BudgetType.LOAN_OUT -> Quadruple(WarningOrange.copy(alpha = 0.1f), WarningOrange, Icons.Outlined.Person, "→")
        BudgetType.LOAN_REPAYMENT -> Quadruple(ElectricTeal.copy(alpha = 0.1f), ElectricTeal, Icons.Outlined.Person, "←")
    }

    AnvilCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(bgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormat.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = entry.balanceType.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (entry.balanceType == BalanceType.GCASH) InfoBlue else ElectricTeal,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${currencyFormat.format(entry.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanItem(
    loan: Loan,
    currencyFormat: NumberFormat,
    onRepayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val progress = 1 - (loan.remainingAmount / loan.originalAmount).coerceIn(0.0, 1.0)
    val isGcash = loan.balanceType == BalanceType.GCASH
    var showMenu by remember { mutableStateOf(false) }

    AnvilCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                WarningOrange.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = WarningOrange
                        )
                    }
                    Column {
                        Text(
                            text = loan.borrowerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${loan.balanceType.name} • ${dateFormat.format(Date(loan.loanDate))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = currencyFormat.format(loan.remainingAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WarningOrange
                        )
                        if (loan.remainingAmount != loan.originalAmount) {
                            Text(
                                text = "of ${currencyFormat.format(loan.originalAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add Repayment") },
                                onClick = {
                                    showMenu = false
                                    onRepayClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Add, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = WarningOrange,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (loan.status != LoanStatus.FULLY_REPAID) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRepayClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarningOrange
                    )
                ) {
                    Text("Add Repayment")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetEntrySheet(
    onDismiss: () -> Unit,
    onSave: (BudgetType, BalanceType, Double, String, String) -> Unit
) {
    var type by remember { mutableStateOf(BudgetType.EXPENSE) }
    var balanceType by remember { mutableStateOf(BalanceType.CASH) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var amountError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "New Transaction",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = type == BudgetType.EXPENSE,
                    onClick = { type = BudgetType.EXPENSE },
                    label = { Text("Expense") },
                    leadingIcon = if (type == BudgetType.EXPENSE) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ErrorRed.copy(alpha = 0.1f),
                        selectedLabelColor = ErrorRed,
                        selectedLeadingIconColor = ErrorRed
                    )
                )
                FilterChip(
                    selected = type == BudgetType.INCOME,
                    onClick = { type = BudgetType.INCOME },
                    label = { Text("Income") },
                    leadingIcon = if (type == BudgetType.INCOME) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricTeal.copy(alpha = 0.1f),
                        selectedLabelColor = ElectricTeal,
                        selectedLeadingIconColor = ElectricTeal
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Balance Type Selector
            Text("Source", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = balanceType == BalanceType.CASH,
                    onClick = { balanceType = BalanceType.CASH },
                    label = { Text("Cash") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = balanceType == BalanceType.GCASH,
                    onClick = { balanceType = BalanceType.GCASH },
                    label = { Text("GCash") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it.filter { c -> c.isDigit() || c == '.' }
                    amountError = false
                },
                label = { Text("Amount") },
                prefix = { Text("₱") },
                isError = amountError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    descriptionError = false
                },
                label = { Text("Description") },
                isError = descriptionError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    when {
                        amountValue == null || amountValue <= 0 -> amountError = true
                        description.isBlank() -> descriptionError = true
                        else -> {
                            onSave(type, balanceType, amountValue, description.trim(), category)
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Save Transaction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLoanSheet(
    onDismiss: () -> Unit,
    onSave: (String, Double, BalanceType, String?) -> Unit
) {
    var borrowerName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var balanceType by remember { mutableStateOf(BalanceType.CASH) }
    var description by remember { mutableStateOf("") }
    var borrowerError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "New Loan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = borrowerName,
                onValueChange = {
                    borrowerName = it
                    borrowerError = false
                },
                label = { Text("Borrower Name") },
                isError = borrowerError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it.filter { c -> c.isDigit() || c == '.' }
                    amountError = false
                },
                label = { Text("Amount") },
                prefix = { Text("₱") },
                isError = amountError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Source", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = balanceType == BalanceType.CASH,
                    onClick = { balanceType = BalanceType.CASH },
                    label = { Text("Cash") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = balanceType == BalanceType.GCASH,
                    onClick = { balanceType = BalanceType.GCASH },
                    label = { Text("GCash") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    when {
                        borrowerName.isBlank() -> borrowerError = true
                        amountValue == null || amountValue <= 0 -> amountError = true
                        else -> {
                            onSave(borrowerName.trim(), amountValue, balanceType, description.ifEmpty { null })
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarningOrange
                )
            ) {
                Text(
                    text = "Create Loan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBudgetEntrySheet(
    entry: BudgetEntry,
    onDismiss: () -> Unit,
    onSave: (BudgetType, BalanceType, Double, String) -> Unit,
    onDelete: () -> Unit
) {
    var type by remember { mutableStateOf(entry.type) }
    var balanceType by remember { mutableStateOf(entry.balanceType) }
    var amount by remember { mutableStateOf(entry.amount.toString()) }
    var description by remember { mutableStateOf(entry.description) }
    var amountError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Entry?") },
            text = { Text("Are you sure you want to delete this budget entry?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Transaction",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = type == BudgetType.EXPENSE,
                    onClick = { type = BudgetType.EXPENSE },
                    label = { Text("Expense") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ErrorRed.copy(alpha = 0.1f),
                        selectedLabelColor = ErrorRed
                    )
                )
                FilterChip(
                    selected = type == BudgetType.INCOME,
                    onClick = { type = BudgetType.INCOME },
                    label = { Text("Income") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricTeal.copy(alpha = 0.1f),
                        selectedLabelColor = ElectricTeal
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = balanceType == BalanceType.CASH,
                    onClick = { balanceType = BalanceType.CASH },
                    label = { Text("Cash") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = balanceType == BalanceType.GCASH,
                    onClick = { balanceType = BalanceType.GCASH },
                    label = { Text("GCash") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }; amountError = false },
                label = { Text("Amount") },
                prefix = { Text("₱") },
                isError = amountError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it; descriptionError = false },
                label = { Text("Description") },
                isError = descriptionError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    when {
                        amountValue == null || amountValue <= 0 -> amountError = true
                        description.isBlank() -> descriptionError = true
                        else -> { onSave(type, balanceType, amountValue, description.trim()); onDismiss() }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRepaymentSheet(
    loan: Loan,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit,
    onSave: (Double, String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Add Repayment",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "From ${loan.borrowerName} • Remaining: ${currencyFormat.format(loan.remainingAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }; amountError = false },
                label = { Text("Amount") },
                prefix = { Text("₱") },
                isError = amountError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { amount = loan.remainingAmount.toString() },
                    modifier = Modifier.weight(1f)
                ) { Text("Full Amount") }
                OutlinedButton(
                    onClick = { amount = (loan.remainingAmount / 2).toString() },
                    modifier = Modifier.weight(1f)
                ) { Text("Half") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    when {
                        amountValue == null || amountValue <= 0 -> amountError = true
                        else -> { onSave(amountValue, note.ifEmpty { null }); onDismiss() }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
            ) {
                Text("Record Repayment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
