package com.james.anvil.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocklistScreen(viewModel: TaskViewModel) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Apps", "Links")

    Scaffold(
        topBar = {
            Column {
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
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTabIndex) {
                0 -> BlockedAppsTab(viewModel)
                1 -> BlockedLinksTab(viewModel)
            }
        }
    }
}

@Composable
fun BlockedAppsTab(viewModel: TaskViewModel) {
    val appListWithCategories by viewModel.appListWithCategories.collectAsState(initial = emptyList())
    var showCategoryDialog by remember { mutableStateOf(false) }
    var selectedAppForCategory by remember { mutableStateOf<AppInfoWithCategory?>(null) }

    if (appListWithCategories.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             CircularProgressIndicator()
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Text(
                    text = "Manage Apps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            items(appListWithCategories, key = { it.appInfo.packageName }) { app ->
                BlocklistItem(
                    app = app,
                    onToggleBlock = { isBlocked ->
                        if (isBlocked) viewModel.blockApp(app.appInfo.packageName)
                        else viewModel.unblockApp(app.appInfo.packageName)
                    },
                    onCategoryClick = {
                        selectedAppForCategory = app
                        showCategoryDialog = true
                    }
                )
            }
        }
    }

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
fun BlockedLinksTab(viewModel: TaskViewModel) {
    val blockedLinks by viewModel.blockedLinkObjects.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (blockedLinks.isEmpty()) {
            EmptyState(
                message = "No blocked links.",
                icon = Icons.Default.Lock,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                item {
                    Text(
                        text = "Blocked URLs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                items(blockedLinks) { link ->
                    Card(
                         modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        val displayText = if (link.isEncrypted) "****** (Hidden)" else link.pattern
                        ListItem(
                            headlineContent = { Text(displayText) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.unblockLink(link.pattern) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        )
                    }
                }
            }
        }
        
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
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
