package com.james.anvil.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.james.anvil.data.Task
import com.james.anvil.formatDate
import com.james.anvil.ui.components.ConsistencyChart
import com.james.anvil.ui.components.EmptyState
import com.james.anvil.ui.components.MotivationCard
import com.james.anvil.ui.components.TaskItem
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: TaskViewModel, snackbarHostState: SnackbarHostState) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Deadlines", "Done")

    // New State for Dialogs and Selection
    var showInfoDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // ViewModel state
    val dailyProgress by viewModel.dailyProgress.collectAsState(initial = 0f)
    val pendingCount by viewModel.dailyPendingCount.collectAsState(initial = 0)
    val dailyQuote by viewModel.dailyQuote.collectAsState(initial = "")
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    // Extract unique categories from tasks to use as suggestions
    val existingCategories = remember(tasks) {
        tasks.map { it.category }.distinct().filter { it != "General" }.sorted()
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(onClick = { showAddTaskDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Header with Motivation Card
            if (selectedTabIndex == 0) {
                MotivationCard(
                    dailyProgress = dailyProgress,
                    pendingCount = pendingCount,
                    quote = dailyQuote,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Tabs
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            // Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTabIndex) {
                    0 -> PendingTasksTab(
                        viewModel = viewModel, 
                        onTaskClick = { task ->
                            selectedTask = task
                            showInfoDialog = true
                        },
                        onTaskLongClick = { task ->
                            selectedTask = task
                            showOptionsDialog = true
                        },
                        onEdit = { task ->
                            selectedTask = task
                            showEditDialog = true
                        },
                        onDelete = { task ->
                            viewModel.deleteTask(task)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Task deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoDeleteTask(task)
                                }
                            }
                        }
                    )
                    1 -> DoneTasksTab(viewModel)
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            existingCategories = existingCategories,
            onDismiss = { showAddTaskDialog = false },
            onTaskAdded = { title, deadline, category ->
                viewModel.addTask(title, deadline, category)
                showAddTaskDialog = false
            }
        )
    }

    if (showInfoDialog && selectedTask != null) {
        TaskInfoDialog(
            task = selectedTask!!,
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showOptionsDialog && selectedTask != null) {
        TaskOptionsDialog(
            task = selectedTask!!,
            onDismiss = { showOptionsDialog = false },
            onEdit = {
                showOptionsDialog = false
                showEditDialog = true
            },
            onDelete = {
                val taskToDelete = selectedTask!!
                showOptionsDialog = false
                viewModel.deleteTask(taskToDelete)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Task deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDeleteTask(taskToDelete)
                    }
                }
            }
        )
    }

    if (showEditDialog && selectedTask != null) {
        EditTaskDialog(
            task = selectedTask!!,
            existingCategories = existingCategories,
            onDismiss = { showEditDialog = false },
            onTaskUpdated = { updatedTask ->
                viewModel.updateTask(updatedTask)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun PendingTasksTab(
    viewModel: TaskViewModel,
    onTaskClick: (Task) -> Unit,
    onTaskLongClick: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (tasks.isEmpty()) {
            EmptyState(
                message = "No pending tasks. You are free.",
                icon = Icons.Default.Check,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn {
                items(tasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task, 
                        onComplete = { viewModel.completeTask(task) },
                        onEdit = { onEdit(task) },
                        onDelete = { onDelete(task) },
                        modifier = Modifier
                            .animateItem()
                            .combinedClickable(
                                onClick = { onTaskClick(task) },
                                onLongClick = { onTaskLongClick(task) }
                            )
                    )
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
                    supportingContent = {
                        Text("Completed: ${formatDate(task.completedAt ?: System.currentTimeMillis())} • ${task.category}")
                    },
                    leadingContent = { Icon(Icons.Default.Check, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
fun CategorySelectionInput(
    category: String,
    onCategoryChange: (String) -> Unit,
    existingCategories: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = category,
            onValueChange = { onCategoryChange(it) },
            label = { Text("Category (e.g. Work, Health)") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Category")
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.8f) // Optional width adjustment
        ) {
            val suggestions = (existingCategories + listOf("Work", "Personal", "Health", "Education", "Finance")).distinct().sorted()
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onCategoryChange(suggestion)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onTaskAdded: (String, Long, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
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
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                CategorySelectionInput(
                    category = category,
                    onCategoryChange = { category = it },
                    existingCategories = existingCategories
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
                    onTaskAdded(title, selectedDeadline, category.ifBlank { "General" }) 
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

@Composable
fun TaskInfoDialog(task: Task, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = task.title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(text = "Category: ${task.category}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Deadline: ${formatDate(task.deadline)}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Status: ${if (task.isCompleted) "Completed" else "Pending"}")
                if (task.isCompleted && task.completedAt != null) {
                    Text(text = "Completed at: ${formatDate(task.completedAt)}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TaskOptionsDialog(
    task: Task,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(
                    text = "Options",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Start)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Edit")
                }
                
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Start),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun EditTaskDialog(
    task: Task, 
    existingCategories: List<String>,
    onDismiss: () -> Unit, 
    onTaskUpdated: (Task) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var category by remember { mutableStateOf(task.category) }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    // Initial state
    var selectedDeadline by remember { mutableStateOf(task.deadline) }
    
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
        title = { Text("Edit Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                CategorySelectionInput(
                    category = category,
                    onCategoryChange = { category = it },
                    existingCategories = existingCategories
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
                    onTaskUpdated(task.copy(title = title, deadline = selectedDeadline, category = category.ifBlank { "General" })) 
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
