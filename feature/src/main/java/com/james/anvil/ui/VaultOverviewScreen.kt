package com.james.anvil.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.data.BalanceType
import com.james.anvil.data.BudgetEntry
import com.james.anvil.data.BudgetType
import com.james.anvil.data.CategoryType
import com.james.anvil.data.Loan
import com.james.anvil.ui.components.ActionTile
import com.james.anvil.ui.components.EmptyState
import com.james.anvil.ui.components.FilterPill
import com.james.anvil.ui.components.PageHeader
import com.james.anvil.ui.components.SearchField
import com.james.anvil.ui.components.SectionCard
import com.james.anvil.ui.components.SectionTitle
import com.james.anvil.ui.components.TopLevelPageScaffold
import com.james.anvil.ui.theme.DesignTokens
import com.james.anvil.ui.theme.ElectricBlue
import com.james.anvil.ui.theme.ForgedGold
import com.james.anvil.ui.theme.InfoBlue
import com.james.anvil.ui.theme.LocalWindowInfo
import com.james.anvil.ui.theme.SuccessGreen
import com.james.anvil.ui.theme.WarningOrange
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultOverviewScreen(
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val budgetEntries by viewModel.budgetEntries.collectAsState(initial = emptyList())
    val activeLoans by viewModel.activeLoans.collectAsState(initial = emptyList())
    val cashBalance by viewModel.cashBalance.collectAsState(initial = 0.0)
    val gcashBalance by viewModel.gcashBalance.collectAsState(initial = 0.0)
    val totalCashLoaned by viewModel.totalCashLoaned.collectAsState(initial = 0.0)
    val totalGcashLoaned by viewModel.totalGcashLoaned.collectAsState(initial = 0.0)

    var selectedFilter by remember { mutableStateOf(BudgetFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var searchTypeFilter by remember { mutableStateOf<BudgetType?>(null) }
    var showAddEntrySheet by remember { mutableStateOf<BudgetType?>(null) }
    var showAddLoanSheet by remember { mutableStateOf(false) }
    var showEditEntrySheet by remember { mutableStateOf<BudgetEntry?>(null) }
    var showRepaymentSheet by remember { mutableStateOf<Loan?>(null) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH")) }
    val windowInfo = LocalWindowInfo.current

    val filteredEntries = remember(budgetEntries, searchQuery, selectedFilter, searchTypeFilter) {
        budgetEntries.filter { entry ->
            val matchesSearch = entry.description.contains(searchQuery, ignoreCase = true)
            val matchesMainFilter = when (selectedFilter) {
                BudgetFilter.NECESSITY -> entry.categoryType == CategoryType.NECESSITY
                BudgetFilter.LEISURE -> entry.categoryType == CategoryType.LEISURE
                else -> true
            }
            val matchesTypeFilter = searchTypeFilter == null || entry.type == searchTypeFilter
            matchesSearch && matchesMainFilter && matchesTypeFilter
        }.sortedByDescending { it.timestamp }
    }

    val totalBalance = cashBalance + gcashBalance
    val totalLoaned = totalCashLoaned + totalGcashLoaned

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40
        }
    }

    TopLevelPageScaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEntrySheet = BudgetType.EXPENSE },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget Entry")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .then(
                        if (windowInfo.maxContentWidth != androidx.compose.ui.unit.Dp.Unspecified) {
                            Modifier.widthIn(max = windowInfo.maxContentWidth)
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = windowInfo.contentPadding),
                contentPadding = PaddingValues(bottom = 100.dp, top = DesignTokens.SpacingSm),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLg)
            ) {
                item {
                    PageHeader(
                        eyebrow = "Finance",
                        title = "Vault",
                        subtitle = if (isScrolled) "" else "See balances first, then move into transactions and loans."
                    )
                }

                item {
                    SectionCard(accentColor = MaterialTheme.colorScheme.primary) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Available now",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = currencyFormat.format(totalBalance),
                                            style = if (isScrolled) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displaySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (isScrolled) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(
                                                onClick = { showAddEntrySheet = BudgetType.EXPENSE },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Outlined.ArrowUpward, "Expense", tint = WarningOrange)
                                            }
                                            IconButton(
                                                onClick = { showAddEntrySheet = BudgetType.INCOME },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Outlined.ArrowDownward, "Income", tint = SuccessGreen)
                                            }
                                        }
                                    }
                                }

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = !isScrolled,
                                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text(
                                            text = if (totalLoaned > 0) {
                                                "${currencyFormat.format(totalLoaned)} currently loaned out"
                                            } else {
                                                "No active liabilities."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            ActionTile(
                                                label = "Add expense",
                                                supporting = "Record an outgoing transaction",
                                                icon = Icons.Outlined.ArrowUpward,
                                                tint = WarningOrange,
                                                modifier = Modifier.weight(1f),
                                                onClick = { showAddEntrySheet = BudgetType.EXPENSE }
                                            )
                                            ActionTile(
                                                label = "Add income",
                                                supporting = "Log money coming in",
                                                icon = Icons.Outlined.ArrowDownward,
                                                tint = SuccessGreen,
                                                modifier = Modifier.weight(1f),
                                                onClick = { showAddEntrySheet = BudgetType.INCOME }
                                            )
                                        }
                                        ActionTile(
                                            label = "Manage loans",
                                            supporting = "Track people, repayments, and outstanding balances",
                                            icon = Icons.Outlined.Payments,
                                            tint = ForgedGold,
                                            onClick = {
                                                selectedFilter = BudgetFilter.LOANS
                                                showAddLoanSheet = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VaultStatCard(
                            label = "Cash",
                            value = currencyFormat.format(cashBalance),
                            supporting = currencyFormat.format(totalCashLoaned) + " loaned",
                            tint = InfoBlue,
                            modifier = Modifier.weight(1f)
                        )
                        VaultStatCard(
                            label = "GCash",
                            value = currencyFormat.format(gcashBalance),
                            supporting = currencyFormat.format(totalGcashLoaned) + " loaned",
                            tint = ElectricBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    SearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search transactions or descriptions",
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
                        trailingIcon = {
                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                IconButton(onClick = { expanded = true }) {
                                    Icon(
                                        imageVector = when (searchTypeFilter) {
                                            BudgetType.INCOME -> Icons.Outlined.ArrowDownward
                                            BudgetType.EXPENSE -> Icons.Outlined.ArrowUpward
                                            else -> Icons.Outlined.AccountBalanceWallet
                                        },
                                        contentDescription = "Filter by type"
                                    )
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All types") },
                                        onClick = {
                                            searchTypeFilter = null
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Income only") },
                                        onClick = {
                                            searchTypeFilter = BudgetType.INCOME
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Expenses only") },
                                        onClick = {
                                            searchTypeFilter = BudgetType.EXPENSE
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterPill(
                                text = "All",
                                selected = selectedFilter == BudgetFilter.ALL,
                                onClick = { selectedFilter = BudgetFilter.ALL }
                            )
                        }
                        item {
                            FilterPill(
                                text = "Necessity",
                                selected = selectedFilter == BudgetFilter.NECESSITY,
                                onClick = { selectedFilter = BudgetFilter.NECESSITY }
                            )
                        }
                        item {
                            FilterPill(
                                text = "Leisure",
                                selected = selectedFilter == BudgetFilter.LEISURE,
                                onClick = { selectedFilter = BudgetFilter.LEISURE }
                            )
                        }
                        item {
                            FilterPill(
                                text = if (activeLoans.isEmpty()) "Loans" else "Loans (${activeLoans.size})",
                                selected = selectedFilter == BudgetFilter.LOANS,
                                onClick = { selectedFilter = BudgetFilter.LOANS }
                            )
                        }
                    }
                }

                item {
                    SectionTitle(
                        title = if (selectedFilter == BudgetFilter.LOANS) "Active loans" else "Recent activity"
                    )
                }

                when (selectedFilter) {
                    BudgetFilter.LOANS -> {
                        if (activeLoans.isEmpty()) {
                            item {
                                EmptyState(
                                    message = "No active loans",
                                    subtitle = "Start tracking one when you lend money out.",
                                    icon = Icons.Outlined.Person
                                )
                            }
                        } else {
                            items(activeLoans, key = { it.id }) { loan ->
                                LoanOverviewRow(
                                    loan = loan,
                                    currencyFormat = currencyFormat,
                                    onRepayClick = { showRepaymentSheet = loan }
                                )
                            }
                        }
                    }

                    else -> {
                        if (filteredEntries.isEmpty()) {
                            item {
                                EmptyState(
                                    message = "No entries found",
                                    subtitle = "Try a broader search or log a new transaction.",
                                    icon = Icons.Outlined.AccountBalanceWallet
                                )
                            }
                        } else {
                            items(filteredEntries, key = { it.id }) { entry ->
                                BudgetEntryOverviewRow(
                                    entry = entry,
                                    currencyFormat = currencyFormat,
                                    onEdit = { showEditEntrySheet = entry },
                                    onDelete = { viewModel.deleteBudgetEntry(entry) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    showAddEntrySheet?.let { initialType ->
        AddBudgetEntrySheet(
            initialType = initialType,
            pastEntries = budgetEntries,
            onDismiss = { showAddEntrySheet = null },
            onSave = { type, balanceType, amount, description, category, categoryType ->
                viewModel.addBudgetEntry(type, balanceType, amount, description, category, categoryType)
            }
        )
    }

    if (showAddLoanSheet) {
        AddLoanSheet(
            onDismiss = { showAddLoanSheet = false },
            onSave = { borrowerName, amount, balanceType, interestRate, totalExpectedAmount, description, dueDate ->
                viewModel.createLoan(
                    borrowerName,
                    amount,
                    balanceType,
                    interestRate,
                    totalExpectedAmount,
                    description,
                    dueDate
                )
            }
        )
    }

    showEditEntrySheet?.let { entry ->
        EditBudgetEntrySheet(
            entry = entry,
            pastEntries = budgetEntries,
            onDismiss = { showEditEntrySheet = null },
            onSave = { type, balanceType, amount, description, categoryType ->
                viewModel.updateBudgetEntry(
                    entry.copy(
                        type = type,
                        balanceType = balanceType,
                        amount = amount,
                        description = description,
                        categoryType = categoryType
                    )
                )
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
            onSave = { amount, balanceType, note ->
                viewModel.addLoanRepayment(loan, amount, balanceType, note)
            }
        )
    }
}

@Composable
private fun VaultStatCard(
    label: String,
    value: String,
    supporting: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier, accentColor = tint) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = tint)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BudgetEntryOverviewRow(
    entry: BudgetEntry,
    currencyFormat: NumberFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val amountColor = if (entry.type == BudgetType.INCOME) SuccessGreen else MaterialTheme.colorScheme.error
    SectionCard(accentColor = amountColor, onClick = onEdit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${entry.balanceType.name.lowercase().replaceFirstChar { it.uppercase() }} • ${entry.categoryType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currencyFormat.format(entry.amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.TextButton(onClick = onEdit) { Text("Edit") }
                    androidx.compose.material3.TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanOverviewRow(
    loan: Loan,
    currencyFormat: NumberFormat,
    onRepayClick: () -> Unit
) {
    SectionCard(accentColor = ForgedGold) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = loan.borrowerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = loan.description?.ifBlank { "No additional note" } ?: "No additional note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currencyFormat.format(loan.totalExpectedAmount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ForgedGold
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.TextButton(onClick = onRepayClick) {
                    Text("Record repayment")
                }
            }
        }
    }
}
