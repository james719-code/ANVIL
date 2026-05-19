package com.james.anvil

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.rememberNavController
import com.james.anvil.ui.TaskViewModel
import com.james.anvil.ui.components.AnvilOnboardingSteps
import com.james.anvil.ui.components.OnboardingOverlay
import com.james.anvil.ui.components.PermissionCheckManager
import com.james.anvil.ui.navigation.BlocklistRoute
import com.james.anvil.ui.navigation.BonusTasksRoute
import com.james.anvil.ui.navigation.NavItem
import com.james.anvil.ui.navigation.NavigationGraph
import com.james.anvil.ui.navigation.TasksRoute
import com.james.anvil.ui.theme.ANVILTheme
import com.james.anvil.ui.theme.DesignTokens
import com.james.anvil.ui.theme.ForgedGold
import com.james.anvil.ui.theme.LocalWindowInfo
import com.james.anvil.ui.theme.ProvideWindowInfo
import com.james.anvil.util.ShortcutProvider
import com.james.anvil.util.WorkerScheduler
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WorkerScheduler.scheduleAllWorkers(this)
        ShortcutProvider.setupShortcuts(this)
        handleIntent(intent)

        setContent {
            ProvideWindowInfo(activity = this) {
                val isDarkTheme by viewModel.isDarkTheme.collectAsState()
                val showOnboarding by viewModel.showOnboarding.collectAsState()
                val onboardingStep by viewModel.onboardingStep.collectAsState()

                ANVILTheme(darkTheme = isDarkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AnvilAppShell(viewModel = viewModel)

                            if (showOnboarding) {
                                OnboardingOverlay(
                                    isVisible = true,
                                    steps = AnvilOnboardingSteps.getFullOnboarding(),
                                    currentStep = onboardingStep,
                                    onStepComplete = { viewModel.nextOnboardingStep() },
                                    onSkip = { viewModel.skipOnboarding() },
                                    onComplete = { viewModel.completeOnboarding() }
                                )
                            } else {
                                PermissionCheckManager()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // Reserved for future deep-link and shortcut handling.
    }
}

@Composable
private fun AnvilAppShell(viewModel: TaskViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val navItems = NavItem.bottomNavItems
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val windowInfo = LocalWindowInfo.current

    if (windowInfo.shouldShowNavRail) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                navItems.forEach { navItem ->
                    val selected = currentDestination.isTopLevelSelected(navItem)
                    NavigationRailItem(
                        selected = selected,
                        onClick = { navController.navigateToTopLevel(navItem) },
                        icon = {
                            Icon(
                                imageVector = if (selected) navItem.selectedIcon else navItem.unselectedIcon,
                                contentDescription = navItem.title
                            )
                        },
                        label = { Text(navItem.title) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = ForgedGold.copy(alpha = 0.22f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            ShellContent(
                modifier = Modifier.weight(1f),
                navController = navController,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState
            )
        }
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = DesignTokens.ElevationMedium
                ) {
                    navItems.forEach { navItem ->
                        val selected = currentDestination.isTopLevelSelected(navItem)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTopLevel(navItem) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) navItem.selectedIcon else navItem.unselectedIcon,
                                    contentDescription = navItem.title
                                )
                            },
                            label = { Text(navItem.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = ForgedGold.copy(alpha = 0.22f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            ShellContent(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@Composable
private fun ShellContent(
    modifier: Modifier = Modifier,
    navController: androidx.navigation.NavHostController,
    viewModel: TaskViewModel,
    snackbarHostState: SnackbarHostState
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        NavigationGraph(
            navController = navController,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    horizontal = DesignTokens.PaddingScreen,
                    vertical = DesignTokens.SpacingMd
                )
        )
    }
}

private fun androidx.navigation.NavHostController.navigateToTopLevel(item: NavItem<*>) {
    navigate(item.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavDestination?.isTopLevelSelected(item: NavItem<*>): Boolean {
    if (this == null) return false

    val matchesPrimary = hierarchy.any { destination ->
        destination.hasRoute(item.routeClass)
    }
    if (matchesPrimary) return true

    return item == NavItem.Tasks && hierarchy.any { destination ->
        destination.hasRoute(BonusTasksRoute::class) || destination.hasRoute(TasksRoute::class)
    } || item == NavItem.Blocklist && hierarchy.any { destination ->
        destination.hasRoute(BlocklistRoute::class)
    }
}

private val dateFormatThreadLocal = ThreadLocal.withInitial {
    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
}

fun formatDate(timestamp: Long): String {
    return dateFormatThreadLocal.get()!!.format(Date(timestamp))
}
