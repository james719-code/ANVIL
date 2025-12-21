package com.james.anvil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.anvil.data.Task
import com.james.anvil.ui.TaskViewModel
import com.james.anvil.ui.theme.ANVILTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ANVILTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: TaskViewModel) {
    var showAddTaskDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ANVIL: Forge Your Will") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTaskDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(
                text = "Pending Tasks",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val tasks by viewModel.tasks.collectAsState(initial = emptyList())
            
            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No pending tasks. You are free.")
                }
            } else {
                LazyColumn {
                    items(tasks) { task ->
                        TaskItem(task = task, onComplete = { viewModel.completeTask(task) })
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onTaskAdded = { title, deadline ->
                viewModel.addTask(title, deadline)
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun TaskItem(task: Task, onComplete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, fontWeight = FontWeight.Bold)
                Text(
                    text = "Deadline: ${formatDate(task.deadline)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onComplete) {
                Icon(Icons.Default.Check, contentDescription = "Complete")
            }
        }
    }
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onTaskAdded: (String, Long) -> Unit) {
    var title by remember { mutableStateOf("") }
    // Default deadline: End of today (for simplicity in this UI)
    // Real app would have a DatePicker
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    val defaultDeadline = calendar.timeInMillis

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Deadline: Today 11:59 PM (Fixed for prototype)")
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (title.isNotBlank()) {
                    onTaskAdded(title, defaultDeadline) 
                }
            }) {
                Text("Forge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
