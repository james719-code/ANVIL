package com.james.anvil.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star

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
        title = "Today",
        selectedIcon = Icons.Filled.Today,
        unselectedIcon = Icons.Outlined.Today
    )
    
    data object Tasks : NavItem<TasksRoute>(
        route = TasksRoute,
        routeClass = TasksRoute::class,
        title = "Tasks",
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle
    )
    
    data object Budget : NavItem<BudgetRoute>(
        route = BudgetRoute,
        routeClass = BudgetRoute::class,
        title = "Vault",
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet
    )
    
    data object Blocklist : NavItem<BlocklistRoute>(
        route = BlocklistRoute,
        routeClass = BlocklistRoute::class,
        title = "Blocks",
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield
    )
    
    data object ForgeHub : NavItem<ForgeHubRoute>(
        route = ForgeHubRoute,
        routeClass = ForgeHubRoute::class,
        title = "Forge",
        selectedIcon = Icons.Filled.LocalFireDepartment,
        unselectedIcon = Icons.Outlined.LocalFireDepartment
    )
    
    data object BonusTasks : NavItem<BonusTasksRoute>(
        route = BonusTasksRoute,
        routeClass = BonusTasksRoute::class,
        title = "Bonus",
        selectedIcon = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.Star
    )

    
    data object Settings : NavItem<SettingsRoute>(
        route = SettingsRoute,
        routeClass = SettingsRoute::class,
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
    
    companion object {
        val bottomNavItems = listOf(Dashboard, Tasks, Blocklist, Budget, ForgeHub)
    }

}
