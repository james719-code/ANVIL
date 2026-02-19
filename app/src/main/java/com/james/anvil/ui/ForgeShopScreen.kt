package com.james.anvil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.core.ForgeCoinManager
import com.james.anvil.ui.components.AnvilCard
import com.james.anvil.ui.theme.*
import com.james.anvil.ui.viewmodel.ForgeCoinViewModel

private data class ShopItem(
    val name: String,
    val description: String,
    val cost: Int,
    val icon: ImageVector,
    val iconTint: Color,
    val onPurchase: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgeShopScreen(
    onBack: () -> Unit,
    viewModel: ForgeCoinViewModel = hiltViewModel()
) {
    val coinBalance by viewModel.coinBalance.collectAsState()
    val purchaseResult by viewModel.purchaseResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val shopItems = remember {
        listOf(
            ShopItem(
                name = "Unblock Pass",
                description = "Temporarily bypass a blocked app for 1 hour",
                cost = ForgeCoinManager.COST_UNBLOCK_PASS,
                icon = Icons.Filled.Lock,
                iconTint = ElectricBlue,
                onPurchase = { viewModel.purchaseUnblockPass() }
            ),
            ShopItem(
                name = "XP Boost",
                description = "Double XP earned for the next hour",
                cost = ForgeCoinManager.COST_XP_BOOST,
                icon = Icons.Filled.Speed,
                iconTint = XpGold,
                onPurchase = { viewModel.purchaseXpBoost() }
            ),
            ShopItem(
                name = "Ice (Streak Freeze)",
                description = "Protect your streak for one missed day",
                cost = ForgeCoinManager.COST_ICE,
                icon = Icons.Outlined.AcUnit,
                iconTint = ElectricTeal,
                onPurchase = { viewModel.purchaseIce() }
            )
        )
    }

    // Handle purchase result
    LaunchedEffect(purchaseResult) {
        purchaseResult?.let { result ->
            val message = if (result.success)
                "${result.itemName} purchased!"
            else
                "Not enough Forge Coins for ${result.itemName}"
            snackbarHostState.showSnackbar(message)
            viewModel.clearPurchaseResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forge Shop", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Coin balance header
            item {
                AnvilCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "\uD83D\uDCB0",
                            fontSize = 32.sp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$coinBalance",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = ForgedGold
                            )
                            Text(
                                text = "Forge Coins",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Available Items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Shop items
            items(shopItems) { item ->
                ShopItemCard(
                    item = item,
                    canAfford = coinBalance >= item.cost
                )
            }
        }
    }
}

@Composable
private fun ShopItemCard(
    item: ShopItem,
    canAfford: Boolean
) {
    AnvilCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = item.iconTint.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = item.iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Buy button
            Button(
                onClick = item.onPurchase,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) ForgedGold else Color.Gray,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "\uD83D\uDCB0 ${item.cost}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
