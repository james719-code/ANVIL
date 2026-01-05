package com.james.anvil.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.formatDate
import com.james.anvil.ui.theme.DeepTeal
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onTaskAdded: (String, Long, String, List<String>, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf(listOf<String>()) }
    var currentStep by remember { mutableStateOf("") }
    var hardnessLevel by remember { mutableFloatStateOf(1f) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var selectedDeadline by remember { mutableStateOf(System.currentTimeMillis()) }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    selectedDeadline = calendar.timeInMillis
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp) 
    ) {
        Text(
            text = "New Task",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select")
                    }
                }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                 val suggestions = (existingCategories + listOf("Work", "Personal", "Health", "Education", "Finance")).distinct().sorted()
                 suggestions.forEach { suggestion ->
                     DropdownMenuItem(
                         text = { Text(suggestion) },
                         onClick = {
                             category = suggestion
                             expanded = false
                         }
                     )
                 }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Deadline", style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = { datePickerDialog.show() }) {
                Text(formatDate(selectedDeadline))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Hardness Level Slider
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hardness Level", style = MaterialTheme.typography.bodyLarge)
                Box(
                    modifier = Modifier
                        .background(DeepTeal.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${hardnessLevel.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepTeal
                    )
                }
            }
            Slider(
                value = hardnessLevel,
                onValueChange = { hardnessLevel = it },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = DeepTeal,
                    activeTrackColor = DeepTeal
                )
            )
            Text(
                text = when (hardnessLevel.toInt()) {
                    1 -> "Complete by deadline"
                    2 -> "Complete 2 days before"
                    3 -> "Complete 3 days before"
                    4 -> "Complete 4 days before"
                    5 -> "Complete 5 days before"
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))
        
        Text("Steps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentStep,
                onValueChange = { currentStep = it },
                placeholder = { Text("Add sub-task") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (currentStep.isNotBlank()) {
                    steps = steps + currentStep
                    currentStep = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
        
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
        ) {
            items(steps.size) { index ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("• ${steps[index]}", modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        val newSteps = steps.toMutableList()
                        newSteps.removeAt(index)
                        steps = newSteps
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                 if (title.isNotBlank()) {
                    onTaskAdded(title, selectedDeadline, category.ifBlank { "General" }, steps, hardnessLevel.toInt())
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Task")
        }
    }
}

