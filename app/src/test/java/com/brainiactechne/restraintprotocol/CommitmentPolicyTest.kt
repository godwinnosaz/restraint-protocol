package com.brainiactechne.restraintprotocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitmentPolicyTest {
    @Test
    fun acceptsOnlyConfiguredDurations() {
        assertTrue(CommitmentPolicy.isAllowedDuration(15))
        assertTrue(CommitmentPolicy.isAllowedDuration(1440))
        assertTrue(CommitmentPolicy.isAllowedDuration(4320))
        assertFalse(CommitmentPolicy.isAllowedDuration(7))
        assertFalse(CommitmentPolicy.isAllowedDuration(2880))
    }

    @Test
    fun convertsMinutesToMilliseconds() {
        assertEquals(900_000L, CommitmentPolicy.durationMillis(15))
        assertEquals(3_600_000L, CommitmentPolicy.durationMillis(60))
        assertEquals(259_200_000L, CommitmentPolicy.durationMillis(4320))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedDuration() {
        CommitmentPolicy.durationMillis(10)
    }

    @Test
    fun remainingTimeNeverBecomesNegative() {
        assertEquals(5_000L, CommitmentPolicy.remainingMillis(15_000L, 10_000L))
        assertEquals(0L, CommitmentPolicy.remainingMillis(10_000L, 15_000L))
    }

    @Test
    fun expiresAtDeadline() {
        assertFalse(CommitmentPolicy.isExpired(10_001L, 10_000L))
        assertTrue(CommitmentPolicy.isExpired(10_000L, 10_000L))
        assertTrue(CommitmentPolicy.isExpired(9_999L, 10_000L))
    }
}
