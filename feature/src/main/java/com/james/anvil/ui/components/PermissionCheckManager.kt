package com.james.anvil.ui.components

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.james.anvil.util.PrefsKeys

/**
 * Data class representing the state of all permission dialogs.
 */
data class PermissionDialogState(
    val showNotificationDialog: Boolean = false,
    val showUsageAccessDialog: Boolean = false,
    val showBatteryDialog: Boolean = false,
    val showRestrictedGuide: Boolean = false
)

/**
 * Composable that manages checking and displaying all permission dialogs.
 * Consolidates the three permission checks into a single component.
 */
@Composable
fun PermissionCheckManager() {
    val context = LocalContext.current
    val prefs = remember { 
        context.getSharedPreferences(PrefsKeys.ANVIL_DIALOG_PREFS, Context.MODE_PRIVATE) 
    }
    
    var dialogState by remember { mutableStateOf(PermissionDialogState()) }
    
    // Check all permissions on launch
    LaunchedEffect(Unit) {
        // Check notification permission
        if (!prefs.getBoolean(PrefsKeys.DONT_SHOW_NOTIFICATION_DIALOG, false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = context.checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPermission) {
                    dialogState = dialogState.copy(showNotificationDialog = true)
                }
            }
        }
        
        // Check usage access permission
        if (!prefs.getBoolean(PrefsKeys.DONT_SHOW_USAGE_DIALOG, false)) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            if (mode != AppOpsManager.MODE_ALLOWED) {
                dialogState = dialogState.copy(showUsageAccessDialog = true)
            }
        }
        
        // Check battery optimization
        if (!prefs.getBoolean(PrefsKeys.DONT_SHOW_BATTERY_DIALOG, false)) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                dialogState = dialogState.copy(showBatteryDialog = true)
            }
        }
    }
    
    // Show notification permission dialog
    if (dialogState.showNotificationDialog) {
        PermissionDialog(
            permissionType = PermissionType.NOTIFICATION,
            onDismiss = { dontShowAgain ->
                if (dontShowAgain) {
                    prefs.edit().putBoolean(PrefsKeys.DONT_SHOW_NOTIFICATION_DIALOG, true).apply()
                }
                dialogState = dialogState.copy(showNotificationDialog = false)
            },
            onConfirm = { dontShowAgain ->
                if (dontShowAgain) {
                    prefs.edit().putBoolean(PrefsKeys.DONT_SHOW_NOTIFICATION_DIALOG, true).apply()
                }
                dialogState = dialogState.copy(showNotificationDialog = false)
            }
        )
    }
    
    // Show usage access dialog
    if (dialogState.showUsageAccessDialog) {
        PermissionDialog(
            permissionType = PermissionType.USAGE_ACCESS,
            onDismiss = { dontShowAgain ->
                if (dontShowAgain) {
                    prefs.edit().putBoolean(PrefsKeys.DONT_SHOW_USAGE_DIALOG, true).apply()
                }
                dialogState = dialogState.copy(showUsageAccessDialog = false)
            },
            onConfirm = { dontShowAgain ->
                if (dontShowAgain) {
                    prefs.edit().putBoolean(PrefsKeys.DONT_SHOW_USAGE_DIALOG, true).apply()
                }
                dialogState = dialogState.copy(showUsageAccessDialog = false)
            }
        )
    }
    
    // Show battery optimization dialog
    if (dialogState.showBatteryDialog) {
        PermissionDialog(
            permissionType = PermissionType.BATTERY_OPTIMIZATION,
            onDismiss = { dontShowAgain ->
                if (dontShowAgain) {
                    prefs.edit().putBoolean(PrefsKeys.DONT_SHOW_BATTERY_DIALOG, true).apply()
                }
                dialogState = dialogState.copy(showBatteryDialog = false)
            },
            onConfirm = { dontShowAgain ->
                if (dontShowAgain) {
                    prefs.edit().putBoolean(PrefsKeys.DONT_SHOW_BATTERY_DIALOG, true).apply()
                }
                dialogState = dialogState.copy(showBatteryDialog = false)
            },
            showRestrictedGuide = {
                dialogState = dialogState.copy(showRestrictedGuide = true)
            }
        )
    }
    
    // Show restricted settings guide
    if (dialogState.showRestrictedGuide) {
        RestrictedSettingsGuideDialog(
            onDismiss = { dialogState = dialogState.copy(showRestrictedGuide = false) }
        )
    }
}
