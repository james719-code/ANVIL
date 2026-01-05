package com.james.anvil.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.james.anvil.service.AnvilAccessibilityService
import com.james.anvil.ui.theme.DeepTeal
import com.james.anvil.ui.theme.GradientEnd
import com.james.anvil.ui.theme.GradientStart
import com.james.anvil.util.PermissionUtils
import androidx.compose.ui.tooling.preview.Preview

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
                title = { 
                    Text(
                        "Settings", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Appearance Section
            SettingsSectionHeader(title = "Appearance")
            SettingsItem(
                title = "Dark Mode",
                subtitle = "Toggle dark/light theme",
                trailing = {
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { viewModel.toggleTheme(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

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
                    if (hasOverlayPermission) {
                        Icon(
                            Icons.Default.CheckCircle, 
                            null, 
                            tint = DeepTeal
                        )
                    } else {
                        Icon(
                            Icons.Default.ArrowForward, 
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    if (hasAccessibilityPermission) {
                        Icon(
                            Icons.Default.CheckCircle, 
                            null, 
                            tint = DeepTeal
                        )
                    } else {
                        Icon(
                            Icons.Default.ArrowForward, 
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // About Section
            SettingsSectionHeader(title = "About ANVIL")
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "ANVIL",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "v1.0.0 • Production Build",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Forge Your Will",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Anvil is your digital shield against distraction. Master your time, block interruptions, and track your financial freedom all in one place.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                           Text(
                               text = "Created by ",
                               style = MaterialTheme.typography.bodySmall,
                               color = MaterialTheme.colorScheme.onSurfaceVariant
                           )
                           Text(
                               text = "James Ryan Gallego",
                               style = MaterialTheme.typography.bodySmall,
                               fontWeight = FontWeight.Bold,
                               color = MaterialTheme.colorScheme.primary
                           )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
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
        headlineContent = { 
            Text(
                title, 
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
        supportingContent = if (subtitle != null) { 
            { 
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            } 
        } else null,
        leadingContent = if (icon != null) { 
            { 
                Icon(
                    icon, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                ) 
            } 
        } else null,
        trailingContent = trailing,
        modifier = Modifier.clickable(enabled = onClick != null) { onClick?.invoke() }
    )
}

// =============================================
// Preview Functions (Removed in Release Builds)
// =============================================

@Preview(name = "Settings Section Header", showBackground = true)
@Composable
private fun SettingsSectionHeaderPreview() {
    com.james.anvil.ui.theme.ANVILTheme {
        SettingsSectionHeader(title = "Appearance")
    }
}

@Preview(name = "Settings Item - Light", showBackground = true)
@Composable
private fun SettingsItemPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = false) {
        SettingsItem(
            title = "Dark Mode",
            subtitle = "Toggle dark/light theme",
            trailing = {
                Switch(checked = false, onCheckedChange = {})
            }
        )
    }
}

@Preview(name = "Settings Item - Dark", showBackground = true)
@Composable
private fun SettingsItemDarkPreview() {
    com.james.anvil.ui.theme.ANVILTheme(darkTheme = true) {
        SettingsItem(
            title = "Draw Over Apps",
            subtitle = "Required for blocking overlays",
            icon = Icons.Default.Settings,
            trailing = {
                Icon(Icons.Default.CheckCircle, null, tint = DeepTeal)
            }
        )
    }
}
