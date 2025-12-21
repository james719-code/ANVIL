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

    @Test
    fun `should unblock when no tasks today or tomorrow`() = runBlocking {
        // Arrange
        whenever(taskDao.countActiveTodayTomorrow(any(), any())).thenReturn(0)
        whenever(penaltyManager.isPenaltyActive()).thenReturn(true) // Should clear penalty

        // Act
        val shouldBlock = decisionEngine.checkBlockingStatus()

        // Assert
        assertFalse(shouldBlock)
        verify(penaltyManager).clearPenalty()
    }

    @Test
    fun `should block when penalty is active`() = runBlocking {
        // Arrange
        whenever(taskDao.countActiveTodayTomorrow(any(), any())).thenReturn(5)
        whenever(penaltyManager.isPenaltyActive()).thenReturn(true)

        // Act
        val shouldBlock = decisionEngine.checkBlockingStatus()

        // Assert
        assertTrue(shouldBlock)
    }

    @Test
    fun `should block and start penalty when overdue tasks exist and no grace`() = runBlocking {
        // Arrange
        whenever(taskDao.countActiveTodayTomorrow(any(), any())).thenReturn(5)
        whenever(penaltyManager.isPenaltyActive()).thenReturn(false)
        whenever(taskDao.getOverdueIncomplete(any())).thenReturn(listOf(Task(title = "Late", deadline = 100L)))
        whenever(bonusManager.consumeGraceDay()).thenReturn(false)

        // Act
        val shouldBlock = decisionEngine.checkBlockingStatus()

        // Assert
        assertTrue(shouldBlock)
        verify(penaltyManager).startPenalty()
    }

    @Test
    fun `should unblock (temporarily) when overdue tasks exist but grace consumes`() = runBlocking {
        // Arrange
        whenever(taskDao.countActiveTodayTomorrow(any(), any())).thenReturn(5)
        whenever(penaltyManager.isPenaltyActive()).thenReturn(false)
        whenever(taskDao.getOverdueIncomplete(any())).thenReturn(listOf(Task(title = "Late", deadline = 100L)))
        whenever(bonusManager.consumeGraceDay()).thenReturn(true)

        // Act
        val shouldBlock = decisionEngine.checkBlockingStatus()

        // Assert
        assertFalse(shouldBlock)
        verify(penaltyManager, org.mockito.kotlin.times(0)).startPenalty()
    }
}
