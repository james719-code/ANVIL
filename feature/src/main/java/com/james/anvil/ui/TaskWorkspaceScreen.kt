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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.james.anvil.formatDate
import com.james.anvil.data.BonusTask
import com.james.anvil.ui.components.BonusTaskBottomSheet
import com.james.anvil.ui.components.EmptyState
import com.james.anvil.ui.components.AnvilSegmentedTabs
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
    var selectedTabIndex by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    val tabs = listOf("Planned", "Bonus wins")
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var showAddBonusSheet by remember { mutableStateOf(false) }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showInfoDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    var editingTaskId by remember { mutableStateOf<Long?>(null) }
    var editingBonusTask by remember { mutableStateOf<BonusTask?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val allTasks by viewModel.tasks.collectAsState(initial = emptyList())
    val completedTasks by viewModel.completedTasks.collectAsState(initial = emptyList())
    val bonusTasks by viewModel.bonusTasks.collectAsState(initial = emptyList())
    val windowInfo = LocalWindowInfo.current

    val selectedTask = remember(allTasks, completedTasks, selectedTaskId) {
        (allTasks + completedTasks).find { it.id == selectedTaskId }
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

    // While the empty state shows, the button shrinks to an icon so it doesn't cover
    // the empty-state text. Once there are items (or a search/filter), the label returns.
    val isListEmpty = if (selectedTabIndex == 0) tasks.isEmpty() else filteredBonusTasks.isEmpty()
    val isFiltering = searchQuery.isNotBlank() || (selectedTabIndex == 0 && selectedCategory != null)
    val addLabel = if (selectedTabIndex == 0) "Add task" else "Log bonus"
    val onAddClick: () -> Unit = {
        if (selectedTabIndex == 0) showAddTaskSheet = true else showAddBonusSheet = true
    }

    TopLevelPageScaffold(
        floatingActionButton = {
            if (isListEmpty && !isFiltering) {
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = addLabel)
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(addLabel) }
                )
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
                    title = "Tasks",
                    subtitle = if (selectedTabIndex == 0) {
                        "${allTasks.size} active · ${completedTasks.size} done this week"
                    } else {
                        "${bonusTasks.size} bonus wins logged"
                    }
                )

                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                AnvilSegmentedTabs(
                    options = tabs,
                    selectedIndex = selectedTabIndex,
                    onSelect = { selectedTabIndex = it }
                )

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
                        contentPadding = PaddingValues(bottom = 104.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (tasks.isEmpty()) {
                            item {
                                EmptyState(
                                    message = if (searchQuery.isNotBlank() || selectedCategory != null) "No matching tasks" else "Your next step starts here",
                                    subtitle = if (searchQuery.isNotBlank() || selectedCategory != null) "Try another search or clear your filters." else "Add a task and break it\ninto manageable steps.",
                                    actionLabel = if (searchQuery.isNotBlank() || selectedCategory != null) "Clear filters" else "Add task",
                                    onAction = {
                                        if (searchQuery.isNotBlank() || selectedCategory != null) {
                                            searchQuery = ""
                                            selectedCategory = null
                                        } else showAddTaskSheet = true
                                    },
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
                        contentPadding = PaddingValues(bottom = 104.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            SectionCard(accentColor = ForgedGold) {
                                Text(
                                    text = "Bonus work builds grace",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Log extra things you finish outside your planned tasks. Enough of them earn a grace day.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (filteredBonusTasks.isEmpty()) {
                            item {
                                EmptyState(
                                    message = if (searchQuery.isNotBlank()) "No matching bonus tasks" else "Give your extra effort credit",
                                    subtitle = if (searchQuery.isNotBlank()) "Try a different search." else "Log something you finished outside your planned tasks.",
                                    actionLabel = if (searchQuery.isNotBlank()) "Clear search" else "Log bonus",
                                    onAction = {
                                        if (searchQuery.isNotBlank()) searchQuery = "" else showAddBonusSheet = true
                                    },
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
