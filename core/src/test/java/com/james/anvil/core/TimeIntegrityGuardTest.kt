package com.james.anvil.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeIntegrityGuardTest {

    private val guard = TimeIntegrityGuard()

    @Test
    fun `reboot resetting elapsed clock does not look like manipulation`() {
        assertFalse(
            guard.isTimeManipulated(
                lastKnownSystemTime = 2_000_000L,
                lastKnownElapsed = 900_000L,
                currentSystemTime = 2_060_000L,
                currentElapsedRealtime = 10_000L
            )
        )
    }

    @Test
    fun `backward wall clock change is manipulation`() {
        assertTrue(
            guard.isTimeManipulated(
                lastKnownSystemTime = 2_000_000L,
                lastKnownElapsed = 900_000L,
                currentSystemTime = 1_999_999L,
                currentElapsedRealtime = 900_001L
            )
        )
    }

    @Test
    fun `normal elapsed and wall clock progress is accepted`() {
        assertFalse(
            guard.isTimeManipulated(
                lastKnownSystemTime = 2_000_000L,
                lastKnownElapsed = 900_000L,
                currentSystemTime = 2_030_000L,
                currentElapsedRealtime = 930_000L
            )
        )
    }

    @Test
    fun `large forward wall clock jump is manipulation`() {
        assertTrue(
            guard.isTimeManipulated(
                lastKnownSystemTime = 2_000_000L,
                lastKnownElapsed = 900_000L,
                currentSystemTime = 2_500_000L,
                currentElapsedRealtime = 930_000L
            )
        )
    }
}
