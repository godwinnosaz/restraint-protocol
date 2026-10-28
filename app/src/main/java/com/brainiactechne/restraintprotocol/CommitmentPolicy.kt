package com.brainiactechne.restraintprotocol

/**
 * Pure policy functions for the first RP milestone.
 * This class does not enforce restrictions outside the RP app.
 */
object CommitmentPolicy {
    val allowedDurationsMinutes = listOf(5, 15, 30, 60, 180)

    fun isAllowedDuration(minutes: Int): Boolean =
        minutes in allowedDurationsMinutes

    fun durationMillis(minutes: Int): Long {
        require(isAllowedDuration(minutes)) {
            "Unsupported restriction duration: $minutes minutes"
        }
        return minutes * 60_000L
    }

    fun remainingMillis(deadlineMillis: Long, nowMillis: Long): Long =
        (deadlineMillis - nowMillis).coerceAtLeast(0L)

    fun isExpired(deadlineMillis: Long, nowMillis: Long): Boolean =
        nowMillis >= deadlineMillis
}
