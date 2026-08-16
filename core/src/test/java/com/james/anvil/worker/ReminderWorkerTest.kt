package com.james.anvil.worker

import com.james.anvil.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderWorkerTest {

    @Test
    fun testFormatTimeRemaining() {
        assertEquals("45m", ReminderWorker.formatTimeRemaining(45 * 60 * 1000L))
        assertEquals("1h", ReminderWorker.formatTimeRemaining(60 * 60 * 1000L))
        assertEquals("1h 30m", ReminderWorker.formatTimeRemaining(90 * 60 * 1000L))
        assertEquals("2h", ReminderWorker.formatTimeRemaining(120 * 60 * 1000L))
    }

    @Test
    fun testNearDeadlineTriggerForSameDayTask() {
        val now = 1000000000L
        val createdAt = now - (30 * 60 * 1000L) // created 30m ago
        val deadline = now + (45 * 60 * 1000L)  // due in 45m (near deadline!)

        val task = Task(
            id = 1,
            title = "Submit Report",
            deadline = deadline,
            createdAt = createdAt
        )

        val action = ReminderWorker.evaluateReminderAction(
            task = task,
            now = now,
            isHalfwaySent = false,
            isNearDeadlineSent = false,
            isOverdueSent = false
        )

        assertEquals(ReminderWorker.ReminderStage.NEAR_DEADLINE, action)
    }

    @Test
    fun testNearDeadlineSuppressedIfAlreadySent() {
        val now = 1000000000L
        val createdAt = now - (30 * 60 * 1000L)
        val deadline = now + (45 * 60 * 1000L)

        val task = Task(
            id = 1,
            title = "Submit Report",
            deadline = deadline,
            createdAt = createdAt
        )

        val action = ReminderWorker.evaluateReminderAction(
            task = task,
            now = now,
            isHalfwaySent = false,
            isNearDeadlineSent = true, // already sent
            isOverdueSent = false
        )

        assertNull(action)
    }

    @Test
    fun testHalfwayTriggerForMultiHourTask() {
        val now = 1000000000L
        val createdAt = now - (3 * 3600 * 1000L) // created 3 hours ago
        val deadline = now + (3 * 3600 * 1000L)  // due in 3 hours (total 6 hours duration)
        // Now is exactly halfway (3 hours past created)

        val task = Task(
            id = 2,
            title = "Build Feature",
            deadline = deadline,
            createdAt = createdAt
        )

        val action = ReminderWorker.evaluateReminderAction(
            task = task,
            now = now,
            isHalfwaySent = false,
            isNearDeadlineSent = false,
            isOverdueSent = false
        )

        assertEquals(ReminderWorker.ReminderStage.HALFWAY, action)
    }

    @Test
    fun testOverdueTriggerWhenDeadlinePassed() {
        val now = 1000000000L
        val createdAt = now - (5 * 3600 * 1000L)
        val deadline = now - (15 * 60 * 1000L) // passed 15m ago

        val task = Task(
            id = 3,
            title = "Pay Bill",
            deadline = deadline,
            createdAt = createdAt
        )

        val action = ReminderWorker.evaluateReminderAction(
            task = task,
            now = now,
            isHalfwaySent = true,
            isNearDeadlineSent = true,
            isOverdueSent = false
        )

        assertEquals(ReminderWorker.ReminderStage.OVERDUE, action)
    }

    @Test
    fun testOverdueNotTriggeredIfOverdueForMoreThan24Hours() {
        val now = 1000000000L
        val createdAt = now - (48 * 3600 * 1000L)
        val deadline = now - (25 * 3600 * 1000L) // passed 25h ago

        val task = Task(
            id = 4,
            title = "Old Forgotten Task",
            deadline = deadline,
            createdAt = createdAt
        )

        val action = ReminderWorker.evaluateReminderAction(
            task = task,
            now = now,
            isHalfwaySent = true,
            isNearDeadlineSent = true,
            isOverdueSent = false
        )

        assertNull(action)
    }
}
