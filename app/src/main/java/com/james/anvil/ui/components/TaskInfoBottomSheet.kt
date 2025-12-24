package com.james.anvil.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.james.anvil.data.Task
import com.james.anvil.formatDate

@Composable
fun TaskInfoBottomSheet(
    task: Task,
    onDismiss: () -> Unit,
    onToggleStep: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssistChip(
                onClick = {},
                label = { Text(task.category) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Due: ${formatDate(task.deadline)}", style = MaterialTheme.typography.bodyMedium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        
        if (task.steps.isNotEmpty()) {
            Text("Steps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
            ) {
                items(task.steps) { step ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = step.isCompleted,
                            onCheckedChange = { isChecked ->
                                onToggleStep(step.id, isChecked)
                            }
                        )
                        Text(
                            text = step.title,
                            modifier = Modifier.padding(start = 8.dp),
                            style = if (step.isCompleted) MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.outline) else MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            Text("No sub-steps.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Close")
        }
    }
}
