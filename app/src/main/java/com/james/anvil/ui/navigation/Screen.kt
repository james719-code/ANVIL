package com.james.anvil.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KClass

/**
 * Navigation items for bottom navigation bar.
 * Uses type-safe routes with filled/outlined icon variants.
 */
sealed class NavItem<T : Any>(
    val route: T,
    val routeClass: KClass<T>,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : NavItem<DashboardRoute>(
        route = DashboardRoute,
        routeClass = DashboardRoute::class,
        title = "Home",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    )
    
    data object Tasks : NavItem<TasksRoute>(
        route = TasksRoute,
        routeClass = TasksRoute::class,
        title = "Tasks",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    data object Blocklist : NavItem<BlocklistRoute>(
        route = BlocklistRoute,
        routeClass = BlocklistRoute::class,
        title = "Blocklist",
        selectedIcon = Icons.Filled.List,
        unselectedIcon = Icons.Outlined.List
    )
    
    data object Settings : NavItem<SettingsRoute>(
        route = SettingsRoute,
        routeClass = SettingsRoute::class,
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
    
    companion object {
        val bottomNavItems = listOf(Dashboard, Tasks, Blocklist, Settings)
    }
}

// Legacy Screen sealed class retained for gradual migration
@Deprecated("Use NavItem and type-safe Routes instead")
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.Home)
    object Blocklist : Screen("blocklist", "Blocklist", Icons.Default.List)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Budget : Screen("budget", "Budget", Icons.Default.AccountBalanceWallet)
    object Loans : Screen("loans", "Loans", Icons.Default.People)
    object EditTask : Screen("edit_task/{taskId}", "Edit Task", Icons.Default.Edit) {
        fun createRoute(taskId: Long) = "edit_task/$taskId"
    }
}
