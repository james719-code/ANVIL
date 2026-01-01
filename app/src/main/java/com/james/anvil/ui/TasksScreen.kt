package com.james.anvil.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.james.anvil.data.Task
import com.james.anvil.data.TaskStep
import com.james.anvil.formatDate
import com.james.anvil.ui.components.EmptyState
import com.james.anvil.ui.components.TaskItem
import com.james.anvil.ui.navigation.Screen
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TaskViewModel,
    snackbarHostState: SnackbarHostState,
    navController: NavController
) {
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Deadlines", "Done")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    
    var showInfoDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    var editingTaskId by remember { mutableStateOf<Long?>(null) }

    val scope = rememberCoroutineScope()

    
    val allTasks by viewModel.tasks.collectAsState(initial = emptyList())
    val selectedTask = remember(allTasks, selectedTaskId) {
        allTasks.find { it.id == selectedTaskId }
    }
    val editingTask = remember(allTasks, editingTaskId) {
        allTasks.find { it.id == editingTaskId }
    }
    
    val existingCategories = remember(allTasks) {
        allTasks.map { it.category }.distinct().filter { it != "General" }.sorted()
    }

    
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val tasks = remember(allTasks, selectedCategory) {
        if (selectedCategory == null) allTasks
        else allTasks.filter { it.category == selectedCategory }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(onClick = { showAddTaskSheet = true }) {
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
            
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            if (selectedTabIndex == 0) {
                CategoryFilterRow(
                    categories = listOf("All") + existingCategories,
                    selectedCategory = selectedCategory ?: "All",
                    onCategorySelected = { selectedCategory = if (it == "All") null else it }
                )
            }

            
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTabIndex) {
                    0 -> PendingTasksTab(
                        viewModel = viewModel,
                        tasks = tasks,
                        onTaskClick = { task ->
                            selectedTaskId = task.id
                            showInfoDialog = true
                        },
                        onTaskLongClick = { task ->
                            selectedTaskId = task.id
                            showOptionsDialog = true
                        },
                        onEdit = { task ->
                            editingTaskId = task.id
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

    

    if (showAddTaskSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddTaskSheet = false },
            sheetState = sheetState
        ) {
            AddTaskBottomSheetContent(
                existingCategories = existingCategories,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) showAddTaskSheet = false
                    }
                },
                onTaskAdded = { title, deadline, category, steps, isDaily ->
                    viewModel.addTask(title, deadline, category, steps, isDaily)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) showAddTaskSheet = false
                    }
                }
            )
        }
    }

    if (showInfoDialog && selectedTask != null) {
        ModalBottomSheet(
            onDismissRequest = { showInfoDialog = false },
            sheetState = sheetState
        ) {
            TaskInfoBottomSheetContent(
                task = selectedTask,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) showInfoDialog = false
                    }
                },
                onToggleStep = { stepId, isDone ->
                    viewModel.toggleTaskStep(selectedTask, stepId, isDone)
                }
            )
        }
    }

    if (editingTask != null) {
        ModalBottomSheet(
            onDismissRequest = { editingTaskId = null },
            sheetState = sheetState
        ) {
            EditTaskBottomSheetContent(
                task = editingTask,
                existingCategories = existingCategories,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) editingTaskId = null
                    }
                },
                onSave = { updatedTask ->
                    viewModel.updateTask(updatedTask)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) editingTaskId = null
                    }
                }
            )
        }
    }

    if (showOptionsDialog && selectedTask != null) {
        TaskOptionsDialog(
            task = selectedTask,
            onDismiss = { showOptionsDialog = false },
            onEdit = {
                showOptionsDialog = false
                editingTaskId = selectedTask.id
            },
            onDelete = {
                val taskToDelete = selectedTask
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                leadingIcon = if (category == selectedCategory) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PendingTasksTab(
    viewModel: TaskViewModel,
    onTaskClick: (Task) -> Unit,
    onTaskLongClick: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    tasks: List<Task>
) {
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
            items(completedTasks, key = { it.id }) { task ->
                SwipeToDismissItem(
                    onDismiss = { viewModel.deleteTask(task) }
                ) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissItem(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                MaterialTheme.colorScheme.errorContainer
            else Color.Transparent

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = {
            content()
        },
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = false
    )
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
            modifier = Modifier.fillMaxWidth(0.8f)
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
fun TaskInfoBottomSheetContent(
    task: Task,
    onDismiss: () -> Unit,
    onToggleStep: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = task.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Category: ${task.category}")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Deadline: ${formatDate(task.deadline)}")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Status: ${if (task.isCompleted) "Completed" else "Pending"}")
        if (task.isCompleted && task.completedAt != null) {
            Text(text = "Completed at: ${formatDate(task.completedAt)}")
        }

        if (task.steps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Steps:", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            task.steps.forEach { step ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = step.isCompleted,
                        onCheckedChange = { isChecked ->
                            onToggleStep(step.id, isChecked)
                        }
                    )
                    Text(
                        text = step.title,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Close")
        }
    }
}

@Composable
fun EditTaskBottomSheetContent(
    task: Task,
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var category by remember { mutableStateOf(task.category) }
    var steps by remember { mutableStateOf(task.steps) }
    var currentStepTitle by remember { mutableStateOf("") }
    var isDaily by remember { mutableStateOf(task.isDaily) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var selectedDeadline by remember { mutableStateOf(task.deadline) }

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
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Edit Task", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))
        
        // Daily task toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Daily Task", fontWeight = FontWeight.Medium)
                Text(
                    "Repeats every day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isDaily,
                onCheckedChange = { isDaily = it }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Steps:")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentStepTitle,
                onValueChange = { currentStepTitle = it },
                label = { Text("Add Step") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (currentStepTitle.isNotBlank()) {
                    steps = steps + TaskStep(title = currentStepTitle)
                    currentStepTitle = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Step")
            }
        }

        steps.forEach { step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "• ${step.title}" + if (step.isCompleted) " (Done)" else "",
                    modifier = Modifier.weight(1f),
                    color = if (step.isCompleted) MaterialTheme.colorScheme.primary else Color.Unspecified
                )
                IconButton(onClick = {
                    val newSteps = steps.toMutableList()
                    newSteps.remove(step)
                    steps = newSteps
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove Step", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (title.isNotBlank()) {
                    onSave(task.copy(
                        title = title, 
                        deadline = selectedDeadline, 
                        category = category.ifBlank { "General" }, 
                        steps = steps,
                        isDaily = isDaily
                    ))
                }
            }) {
                Text("Save")
            }
        }
    }
}


@Composable
fun AddTaskBottomSheetContent(
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onTaskAdded: (String, Long, String, List<TaskStep>, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf(listOf<TaskStep>()) }
    var currentStepTitle by remember { mutableStateOf("") }
    var isDaily by remember { mutableStateOf(false) }

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
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("New Task", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))
        
        // Daily task toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Daily Task", fontWeight = FontWeight.Medium)
                Text(
                    "Repeats every day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isDaily,
                onCheckedChange = { isDaily = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Steps:")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentStepTitle,
                onValueChange = { currentStepTitle = it },
                label = { Text("Add Step") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (currentStepTitle.isNotBlank()) {
                    steps = steps + TaskStep(title = currentStepTitle)
                    currentStepTitle = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Step")
            }
        }

        steps.forEach { step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "• ${step.title}", modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    val newSteps = steps.toMutableList()
                    newSteps.remove(step)
                    steps = newSteps
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove Step", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (title.isNotBlank()) {
                    onTaskAdded(title, selectedDeadline, category.ifBlank { "General" }, steps, isDaily)
                }
            }) {
                Text("Create")
            }
        }
    }
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