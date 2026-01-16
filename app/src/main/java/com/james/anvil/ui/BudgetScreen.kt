package com.james.anvil.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.data.BalanceType
import com.james.anvil.data.BudgetEntry
import com.james.anvil.data.BudgetType
import com.james.anvil.data.CategoryType
import com.james.anvil.data.Loan
import com.james.anvil.data.LoanStatus
import com.james.anvil.ui.components.AnvilCard
import com.james.anvil.ui.components.AnvilHeader
import com.james.anvil.ui.theme.*
import java.text.NumberFormat

import java.text.SimpleDateFormat
import java.util.*

enum class BudgetFilter {
    ALL, NECESSITY, LEISURE, LOANS
}
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
    var searchQuery by remember { mutableStateOf("") }
    var searchTypeFilter by remember { mutableStateOf<BudgetType?>(null) } // null = All types
    
    var showAddEntrySheet by remember { mutableStateOf<BudgetType?>(null) }
    var showAddLoanSheet by remember { mutableStateOf(false) }
    var showEditEntrySheet by remember { mutableStateOf<BudgetEntry?>(null) }
    var showRepaymentSheet by remember { mutableStateOf<Loan?>(null) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH")) }
    
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

    val maxHeaderHeight = 460.dp
    val minHeaderHeight = 64.dp
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    val density = androidx.compose.ui.platform.LocalDensity.current
    LaunchedEffect(maxHeaderHeight, minHeaderHeight) {
        val maxPx = with(density) { maxHeaderHeight.toPx() }
        val minPx = with(density) { minHeaderHeight.toPx() }
        scrollBehavior.state.heightOffsetLimit = minPx - maxPx
    }

    val collapsedFraction = scrollBehavior.state.collapsedFraction

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(androidx.compose.ui.unit.lerp(maxHeaderHeight, minHeaderHeight, collapsedFraction)),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = if (collapsedFraction > 0.9f) 3.dp else 0.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. Collapsed Title (Centered or start, visible only when collapsed)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = collapsedFraction > 0.8f,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(
                            text = "The Vault",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 2. Expanded Content (Fades out as we scroll)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .graphicsLayer {
                                alpha = (1f - collapsedFraction * 1.5f).coerceIn(0f, 1f)
                                translationY = -collapsedFraction * 100f // Subtle parallax
                            }
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Large Title
                        Text(
                            text = "The Vault",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        Text(
                            text = "Financial logistics & audit",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        AnvilCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            0.0f to Color(0xFF1E3A8A),
                                            0.4f to Color(0xFF0F172A),
                                            1.0f to Color(0xFF000000)
                                        )
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .offset(x = 100.dp, y = (-120).dp)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.15f), Color.Transparent)
                                            )
                                        )
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Top Row: Brand & Wireless
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column {
                                            Text(
                                                text = "ANVIL",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 2.sp
                                                ),
                                                color = Color.White
                                            )
                                            Text(
                                                text = "PREMIER VAULT",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 3.sp
                                                ),
                                                color = ForgedGold.copy(alpha = 0.8f)
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Contactless,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(28.dp).rotate(90f)
                                        )
                                    }

                                    // Middle Row: Chip & Balance
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Gold EMV Chip
                                        Box(
                                            modifier = Modifier
                                                .size(width = 45.dp, height = 35.dp)
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(Color(0xFFFFD700), Color(0xFFB8860B))
                                                    ),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(4.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.SpaceEvenly) {
                                                repeat(3) {
                                                    Divider(color = Color.Black.copy(alpha = 0.2f), thickness = 0.5.dp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(20.dp))
                                        Text(
                                            text = currencyFormat.format(cashBalance + gcashBalance),
                                            style = MaterialTheme.typography.displaySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = (-1).sp
                                            ),
                                            color = Color.White
                                        )
                                    }

                                    // Bottom Row: Cardholder & Liabilities
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column {
                                            Text(
                                                text = "MEMBERSHIP CAPITAL",
                                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                text = "JAMES RYAN",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                                color = Color.White
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "TOTAL LIABILITIES",
                                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                text = currencyFormat.format(totalCashLoaned + totalGcashLoaned),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (totalCashLoaned + totalGcashLoaned > 0) Color(0xFFFFB3B3) else ForgedGold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Balance Cards inside Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BalanceCard(
                                title = "Cash Reserves",
                                balance = cashBalance,
                                loaned = totalCashLoaned,
                                currencyFormat = currencyFormat,
                                modifier = Modifier.weight(1f),
                                color = SteelBlue,
                                icon = Icons.Default.Wallet
                            )
                            BalanceCard(
                                title = "GCash Digital",
                                balance = gcashBalance,
                                loaned = totalGcashLoaned,
                                currencyFormat = currencyFormat,
                                modifier = Modifier.weight(1f),
                                color = InfoBlue,
                                icon = Icons.Default.Smartphone
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (selectedFilter == BudgetFilter.LOANS) {
                        showAddLoanSheet = true
                    } else {
                        showAddEntrySheet = BudgetType.EXPENSE
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
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp)
        ) {

            // Search Bar & Type Toggle
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    placeholder = { Text("Search transactions...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                            
                            // Type filter toggle inside search bar
                            Box(modifier = Modifier.padding(end = 4.dp)) {
                                var showMenu by remember { mutableStateOf(false) }
                                IconButton(onClick = { showMenu = true }) {
                                    val icon = when (searchTypeFilter) {
                                        BudgetType.INCOME -> Icons.Default.ArrowDownward
                                        BudgetType.EXPENSE -> Icons.Default.ArrowUpward
                                        else -> Icons.Default.FilterList
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "Filter Type",
                                        tint = when (searchTypeFilter) {
                                            BudgetType.INCOME -> ElectricTeal
                                            BudgetType.EXPENSE -> ErrorRed
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Types") },
                                        onClick = { searchTypeFilter = null; showMenu = false },
                                        leadingIcon = { Icon(Icons.Default.FilterList, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Income Only") },
                                        onClick = { searchTypeFilter = BudgetType.INCOME; showMenu = false },
                                        leadingIcon = { Icon(Icons.Default.ArrowDownward, null, tint = ElectricTeal) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Expenses Only") },
                                        onClick = { searchTypeFilter = BudgetType.EXPENSE; showMenu = false },
                                        leadingIcon = { Icon(Icons.Default.ArrowUpward, null, tint = ErrorRed) }
                                    )
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    singleLine = true
                )
            }

            // Filter Chips (Type-based)
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp),
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
                            selected = selectedFilter == BudgetFilter.NECESSITY,
                            onClick = { selectedFilter = BudgetFilter.NECESSITY },
                            label = { Text("Necessity") },
                            leadingIcon = if (selectedFilter == BudgetFilter.NECESSITY) {
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
                            selected = selectedFilter == BudgetFilter.LEISURE,
                            onClick = { selectedFilter = BudgetFilter.LEISURE },
                            label = { Text("Leisure") },
                            leadingIcon = if (selectedFilter == BudgetFilter.LEISURE) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForgedGold.copy(alpha = 0.1f),
                                selectedLabelColor = ForgedGold,
                                selectedLeadingIconColor = ForgedGold
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
                                modifier = Modifier.padding(vertical = 6.dp)
                            )

                        }
                    }
                }
                else -> {
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
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheets
    showAddEntrySheet?.let { initialType ->
        AddBudgetEntrySheet(
            initialType = initialType,
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
                viewModel.createLoan(borrowerName, amount, balanceType, interestRate, totalExpectedAmount, description, dueDate)
            }
        )
    }

    showEditEntrySheet?.let { entry ->
        EditBudgetEntrySheet(
            entry = entry,
            onDismiss = { showEditEntrySheet = null },
                onSave = { type, balanceType, amount, description, categoryType ->
                viewModel.updateBudgetEntry(entry.copy(
                    type = type,
                    balanceType = balanceType,
                    amount = amount,
                    description = description,
                    categoryType = categoryType
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
            onSave = { amount, balanceType, note ->
                viewModel.addLoanRepayment(loan, amount, balanceType, note)
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
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.AccountBalanceWallet
) {
    AnvilCard(
        modifier = modifier.height(130.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Text(
                    text = title.split(" ").first().uppercase(), // Just first word for professional look
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Column {
                Text(
                    text = currencyFormat.format(balance),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (loaned > 0) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "→ ${currencyFormat.format(loaned)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(ForgedGold.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "LOANED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                                color = ForgedGold
                            )
                        }
                    }
                }
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
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    var showMenu by remember { mutableStateOf(false) }
    
    val (bgColor, iconColor, icon, prefix) = when (entry.type) {
        BudgetType.INCOME -> Quadruple(ElectricTeal.copy(alpha = 0.15f), ElectricTeal, Icons.Outlined.ArrowDownward, "+")
        BudgetType.EXPENSE -> Quadruple(ErrorRed.copy(alpha = 0.15f), ErrorRed, Icons.Outlined.ArrowUpward, "-")
        BudgetType.LOAN_OUT -> Quadruple(WarningOrange.copy(alpha = 0.15f), WarningOrange, Icons.Outlined.Person, "→")
        BudgetType.LOAN_REPAYMENT -> Quadruple(ElectricTeal.copy(alpha = 0.15f), ElectricTeal, Icons.Outlined.Person, "←")
    }

    AnvilCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(bgColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormat.format(Date(entry.timestamp)).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    Text(
                        text = entry.balanceType.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = if (entry.balanceType == BalanceType.GCASH) InfoBlue else SteelBlueLight
                    )

                    if (entry.categoryType != CategoryType.NONE) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )

                        Box(
                            modifier = Modifier
                                .background(
                                    if (entry.categoryType == CategoryType.NECESSITY) ElectricTeal.copy(alpha = 0.15f) 
                                    else ForgedGold.copy(alpha = 0.15f), 
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = entry.categoryType.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                                color = if (entry.categoryType == CategoryType.NECESSITY) ElectricTeal else ForgedGold
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${currencyFormat.format(entry.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = iconColor
                )

                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )

                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { onEdit(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { onDelete(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
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
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val progress = 1 - (loan.remainingAmount / loan.originalAmount).coerceIn(0.0, 1.0)
    var showMenu by remember { mutableStateOf(false) }

    AnvilCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(ForgedGold.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = ForgedGold
                        )
                    }
                    Column {
                        Text(
                            text = loan.borrowerName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "${loan.balanceType.name} • ${dateFormat.format(Date(loan.loanDate)).uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormat.format(loan.remainingAmount),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = ForgedGold
                    )
                    Text(
                        text = "REMAINING",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                        color = ForgedGold.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Premium Progress indicator
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "REPAYMENT PROGRESS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                }
                
                LinearProgressIndicator(
                    progress = { progress.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ForgedGold,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

            }

            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRepayClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForgedGold, contentColor = Color.Black)
                ) {
                    Text("SETTLE PAYMENT", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
                
            }
        }
    }
}

// Private sheets removed - now in BudgetSheets.kt
