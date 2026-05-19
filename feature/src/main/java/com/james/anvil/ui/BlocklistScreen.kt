package com.james.anvil.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.james.anvil.ui.components.BlocklistItem
import com.james.anvil.ui.components.EmptyState
import com.james.anvil.ui.components.PageHeader
import com.james.anvil.ui.components.SearchField
import com.james.anvil.ui.components.SectionCard
import com.james.anvil.ui.components.StatusPill
import com.james.anvil.ui.components.TopLevelPageScaffold
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.ui.theme.DesignTokens
import com.james.anvil.ui.theme.ForgedGold
import com.james.anvil.ui.theme.LocalWindowInfo
import com.james.anvil.ui.theme.SuccessGreen
import com.james.anvil.ui.theme.WarningOrange
import com.james.anvil.ui.viewmodel.CombatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocklistScreen(
    viewModel: BlocklistViewModel = hiltViewModel(),
    combatViewModel: CombatViewModel = hiltViewModel(),
    onNavigateToMonsterCombat: (Long) -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Apps", "Links")

    val windowInfo = LocalWindowInfo.current

    TopLevelPageScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
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
                eyebrow = "Shield",
                title = "Blocklist",
                subtitle = "Control distractions, schedules, and unlock challenges from one place."
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

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

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> BlockedAppsTab(
                        viewModel = viewModel,
                        combatViewModel = combatViewModel,
                        onNavigateToMonsterCombat = onNavigateToMonsterCombat
                    )
                    1 -> BlockedLinksTab(viewModel)
                }
            }
        }
    }
}

@Composable
fun BlockedAppsTab(
    viewModel: BlocklistViewModel,
    combatViewModel: CombatViewModel = hiltViewModel(),
    onNavigateToMonsterCombat: (Long) -> Unit = {}
) {
    val appListWithCategories by viewModel.appListWithCategories.collectAsState(initial = emptyList())
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedAppForCategory by remember { mutableStateOf<AppInfoWithCategory?>(null) }
    var selectedAppForSchedule by remember { mutableStateOf<AppInfoWithCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredApps = remember(appListWithCategories, searchQuery) {
        if (searchQuery.isBlank()) appListWithCategories
        else appListWithCategories.filter { 
            it.appInfo.name.contains(searchQuery, ignoreCase = true) 
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search apps",
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search apps") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = DesignTokens.SpacingMd)
        )

        if (appListWithCategories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
            ) {
                item {
                    SectionCard(accentColor = SuccessGreen) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${filteredApps.count { it.blockedApp != null }} blocked apps",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${filteredApps.size} apps available for rules",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(
                                text = "Combat unlock",
                                tint = ForgedGold
                            )
                        }
                    }
                }
                items(filteredApps, key = { it.appInfo.packageName }) { app ->
                    BlocklistItem(
                        app = app,
                        onToggleBlock = { isBlocked ->
                            if (isBlocked) {
                                viewModel.blockApp(app.appInfo.packageName)
                            } else {
                                // Spawn a monster instead of direct unblock
                                combatViewModel.spawnMonsterForApp(
                                    packageName = app.appInfo.packageName,
                                    appLabel = app.appInfo.name
                                ) { monsterId ->
                                    onNavigateToMonsterCombat(monsterId)
                                }
                            }
                        },
                        onCategoryClick = {
                            selectedAppForCategory = app
                            showCategoryDialog = true
                        },
                        onScheduleClick = {
                            selectedAppForSchedule = app
                            showScheduleDialog = true
                        }
                    )
                }
            }
        }
    }

    // Category Edit Dialog
    if (showCategoryDialog && selectedAppForCategory != null) {
        CategoryEditDialog(
            app = selectedAppForCategory!!,
            onDismiss = { showCategoryDialog = false },
            onSave = { newCategory ->
                viewModel.setAppCategory(selectedAppForCategory!!.appInfo.packageName, newCategory)
                showCategoryDialog = false
            }
        )
    }

    // Schedule Edit Dialog
    if (showScheduleDialog && selectedAppForSchedule != null) {
        val blockedApp = selectedAppForSchedule!!.blockedApp
        if (blockedApp != null) {
            com.james.anvil.ui.components.ScheduleEditDialog(
                title = "Schedule for ${selectedAppForSchedule!!.appInfo.name}",
                currentScheduleType = blockedApp.scheduleType,
                currentDayMask = blockedApp.dayMask,
                currentStartMinutes = blockedApp.startTimeMinutes,
                currentEndMinutes = blockedApp.endTimeMinutes,
                currentStartDayOfWeek = blockedApp.startDayOfWeek,
                currentEndDayOfWeek = blockedApp.endDayOfWeek,
                onDismiss = { showScheduleDialog = false },
                onSave = { scheduleType, dayMask, startMinutes, endMinutes, startDayOfWeek, endDayOfWeek ->
                    viewModel.updateAppSchedule(
                        packageName = blockedApp.packageName,
                        scheduleType = scheduleType,
                        dayMask = dayMask,
                        startTimeMinutes = startMinutes,
                        endTimeMinutes = endMinutes,
                        startDayOfWeek = startDayOfWeek,
                        endDayOfWeek = endDayOfWeek
                    )
                    showScheduleDialog = false
                }
            )
        }
    }
}

