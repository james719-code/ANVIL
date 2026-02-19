package com.james.anvil.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.james.anvil.ui.TaskViewModel
import com.james.anvil.ui.TasksScreen
import com.james.anvil.ui.BlocklistScreen
import com.james.anvil.ui.BudgetScreen
import com.james.anvil.ui.BudgetViewModel
import com.james.anvil.ui.DashboardScreen
import com.james.anvil.ui.EditTaskScreen
import com.james.anvil.ui.FocusSessionScreen
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

private const val NAV_ANIMATION_DURATION = 300

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
            fadeIn(animationSpec = tween(NAV_ANIMATION_DURATION)) +
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(NAV_ANIMATION_DURATION)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(NAV_ANIMATION_DURATION)) +
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(NAV_ANIMATION_DURATION)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(NAV_ANIMATION_DURATION)) +
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(NAV_ANIMATION_DURATION)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(NAV_ANIMATION_DURATION)) +
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(NAV_ANIMATION_DURATION)
            )
        }
    ) {
        // Bottom Navigation Destinations
        composable<DashboardRoute> {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToForge = { navController.navigate(ForgeProfileRoute) },
                onNavigateToFocus = { navController.navigate(FocusSessionRoute) },
                onNavigateToSavings = { navController.navigate(SavingsGoalsRoute) },
                onNavigateToShop = { navController.navigate(ForgeShopRoute) },
                onNavigateToQuests = { navController.navigate(QuestLogRoute) },
                onNavigateToReport = { navController.navigate(ForgeReportRoute) }
            )
        }
        
        composable<TasksRoute> {
            TasksScreen(viewModel, snackbarHostState)
        }
        
        composable<BudgetRoute> {
            BudgetScreen()
        }
        
        composable<BlocklistRoute> {
            BlocklistScreen()
        }
        
        composable<SettingsRoute> {
            SettingsScreen(viewModel)
        }
        
        // Detail Destinations (with arguments)
        composable<EditTaskRoute> { backStackEntry ->
            val route: EditTaskRoute = backStackEntry.toRoute()
            EditTaskScreen(viewModel, route.taskId, navController)
        }

        composable<ForgeProfileRoute> {
            ForgeProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<FocusSessionRoute> {
            FocusSessionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Gamification Destinations
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
