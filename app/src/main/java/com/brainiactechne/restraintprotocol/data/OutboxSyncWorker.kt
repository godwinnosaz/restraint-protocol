package com.brainiactechne.restraintprotocol.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.brainiactechne.restraintprotocol.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends only explicitly queued commitment metadata and intervention events.
 * Private images, messages, browsing history and page contents are never collected here.
 */
class OutboxSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val baseUrl = BuildConfig.RP_SYNC_BASE_URL.trim().trimEnd('/')
        if (baseUrl.isBlank()) return Result.success()

        val authToken = applicationContext
            .getSharedPreferences("rp_auth", Context.MODE_PRIVATE)
            .getString("access_token", null)
            ?.takeIf { it.isNotBlank() }
            ?: return Result.success() // Keep events local until the user signs in.

        val dao = RpDatabase.get(applicationContext).dao()
        val batch = dao.pendingOutbox(limit = 50)
        if (batch.isEmpty()) return Result.success()

        var connection: HttpURLConnection? = null
        return try {
            val events = JSONArray()
            batch.forEach { event ->
                events.put(JSONObject()
                    .put("id", event.id)
                    .put("type", event.eventType)
                    .put("createdAtEpochMillis", event.createdAtEpochMillis)
                    .put("payload", JSONObject(event.payloadJson)))
            }
            connection = (URL("$baseUrl/api/v1/sync/batch").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $authToken")
            }
            val body = JSONObject().put("schemaVersion", 1).put("events", events)
                .toString().toByteArray(Charsets.UTF_8)
            connection.outputStream.use { it.write(body) }

            val status = connection.responseCode
            if (status == 429 || status >= 500) return Result.retry()
            if (status !in 200..299) return Result.failure()

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val accepted = JSONObject(responseText).optJSONArray("acceptedIds") ?: return Result.retry()
            val batchIds = batch.map { it.id }.toSet()
            val acceptedIds = buildList {
                for (index in 0 until accepted.length()) {
                    accepted.optString(index).takeIf { it in batchIds }?.let(::add)
                }
            }.distinct()

            if (acceptedIds.isNotEmpty()) dao.markSynced(acceptedIds, System.currentTimeMillis())
            if (acceptedIds.size == batch.size) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "rp-outbox-sync"
    }
}
