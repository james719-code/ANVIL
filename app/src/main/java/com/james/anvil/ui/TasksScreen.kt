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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.james.anvil.data.BonusTask
import com.james.anvil.ui.components.BonusTaskBottomSheet
import com.james.anvil.ui.components.ScreenScaffold
import com.james.anvil.ui.components.ScreenHeader
import com.james.anvil.ui.components.EmptyState
import com.james.anvil.ui.components.TaskItem
import com.james.anvil.ui.components.AnvilCard
import com.james.anvil.ui.theme.ForgedGold
import com.james.anvil.ui.theme.IndustrialGreyLight
import com.james.anvil.ui.theme.IndustrialBorder


import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    viewModel: TaskViewModel,
    snackbarHostState: SnackbarHostState,
    navController: NavController? = null
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Standard", "Bonus")

    var showAddTaskSheet by remember { mutableStateOf(false) }
    var showAddBonusSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)



    var showInfoDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    var editingTaskId by remember { mutableStateOf<Long?>(null) }
    var editingBonusTask by remember { mutableStateOf<BonusTask?>(null) }


    val scope = rememberCoroutineScope()


    val allTasks by viewModel.tasks.collectAsState(initial = emptyList())
    val completedTasks by viewModel.completedTasks.collectAsState(initial = emptyList())
    val bonusTasks by viewModel.bonusTasks.collectAsState(initial = emptyList())

    val selectedTask = remember(allTasks, selectedTaskId) {
        allTasks.find { it.id == selectedTaskId }
    }

    val editingTask = remember(allTasks, editingTaskId) {
        allTasks.find { it.id == editingTaskId }
    }

    val existingCategories = remember(allTasks, completedTasks) {
        (allTasks.map { it.category } + completedTasks.map { it.category })
            .distinct()
            .filter { it != "General" }
            .sorted()
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val tasks = remember(allTasks, selectedCategory) {
        if (selectedCategory == null) allTasks
        else allTasks.filter { it.category == selectedCategory }
    }

    val filteredCompletedTasks = remember(completedTasks, selectedCategory) {
        if (selectedCategory == null) completedTasks
        else completedTasks.filter { it.category == selectedCategory }
    }

    ScreenScaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) showAddTaskSheet = true
                    else showAddBonusSheet = true
                },
                containerColor = if (selectedTabIndex == 0) MaterialTheme.colorScheme.primary else ForgedGold,
                contentColor = if (selectedTabIndex == 0) MaterialTheme.colorScheme.onPrimary else Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            ScreenHeader(
                title = "Tasks",
                subtitle = "Manage your work"
            )
            
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            if (selectedTabIndex == 0) {
                // Standard Tasks Content
                CategoryFilterRow(
                    categories = listOf("All") + existingCategories,
                    selectedCategory = selectedCategory ?: "All",
                    onCategorySelected = { selectedCategory = if (it == "All") null else it }
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pending Tasks
                    if (tasks.isEmpty()) {
                        item {
                            EmptyState(
                                message = "No pending tasks. You are free.",
                                icon = Icons.Default.Check,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                            )
                        }
                    } else {
                        items(tasks, key = { it.id }) { task ->
                            TaskItem(
                                task = task,
                                onComplete = { viewModel.completeTask(task) },
                                onEdit = { editingTaskId = task.id },
                                onDelete = {
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
                                },
                                modifier = Modifier
                                    .animateItem()
                                    .combinedClickable(
                                        onClick = {
                                            selectedTaskId = task.id
                                            showInfoDialog = true
                                        },
                                        onLongClick = {
                                            selectedTaskId = task.id
                                            showOptionsDialog = true
                                        }
                                    )
                            )
                        }
                    }

                    // Completed Tasks Section
                    if (filteredCompletedTasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }

                        items(filteredCompletedTasks, key = { "completed_${it.id}" }) { task ->
                            var showMenu by remember { mutableStateOf(false) }
                            AnvilCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = {
                                        selectedTaskId = task.id
                                        showInfoDialog = true
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Completion checkbox (allows un-completing)
                                        IconButton(
                                            onClick = { viewModel.updateTask(task.copy(isCompleted = false, completedAt = null)) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Uncomplete task",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.title,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Text(
                                                    text = task.category,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                )
                                                Text(
                                                    text = " • ",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                                )
                                                Text(
                                                    text = "Completed ${formatDate(task.completedAt ?: System.currentTimeMillis())}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
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
                                                    text = { Text("Uncheck") },
                                                    onClick = {
                                                        showMenu = false
                                                        viewModel.updateTask(task.copy(isCompleted = false, completedAt = null))
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                                    onClick = {
                                                        showMenu = false
                                                        viewModel.deleteTask(task)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
             else {
                // Bonus Tasks Content
                if (bonusTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            message = "No bonus tasks recorded yet.\nDo something extra today!",
                            icon = Icons.Default.Star
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(bonusTasks, key = { it.id }) { task ->
                            BonusTaskItem(
                                task = task,
                                onEdit = { editingBonusTask = task },
                                onDelete = {
                                    viewModel.deleteBonusTask(task)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Bonus task removed",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.addBonusTask(task.title, task.description, task.category)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddBonusSheet) {
        BonusTaskBottomSheet(
            onDismiss = { showAddBonusSheet = false },
            onSave = { title, description ->
                viewModel.addBonusTask(title, description)
                showAddBonusSheet = false
            }
        )
    }

    if (editingBonusTask != null) {
        BonusTaskBottomSheet(
            task = editingBonusTask,
            onDismiss = { editingBonusTask = null },
            onSave = { title, description ->
                viewModel.updateBonusTask(editingBonusTask!!.copy(title = title, description = description))
                editingBonusTask = null
            }
        )
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
                onTaskAdded = { title, deadline, category, steps, isDaily, hardnessLevel, notes ->
                    viewModel.addTask(title, deadline, category, steps, isDaily, hardnessLevel, notes)
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
                    { Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(18.dp)) }
                } else null
            )
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
        if (!task.isDaily) {
            Text(text = "Deadline: ${formatDate(task.deadline)}")
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(text = "Status: ${if (task.isCompleted) "Completed" else "Pending"}")

        if (task.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Notes:", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
    var notes by remember { mutableStateOf(task.notes) }
    var steps by remember { mutableStateOf(task.steps) }
    var currentStepTitle by remember { mutableStateOf("") }
    var isDaily by remember { mutableStateOf(task.isDaily) }
    var hardnessLevel by remember { mutableFloatStateOf(task.hardnessLevel.toFloat()) }

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

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        Spacer(modifier = Modifier.height(16.dp))

        CategorySelectionInput(
            category = category,
            onCategoryChange = { category = it },
            existingCategories = existingCategories
        )

        if (!isDaily) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Deadline:")
            Button(onClick = { datePickerDialog.show() }) {
                Text(text = formatDate(selectedDeadline))
            }
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
        
        if (!isDaily) {
            Spacer(modifier = Modifier.height(16.dp))
            // Hardness Level Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hardness Level", fontWeight = FontWeight.Medium)
                        Text(
                            when (hardnessLevel.toInt()) {
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
                    Text(
                        text = "${hardnessLevel.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = hardnessLevel,
                    onValueChange = { hardnessLevel = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
                        isDaily = isDaily,
                        hardnessLevel = hardnessLevel.toInt(),
                        notes = notes
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
    onTaskAdded: (String, Long, String, List<TaskStep>, Boolean, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf(listOf<TaskStep>()) }
    var currentStepTitle by remember { mutableStateOf("") }
    var isDaily by remember { mutableStateOf(false) }
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

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        Spacer(modifier = Modifier.height(16.dp))

        CategorySelectionInput(
            category = category,
            onCategoryChange = { category = it },
            existingCategories = existingCategories
        )

        if (!isDaily) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Deadline:")
            Button(onClick = { datePickerDialog.show() }) {
                Text(text = formatDate(selectedDeadline))
            }
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

        if (!isDaily) {
            // Hardness Level Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hardness Level", fontWeight = FontWeight.Medium)
                        Text(
                            when (hardnessLevel.toInt()) {
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
                    Text(
                        text = "${hardnessLevel.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = hardnessLevel,
                    onValueChange = { hardnessLevel = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
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
                    onTaskAdded(title, selectedDeadline, category.ifBlank { "General" }, steps, isDaily, hardnessLevel.toInt(), notes)
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
                    Icon(Icons.Default.Edit, contentDescription = "Edit task", modifier = Modifier.padding(end = 8.dp))
                    Text("Edit")
                }

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Start),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete task", modifier = Modifier.padding(end = 8.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun BonusTaskItem(
    task: BonusTask,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AnvilCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!task.description.isNullOrBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Bonus task",
                        tint = ForgedGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RECOGNIZED: ${formatDate(task.completedAt).uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ForgedGold
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}