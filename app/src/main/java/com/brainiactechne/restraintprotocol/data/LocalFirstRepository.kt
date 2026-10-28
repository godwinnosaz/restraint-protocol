package com.brainiactechne.restraintprotocol.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONObject
import java.util.UUID

class LocalFirstRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = RpDatabase.get(appContext).dao()

    suspend fun loadCommitment() = dao.getCommitment()
    fun observeCommitment() = dao.observeCommitment()
    fun observePendingCount() = dao.observePendingCount()

    suspend fun saveCommitment(durationMinutes: Int, deadlineEpochMillis: Long) {
        require(durationMinutes in listOf(5, 15, 30, 60, 180))
        val updatedAt = System.currentTimeMillis()
        val commitment = CommitmentEntity(
            selectedDurationMinutes = durationMinutes,
            activeDeadlineEpochMillis = deadlineEpochMillis,
            updatedAtEpochMillis = updatedAt
        )
        val payload = JSONObject()
            .put("selectedDurationMinutes", durationMinutes)
            .put("activeDeadlineEpochMillis", deadlineEpochMillis)
            .put("updatedAtEpochMillis", updatedAt)
            .toString()
        dao.saveCommitmentAndQueue(
            commitment,
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                eventType = "commitment.updated",
                payloadJson = payload,
                createdAtEpochMillis = updatedAt
            )
        )
        scheduleSync()
    }

    suspend fun recordIntervention(outcome: String, durationMinutes: Int) {
        require(outcome in setOf("warning_shown", "restriction_started", "restriction_completed", "user_returned_to_safety"))
        val createdAt = System.currentTimeMillis()
        val payload = JSONObject()
            .put("outcome", outcome)
            .put("durationMinutes", durationMinutes)
            .put("createdAtEpochMillis", createdAt)
            .toString()
        dao.insertOutbox(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                eventType = "intervention.event",
                payloadJson = payload,
                createdAtEpochMillis = createdAt
            )
        )
        scheduleSync()
    }

    fun scheduleSync() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = OneTimeWorkRequestBuilder<OutboxSyncWorker>().setConstraints(constraints).build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            OutboxSyncWorker.UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request
        )
    }
}
