package com.james.anvil.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.james.anvil.R
import com.james.anvil.ui.theme.DesignTokens

/**
 * Enum representing different permission types that require user action.
 */
enum class PermissionType {
    NOTIFICATION,
    USAGE_ACCESS,
    BATTERY_OPTIMIZATION
}

/**
 * Reusable permission dialog component.
 * Consolidates the common UI pattern for requesting permissions with
 * a "don't show again" option.
 *
 * @param permissionType The type of permission being requested
 * @param onDismiss Callback when dialog is dismissed
 * @param onConfirm Callback when user confirms (navigates to settings)
 * @param onDontShowAgainChanged Callback when "don't show again" checkbox changes
 * @param showRestrictedGuide Optional callback to show restricted settings guide (for battery)
 */
@Composable
fun PermissionDialog(
    permissionType: PermissionType,
    onDismiss: (dontShowAgain: Boolean) -> Unit,
    onConfirm: (dontShowAgain: Boolean) -> Unit,
    showRestrictedGuide: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var dontShowAgain by remember { mutableStateOf(false) }
    
    val (title, message, confirmText) = when (permissionType) {
        PermissionType.NOTIFICATION -> Triple(
            stringResource(R.string.permission_notification_title),
            stringResource(R.string.permission_notification_message),
            stringResource(R.string.permission_enable)
        )
        PermissionType.USAGE_ACCESS -> Triple(
            stringResource(R.string.permission_usage_title),
            stringResource(R.string.permission_usage_message),
            stringResource(R.string.permission_fix)
        )
        PermissionType.BATTERY_OPTIMIZATION -> Triple(
            stringResource(R.string.permission_battery_title),
            stringResource(R.string.permission_battery_message),
            stringResource(R.string.permission_fix)
        )
    }
    
    AlertDialog(
        onDismissRequest = { onDismiss(dontShowAgain) },
        title = { Text(title) },
        text = {
            Column {
                Text(message)
                Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it }
                    )
                    Text(
                        text = stringResource(R.string.permission_dont_show_again),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { dontShowAgain = !dontShowAgain }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(dontShowAgain)
                    openPermissionSettings(context, permissionType)
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(dontShowAgain) }) {
                Text(stringResource(R.string.permission_later))
            }
        },
        icon = {
            if (permissionType == PermissionType.BATTERY_OPTIMIZATION && 
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                showRestrictedGuide != null
            ) {
                TextButton(onClick = showRestrictedGuide) {
                    Text(stringResource(R.string.permission_cant_enable_accessibility))
                }
            }
        }
    )
}

/**
 * Opens the appropriate system settings for the given permission type.
 */
private fun openPermissionSettings(context: Context, permissionType: PermissionType) {
    val intent = when (permissionType) {
        PermissionType.NOTIFICATION -> {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        }
        PermissionType.USAGE_ACCESS -> {
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        }
        PermissionType.BATTERY_OPTIMIZATION -> {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }
    context.startActivity(intent)
}

/**
 * Dialog showing a step-by-step guide for enabling restricted settings on Android 13+.
 */
@Composable
fun RestrictedSettingsGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(DesignTokens.RadiusLarge),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .height(DesignTokens.DialogMaxHeight)
        ) {
            Column(
                modifier = Modifier
                    .padding(DesignTokens.PaddingCardLarge)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.restricted_settings_guide_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
                
                Text(
                    text = stringResource(R.string.restricted_settings_guide_intro),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
                
                GuideStep(1, 
                    stringResource(R.string.guide_step1_title), 
                    stringResource(R.string.guide_step1_desc)
                )
                GuideStep(2, 
                    stringResource(R.string.guide_step2_title), 
                    stringResource(R.string.guide_step2_desc)
                )
                GuideStep(3, 
                    stringResource(R.string.guide_step3_title), 
                    stringResource(R.string.guide_step3_desc)
                )
                GuideStep(4, 
                    stringResource(R.string.guide_step4_title), 
                    stringResource(R.string.guide_step4_desc)
                )
                
                Spacer(modifier = Modifier.height(DesignTokens.SpacingXl))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.got_it))
                }
            }
        }
    }
}

/**
 * A single step in the guide with a numbered title and description.
 */
@Composable
fun GuideStep(number: Int, title: String, description: String) {
    Column(modifier = Modifier.padding(bottom = DesignTokens.SpacingLg)) {
        Text(
            text = "$number. $title",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = DesignTokens.SpacingXs)
        )
    }
}
