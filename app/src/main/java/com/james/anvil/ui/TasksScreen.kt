package com.james.anvil.ui

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
import com.james.anvil.MainActivity
import com.james.anvil.data.Task
import com.james.anvil.formatDate
import com.james.anvil.ui.components.ConsistencyChart
import com.james.anvil.ui.components.EmptyState
import java.util.*
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: TaskViewModel) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Deadlines", "Done")

    Scaffold(
        topBar = {
            Column {
                // Removed the "ANVIL: Forge Your Will" TopAppBar as requested
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(onClick = { showAddTaskDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTabIndex) {
                0 -> PendingTasksTab(viewModel)
                1 -> DoneTasksTab(viewModel)
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
fun PendingTasksTab(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp)) {
        if (tasks.isEmpty()) {
            EmptyState(
                message = "No pending tasks. You are free.",
                icon = Icons.Default.Check,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn {
                items(tasks) { task ->
                    TaskItem(task = task, onComplete = { viewModel.completeTask(task) })
                }
            }
        }
    }
}

@Composable
fun DoneTasksTab(viewModel: TaskViewModel) {
    val completedTasks by viewModel.completedTasks.collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = "Consistency (Last 7 Days)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            ConsistencyChart(completedTasks = completedTasks)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "History",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (completedTasks.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No completed tasks yet.")
                }
            }
        } else {
            items(completedTasks) { task ->
                ListItem(
                    headlineContent = { Text(task.title, style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) },
                    // Change line 128 to this:
                    supportingContent = {
                        Text("Completed: ${formatDate(task.completedAt ?: System.currentTimeMillis())}")
                    },
                    leadingContent = { Icon(Icons.Default.Check, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, onComplete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Deadline: ${formatDate(task.deadline)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (task.deadline < System.currentTimeMillis()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                )
            }
            FilledIconButton(
                onClick = onComplete,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Complete", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onTaskAdded: (String, Long) -> Unit) {
    var title by remember { mutableStateOf("") }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    // Initial state
    var selectedDeadline by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Updates
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            // After date picked, show time picker
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
                Spacer(modifier = Modifier.height(16.dp))
                Text("Deadline:")
                Button(onClick = { datePickerDialog.show() }) {
                    Text(text = formatDate(selectedDeadline))
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (title.isNotBlank()) {
                    onTaskAdded(title, selectedDeadline) 
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
