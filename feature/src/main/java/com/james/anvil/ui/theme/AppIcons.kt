package com.james.anvil.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector
import com.james.anvil.data.GearSlot
import com.james.anvil.data.SkillBranch

/**
 * Icons for things that used to be shown as emoji. Domain code stores a short key (or, for
 * savings goals created before this, the old emoji); this maps it to a Material icon.
 */
object AppIcons {

    // ---- Savings goals -------------------------------------------------------------------

    val goalIcons: List<Pair<String, ImageVector>> = listOf(
        "savings" to Icons.Outlined.Savings,
        "target" to Icons.Outlined.Flag,
        "phone" to Icons.Outlined.Smartphone,
        "travel" to Icons.Outlined.Flight,
        "home" to Icons.Outlined.Home,
        "car" to Icons.Outlined.DirectionsCar,
        "education" to Icons.Outlined.School,
        "gem" to Icons.Outlined.Diamond
    )

    /** Emoji that earlier versions stored as a goal's icon, mapped to the matching key. */
    private val legacyGoalEmoji = mapOf(
        "💰" to "savings",      // money bag
        "🎯" to "target",       // direct hit
        "📱" to "phone",        // mobile phone
        "✈️" to "travel",       // airplane
        "🏠" to "home",         // house
        "🚗" to "car",          // car
        "🎓" to "education",    // graduation cap
        "💎" to "gem",          // gem
        "💻" to "laptop",       // laptop
        "🛡️" to "shield"  // shield
    )

    private val extraGoalIcons = mapOf(
        "laptop" to Icons.Outlined.Laptop,
        "shield" to Icons.Outlined.Shield
    )

    fun goalIcon(stored: String): ImageVector {
        val key = legacyGoalEmoji[stored] ?: stored
        return goalIcons.firstOrNull { it.first == key }?.second
            ?: extraGoalIcons[key]
            ?: Icons.Outlined.Savings
    }

    // ---- Achievements --------------------------------------------------------------------

    private val achievementIcons = mapOf(
        "forge" to Icons.Outlined.Construction,
        "hammer" to Icons.Outlined.Build,
        "gavel" to Icons.Outlined.Gavel,
        "trophy" to Icons.Outlined.EmojiEvents,
        "flame" to Icons.Outlined.LocalFireDepartment,
        "star" to Icons.Outlined.Star,
        "gem" to Icons.Outlined.Diamond,
        "crown" to Icons.Outlined.WorkspacePremium,
        "sparkle" to Icons.Outlined.AutoAwesome,
        "levelup" to Icons.Outlined.ArrowCircleUp,
        "coins" to Icons.Outlined.Paid,
        "bank" to Icons.Outlined.AccountBalance,
        "timer" to Icons.Outlined.Timer,
        "focus" to Icons.Outlined.TrackChanges,
        "bolt" to Icons.Outlined.Bolt,
        "sprout" to Icons.Outlined.Eco,
        "ice" to Icons.Outlined.AcUnit,
        "done" to Icons.Outlined.TaskAlt,
        "monster" to Icons.Outlined.BugReport,
        "hunter" to Icons.Outlined.GpsFixed,
        "boss" to Icons.Outlined.MilitaryTech,
        "scroll" to Icons.Outlined.MenuBook,
        "shield" to Icons.Outlined.Shield,
        "tree" to Icons.Outlined.AccountTree,
        "treasure" to Icons.Outlined.Savings,
        "hoard" to Icons.Outlined.Inventory2
    )

    fun achievementIcon(key: String): ImageVector = achievementIcons[key] ?: Icons.Outlined.EmojiEvents

    // ---- Gear and skills -----------------------------------------------------------------

    fun gearSlotIcon(slot: GearSlot): ImageVector = when (slot) {
        GearSlot.WEAPON -> Icons.Outlined.Gavel
        GearSlot.ARMOR -> Icons.Outlined.Shield
        GearSlot.ACCESSORY -> Icons.Outlined.Diamond
    }

    fun skillBranchIcon(branch: SkillBranch): ImageVector = when (branch) {
        SkillBranch.DISCIPLINE -> Icons.Outlined.FitnessCenter
        SkillBranch.WEALTH -> Icons.Outlined.Paid
        SkillBranch.FOCUS -> Icons.Outlined.SelfImprovement
        SkillBranch.GUARDIAN -> Icons.Outlined.Shield
    }

    val lootGift: ImageVector get() = Icons.Outlined.CardGiftcard
    val coins: ImageVector get() = Icons.Outlined.Paid
    val flame: ImageVector get() = Icons.Outlined.LocalFireDepartment
}
