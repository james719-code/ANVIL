package com.james.anvil.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.ui.theme.*

// ============================================
// GLASSMORPHISM FINANCE CARD
// ============================================
@Composable
fun GlassFinanceCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = ForgedGold
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = IndustrialGrey.copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, IndustrialBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(200f, 200f)
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ============================================
// THE VAULT - FINANCE SECTION
// ============================================
@Composable
fun VaultSection(
    totalBalance: String,
    dailySpend: String,
    savingsGoal: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Text(
            text = "THE VAULT",
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = ForgedGold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Horizontal scrolling finance cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            item {
                GlassFinanceCard(
                    title = "Total Balance",
                    value = totalBalance,
                    icon = Icons.Outlined.AccountBalance,
                    accentColor = ForgedGold
                )
            }
            item {
                GlassFinanceCard(
                    title = "Daily Spend",
                    value = dailySpend,
                    icon = Icons.Outlined.TrendingDown,
                    accentColor = ForgedGoldDark
                )
            }
            item {
                GlassFinanceCard(
                    title = "Savings Goal",
                    value = savingsGoal,
                    icon = Icons.Outlined.Savings,
                    accentColor = ForgedGold
                )
            }
        }
    }
}

// ============================================
// PRIORITY BADGE
// ============================================
enum class TaskPriority {
    HIGH, MEDIUM, LOW
}

@Composable
fun PriorityBadge(
    priority: TaskPriority,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, text) = when (priority) {
        TaskPriority.HIGH -> Pair(Color(0xFFE53935), "HIGH")
        TaskPriority.MEDIUM -> Pair(ForgedGold, "MED")
        TaskPriority.LOW -> Pair(SteelBlue, "LOW")
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, backgroundColor.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = backgroundColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ============================================
// CIRCULAR CHECKBOX
// ============================================
@Composable
fun CircularCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    checkedColor: Color = SteelBlue
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(200),
        label = "checkbox"
    )
    
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                if (checked) checkedColor else Color.Transparent
            )
            .border(
                width = 2.dp,
                color = if (checked) checkedColor else IndustrialBorder,
                shape = CircleShape
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ============================================
// INDUSTRIAL TASK ITEM
// ============================================
@Composable
fun IndustrialTaskItem(
    title: String,
    priority: TaskPriority,
    isCompleted: Boolean,
    onCompletedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = IndustrialGrey
        ),
        border = BorderStroke(1.dp, IndustrialBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularCheckbox(
                checked = isCompleted,
                onCheckedChange = onCompletedChange
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = if (isCompleted) {
                    Color.White.copy(alpha = 0.5f)
                } else {
                    Color.White
                },
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            PriorityBadge(priority = priority)
        }
    }
}

// ============================================
// THE GRIND - TASKS SECTION
// ============================================
data class GrindTask(
    val id: String,
    val title: String,
    val priority: TaskPriority,
    val isCompleted: Boolean
)

@Composable
fun GrindSection(
    tasks: List<GrindTask>,
    onTaskCompletedChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Text(
            text = "THE GRIND",
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = SteelBlue,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Task items
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            tasks.forEach { task ->
                IndustrialTaskItem(
                    title = task.title,
                    priority = task.priority,
                    isCompleted = task.isCompleted,
                    onCompletedChange = { completed ->
                        onTaskCompletedChange(task.id, completed)
                    }
                )
            }
        }
    }
}

// ============================================
// DASHBOARD HEADER (DAILY BRIEFING)
// ============================================
@Composable
fun IndustrialDashboardHeader(
    userName: String,
    budgetProgress: Float,
    tasksCompleted: Int,
    totalTasks: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section label
        Text(
            text = "DAILY BRIEFING",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Color.White.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Greeting
        Text(
            text = "Welcome back, $userName",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Combined progress card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = IndustrialGrey
            ),
            border = BorderStroke(1.dp, IndustrialBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "BUDGET",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp
                            ),
                            color = ForgedGold
                        )
                        Text(
                            text = "${(budgetProgress * 100).toInt()}% remaining",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TASKS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp
                            ),
                            color = SteelBlue
                        )
                        Text(
                            text = "$tasksCompleted/$totalTasks completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Dual progress bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(IndustrialGreyLight)
                ) {
                    // Budget progress (left half)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(budgetProgress)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(ForgedGoldDark, ForgedGold)
                                    )
                                )
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Tasks progress (right half)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val taskProgress = if (totalTasks > 0) {
                            tasksCompleted.toFloat() / totalTasks
                        } else 0f
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(taskProgress)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(SteelBlueDark, SteelBlue)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// DIAMOND FAB
// ============================================
@Composable
fun DiamondFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .rotate(45f)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(ForgedGold, SteelBlue),
                    start = Offset(0f, 0f),
                    end = Offset(100f, 100f)
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add",
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .rotate(-45f) // Counter-rotate icon
        )
    }
}

// ============================================
// PREVIEWS
// ============================================
@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun VaultSectionPreview() {
    ANVILTheme(darkTheme = true) {
        VaultSection(
            totalBalance = "₱45,230",
            dailySpend = "₱1,250",
            savingsGoal = "₱10,000",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun GrindSectionPreview() {
    ANVILTheme(darkTheme = true) {
        GrindSection(
            tasks = listOf(
                GrindTask("1", "Complete project report", TaskPriority.HIGH, false),
                GrindTask("2", "Review pull requests", TaskPriority.MEDIUM, true),
                GrindTask("3", "Update documentation", TaskPriority.LOW, false)
            ),
            onTaskCompletedChange = { _, _ -> },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun IndustrialDashboardHeaderPreview() {
    ANVILTheme(darkTheme = true) {
        IndustrialDashboardHeader(
            userName = "James",
            budgetProgress = 0.65f,
            tasksCompleted = 4,
            totalTasks = 7,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun DiamondFABPreview() {
    ANVILTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color(0xFF0F0F0F)),
            contentAlignment = Alignment.Center
        ) {
            DiamondFAB(onClick = {})
        }
    }
}
