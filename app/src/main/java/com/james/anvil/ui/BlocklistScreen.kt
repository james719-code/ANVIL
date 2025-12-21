package com.james.anvil.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.james.anvil.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocklistScreen(viewModel: TaskViewModel) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Apps", "Links")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Blocklist") })
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
    val installedApps by viewModel.installedApps.collectAsState(initial = emptyList())
    val blockedPackages by viewModel.blockedApps.collectAsState(initial = emptyList())

    if (installedApps.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             CircularProgressIndicator()
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(installedApps) { app ->
                val isBlocked = blockedPackages.contains(app.packageName)
                ListItem(
                    headlineContent = { Text(app.name) },
                    supportingContent = { Text(app.packageName) },
                    leadingContent = {
                        Image(
                            painter = rememberAsyncImagePainter(app.icon),
                            contentDescription = app.name,
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isBlocked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    viewModel.blockApp(app.packageName)
                                } else {
                                    viewModel.unblockApp(app.packageName)
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun BlockedLinksTab(viewModel: TaskViewModel) {
    val blockedLinks by viewModel.blockedLinks.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (blockedLinks.isEmpty()) {
            EmptyState(
                message = "No blocked links.",
                icon = Icons.Default.Lock,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(blockedLinks) { link ->
                    ListItem(
                        headlineContent = { Text(link) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.unblockLink(link) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    )
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
            onAdd = { link ->
                viewModel.blockLink(link)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddLinkDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var link by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Block Link") },
        text = {
            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("URL or Domain") },
                placeholder = { Text("example.com") }
            )
        },
        confirmButton = {
            Button(onClick = { 
                if (link.isNotBlank()) {
                    onAdd(link) 
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
