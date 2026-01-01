package com.james.anvil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.james.anvil.ui.TaskViewModel
import com.james.anvil.ui.navigation.NavigationGraph
import com.james.anvil.ui.navigation.Screen
import com.james.anvil.ui.theme.ANVILTheme
import java.text.SimpleDateFormat
import java.util.*
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.james.anvil.worker.DailyTaskResetWorker
import com.james.anvil.worker.ReminderWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleWorkers()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            ANVILTheme(darkTheme = isDarkTheme) {
                MainScreen(viewModel)
                BatteryOptimizationCheck()
                UsageAccessCheck()
                NotificationPermissionCheck()
            }
        }
    }

    private fun scheduleWorkers() {
        val workManager = WorkManager.getInstance(this)
        
        // Schedule ReminderWorker to run every 15 minutes
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "AnvilReminderCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
        
        // Schedule DailyTaskResetWorker to run every 12 hours
        val dailyResetRequest = PeriodicWorkRequestBuilder<DailyTaskResetWorker>(12, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "AnvilDailyReset",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyResetRequest
        )
    }
}

@Composable
fun NotificationPermissionCheck() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                showDialog = true
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Enable Notifications") },
            text = { Text("Anvil needs notification permission to remind you about upcoming task deadlines.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                        showDialog = false
                    }
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Later")
                }
            }
        )
    }
}

@Composable
fun UsageAccessCheck() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        if (mode != AppOpsManager.MODE_ALLOWED) {
            showDialog = true
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Permit Usage Access") },
            text = { Text("Anvil needs usage access permission to track app usage and improve blocking accuracy.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                        showDialog = false
                    }
                ) {
                    Text("Fix")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BatteryOptimizationCheck() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var showRestrictedGuide by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            showDialog = true
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Disable Battery Optimization") },
            text = { Text("To ensure Anvil works correctly (especially Accessibility Services after restart), please tap 'Battery' and select 'Unrestricted' (or 'Don't optimize').") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                        showDialog = false
                    }
                ) {
                    Text("Fix")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
            
            icon = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    TextButton(onClick = { showRestrictedGuide = true }) {
                        Text("Can't enable Accessibility?")
                    }
                }
            }
        )
    }

    if (showRestrictedGuide) {
        RestrictedSettingsGuideDialog(onDismiss = { showRestrictedGuide = false })
    }
}

@Composable
fun RestrictedSettingsGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().height(600.dp) 
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Restricted Settings Guide",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Starting with Android 13, Google introduced 'Restricted Settings' for sideloaded apps. If you cannot enable Accessibility for Anvil, follow these steps:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                GuideStep(1, "Trigger the error", "Try to enable the Accessibility setting for Anvil. You’ll get a 'Restricted Setting' popup. Tap OK. (This is required).")
                GuideStep(2, "Go to App Info", "Open Settings > Apps > Anvil.")
                GuideStep(3, "Unlock the setting", "Tap the three dots (⋮) in the top-right corner. Tap 'Allow restricted settings'. Confirm with your PIN/Password.")
                GuideStep(4, "Enable Accessibility", "Go back to Settings > Accessibility > Anvil. You can now enable it.")
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.End)
                ) {
                    Text("Got it")
                }
            }
        }
    }
}

@Composable
fun GuideStep(number: Int, title: String, description: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = "$number. $title",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun MainScreen(viewModel: TaskViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val screens = listOf(
        Screen.Dashboard,
        Screen.Tasks,
        Screen.Blocklist,
        Screen.Settings
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Surface(modifier = Modifier.padding(innerPadding)) {
                NavigationGraph(
                    navController = navController, 
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState
                )
            }
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
        )
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
