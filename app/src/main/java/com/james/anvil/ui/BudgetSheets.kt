package com.james.anvil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.data.*
import com.james.anvil.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetEntrySheet(
    onDismiss: () -> Unit,
    onSave: (BudgetType, BalanceType, Double, String, String, CategoryType) -> Unit,
    initialType: BudgetType = BudgetType.EXPENSE
) {
    var type by remember { mutableStateOf(initialType) }
    var balanceType by remember { mutableStateOf(BalanceType.CASH) }
    var categoryType by remember { mutableStateOf(CategoryType.NONE) }
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
                .verticalScroll(rememberScrollState())
                .imePadding()
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

            Spacer(modifier = Modifier.height(8.dp))

            // Transaction Category Type (Necessity/Leisure)
            if (type == BudgetType.EXPENSE || type == BudgetType.LOAN_OUT) {
                Text("Classification", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = categoryType == CategoryType.NECESSITY,
                        onClick = { categoryType = CategoryType.NECESSITY },
                        label = { Text("Necessity") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricTeal.copy(alpha = 0.1f),
                            selectedLabelColor = ElectricTeal
                        )
                    )
                    FilterChip(
                        selected = categoryType == CategoryType.LEISURE,
                        onClick = { categoryType = CategoryType.LEISURE },
                        label = { Text("Leisure") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForgedGold.copy(alpha = 0.1f),
                            selectedLabelColor = ForgedGold
                        )
                    )
                    FilterChip(
                        selected = categoryType == CategoryType.NONE,
                        onClick = { categoryType = CategoryType.NONE },
                        label = { Text("None") },
                        modifier = Modifier.weight(0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                            onSave(type, balanceType, amountValue, description.trim(), category, categoryType)
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
fun AddLoanSheet(
    onDismiss: () -> Unit,
    onSave: (String, Double, BalanceType, Double, Double, String?, Long?) -> Unit
) {
    var borrowerName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var totalExpectedAmount by remember { mutableStateOf("") }
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
                .verticalScroll(rememberScrollState())
                .imePadding()
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
                onValueChange = { input ->
                    amount = input.filter { c -> c.isDigit() || c == '.' }
                    amountError = false
                    
                    val a = amount.toDoubleOrNull() ?: 0.0
                    val r = interestRate.toDoubleOrNull() ?: 0.0
                    if (a > 0) {
                        totalExpectedAmount = String.format("%.2f", a + (a * r / 100))
                    }
                },
                label = { Text("Amount Lent") },
                prefix = { Text("₱") },
                isError = amountError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = interestRate,
                    onValueChange = { input ->
                        interestRate = input.filter { c -> c.isDigit() || c == '.' }
                        val a = amount.toDoubleOrNull() ?: 0.0
                        val r = interestRate.toDoubleOrNull() ?: 0.0
                        if (a > 0) {
                            totalExpectedAmount = String.format("%.2f", a + (a * r / 100))
                        }
                    },
                    label = { Text("Interest") },
                    suffix = { Text("%") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = totalExpectedAmount,
                    onValueChange = { input ->
                        totalExpectedAmount = input.filter { c -> c.isDigit() || c == '.' }
                        val a = amount.toDoubleOrNull() ?: 0.0
                        val t = totalExpectedAmount.toDoubleOrNull() ?: 0.0
                        if (a > 0) {
                            interestRate = String.format("%.2f", ((t / a) - 1) * 100)
                        }
                    },
                    label = { Text("Total to Receive") },
                    prefix = { Text("₱") },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Source Account", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    val interestRateValue = interestRate.toDoubleOrNull() ?: 0.0
                    val totalExpectedValue = totalExpectedAmount.toDoubleOrNull() ?: amountValue ?: 0.0
                    
                    when {
                        borrowerName.isBlank() -> borrowerError = true
                        amountValue == null || amountValue <= 0 -> amountError = true
                        else -> {
                            onSave(
                                borrowerName.trim(), 
                                amountValue, 
                                balanceType, 
                                interestRateValue,
                                totalExpectedValue,
                                description.ifEmpty { null },
                                null
                            )
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
fun AddRepaymentSheet(
    loan: Loan,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit,
    onSave: (Double, BalanceType, String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var balanceType by remember { mutableStateOf(loan.balanceType) }
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
                .verticalScroll(rememberScrollState())
                .imePadding()
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
            
            Text("Receive To", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        else -> { onSave(amountValue, balanceType, note.ifEmpty { null }); onDismiss() }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBudgetEntrySheet(
    entry: BudgetEntry,
    onDismiss: () -> Unit,
    onSave: (BudgetType, BalanceType, Double, String, CategoryType) -> Unit,
    onDelete: () -> Unit
) {
    var type by remember(entry.id) { mutableStateOf(entry.type) }
    var balanceType by remember(entry.id) { mutableStateOf(entry.balanceType) }
    var categoryType by remember(entry.id) { mutableStateOf(entry.categoryType) }
    var amount by remember(entry.id) { mutableStateOf(entry.amount.toString()) }
    var description by remember(entry.id) { mutableStateOf(entry.description) }
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
                .verticalScroll(rememberScrollState())
                .imePadding()
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
                    selected = type == BudgetType.EXPENSE || type == BudgetType.LOAN_OUT,
                    onClick = { type = BudgetType.EXPENSE },
                    label = { Text("Expense") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ErrorRed.copy(alpha = 0.1f),
                        selectedLabelColor = ErrorRed
                    )
                )
                FilterChip(
                    selected = type == BudgetType.INCOME || type == BudgetType.LOAN_REPAYMENT,
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

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Category Type (Necessity/Leisure)
            if (type == BudgetType.EXPENSE || type == BudgetType.LOAN_OUT) {
                Text("Classification", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = categoryType == CategoryType.NECESSITY,
                        onClick = { categoryType = CategoryType.NECESSITY },
                        label = { Text("Necessity") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricTeal.copy(alpha = 0.1f),
                            selectedLabelColor = ElectricTeal
                        )
                    )
                    FilterChip(
                        selected = categoryType == CategoryType.LEISURE,
                        onClick = { categoryType = CategoryType.LEISURE },
                        label = { Text("Leisure") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForgedGold.copy(alpha = 0.1f),
                            selectedLabelColor = ForgedGold
                        )
                    )
                    FilterChip(
                        selected = categoryType == CategoryType.NONE,
                        onClick = { categoryType = CategoryType.NONE },
                        label = { Text("None") },
                        modifier = Modifier.weight(0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                        else -> { onSave(type, balanceType, amountValue, description.trim(), categoryType); onDismiss() }
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
fun RepaymentHistorySheet(
    loan: Loan,
    repayments: List<LoanRepayment>,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }

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
                text = "Repayment History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Loan to ${loan.borrowerName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (repayments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No repayments recorded yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(repayments) { repayment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currencyFormat.format(repayment.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricTeal
                                )
                                Text(
                                    text = dateFormat.format(Date(repayment.repaymentDate)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                repayment.note?.let { note ->
                                    if (note.isNotBlank()) {
                                        Text(
                                            text = note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                            
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = ElectricTeal.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Close", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
