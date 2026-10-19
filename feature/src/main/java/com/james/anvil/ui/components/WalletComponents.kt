package com.james.anvil.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.data.Wallet
import com.james.anvil.data.WalletDefaults
import com.james.anvil.ui.viewmodel.WalletDeleteResult
import com.james.anvil.ui.viewmodel.WalletSaveResult
import com.james.anvil.ui.viewmodel.WalletViewModel

fun Wallet.color(): Color = Color(colorArgb)

/** id -> Wallet lookup for screens that only need to label or tint a stored wallet id. */
@Composable
fun rememberWalletLookup(viewModel: WalletViewModel = hiltViewModel()): (String) -> Wallet? {
    val wallets by viewModel.wallets.collectAsState()
    val byId = remember(wallets) { wallets.associateBy { it.id } }
    return remember(byId) { { id: String -> byId[id] } }
}

@Composable
fun WalletDot(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    Box(modifier.size(size).background(color, CircleShape))
}

/**
 * Pick which wallet a transaction, loan or goal uses. Lists every wallet the user has and
 * lets them add a new one in place.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletPicker(
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val wallets by viewModel.wallets.collectAsState()
    var showNewWallet by remember { mutableStateOf(false) }

    // The default selection ("CASH") may not exist any more; fall back to the first wallet.
    LaunchedEffect(wallets, selectedId) {
        if (wallets.isNotEmpty() && wallets.none { it.id == selectedId }) onSelect(wallets.first().id)
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        wallets.forEach { wallet ->
            val selected = wallet.id == selectedId
            val tint = wallet.color()
            Surface(
                onClick = { onSelect(wallet.id) },
                shape = RoundedCornerShape(50),
                color = if (selected) tint.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.5.dp, if (selected) tint else Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WalletDot(tint)
                    Text(
                        text = wallet.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Surface(
            onClick = { showNewWallet = true },
            shape = RoundedCornerShape(50),
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text("New", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showNewWallet) {
        WalletEditorDialog(wallet = null, onDismiss = { showNewWallet = false })
    }
}

/** Create ([wallet] == null), rename/recolour, or delete a wallet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletEditorDialog(
    wallet: Wallet?,
    onDismiss: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf(wallet?.name.orEmpty()) }
    var color by remember { mutableStateOf(wallet?.colorArgb ?: WalletDefaults.palette.first()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (wallet == null) "New wallet" else "Edit wallet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Wallet name") },
                    placeholder = { Text("e.g. Maya, BDO, Savings") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    shape = RoundedCornerShape(14.dp)
                )
                Text("Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WalletDefaults.palette.forEach { swatch ->
                        val selected = swatch == color
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(swatch), CircleShape)
                                .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .clickable { color = swatch }
                                .semantics { contentDescription = if (selected) "Selected color" else "Color option" },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.saveWallet(wallet, name, color) { result ->
                    when (result) {
                        WalletSaveResult.OK -> onDismiss()
                        WalletSaveResult.EMPTY_NAME -> error = "Enter a name"
                        WalletSaveResult.NAME_TAKEN -> error = "You already have a wallet with that name"
                    }
                }
            }) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                if (wallet != null) {
                    TextButton(onClick = {
                        viewModel.deleteWallet(wallet) { result ->
                            when (result) {
                                WalletDeleteResult.OK -> onDismiss()
                                WalletDeleteResult.IN_USE ->
                                    error = "This wallet has transactions, loans or goals, so it can't be deleted."
                                WalletDeleteResult.LAST_WALLET -> error = "Keep at least one wallet."
                            }
                        }
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.width(4.dp))
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
