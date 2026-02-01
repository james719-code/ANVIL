package com.james.anvil.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.james.anvil.ui.TaskViewModel
import com.james.anvil.ui.TasksScreen
import com.james.anvil.ui.BlocklistScreen
import com.james.anvil.ui.SettingsScreen
import com.james.anvil.ui.DashboardScreen
import com.james.anvil.ui.EditTaskScreen
import com.james.anvil.ui.BudgetScreen
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
            DashboardScreen(viewModel)
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
    }
}

