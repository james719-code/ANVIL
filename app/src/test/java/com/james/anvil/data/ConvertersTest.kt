package com.james.anvil.data

import org.junit.Assert.*
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `test fromStepList and toStepList`() {
        val steps = listOf(
            TaskStep(title = "Step 1", isCompleted = false),
            TaskStep(title = "Step 2", isCompleted = true)
        )

        val json = converters.fromStepList(steps)
        val result = converters.toStepList(json)

        assertEquals(steps.size, result.size)
        assertEquals(steps[0].title, result[0].title)
        assertEquals(steps[1].isCompleted, result[1].isCompleted)
    }

    @Test
    fun `test empty list`() {
        val json = converters.fromStepList(emptyList())
        val result = converters.toStepList(json)
        assertTrue(result.isEmpty())
    }
}
