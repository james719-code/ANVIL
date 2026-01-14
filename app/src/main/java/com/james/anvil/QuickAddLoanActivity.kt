package com.james.anvil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.james.anvil.ui.AddLoanSheet
import com.james.anvil.ui.TaskViewModel
import com.james.anvil.ui.theme.ANVILTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuickAddLoanActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ANVILTheme {
                AddLoanSheet(
                    onDismiss = { finish() },
                    onSave = { borrowerName, amount, balanceType, interestRate, totalExpectedAmount, description, dueDate ->
                        viewModel.createLoan(
                            borrowerName = borrowerName,
                            amount = amount,
                            balanceType = balanceType,
                            interestRate = interestRate,
                            totalExpectedAmount = totalExpectedAmount,
                            description = description,
                            dueDate = dueDate
                        )
                        finish()
                    }
                )
            }
        }
    }
}
