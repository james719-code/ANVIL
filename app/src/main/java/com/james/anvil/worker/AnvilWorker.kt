package com.james.anvil.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.james.anvil.core.BonusManager
import com.james.anvil.core.DecisionEngine
import com.james.anvil.core.PenaltyManager
import com.james.anvil.data.AnvilDatabase

class AnvilWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AnvilDatabase.getDatabase(applicationContext)
        val penaltyManager = PenaltyManager(applicationContext)
        val bonusManager = BonusManager(applicationContext)
        val decisionEngine = DecisionEngine(db.taskDao(), penaltyManager, bonusManager)

        
        decisionEngine.updateState()
        
        return Result.success()
    }
}
