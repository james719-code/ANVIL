package com.james.anvil.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.james.anvil.data.GearItem
import com.james.anvil.data.GearRarity
import com.james.anvil.data.GearSlot
import com.james.anvil.data.StatType
import com.james.anvil.ui.components.AnvilCard
import com.james.anvil.ui.theme.*
import com.james.anvil.ui.viewmodel.GearViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GearEquipmentScreen(
    onBack: () -> Unit,
    viewModel: GearViewModel = hiltViewModel()
) {
    val allGear by viewModel.allGear.collectAsState()
    val equippedGear by viewModel.equippedGear.collectAsState()

    val equippedMap = equippedGear.associateBy { it.slot }
    val inventory = allGear.filter { !it.isEquipped }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equipment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Equipped Slots
            item {
                Text(
                    "Equipped Gear",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GearSlot.entries.forEach { slot ->
                        EquipSlotCard(
                            slot = slot,
                            item = equippedMap[slot],
                            modifier = Modifier.weight(1f),
                            onUnequip = { itemId -> viewModel.unequipItem(itemId) }
                        )
                    }
                }
            }

            // Inventory
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Inventory (${inventory.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (inventory.isEmpty()) {
                item {
                    AnvilCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("\uD83C\uDFF0", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No gear in inventory",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                "Defeat monsters to earn gear drops!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            } else {
                items(inventory, key = { it.id }) { item ->
                    GearItemCard(
                        item = item,
                        onEquip = { viewModel.equipItem(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EquipSlotCard(
    slot: GearSlot,
    item: GearItem?,
    modifier: Modifier = Modifier,
    onUnequip: (Long) -> Unit
) {
    val slotEmoji = when (slot) {
        GearSlot.WEAPON -> "\u2694\uFE0F"
        GearSlot.ARMOR -> "\uD83D\uDEE1\uFE0F"
        GearSlot.ACCESSORY -> "\uD83D\uDC8D"
    }

    val borderColor = if (item != null) rarityColor(item.rarity) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    AnvilCard(
        modifier = modifier
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .then(if (item != null) Modifier.clickable { onUnequip(item.id) } else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(slotEmoji, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                slot.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            if (item != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    item.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = rarityColor(item.rarity),
                    maxLines = 2
                )
                Text(
                    "+${String.format("%.0f", item.statValue)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Empty",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun GearItemCard(
    item: GearItem,
    onEquip: () -> Unit
) {
    val rColor = rarityColor(item.rarity)

    AnvilCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, rColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onEquip() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slot icon
            Text(
                when (item.slot) {
                    GearSlot.WEAPON -> "\u2694\uFE0F"
                    GearSlot.ARMOR -> "\uD83D\uDEE1\uFE0F"
                    GearSlot.ACCESSORY -> "\uD83D\uDC8D"
                },
                fontSize = 28.sp
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = rColor
                )
                Text(
                    "${item.rarity.name} ${item.slot.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    "+${String.format("%.1f", item.statValue)}% ${statTypeName(item.statType)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            TextButton(onClick = onEquip) {
                Text("Equip", color = rColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun rarityColor(rarity: GearRarity) = when (rarity) {
    GearRarity.COMMON -> Color.Gray
    GearRarity.RARE -> ElectricBlue
    GearRarity.EPIC -> Color(0xFF9C27B0)
    GearRarity.LEGENDARY -> ForgedGold
}

private fun statTypeName(statType: StatType) = when (statType) {
    StatType.TASK_XP_BONUS -> "Task XP"
    StatType.FOCUS_XP_BONUS -> "Focus XP"
    StatType.SAVINGS_BONUS -> "Savings"
    StatType.MONSTER_DAMAGE_BONUS -> "Monster Damage"
    StatType.MONSTER_HP_REDUCTION -> "Monster HP Reduction"
    StatType.COIN_BONUS -> "Coin Earnings"
    StatType.QUEST_XP_BONUS -> "Quest XP"
}
