package com.james.anvil.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.james.anvil.ui.TaskViewModel
import com.james.anvil.ui.TasksScreen
import com.james.anvil.ui.BlocklistScreen
import com.james.anvil.ui.SettingsScreen
import com.james.anvil.ui.DashboardScreen
import com.james.anvil.ui.EditTaskScreen
import com.james.anvil.ui.BudgetScreen
import com.james.anvil.ui.LoansScreen
import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun NavigationGraph(
    navController: NavHostController, 
    viewModel: TaskViewModel,
    snackbarHostState: SnackbarHostState
) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(viewModel, navController)
        }
        composable(Screen.Tasks.route) {
            TasksScreen(viewModel, snackbarHostState, navController)
        }
        composable(Screen.Blocklist.route) {
            BlocklistScreen(viewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel)
        }
        composable(Screen.Budget.route) {
            BudgetScreen(viewModel, navController, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Loans.route) {
            LoansScreen(viewModel, onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.EditTask.route,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: return@composable
            EditTaskScreen(viewModel, taskId, navController)
        }
    }
}

