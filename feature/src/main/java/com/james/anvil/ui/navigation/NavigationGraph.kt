package com.james.anvil.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.james.anvil.ui.AboutScreen
import com.james.anvil.ui.TaskViewModel
import com.james.anvil.ui.TasksScreen
import com.james.anvil.ui.TaskWorkspaceScreen
import com.james.anvil.ui.BlocklistScreen
import com.james.anvil.ui.BudgetScreen
import com.james.anvil.ui.BudgetViewModel
import com.james.anvil.ui.VaultOverviewScreen
import com.james.anvil.ui.DashboardScreen
import com.james.anvil.ui.EditTaskScreen
import com.james.anvil.ui.FocusSessionScreen
import com.james.anvil.ui.ForgeHubScreen
import com.james.anvil.ui.ForgeProfileScreen
import com.james.anvil.ui.ForgeReportScreen
import com.james.anvil.ui.ForgeShopScreen
import com.james.anvil.ui.GearEquipmentScreen
import com.james.anvil.ui.LoansScreen
import com.james.anvil.ui.MonsterCombatScreen
import com.james.anvil.ui.QuestLogScreen
import com.james.anvil.ui.SavingsGoalsScreen
import com.james.anvil.ui.SettingsScreen
import com.james.anvil.ui.SkillTreeScreen
import androidx.compose.material3.SnackbarHostState

private const val NAV_ANIMATION_DURATION = 280

/** Position of a destination in the bottom bar, or null for screens that aren't a tab. */
private fun NavDestination?.tabIndex(): Int? = when {
    this == null -> null
    hasRoute(DashboardRoute::class) -> 0
    hasRoute(TasksRoute::class) || hasRoute(BonusTasksRoute::class) -> 1
    hasRoute(BlocklistRoute::class) -> 2
    hasRoute(BudgetRoute::class) -> 3
    hasRoute(ForgeHubRoute::class) -> 4
    else -> null
}

/**
 * Between two tabs the screen slides in from the side the new tab sits on: moving right in the
 * bar brings the new screen in from the right, moving left brings it in from the left.
 * Returns null when either end isn't a tab, so push/pop rules apply instead.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): AnimatedContentTransitionScope.SlideDirection? {
    val from = initialState.destination.tabIndex() ?: return null
    val to = targetState.destination.tabIndex() ?: return null
    if (from == to) return null
    return if (to > from) AnimatedContentTransitionScope.SlideDirection.Start
    else AnimatedContentTransitionScope.SlideDirection.End
}

/** Switch to a bottom-bar destination the same way tapping its tab does. */
private fun NavHostController.navigateTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun NavigationGraph(
    navController: NavHostController, 
    viewModel: TaskViewModel,
    snackbarHostState: SnackbarHostState
) {
    NavHost(
        navController = navController, 
        startDestination = DashboardRoute,
        enterTransition = {
            val direction = tabDirection() ?: AnimatedContentTransitionScope.SlideDirection.Start
            fadeIn(tween(NAV_ANIMATION_DURATION)) +
                slideIntoContainer(direction, tween(NAV_ANIMATION_DURATION))
        },
        exitTransition = {
            val direction = tabDirection() ?: AnimatedContentTransitionScope.SlideDirection.Start
            fadeOut(tween(NAV_ANIMATION_DURATION)) +
                slideOutOfContainer(direction, tween(NAV_ANIMATION_DURATION))
        },
        popEnterTransition = {
            val direction = tabDirection() ?: AnimatedContentTransitionScope.SlideDirection.End
            fadeIn(tween(NAV_ANIMATION_DURATION)) +
                slideIntoContainer(direction, tween(NAV_ANIMATION_DURATION))
        },
        popExitTransition = {
            val direction = tabDirection() ?: AnimatedContentTransitionScope.SlideDirection.End
            fadeOut(tween(NAV_ANIMATION_DURATION)) +
                slideOutOfContainer(direction, tween(NAV_ANIMATION_DURATION))
        }
    ) {
        composable<DashboardRoute> {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToTasks = { navController.navigateTab(TasksRoute) },
                onNavigateToBonusTasks = { navController.navigate(BonusTasksRoute) },
                onNavigateToBudget = { navController.navigateTab(BudgetRoute) },
                onNavigateToBlocklist = { navController.navigateTab(BlocklistRoute) },
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
                onNavigateToForge = { navController.navigateTab(ForgeHubRoute) },
                onNavigateToFocus = { navController.navigate(FocusSessionRoute) },
                onNavigateToSavings = { navController.navigate(SavingsGoalsRoute) },
                onNavigateToShop = { navController.navigate(ForgeShopRoute) },
                onNavigateToQuests = { navController.navigate(QuestLogRoute) },
                onNavigateToReport = { navController.navigate(ForgeReportRoute) }
            )
        }
        
        composable<TasksRoute> {
            TaskWorkspaceScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                initialTab = 0
            )
        }

        composable<BonusTasksRoute> {
            TaskWorkspaceScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                initialTab = 1
            )
        }
        
        composable<BudgetRoute> {
            VaultOverviewScreen()
        }
        
        composable<ForgeHubRoute> {
            ForgeHubScreen(
                onNavigateToForgeProfile = { navController.navigate(ForgeProfileRoute) },
                onNavigateToSkillTree = { navController.navigate(SkillTreeRoute) },
                onNavigateToGearEquipment = { navController.navigate(GearEquipmentRoute) },
                onNavigateToForgeReport = { navController.navigate(ForgeReportRoute) },
                onNavigateToForgeShop = { navController.navigate(ForgeShopRoute) },
                onNavigateToQuestLog = { navController.navigate(QuestLogRoute) },
                onNavigateToFocusSession = { navController.navigate(FocusSessionRoute) },
                onNavigateToSavingsGoals = { navController.navigate(SavingsGoalsRoute) }
            )
        }
        
        composable<BlocklistRoute> {
            BlocklistScreen(
                onNavigateToMonsterCombat = { monsterId ->
                    navController.navigate(MonsterCombatRoute(monsterId))
                }
            )
        }
        
        composable<SettingsRoute> {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToAbout = { navController.navigate(AboutRoute) }
            )
        }

        composable<AboutRoute> {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable<EditTaskRoute> { backStackEntry ->
            val route: EditTaskRoute = backStackEntry.toRoute()
            EditTaskScreen(
                viewModel = viewModel,
                taskId = route.taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<ForgeProfileRoute> {
            ForgeProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSkillTree = { navController.navigate(SkillTreeRoute) },
                onNavigateToGear = { navController.navigate(GearEquipmentRoute) }
            )
        }

        composable<FocusSessionRoute> {
            FocusSessionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<SavingsGoalsRoute> {
            SavingsGoalsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<ForgeShopRoute> {
            ForgeShopScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<MonsterCombatRoute> { backStackEntry ->
            val route: MonsterCombatRoute = backStackEntry.toRoute()
            MonsterCombatScreen(
                monsterId = route.monsterId,
                onBack = { navController.popBackStack() }
            )
        }

        composable<QuestLogRoute> {
            QuestLogScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<SkillTreeRoute> {
            SkillTreeScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<GearEquipmentRoute> {
            GearEquipmentScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<LoansRoute> {
            val budgetViewModel: BudgetViewModel = hiltViewModel()
            LoansScreen(
                viewModel = budgetViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<ForgeReportRoute> {
            ForgeReportScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