@Composable
fun CategoryEditDialog(
    app: AppInfoWithCategory,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var category by remember { mutableStateOf(app.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Category") },
        text = {
            Column {
                Text("Category for ${app.appInfo.name}")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(category) }) {
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

@Composable
fun BlockedLinksTab(viewModel: BlocklistViewModel) {
    val blockedLinks by viewModel.blockedLinkObjects.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedLinkForSchedule by remember { mutableStateOf<com.james.anvil.data.BlockedLink?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (blockedLinks.isEmpty()) {
            EmptyState(
                message = "No blocked links.",
                icon = Icons.Default.Lock,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = DesignTokens.SpacingMd, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SectionCard(accentColor = WarningOrange) {
                        Text(
                            text = "${blockedLinks.size} blocked links",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap a link to tune its schedule or remove rules from the action button.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(blockedLinks) { link ->
                    SectionCard(
                        onClick = {
                            selectedLinkForSchedule = link
                            showScheduleDialog = true
                        },
                        accentColor = if (link.isEncrypted) ForgedGold else MaterialTheme.colorScheme.primary
                    ) {
                        val displayText = if (link.isEncrypted) "****** (Hidden)" else link.pattern
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = link.getScheduleDescription(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.unblockLink(link.pattern) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }
        }
        
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Link")
        }
    }

    if (showAddDialog) {
        AddLinkDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { link, isEncrypted ->
                viewModel.blockLink(link, isEncrypted)
                showAddDialog = false
            }
        )
    }

    // Schedule Edit Dialog for Links
    if (showScheduleDialog && selectedLinkForSchedule != null) {
        val link = selectedLinkForSchedule!!
        com.james.anvil.ui.components.ScheduleEditDialog(
            title = "Schedule for ${if (link.isEncrypted) "Hidden Link" else link.pattern}",
            currentScheduleType = link.scheduleType,
            currentDayMask = link.dayMask,
            currentStartMinutes = link.startTimeMinutes,
            currentEndMinutes = link.endTimeMinutes,
            currentStartDayOfWeek = link.startDayOfWeek,
            currentEndDayOfWeek = link.endDayOfWeek,
            onDismiss = { showScheduleDialog = false },
            onSave = { scheduleType, dayMask, startMinutes, endMinutes, startDayOfWeek, endDayOfWeek ->
                viewModel.updateLinkSchedule(
                    pattern = link.pattern,
                    scheduleType = scheduleType,
                    dayMask = dayMask,
                    startTimeMinutes = startMinutes,
                    endTimeMinutes = endMinutes,
                    startDayOfWeek = startDayOfWeek,
                    endDayOfWeek = endDayOfWeek
                )
                showScheduleDialog = false
            }
        )
    }
}

@Composable
fun AddLinkDialog(onDismiss: () -> Unit, onAdd: (String, Boolean) -> Unit) {
    var link by remember { mutableStateOf("") }
    var isEncrypted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Block Link") },
        text = {
            Column {
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("URL or Domain") },
                    placeholder = { Text("example.com") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isEncrypted,
                        onCheckedChange = { isEncrypted = it }
                    )
                    Text("Encrypt (Hide in list)")
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (link.isNotBlank()) {
                    onAdd(link, isEncrypted) 
                }
            }) {
                Text("Block")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// =============================================
// Preview Functions (Removed in Release Builds)
// =============================================

@Preview(name = "Add Link Dialog", showBackground = true)
@Composable
private fun AddLinkDialogPreview() {
    com.james.anvil.ui.theme.ANVILTheme {
        AddLinkDialog(onDismiss = {}, onAdd = { _, _ -> })
    }
}

@Preview(name = "Blocked Links Empty - Light", showBackground = true)
@Composable
private fun BlockedLinksEmptyPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = false) {
        EmptyState(
            message = "No blocked links.",
            icon = Icons.Default.Lock
        )
    }
}

@Preview(name = "Blocked Links Empty - Dark", showBackground = true)
@Composable
private fun BlockedLinksEmptyDarkPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = true) {
        EmptyState(
            message = "No blocked links.",
            icon = Icons.Default.Lock
        )
    }
}
