package com.james.anvil.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.james.anvil.QuickAddBudgetEntryActivity
import com.james.anvil.QuickAddLoanActivity
import com.james.anvil.R

/**
 * Provides and manages dynamic app shortcuts.
 * Centralizes shortcut creation logic for easier maintenance.
 */
object ShortcutProvider {
    
    /**
     * Sets up all dynamic shortcuts for the app.
     * Should be called during app initialization.
     */
    fun setupShortcuts(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            shortcutManager.dynamicShortcuts = buildShortcuts(context)
        }
    }
    
    /**
     * Builds the list of dynamic shortcuts.
     */
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun buildShortcuts(context: Context): List<ShortcutInfo> {
        return listOf(
            buildExpenseShortcut(context),
            buildIncomeShortcut(context),
            buildLoanShortcut(context)
        )
    }
    
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun buildExpenseShortcut(context: Context): ShortcutInfo {
        return ShortcutInfo.Builder(context, SHORTCUT_ID_EXPENSE)
            .setShortLabel(context.getString(R.string.shortcut_add_expense))
            .setLongLabel(context.getString(R.string.shortcut_add_expense_long))
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(Intent(context, QuickAddBudgetEntryActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(EXTRA_TYPE, TYPE_EXPENSE)
            })
            .build()
    }
    
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun buildIncomeShortcut(context: Context): ShortcutInfo {
        return ShortcutInfo.Builder(context, SHORTCUT_ID_INCOME)
            .setShortLabel(context.getString(R.string.shortcut_add_income))
            .setLongLabel(context.getString(R.string.shortcut_add_income_long))
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(Intent(context, QuickAddBudgetEntryActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(EXTRA_TYPE, TYPE_INCOME)
            })
            .build()
    }
    
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun buildLoanShortcut(context: Context): ShortcutInfo {
        return ShortcutInfo.Builder(context, SHORTCUT_ID_LOAN)
            .setShortLabel(context.getString(R.string.shortcut_add_loan))
            .setLongLabel(context.getString(R.string.shortcut_add_loan_long))
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(Intent(context, QuickAddLoanActivity::class.java).apply {
                action = Intent.ACTION_VIEW
            })
            .build()
    }
    
    /**
     * Reports shortcut usage for better ranking.
     */
    fun reportShortcutUsed(context: Context, shortcutId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            shortcutManager.reportShortcutUsed(shortcutId)
        }
    }
    
    /**
     * Checks if pinned shortcuts are supported.
     */
    fun isPinnedShortcutSupported(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            shortcutManager.isRequestPinShortcutSupported
        } else {
            false
        }
    }
    
    // Shortcut IDs
    const val SHORTCUT_ID_EXPENSE = "add_expense"
    const val SHORTCUT_ID_INCOME = "add_income"
    const val SHORTCUT_ID_LOAN = "add_loan"
    
    // Intent extras
    const val EXTRA_TYPE = "type"
    const val TYPE_EXPENSE = "EXPENSE"
    const val TYPE_INCOME = "INCOME"
}
