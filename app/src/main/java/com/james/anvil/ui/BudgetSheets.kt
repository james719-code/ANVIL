package com.james.anvil.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.data.*
import com.james.anvil.ui.theme.*
import com.james.anvil.util.Categories
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Common quick suggestions for descriptions
private val expenseSuggestions = Categories.Budget.expenseCategories

private val incomeSuggestions = listOf(
    "Salary", "Freelance", "Side Hustle", 
    "Gift", "Refund", "Investment", "Bonus"
)

/**
 * Reusable section header with animated indicator
 */
@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Premium quick suggestion chip with subtle animation
 */
@Composable
private fun QuickSuggestionChip(
    text: String,
    onClick: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = null,
        modifier = Modifier.height(32.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = accentColor
            )
        }
    }
}

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
    var isDescriptionFocused by remember { mutableStateOf(false) }
    
    val maxDescriptionLength = 50
    val currentSuggestions = if (type == BudgetType.INCOME) incomeSuggestions else expenseSuggestions
    
    // Animated colors based on type
    val typeAccentColor by animateColorAsState(
        targetValue = if (type == BudgetType.INCOME) ElectricTeal else ErrorRed,
        animationSpec = tween(300),
        label = "typeColor"
    )

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
            // Header with animated accent
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    typeAccentColor.copy(alpha = 0.2f),
                                    typeAccentColor.copy(alpha = 0.05f)
                                )
                            ),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (type == BudgetType.INCOME) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                        contentDescription = if (type == BudgetType.INCOME) "Income" else "Expense",
                        tint = typeAccentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "New Transaction",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Record your ${if (type == BudgetType.INCOME) "income" else "expense"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Type Selector - Premium toggle style
            SectionHeader(
                title = "Transaction Type",
                icon = Icons.Outlined.SwapVert,
                accentColor = typeAccentColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Expense button
                Surface(
                    onClick = { type = BudgetType.EXPENSE },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (type == BudgetType.EXPENSE) ErrorRed.copy(alpha = 0.15f) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (type == BudgetType.EXPENSE) {
                            Icon(
                                Icons.Outlined.ArrowUpward,
                                null,
                                tint = ErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            "Expense",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (type == BudgetType.EXPENSE) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Income button
                Surface(
                    onClick = { type = BudgetType.INCOME },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (type == BudgetType.INCOME) ElectricTeal.copy(alpha = 0.15f) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (type == BudgetType.INCOME) {
                            Icon(
                                Icons.Outlined.ArrowDownward,
                                null,
                                tint = ElectricTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            "Income",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (type == BudgetType.INCOME) ElectricTeal else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Balance Type Selector
            SectionHeader(
                title = "Payment Source",
                icon = Icons.Outlined.Wallet,
                accentColor = if (balanceType == BalanceType.GCASH) InfoBlue else SteelBlue
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cash option
                Surface(
                    onClick = { balanceType = BalanceType.CASH },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = if (balanceType == BalanceType.CASH) SteelBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (balanceType == BalanceType.CASH) 
                        androidx.compose.foundation.BorderStroke(1.5.dp, SteelBlue.copy(alpha = 0.3f)) 
                    else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.Wallet,
                            null,
                            tint = if (balanceType == BalanceType.CASH) SteelBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Cash",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (balanceType == BalanceType.CASH) SteelBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // GCash option
                Surface(
                    onClick = { balanceType = BalanceType.GCASH },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = if (balanceType == BalanceType.GCASH) InfoBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (balanceType == BalanceType.GCASH) 
                        androidx.compose.foundation.BorderStroke(1.5.dp, InfoBlue.copy(alpha = 0.3f)) 
                    else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.Smartphone,
                            null,
                            tint = if (balanceType == BalanceType.GCASH) InfoBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "GCash",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (balanceType == BalanceType.GCASH) InfoBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Transaction Category Type (Necessity/Leisure) - only for expenses
            AnimatedVisibility(
                visible = type == BudgetType.EXPENSE || type == BudgetType.LOAN_OUT,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(
                        title = "Classification",
                        icon = Icons.Outlined.Category,
                        accentColor = when (categoryType) {
                            CategoryType.NECESSITY -> ElectricTeal
                            CategoryType.LEISURE -> ForgedGold
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Necessity
                        Surface(
                            onClick = { categoryType = CategoryType.NECESSITY },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (categoryType == CategoryType.NECESSITY) ElectricTeal.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = if (categoryType == CategoryType.NECESSITY)
                                androidx.compose.foundation.BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.3f))
                            else null
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    null,
                                    tint = if (categoryType == CategoryType.NECESSITY) ElectricTeal else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Necessity",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (categoryType == CategoryType.NECESSITY) ElectricTeal else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Leisure
                        Surface(
                            onClick = { categoryType = CategoryType.LEISURE },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (categoryType == CategoryType.LEISURE) ForgedGold.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = if (categoryType == CategoryType.LEISURE)
                                androidx.compose.foundation.BorderStroke(1.dp, ForgedGold.copy(alpha = 0.3f))
                            else null
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.Celebration,
                                    null,
                                    tint = if (categoryType == CategoryType.LEISURE) ForgedGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Leisure",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (categoryType == CategoryType.LEISURE) ForgedGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // None
                        Surface(
                            onClick = { categoryType = CategoryType.NONE },
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (categoryType == CategoryType.NONE) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.Remove,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "None",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            
            // Divider with subtle gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Spacer(modifier = Modifier.height(28.dp))

            // Amount Field - Enhanced
            SectionHeader(
                title = "Amount",
                icon = Icons.Outlined.Payments,
                accentColor = typeAccentColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it.filter { c -> c.isDigit() || c == '.' }
                    amountError = false
                },
                placeholder = { 
                    Text(
                        "0.00",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    ) 
                },
                leadingIcon = {
                    Text(
                        "₱",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = typeAccentColor
                    )
                },
                isError = amountError,
                supportingText = if (amountError) {
                    { Text("Please enter a valid amount", color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = typeAccentColor,
                    focusedLabelColor = typeAccentColor,
                    cursorColor = typeAccentColor
                ),
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Description Field - Significantly Enhanced
            SectionHeader(
                title = "Description",
                icon = Icons.Outlined.Description,
                accentColor = typeAccentColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = {
                    if (it.length <= maxDescriptionLength) {
                        description = it
                        descriptionError = false
                    }
                },
                placeholder = { 
                    Text(
                        "What was this for?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    ) 
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.EditNote,
                        contentDescription = "Description",
                        tint = if (isDescriptionFocused) typeAccentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingIcon = {
                    // Character counter
                    Text(
                        "${description.length}/$maxDescriptionLength",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            description.length >= maxDescriptionLength -> MaterialTheme.colorScheme.error
                            description.length >= maxDescriptionLength * 0.8 -> ForgedGold
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        }
                    )
                },
                isError = descriptionError,
                supportingText = if (descriptionError) {
                    { Text("Description is required", color = MaterialTheme.colorScheme.error) }
                } else {
                    { Text("Brief note about this transaction", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isDescriptionFocused = it.isFocused },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = typeAccentColor,
                    focusedLabelColor = typeAccentColor,
                    cursorColor = typeAccentColor
                )
            )
            
            // Quick Suggestions - Animated based on focus
            AnimatedVisibility(
                visible = description.isEmpty() || isDescriptionFocused,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Quick Suggestions",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentSuggestions) { suggestion ->
                            QuickSuggestionChip(
                                text = suggestion,
                                onClick = { description = suggestion },
                                accentColor = typeAccentColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button - Premium style
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
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = typeAccentColor
                )
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Save",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save ${if (type == BudgetType.INCOME) "Income" else "Expense"}",
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
    var isDescriptionFocused by remember { mutableStateOf(false) }
    
    val maxDescriptionLength = 50
    val currentSuggestions = if (type == BudgetType.INCOME || type == BudgetType.LOAN_REPAYMENT) incomeSuggestions else expenseSuggestions
    
    // Animated colors based on type
    val typeAccentColor by animateColorAsState(
        targetValue = when (type) {
            BudgetType.INCOME, BudgetType.LOAN_REPAYMENT -> ElectricTeal
            else -> ErrorRed
        },
        animationSpec = tween(300),
        label = "editTypeColor"
    )

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
            // Premium Header with gradient icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        typeAccentColor.copy(alpha = 0.2f),
                                        typeAccentColor.copy(alpha = 0.05f)
                                    )
                                ),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit transaction",
                            tint = typeAccentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Edit Transaction",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Modify your ${if (type == BudgetType.INCOME || type == BudgetType.LOAN_REPAYMENT) "income" else "expense"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Delete button with subtle styling
                Surface(
                    onClick = { showDeleteConfirm = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                ) {
                    Box(
                        modifier = Modifier.padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Type Selector - Premium toggle style
            SectionHeader(
                title = "Transaction Type",
                icon = Icons.Outlined.SwapVert,
                accentColor = typeAccentColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Expense button
                Surface(
                    onClick = { type = BudgetType.EXPENSE },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (type == BudgetType.EXPENSE || type == BudgetType.LOAN_OUT) ErrorRed.copy(alpha = 0.15f) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (type == BudgetType.EXPENSE || type == BudgetType.LOAN_OUT) {
                            Icon(
                                Icons.Outlined.ArrowUpward,
                                null,
                                tint = ErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            "Expense",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (type == BudgetType.EXPENSE || type == BudgetType.LOAN_OUT) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Income button
                Surface(
                    onClick = { type = BudgetType.INCOME },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (type == BudgetType.INCOME || type == BudgetType.LOAN_REPAYMENT) ElectricTeal.copy(alpha = 0.15f) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (type == BudgetType.INCOME || type == BudgetType.LOAN_REPAYMENT) {
                            Icon(
                                Icons.Outlined.ArrowDownward,
                                null,
                                tint = ElectricTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            "Income",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (type == BudgetType.INCOME || type == BudgetType.LOAN_REPAYMENT) ElectricTeal else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Balance Type Selector
            SectionHeader(
                title = "Payment Source",
                icon = Icons.Outlined.Wallet,
                accentColor = if (balanceType == BalanceType.GCASH) InfoBlue else SteelBlue
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cash option
                Surface(
                    onClick = { balanceType = BalanceType.CASH },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = if (balanceType == BalanceType.CASH) SteelBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (balanceType == BalanceType.CASH) 
                        androidx.compose.foundation.BorderStroke(1.5.dp, SteelBlue.copy(alpha = 0.3f)) 
                    else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.Wallet,
                            null,
                            tint = if (balanceType == BalanceType.CASH) SteelBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Cash",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (balanceType == BalanceType.CASH) SteelBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // GCash option
                Surface(
                    onClick = { balanceType = BalanceType.GCASH },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = if (balanceType == BalanceType.GCASH) InfoBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (balanceType == BalanceType.GCASH) 
                        androidx.compose.foundation.BorderStroke(1.5.dp, InfoBlue.copy(alpha = 0.3f)) 
                    else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.Smartphone,
                            null,
                            tint = if (balanceType == BalanceType.GCASH) InfoBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "GCash",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (balanceType == BalanceType.GCASH) InfoBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Transaction Category Type (Necessity/Leisure) - Animated
            AnimatedVisibility(
                visible = type == BudgetType.EXPENSE || type == BudgetType.LOAN_OUT,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(
                        title = "Classification",
                        icon = Icons.Outlined.Category,
                        accentColor = when (categoryType) {
                            CategoryType.NECESSITY -> ElectricTeal
                            CategoryType.LEISURE -> ForgedGold
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Necessity
                        Surface(
                            onClick = { categoryType = CategoryType.NECESSITY },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (categoryType == CategoryType.NECESSITY) ElectricTeal.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = if (categoryType == CategoryType.NECESSITY)
                                androidx.compose.foundation.BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.3f))
                            else null
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    null,
                                    tint = if (categoryType == CategoryType.NECESSITY) ElectricTeal else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Necessity",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (categoryType == CategoryType.NECESSITY) ElectricTeal else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Leisure
                        Surface(
                            onClick = { categoryType = CategoryType.LEISURE },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (categoryType == CategoryType.LEISURE) ForgedGold.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = if (categoryType == CategoryType.LEISURE)
                                androidx.compose.foundation.BorderStroke(1.dp, ForgedGold.copy(alpha = 0.3f))
                            else null
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.Celebration,
                                    null,
                                    tint = if (categoryType == CategoryType.LEISURE) ForgedGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Leisure",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (categoryType == CategoryType.LEISURE) ForgedGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // None
                        Surface(
                            onClick = { categoryType = CategoryType.NONE },
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (categoryType == CategoryType.NONE) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.Remove,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "None",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            
            // Divider with subtle gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Spacer(modifier = Modifier.height(28.dp))

            // Amount Field - Enhanced
            SectionHeader(
                title = "Amount",
                icon = Icons.Outlined.Payments,
                accentColor = typeAccentColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }; amountError = false },
                placeholder = { 
                    Text(
                        "0.00",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    ) 
                },
                leadingIcon = {
                    Text(
                        "₱",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = typeAccentColor
                    )
                },
                isError = amountError,
                supportingText = if (amountError) {
                    { Text("Please enter a valid amount", color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = typeAccentColor,
                    focusedLabelColor = typeAccentColor,
                    cursorColor = typeAccentColor
                ),
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Description Field - Enhanced
            SectionHeader(
                title = "Description",
                icon = Icons.Outlined.Description,
                accentColor = typeAccentColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = {
                    if (it.length <= maxDescriptionLength) {
                        description = it
                        descriptionError = false
                    }
                },
                placeholder = { 
                    Text(
                        "What was this for?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    ) 
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.EditNote,
                        contentDescription = "Description",
                        tint = if (isDescriptionFocused) typeAccentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingIcon = {
                    // Character counter
                    Text(
                        "${description.length}/$maxDescriptionLength",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            description.length >= maxDescriptionLength -> MaterialTheme.colorScheme.error
                            description.length >= maxDescriptionLength * 0.8 -> ForgedGold
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        }
                    )
                },
                isError = descriptionError,
                supportingText = if (descriptionError) {
                    { Text("Description is required", color = MaterialTheme.colorScheme.error) }
                } else {
                    { Text("Brief note about this transaction", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isDescriptionFocused = it.isFocused },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = typeAccentColor,
                    focusedLabelColor = typeAccentColor,
                    cursorColor = typeAccentColor
                )
            )
            
            // Quick Suggestions - Animated based on focus
            AnimatedVisibility(
                visible = isDescriptionFocused,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Quick Suggestions",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentSuggestions) { suggestion ->
                            QuickSuggestionChip(
                                text = suggestion,
                                onClick = { description = suggestion },
                                accentColor = typeAccentColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button - Premium style
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    when {
                        amountValue == null || amountValue <= 0 -> amountError = true
                        description.isBlank() -> descriptionError = true
                        else -> { onSave(type, balanceType, amountValue, description.trim(), categoryType); onDismiss() }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = typeAccentColor
                )
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Save changes",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Save Changes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
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
                                contentDescription = "Payment complete",
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
