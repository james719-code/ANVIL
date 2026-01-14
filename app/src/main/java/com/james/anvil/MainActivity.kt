package com.james.anvil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import kotlin.math.roundToInt


import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.james.anvil.ui.TaskViewModel
import com.james.anvil.ui.navigation.NavItem
import com.james.anvil.ui.DashboardScreen
import com.james.anvil.ui.TasksScreen
import com.james.anvil.ui.BudgetScreen
import com.james.anvil.ui.BlocklistScreen
import com.james.anvil.ui.SettingsScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.james.anvil.ui.theme.ANVILTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.SharedPreferences
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
import androidx.compose.ui.unit.sp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.james.anvil.worker.DailyTaskResetWorker
import com.james.anvil.worker.ReminderWorker
import com.james.anvil.worker.WidgetRefreshWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleWorkers()
        setupShortcuts()
        handleIntent(intent)
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            ANVILTheme(darkTheme = isDarkTheme) {
                var showSettings by remember { mutableStateOf(false) }
                
                // Handle back button when settings is open
                BackHandler(enabled = showSettings) {
                    showSettings = false
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main App Content - Stays alive in the background
                        MainScreen(
                            viewModel = viewModel, 
                            onNavigateToSettings = { showSettings = true }
                        )
                        
                        // Settings Overlay - Slides over the main content
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showSettings,
                            enter = slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(400)
                            ),
                            exit = slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(400)
                            )
                        ) {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { showSettings = false }
                            )

                        }
                    }
                }



                BatteryOptimizationCheck()
                UsageAccessCheck()
                NotificationPermissionCheck()
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // Handle other intents if any
    }

    private fun setupShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(ShortcutManager::class.java)

            val expenseShortcut = ShortcutInfo.Builder(this, "add_expense")
                .setShortLabel("Add Expense")
                .setLongLabel("Record a new expense")
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(Intent(this, QuickAddBudgetEntryActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("type", "EXPENSE")
                })
                .build()

            val incomeShortcut = ShortcutInfo.Builder(this, "add_income")
                .setShortLabel("Add Income")
                .setLongLabel("Record a new income")
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(Intent(this, QuickAddBudgetEntryActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("type", "INCOME")
                })
                .build()

            val loanShortcut = ShortcutInfo.Builder(this, "add_loan")
                .setShortLabel("Add Loan")
                .setLongLabel("Record a new loan")
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(Intent(this, QuickAddLoanActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                })
                .build()

            shortcutManager.dynamicShortcuts = listOf(expenseShortcut, incomeShortcut, loanShortcut)
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
        
        // Schedule WidgetRefreshWorker to run every 30 minutes
        val widgetRefreshRequest = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WidgetRefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            widgetRefreshRequest
        )
    }
}

private const val PREFS_NAME = "anvil_dialog_prefs"
private const val KEY_DONT_SHOW_NOTIFICATION = "dont_show_notification_dialog"
private const val KEY_DONT_SHOW_USAGE = "dont_show_usage_dialog"
private const val KEY_DONT_SHOW_BATTERY = "dont_show_battery_dialog"

@Composable
fun NotificationPermissionCheck() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var showDialog by remember { mutableStateOf(false) }
    var dontShowAgain by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (prefs.getBoolean(KEY_DONT_SHOW_NOTIFICATION, false)) return@LaunchedEffect
        
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
            onDismissRequest = { 
                if (dontShowAgain) prefs.edit().putBoolean(KEY_DONT_SHOW_NOTIFICATION, true).apply()
                showDialog = false 
            },
            title = { Text("Enable Notifications") },
            text = { 
                Column {
                    Text("Anvil needs notification permission to remind you about upcoming task deadlines.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it }
                        )
                        Text(
                            text = "Don't show again",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable { dontShowAgain = !dontShowAgain }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (dontShowAgain) prefs.edit().putBoolean(KEY_DONT_SHOW_NOTIFICATION, true).apply()
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
                TextButton(onClick = { 
                    if (dontShowAgain) prefs.edit().putBoolean(KEY_DONT_SHOW_NOTIFICATION, true).apply()
                    showDialog = false 
                }) {
                    Text("Later")
                }
            }
        )
    }
}

