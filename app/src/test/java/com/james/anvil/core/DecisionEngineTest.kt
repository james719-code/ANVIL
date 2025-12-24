package com.james.anvil.core

import com.james.anvil.data.Task
import com.james.anvil.data.TaskDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DecisionEngineTest {

    private lateinit var taskDao: TaskDao
    private lateinit var penaltyManager: PenaltyManager
    private lateinit var bonusManager: BonusManager
    private lateinit var decisionEngine: DecisionEngine

    @Before
    fun setup() {
        taskDao = mock()
        penaltyManager = mock()
        bonusManager = mock()
        decisionEngine = DecisionEngine(taskDao, penaltyManager, bonusManager)
    }

    // FIX: Changed from "= runBlocking" to "{ runBlocking { } }"
    @Test
    fun `isBlocked should be false when no tasks today or tomorrow`() {
        runBlocking {
            whenever(taskDao.countActiveTodayTomorrow(any(), any())).thenReturn(0)

            val isBlocked = decisionEngine.isBlocked()

            assertFalse(isBlocked)
        }
    }

    @Test
    fun `isBlocked should be true when penalty is active`() {
        runBlocking {
            whenever(taskDao.countActiveTodayTomorrow(any(), any())).thenReturn(5)
            whenever(penaltyManager.isPenaltyActive()).thenReturn(true)

            val isBlocked = decisionEngine.isBlocked()

            assertTrue(isBlocked)
        }
    }

    @Test
    fun `isBlocked should be true when tasks exist even without penalty`() {
        runBlocking {
            whenever(taskDao.countActiveTodayTomorrow(any(), any())).thenReturn(5)
            whenever(penaltyManager.isPenaltyActive()).thenReturn(false)

            val isBlocked = decisionEngine.isBlocked()

            assertTrue(isBlocked)
        }
    }

    @Test
    fun `updateState should start penalty when overdue tasks exist and no grace`() {
        runBlocking {
            whenever(taskDao.countActiveTodayTomorrow(any(), any())).thenReturn(5)
            whenever(penaltyManager.isPenaltyActive()).thenReturn(false)
            whenever(taskDao.getOverdueIncomplete(any())).thenReturn(listOf(Task(title = "Late", deadline = 100L)))
            whenever(bonusManager.consumeGraceDay()).thenReturn(false)

            decisionEngine.updateState()

            verify(penaltyManager).startPenalty()
        }
    }

    @Test
    fun `updateState should consume grace and NOT start penalty when overdue tasks exist and grace available`() {
        runBlocking {
            whenever(taskDao.countActiveTodayTomorrow(any(), any())).thenReturn(5)
            whenever(penaltyManager.isPenaltyActive()).thenReturn(false)
            whenever(taskDao.getOverdueIncomplete(any())).thenReturn(listOf(Task(title = "Late", deadline = 100L)))
            whenever(bonusManager.consumeGraceDay()).thenReturn(true)

            decisionEngine.updateState()

            verify(penaltyManager, never()).startPenalty()
            verify(bonusManager).consumeGraceDay()
        }
    }

    @Test
    fun `updateState should clear penalty if no tasks exist`() {
        runBlocking {
            whenever(taskDao.countActiveTodayTomorrow(any(), any())).thenReturn(0)
            whenever(penaltyManager.isPenaltyActive()).thenReturn(true)

            decisionEngine.updateState()

            verify(penaltyManager).clearPenalty()
        }
    }
}