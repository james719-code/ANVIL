package com.james.anvil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.james.anvil.data.BudgetType
import com.james.anvil.ui.AddBudgetEntrySheet
import com.james.anvil.ui.BudgetViewModel
import com.james.anvil.ui.theme.ANVILTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuickAddBudgetEntryActivity : ComponentActivity() {
    private val viewModel: BudgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val typeStr = intent.getStringExtra("type") ?: "EXPENSE"
        val initialType = try {
            BudgetType.valueOf(typeStr)
        } catch (e: Exception) {
            BudgetType.EXPENSE
        }

        setContent {
            ANVILTheme {
                AddBudgetEntrySheet(
                    onDismiss = { finish() },
                    onSave = { type, balanceType, amount, description, category, categoryType ->
                        viewModel.addBudgetEntry(
                            type = type,
                            balanceType = balanceType,
                            amount = amount,
                            description = description,
                            category = category,
                            categoryType = categoryType
                        )
                        finish()
                    },
                    initialType = initialType
                )
            }
        }
    }
}