@Composable
fun UsageAccessCheck() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var showDialog by remember { mutableStateOf(false) }
    var dontShowAgain by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (prefs.getBoolean(KEY_DONT_SHOW_USAGE, false)) return@LaunchedEffect
        
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
            onDismissRequest = { 
                if (dontShowAgain) prefs.edit().putBoolean(KEY_DONT_SHOW_USAGE, true).apply()
                showDialog = false 
            },
            title = { Text("Permit Usage Access") },
            text = { 
                Column {
                    Text("Anvil needs usage access permission to track app usage and improve blocking accuracy.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it }
                        )
                        Text(
                            text = "Don't show again",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable { dontShowAgain = !dontShowAgain }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (dontShowAgain) prefs.edit().putBoolean(KEY_DONT_SHOW_USAGE, true).apply()
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                        showDialog = false
                    }
                ) {
                    Text("Fix")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    if (dontShowAgain) prefs.edit().putBoolean(KEY_DONT_SHOW_USAGE, true).apply()
                    showDialog = false 
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BatteryOptimizationCheck() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var showDialog by remember { mutableStateOf(false) }
    var showRestrictedGuide by remember { mutableStateOf(false) }
    var dontShowAgain by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (prefs.getBoolean(KEY_DONT_SHOW_BATTERY, false)) return@LaunchedEffect
        
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            showDialog = true
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (dontShowAgain) prefs.edit().putBoolean(KEY_DONT_SHOW_BATTERY, true).apply()
                showDialog = false 
            },
            title = { Text("Disable Battery Optimization") },
            text = { 
                Column {
                    Text("To ensure Anvil works correctly (especially Accessibility Services after restart), please tap 'Battery' and select 'Unrestricted' (or 'Don't optimize').")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it }
                        )
                        Text(
                            text = "Don't show again",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable { dontShowAgain = !dontShowAgain }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (dontShowAgain) prefs.edit().putBoolean(KEY_DONT_SHOW_BATTERY, true).apply()
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
                TextButton(onClick = { 
                    if (dontShowAgain) prefs.edit().putBoolean(KEY_DONT_SHOW_BATTERY, true).apply()
                    showDialog = false 
                }) {
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
fun MainScreen(viewModel: TaskViewModel, onNavigateToSettings: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navItems = NavItem.bottomNavItems
    val pagerState = rememberPagerState(pageCount = { 4 }) // Home, Tasks, Budget, Blocklist
    val scope = rememberCoroutineScope()


    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                SlidingNavigationBar(
                    pagerState = pagerState,
                    navItems = navItems,
                    onItemClick = { index ->
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                beyondViewportPageCount = 3
            ) { page ->
                when (page) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToPage = { targetPage ->
                            // Map existing indices if needed, or handle special cases
                            if (targetPage == 5) {
                                onNavigateToSettings()
                            } else if (targetPage == 4) {
                                // Bonus tasks is now a tab in Tasks (index 1)
                                scope.launch { 
                                    pagerState.animateScrollToPage(1)
                                }
                            } else {
                                scope.launch { pagerState.animateScrollToPage(targetPage) }
                            }
                        }
                    )

                    1 -> TasksScreen(viewModel = viewModel, snackbarHostState = snackbarHostState)
                    2 -> BudgetScreen(viewModel = viewModel)
                    3 -> BlocklistScreen(viewModel = viewModel)
                }
            }
        }

        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        )
    }
}

@Composable
fun SlidingNavigationBar(
    pagerState: PagerState,
    navItems: List<NavItem<*>>,
    onItemClick: (Int) -> Unit
) {
    val density = LocalDensity.current
    var itemWidth by remember { mutableStateOf(0.dp) }
    var barWidth by remember { mutableStateOf(0.dp) }
    
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(28.dp))
                .onGloballyPositioned { coordinates ->
                    barWidth = with(density) { coordinates.size.width.toDp() }
                    itemWidth = barWidth / navItems.size
                },
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                // Sliding indicator
                val indicatorOffset = with(density) {
                    val currentPage = pagerState.currentPage
                    val offset = pagerState.currentPageOffsetFraction
                    ((currentPage + offset) * itemWidth.toPx()).roundToInt()
                }
                
                // Sleek Pill Highlight
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorOffset, 0) }
                        .width(itemWidth)
                        .height(80.dp)
                        .padding(horizontal = 10.dp, vertical = 14.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(24.dp)
                        )
                )
                
                // Navigation items
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    navItems.forEachIndexed { index, item ->
                        val isSelected = pagerState.currentPage == index
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null // Remove default ripple to prevent double highlight
                                ) { onItemClick(index) },
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            val tint by androidx.compose.animation.animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                animationSpec = tween(400)
                            )
                            
                            val scale by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (isSelected) 1.1f else 1f,
                                animationSpec = tween(400)
                            )

                            Column(
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.scale(scale)
                            ) {

                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    tint = tint,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = tint
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
