package com.james.anvil.core

import com.james.anvil.data.Task
import com.james.anvil.data.TaskDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for DecisionEngine.
 * Tests the blocking logic based on tasks, penalties, and grace days.
 */
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
        
        // Default stubs for PenaltyManager methods
        whenever(penaltyManager.getLastSystemTime()).thenReturn(0L)
        whenever(penaltyManager.getLastElapsedRealtime()).thenReturn(0L)
        whenever(penaltyManager.isPenaltyActive()).thenReturn(false)
    }

    // ============================================
    // isBlocked() Tests
    // ============================================

    @Test
    fun `isBlocked should be false when no incomplete tasks exist`() = runTest {
        whenever(taskDao.countAllIncompleteNonDailyTasks()).thenReturn(0)

        val isBlocked = decisionEngine.isBlocked()

        assertFalse(isBlocked)
    }

    @Test
    fun `isBlocked should be true when penalty is active`() = runTest {
        whenever(taskDao.countAllIncompleteNonDailyTasks()).thenReturn(5)
        whenever(penaltyManager.isPenaltyActive()).thenReturn(true)
        whenever(taskDao.getTasksViolatingHardness(any())).thenReturn(emptyList())
        whenever(taskDao.getOverdueIncomplete(any())).thenReturn(emptyList())

        val isBlocked = decisionEngine.isBlocked()

        assertTrue(isBlocked)
    }

    @Test
    fun `isBlocked should be true when tasks violating hardness exist`() = runTest {
        whenever(taskDao.countAllIncompleteNonDailyTasks()).thenReturn(5)
        whenever(penaltyManager.isPenaltyActive()).thenReturn(false)
        whenever(taskDao.getTasksViolatingHardness(any())).thenReturn(
            listOf(Task(title = "Hard Task", deadline = 100L))
        )

        val isBlocked = decisionEngine.isBlocked()

        assertTrue(isBlocked)
    }

    @Test
    fun `isBlocked should be true when overdue tasks exist`() = runTest {
        whenever(taskDao.countAllIncompleteNonDailyTasks()).thenReturn(5)
        whenever(penaltyManager.isPenaltyActive()).thenReturn(false)
        whenever(taskDao.getTasksViolatingHardness(any())).thenReturn(emptyList())
        whenever(taskDao.getOverdueIncomplete(any())).thenReturn(
            listOf(Task(title = "Overdue Task", deadline = 100L))
        )

        val isBlocked = decisionEngine.isBlocked()

        assertTrue(isBlocked)
    }

    @Test
    fun `isBlocked should be false when tasks exist but not overdue or violating hardness`() = runTest {
        whenever(taskDao.countAllIncompleteNonDailyTasks()).thenReturn(5)
        whenever(penaltyManager.isPenaltyActive()).thenReturn(false)
        whenever(taskDao.getTasksViolatingHardness(any())).thenReturn(emptyList())
        whenever(taskDao.getOverdueIncomplete(any())).thenReturn(emptyList())

        val isBlocked = decisionEngine.isBlocked()

        assertFalse(isBlocked)
    }
}