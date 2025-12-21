package com.james.anvil.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Tasks : Screen("tasks", "Tasks", Icons.Default.Home)
    object Blocklist : Screen("blocklist", "Blocklist", Icons.Default.List)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
