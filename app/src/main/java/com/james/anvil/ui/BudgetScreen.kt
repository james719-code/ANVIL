package com.james.anvil.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.james.anvil.data.BalanceType
import com.james.anvil.data.BudgetEntry
import com.james.anvil.data.BudgetType
import com.james.anvil.ui.components.AnvilCard
import com.james.anvil.ui.components.AnvilHeader
import com.james.anvil.ui.navigation.LoansRoute
import com.james.anvil.ui.theme.ElectricTeal
import com.james.anvil.ui.theme.ErrorRed
import com.james.anvil.ui.theme.InfoBlue
import com.james.anvil.ui.theme.WarningOrange
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: TaskViewModel,
    navController: NavController? = null,
    onNavigateBack: () -> Unit = {}
) {
    val budgetEntries by viewModel.budgetEntries.collectAsState(initial = emptyList())
    val cashBalance by viewModel.cashBalance.collectAsState(initial = 0.0)
    val gcashBalance by viewModel.gcashBalance.collectAsState(initial = 0.0)
    val totalCashLoaned by viewModel.totalCashLoaned.collectAsState(initial = 0.0)
    val totalGcashLoaned by viewModel.totalGcashLoaned.collectAsState(initial = 0.0)
    val totalActiveLoanedAmount by viewModel.totalActiveLoanedAmount.collectAsState(initial = 0.0)

    var showAddEntrySheet by remember { mutableStateOf(false) }
    var showEditEntrySheet by remember { mutableStateOf<BudgetEntry?>(null) }
    
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val tabs = listOf("Overview", "Income", "Expenses")
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Loans FAB
                SmallFloatingActionButton(
                    onClick = { navController?.navigate(LoansRoute) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Outlined.People, contentDescription = "Loans")
                }
                // Add Entry FAB
                FloatingActionButton(
                    onClick = { showAddEntrySheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add Entry")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                AnvilHeader(title = "Budget", subtitle = "Manage your finances")
            }

            // Balance Cards (Always Visible)
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
            
             // Loans Quick Access (if active)
            if (totalActiveLoanedAmount > 0) {
                 AnvilCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha=0.3f),
                    onClick = { navController?.navigate(LoansRoute) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Active Loans: ${currencyFormat.format(totalActiveLoanedAmount)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = WarningOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = WarningOrange)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val selected = pagerState.currentPage == index
                    Tab(
                        selected = selected,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                text = title,
                                style = if (selected) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        else MaterialTheme.typography.titleSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val filteredEntries = when (page) {
                    1 -> budgetEntries.filter { it.type == BudgetType.INCOME }
                    2 -> budgetEntries.filter { it.type == BudgetType.EXPENSE }
                    else -> budgetEntries // Overview shows all (or you can limit to recent)
                }
                
                // Sort by date descending
                val sortedEntries = filteredEntries.sortedByDescending { it.timestamp }

                if (sortedEntries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No entries found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedEntries, key = { it.id }) { entry ->
                             BudgetEntryItem(
                                entry = entry,
                                currencyFormat = currencyFormat,
                                onEdit = { showEditEntrySheet = entry },
                                onDelete = { viewModel.deleteBudgetEntry(entry) }
                            )
                        }
                        // Bottom padding for FAB
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    if (showAddEntrySheet) {
        AddBudgetEntrySheet(
            onDismiss = { showAddEntrySheet = false },
            onSave = { type, balanceType, amount, description, category ->
                viewModel.addBudgetEntry(type, balanceType, amount, description, category)
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
    onDelete: () -> Unit
) {
    val isIncome = entry.type == BudgetType.INCOME
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    var showMenu by remember { mutableStateOf(false) }

    AnvilCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isIncome) ElectricTeal.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                    contentDescription = null,
                    tint = if (isIncome) ElectricTeal else ErrorRed
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
                    text = "${if (isIncome) "+" else "-"}${currencyFormat.format(entry.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) ElectricTeal else ErrorRed
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
                // Custom Toggle buttons implementation could be better, but FilterChip is standard
                FilterChip(
                    selected = type == BudgetType.EXPENSE,
                    onClick = { type = BudgetType.EXPENSE },
                    label = { Text("Expense") },
                    leadingIcon = if (type == BudgetType.EXPENSE) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ErrorRed.copy(alpha=0.1f),
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
                        selectedContainerColor = ElectricTeal.copy(alpha=0.1f),
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
                    label = { Text("Cash Wallet") },
                    leadingIcon = if (balanceType == BalanceType.CASH) {
                        { Icon(Icons.Outlined.AccountBalanceWallet, null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricTeal.copy(alpha=0.1f),
                        selectedLabelColor = ElectricTeal,
                        selectedLeadingIconColor = ElectricTeal
                    )
                )
                FilterChip(
                    selected = balanceType == BalanceType.GCASH,
                    onClick = { balanceType = BalanceType.GCASH },
                    label = { Text("GCash") },
                    leadingIcon = if (balanceType == BalanceType.GCASH) {
                        { Icon(Icons.Outlined.AccountBalanceWallet, null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = InfoBlue.copy(alpha=0.1f),
                        selectedLabelColor = InfoBlue,
                        selectedLeadingIconColor = InfoBlue
                    )
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
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
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
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
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

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
                        selectedContainerColor = ErrorRed.copy(alpha=0.1f),
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
                        selectedContainerColor = ElectricTeal.copy(alpha=0.1f),
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
                    label = { Text("Cash Wallet") },
                    leadingIcon = if (balanceType == BalanceType.CASH) {
                        { Icon(Icons.Outlined.AccountBalanceWallet, null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricTeal.copy(alpha=0.1f),
                        selectedLabelColor = ElectricTeal,
                        selectedLeadingIconColor = ElectricTeal
                    )
                )
                FilterChip(
                    selected = balanceType == BalanceType.GCASH,
                    onClick = { balanceType = BalanceType.GCASH },
                    label = { Text("GCash") },
                    leadingIcon = if (balanceType == BalanceType.GCASH) {
                        { Icon(Icons.Outlined.AccountBalanceWallet, null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f),
                     colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = InfoBlue.copy(alpha=0.1f),
                        selectedLabelColor = InfoBlue,
                        selectedLeadingIconColor = InfoBlue
                    )
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
                            onSave(type, balanceType, amountValue, description.trim())
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
             colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Save Changes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
