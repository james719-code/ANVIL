package com.james.anvil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.data.Task
import com.james.anvil.ui.theme.DeepTeal
import com.james.anvil.ui.theme.MutedTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview
import com.james.anvil.data.TaskStep

@Composable
fun TaskItem(
    task: Task,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left accent border
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        if (task.isDaily) MutedTeal else MaterialTheme.colorScheme.primary
                    )
            )
            
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Completion radio button
                RadioButton(
                    selected = task.isCompleted,
                    onClick = onComplete,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = DeepTeal,
                        unselectedColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Daily indicator
                        if (task.isDaily) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Daily Task",
                                modifier = Modifier.size(14.dp),
                                tint = MutedTeal
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = task.category,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeFormat.format(Date(task.deadline)),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        // Hardness level indicator
                        if (task.hardnessLevel > 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (task.hardnessLevel) {
                                            5 -> Color(0xFFE53935)
                                            4 -> Color(0xFFFF9800)
                                            3 -> Color(0xFFFFC107)
                                            else -> DeepTeal.copy(alpha = 0.7f)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "H${task.hardnessLevel}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

// =============================================
// Preview Functions (Removed in Release Builds)
// =============================================

@Preview(name = "Task Item - Pending", showBackground = true)
@Composable
private fun TaskItemPendingPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = false) {
        TaskItem(
            task = Task(
                id = 1,
                title = "Complete project report",
                category = "Work",
                deadline = System.currentTimeMillis() + 86400000,
                isCompleted = false,
                isDaily = false,
                steps = emptyList()
            ),
            onComplete = {},
            onDelete = {},
            onEdit = {}
        )
    }
}

@Preview(name = "Task Item - Daily", showBackground = true)
@Composable
private fun TaskItemDailyPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = false) {
        TaskItem(
            task = Task(
                id = 2,
                title = "Morning exercise",
                category = "Health",
                deadline = System.currentTimeMillis(),
                isCompleted = false,
                isDaily = true,
                steps = emptyList()
            ),
            onComplete = {},
            onDelete = {},
            onEdit = {}
        )
    }
}

@Preview(name = "Task Item - Dark", showBackground = true)
@Composable
private fun TaskItemDarkPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = true) {
        TaskItem(
            task = Task(
                id = 3,
                title = "Read documentation",
                category = "Education",
                deadline = System.currentTimeMillis() + 3600000,
                isCompleted = false,
                isDaily = false,
                steps = listOf(TaskStep(id = "1", title = "Chapter 1"))
            ),
            onComplete = {},
            onDelete = {},
            onEdit = {}
        )
    }
}
