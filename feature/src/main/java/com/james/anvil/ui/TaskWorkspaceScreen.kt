package com.james.anvil.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.james.anvil.formatDate
import com.james.anvil.data.BonusTask
import com.james.anvil.ui.components.BonusTaskBottomSheet
import com.james.anvil.ui.components.EmptyState
import com.james.anvil.ui.components.PageHeader
import com.james.anvil.ui.components.SearchField
import com.james.anvil.ui.components.SectionCard
import com.james.anvil.ui.components.TopLevelPageScaffold
import com.james.anvil.ui.components.TaskItem
import com.james.anvil.ui.theme.DesignTokens
import com.james.anvil.ui.theme.ForgedGold
import com.james.anvil.ui.theme.LocalWindowInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskWorkspaceScreen(
    viewModel: TaskViewModel,
    snackbarHostState: SnackbarHostState,
    initialTab: Int = 0
) {
    var selectedTabIndex by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    val tabs = listOf("Standard", "Bonus")
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var showAddBonusSheet by remember { mutableStateOf(false) }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showInfoDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    var editingTaskId by remember { mutableStateOf<Long?>(null) }
    var editingBonusTask by remember { mutableStateOf<BonusTask?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val allTasks by viewModel.tasks.collectAsState(initial = emptyList())
    val completedTasks by viewModel.completedTasks.collectAsState(initial = emptyList())
    val bonusTasks by viewModel.bonusTasks.collectAsState(initial = emptyList())
    val windowInfo = LocalWindowInfo.current

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
    val tasks = remember(allTasks, selectedCategory, searchQuery) {
        allTasks.filter { task ->
            val categoryMatches = selectedCategory == null || task.category == selectedCategory
            val searchMatches = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.category.contains(searchQuery, ignoreCase = true) ||
                task.notes.contains(searchQuery, ignoreCase = true)
            categoryMatches && searchMatches
        }
    }
    val filteredCompletedTasks = remember(completedTasks, selectedCategory, searchQuery) {
        completedTasks.filter { task ->
            val categoryMatches = selectedCategory == null || task.category == selectedCategory
            val searchMatches = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.category.contains(searchQuery, ignoreCase = true)
            categoryMatches && searchMatches
        }
    }
    val filteredBonusTasks = remember(bonusTasks, searchQuery) {
        bonusTasks.filter { task ->
            searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.description.orEmpty().contains(searchQuery, ignoreCase = true) ||
                task.category.orEmpty().contains(searchQuery, ignoreCase = true)
        }
    }

    val standardListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val bonusListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val activeListState = if (selectedTabIndex == 0) standardListState else bonusListState
    val isScrolled by remember {
        androidx.compose.runtime.derivedStateOf {
            activeListState.firstVisibleItemIndex > 0 || activeListState.firstVisibleItemScrollOffset > 40
        }
    }

    TopLevelPageScaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) showAddTaskSheet = true else showAddBonusSheet = true
                },
                containerColor = if (selectedTabIndex == 0) MaterialTheme.colorScheme.primary else ForgedGold,
                contentColor = if (selectedTabIndex == 0) MaterialTheme.colorScheme.onPrimary else Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .then(
                        if (windowInfo.maxContentWidth != androidx.compose.ui.unit.Dp.Unspecified) {
                            Modifier.widthIn(max = windowInfo.maxContentWidth)
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = windowInfo.contentPadding)
            ) {
                PageHeader(
                    eyebrow = "Execution",
                    title = "Tasks",
                    subtitle = if (isScrolled) "" else "Keep the next action obvious and the interaction path short."
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = !isScrolled,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

                        SectionCard(accentColor = MaterialTheme.colorScheme.primary) {
                            Text(
                                text = "${tasks.size} active tasks in focus",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${filteredCompletedTasks.size} completed, ${bonusTasks.size} bonus logged",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isScrolled) DesignTokens.SpacingSm else DesignTokens.SpacingLg))

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
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

                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                SearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = if (selectedTabIndex == 0) "Search tasks, categories, or notes" else "Search bonus tasks",
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else {
                        null
                    }
                )

                if (selectedTabIndex == 0) {
                    CategoryFilterRow(
                        categories = listOf("All") + existingCategories,
                        selectedCategory = selectedCategory ?: "All",
                        onCategorySelected = { selectedCategory = if (it == "All") null else it }
                    )
                }

                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                if (selectedTabIndex == 0) {
                    LazyColumn(
                        state = standardListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (tasks.isEmpty()) {
                            item {
                                EmptyState(
                                    message = "No active tasks to work through.",
                                    subtitle = "Add a task or clear your filters to bring the next action back into focus.",
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
                                    modifier = Modifier.combinedClickable(
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

                        if (filteredCompletedTasks.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Completed recently",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            items(filteredCompletedTasks, key = { "completed_${it.id}" }) { task ->
                                CompletedTaskRow(
                                    task = task,
                                    onOpen = {
                                        selectedTaskId = task.id
                                        showInfoDialog = true
                                    },
                                    onUncheck = {
                                        viewModel.updateTask(task.copy(isCompleted = false, completedAt = null))
                                    },
                                    onDelete = { viewModel.deleteTask(task) }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = bonusListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            SectionCard(accentColor = ForgedGold) {
                                Text(
                                    text = "Bonus work builds grace",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Track the extra things you finish outside the core queue so they stay visible and rewarding.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (filteredBonusTasks.isEmpty()) {
                            item {
                                EmptyState(
                                    message = "No bonus tasks recorded yet.",
                                    subtitle = "Log extra wins here so they can contribute to grace and momentum.",
                                    icon = Icons.Default.Star
                                )
                            }
                        } else {
                            items(filteredBonusTasks, key = { it.id }) { task ->
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

@Composable
private fun CompletedTaskRow(
    task: com.james.anvil.data.Task,
    onOpen: () -> Unit,
    onUncheck: () -> Unit,
    onDelete: () -> Unit
) {
    SectionCard(
        onClick = onOpen,
        accentColor = MaterialTheme.colorScheme.secondary
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${task.category} • Completed ${formatDate(task.completedAt ?: System.currentTimeMillis())}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.TextButton(onClick = onUncheck) {
                Text("Uncheck")
            }
            androidx.compose.material3.TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
