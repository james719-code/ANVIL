package com.james.anvil.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.james.anvil.service.ServiceBridge
import com.james.anvil.ui.components.PageHeader
import com.james.anvil.ui.components.SectionCard
import com.james.anvil.ui.components.StatusPill
import com.james.anvil.ui.components.TopLevelPageScaffold
import com.james.anvil.ui.theme.DesignTokens
import com.james.anvil.ui.theme.ElectricTeal
import com.james.anvil.ui.theme.InfoBlue
import com.james.anvil.ui.theme.LocalWindowInfo
import com.james.anvil.ui.theme.SuccessGreen
import com.james.anvil.ui.theme.WarningOrange
import com.james.anvil.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TaskViewModel,
    navController: androidx.navigation.NavController? = null,
    onBack: (() -> Unit)? = null,
    onNavigateToAbout: (() -> Unit)? = null
) {


    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isPauseModeActive by viewModel.isPauseModeActive.collectAsState()
    val expenseReminderEnabled by viewModel.expenseReminderEnabled.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasOverlayPermission by remember { mutableStateOf(false) }
    var hasAccessibilityPermission by remember { mutableStateOf(false) }
    var isVpnRunning by remember { mutableStateOf(ServiceBridge.isVpnRunning()) }

    // VPN permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            ServiceBridge.startVpn(context)
            isVpnRunning = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = PermissionUtils.hasOverlayPermission(context)
                hasAccessibilityPermission = ServiceBridge.accessibilityServiceClass?.let { PermissionUtils.hasAccessibilityPermission(context, it) } ?: false
                isVpnRunning = ServiceBridge.isVpnRunning()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val windowInfo = LocalWindowInfo.current

    TopLevelPageScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .then(
                    if (windowInfo.maxContentWidth != androidx.compose.ui.unit.Dp.Unspecified) {
                        Modifier.widthIn(max = windowInfo.maxContentWidth)
                    } else {
                        Modifier
                    }
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = windowInfo.contentPadding)
        ) {

            PageHeader(
                eyebrow = "Controls",
                title = "Settings",
                subtitle = "Tune blocking, reminders, permissions, and the forge interface.",
                onBack = onBack ?: navController?.let { { it.popBackStack() } }
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            SectionCard(accentColor = if (isPauseModeActive) WarningOrange else SuccessGreen) {
                Text(
                    text = "Shield status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
                ) {
                    StatusPill(
                        text = if (hasOverlayPermission) "Overlay ready" else "Overlay needed",
                        tint = if (hasOverlayPermission) SuccessGreen else WarningOrange,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(
                        text = if (hasAccessibilityPermission) "Access ready" else "Access needed",
                        tint = if (hasAccessibilityPermission) SuccessGreen else WarningOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
                ) {
                    StatusPill(
                        text = if (isVpnRunning) "VPN active" else "VPN off",
                        tint = if (isVpnRunning) SuccessGreen else InfoBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(
                        text = if (isPauseModeActive) "Paused" else "Blocking live",
                        tint = if (isPauseModeActive) WarningOrange else SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Appearance Section
            SettingsSectionHeader(title = "APPEARANCE")
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.DarkMode,
                    iconTint = InfoBlue,
                    title = "Dark Mode",
                    subtitle = if (isDarkTheme) "On" else "Off",
                    trailing = {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { viewModel.toggleTheme(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notifications Section
            SettingsSectionHeader(title = "NOTIFICATIONS")
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.Notifications,
                    iconTint = WarningOrange,
                    title = "Budget Reminders",
                    subtitle = if (expenseReminderEnabled)
                        "Reminding you at 12:00 PM & 6:00 PM"
                    else
                        "Off",
                    trailing = {
                        Switch(
                            checked = expenseReminderEnabled,
                            onCheckedChange = { viewModel.toggleExpenseReminder(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Permissions Section
            SettingsSectionHeader(title = "PERMISSIONS")
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.Layers,
                    iconTint = if (hasOverlayPermission) 
                        SuccessGreen 
                    else 
                        WarningOrange,
                    title = "Draw Over Apps",
                    subtitle = if (hasOverlayPermission) "Permission granted" else "Tap to enable",
                    onClick = {
                        if (!hasOverlayPermission) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            context.startActivity(intent)
                        }
                    },
                    trailing = {
                        PermissionIndicator(isGranted = hasOverlayPermission)
                    }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                
                SettingsRow(
                    icon = Icons.Outlined.AccessibilityNew,
                    iconTint = if (hasAccessibilityPermission) 
                        SuccessGreen 
                    else 
                        WarningOrange,
                    title = "Accessibility Service",
                    subtitle = if (hasAccessibilityPermission) "Service active" else "Tap to enable",
                    onClick = {
                        if (!hasAccessibilityPermission) {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    },
                    trailing = {
                        PermissionIndicator(isGranted = hasAccessibilityPermission)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Blocking Section
            SettingsSectionHeader(title = "BLOCKING")
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isVpnRunning) {
                                val vpnIntent = ServiceBridge.prepareVpn(context)
                                if (vpnIntent != null) {
                                    vpnPermissionLauncher.launch(vpnIntent)
                                } else {
                                    ServiceBridge.startVpn(context)
                                    isVpnRunning = true
                                }
                            } else {
                                ServiceBridge.stopVpn(context)
                                isVpnRunning = false
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = (if (isVpnRunning) SuccessGreen else InfoBlue).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VpnKey,
                            contentDescription = "VPN",
                            tint = if (isVpnRunning) SuccessGreen else InfoBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VPN Link Blocking",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isVpnRunning) "Active" else "Off",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Switch(
                        checked = isVpnRunning,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val vpnIntent = ServiceBridge.prepareVpn(context)
                                if (vpnIntent != null) {
                                    vpnPermissionLauncher.launch(vpnIntent)
                                } else {
                                    ServiceBridge.startVpn(context)
                                    isVpnRunning = true
                                }
                            } else {
                                ServiceBridge.stopVpn(context)
                                isVpnRunning = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = if (isDarkTheme) Color.LightGray else MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = if (isDarkTheme) Color.Gray.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = if (isDarkTheme) Color.Gray else MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.Shield,
                    iconTint = if (isPauseModeActive) WarningOrange else SuccessGreen,
                    title = "Pause Mode",
                    subtitle = if (isPauseModeActive) "All blocking is paused" else "Blocking is active",
                    trailing = {
                        Switch(
                            checked = isPauseModeActive,
                            onCheckedChange = { viewModel.togglePauseMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = if (isDarkTheme) Color.LightGray else MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = if (isDarkTheme) Color.Gray.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedBorderColor = if (isDarkTheme) Color.Gray else MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            // About Section
            SettingsSectionHeader(title = "ABOUT")
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    iconTint = ElectricTeal,
                    title = "About ANVIL",
                    subtitle = "Version, features & developer info",
                    onClick = { onNavigateToAbout?.invoke() },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = "View",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
            }

            if (com.james.anvil.feature.BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(24.dp))

                // Developer Tools Section
                SettingsSectionHeader(title = "DEVELOPER TOOLS")
                Spacer(modifier = Modifier.height(8.dp))

                SettingsCard {
                    SettingsRow(
                        icon = Icons.Outlined.Layers,
                        iconTint = ElectricTeal,
                        title = "Populate Dev Mock Data",
                        subtitle = "Seed realistic tasks, budget entries, loans & RPG stats",
                        onClick = {
                            viewModel.seedMockData(clearExisting = false)
                            android.widget.Toast.makeText(context, "Mock data populated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = "Seed",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp),
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = iconTint.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        trailing()
    }
}

@Composable
private fun PermissionIndicator(isGranted: Boolean) {
    if (isGranted) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = SuccessGreen.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Granted",
                tint = SuccessGreen,
                modifier = Modifier.size(16.dp)
            )
        }
    } else {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = "Configure",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}
