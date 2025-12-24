package com.james.anvil.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit 
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.Home)
    object Blocklist : Screen("blocklist", "Blocklist", Icons.Default.List)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object EditTask : Screen("edit_task/{taskId}", "Edit Task", Icons.Default.Edit) {
        fun createRoute(taskId: Long) = "edit_task/$taskId"
    }
}