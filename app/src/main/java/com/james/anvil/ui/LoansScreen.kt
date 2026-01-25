package com.james.anvil.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.james.anvil.data.BalanceType
import com.james.anvil.data.Loan
import com.james.anvil.data.LoanRepayment
import com.james.anvil.data.LoanStatus
import com.james.anvil.ui.components.SecondaryScreenScaffold
import com.james.anvil.ui.theme.DeepTeal
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val activeLoans by viewModel.activeLoans.collectAsState(initial = emptyList())
    val repaidLoans by viewModel.repaidLoans.collectAsState(initial = emptyList())

    var showAddLoanSheet by remember { mutableStateOf(false) }
    var showRepaymentSheet by remember { mutableStateOf<Loan?>(null) }
    var showHistorySheet by remember { mutableStateOf<Loan?>(null) }
    
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    val tabs = listOf("Active (${activeLoans.size})", "Repaid (${repaidLoans.size})")
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH"))

    SecondaryScreenScaffold(
        title = "Loans",
        subtitle = "Track what you owe",
        onNavigateBack = onNavigateBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddLoanSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Loan")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Animated Tab Row
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        val indicatorOffset by animateDpAsState(
                            targetValue = tabPositions[pagerState.currentPage].left,
                            animationSpec = tween(300),
                            label = "tabIndicator"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.BottomStart)
                                .offset(x = indicatorOffset)
                                .width(tabPositions[pagerState.currentPage].width)
                                .height(3.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                )
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val selected = pagerState.currentPage == index
                    val color by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary 
                                      else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(300),
                        label = "tabColor"
                    )
                    Tab(
                        selected = selected,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { 
                            Text(
                                title, 
                                color = color,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }

            // HorizontalPager for smooth tab transitions
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val displayLoans = if (page == 0) activeLoans else repaidLoans

                if (displayLoans.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "No loans",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (page == 0) "No active loans" else "No repaid loans",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (page == 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap + to record a new loan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayLoans, key = { it.id }) { loan ->
                            LoanItem(
                                loan = loan,
                                currencyFormat = currencyFormat,
                                onRepayClick = { showRepaymentSheet = loan },
                                onDeleteClick = { viewModel.deleteLoan(loan) },
                                onViewHistory = { showHistorySheet = loan }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddLoanSheet) {
        AddLoanSheet(
            onDismiss = { showAddLoanSheet = false },
            onSave = { borrowerName, amount, balanceType, interestRate, totalExpectedAmount, description, dueDate ->
                viewModel.createLoan(borrowerName, amount, balanceType, interestRate, totalExpectedAmount, description, dueDate)
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
    
    val repayments by viewModel.getLoanRepayments(showHistorySheet?.id ?: -1L).collectAsState(initial = emptyList())
    
    showHistorySheet?.let { loan: com.james.anvil.data.Loan ->
        RepaymentHistorySheet(
            loan = loan,
            repayments = repayments,
            currencyFormat = currencyFormat,
            onDismiss = { showHistorySheet = null }
        )
    }
}


@Composable
private fun LoanItem(
    loan: Loan,
    currencyFormat: NumberFormat,
    onRepayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onViewHistory: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val progress = 1 - (loan.remainingAmount / loan.totalExpectedAmount).coerceIn(0.0, 1.0)
    val isGcash = loan.balanceType == BalanceType.GCASH
    val isActive = loan.status != LoanStatus.FULLY_REPAID
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
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
                                if (isGcash) Color(0xFF007DFE).copy(alpha = 0.1f) 
                                else DeepTeal.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Borrower",
                            tint = if (isGcash) Color(0xFF007DFE) else DeepTeal
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
                            text = "${loan.balanceType.name} • ${dateFormat.format(Date(loan.loanDate))}${if (loan.interestRate > 0) " • ${loan.interestRate}% Int." else ""}",
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
                            color = if (isActive) MaterialTheme.colorScheme.onSurface else DeepTeal
                        )
                        if (loan.remainingAmount != loan.totalExpectedAmount) {
                            Text(
                                text = "of ${currencyFormat.format(loan.totalExpectedAmount)}",
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
                                text = { Text("View History") },
                                onClick = {
                                    showMenu = false
                                    onViewHistory()
                                },
                                leadingIcon = { Icon(Icons.Default.History, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirm = true
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
                color = if (isGcash) Color(0xFF007DFE) else DeepTeal,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            loan.description?.let { desc ->
                if (desc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            loan.dueDate?.let { dueDate ->
                Spacer(modifier = Modifier.height(8.dp))
                val daysRemaining = ((dueDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
                Text(
                    text = if (daysRemaining > 0) "Due in $daysRemaining days" 
                           else if (daysRemaining == 0) "Due today" 
                           else "Overdue by ${-daysRemaining} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (daysRemaining < 0) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRepayClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add Repayment")
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Loan?") },
            text = { Text("This will permanently delete the loan record for ${loan.borrowerName}. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteClick()
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
}


// Private sheets removed - now in BudgetSheets.kt
