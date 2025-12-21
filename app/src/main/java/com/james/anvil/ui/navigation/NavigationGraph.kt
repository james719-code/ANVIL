package com.james.anvil.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.james.anvil.ui.TaskViewModel
import com.james.anvil.ui.TasksScreen
import com.james.anvil.ui.BlocklistScreen
import com.james.anvil.ui.SettingsScreen

@Composable
fun NavigationGraph(navController: NavHostController, viewModel: TaskViewModel) {
    NavHost(navController = navController, startDestination = Screen.Tasks.route) {
        composable(Screen.Tasks.route) {
            TasksScreen(viewModel)
        }
        composable(Screen.Blocklist.route) {
            BlocklistScreen(viewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel)
        }
    }
}
