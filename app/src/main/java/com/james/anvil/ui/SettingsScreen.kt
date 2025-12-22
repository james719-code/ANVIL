package com.james.anvil.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.james.anvil.util.PermissionUtils
import com.james.anvil.service.AnvilAccessibilityService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TaskViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasOverlayPermission by remember { mutableStateOf(false) }
    var hasAccessibilityPermission by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = PermissionUtils.hasOverlayPermission(context)
                hasAccessibilityPermission = PermissionUtils.hasAccessibilityPermission(context, AnvilAccessibilityService::class.java)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            
            // Appearance Section
            SettingsSectionHeader(title = "Appearance")
            SettingsItem(
                title = "Dark Mode",
                subtitle = "Toggle dark/light theme",
                trailing = {
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { viewModel.toggleTheme(it) }
                    )
                }
            )

            HorizontalDivider()

            // Permissions Section
            SettingsSectionHeader(title = "Permissions")
            SettingsItem(
                title = "Draw Over Apps",
                subtitle = if (hasOverlayPermission) "Granted" else "Required for blocking overlays",
                onClick = {
                    if (!hasOverlayPermission) {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    }
                },
                trailing = {
                     if (!hasOverlayPermission) Icon(Icons.Default.ArrowForward, null)
                }
            )
            SettingsItem(
                title = "Accessibility Service",
                subtitle = if (hasAccessibilityPermission) "Active" else "Required for detecting app usage",
                onClick = {
                    if (!hasAccessibilityPermission) {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                },
                 trailing = {
                     if (!hasAccessibilityPermission) Icon(Icons.Default.ArrowForward, null)
                }
            )

            HorizontalDivider()

            // About Us Section
            SettingsSectionHeader(title = "About")
            SettingsItem(
                title = "ANVIL v1.0",
                subtitle = "Created by James Ryan Gallego",
                icon = Icons.Default.Info
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Forge Your Will. Stay focused, track your tasks, and eliminate distractions with ANVIL.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = if (subtitle != null) { { Text(subtitle) } } else null,
        leadingContent = if (icon != null) { { Icon(icon, contentDescription = null) } } else null,
        trailingContent = trailing,
        modifier = Modifier.clickable(enabled = onClick != null) { onClick?.invoke() }
    )
}
