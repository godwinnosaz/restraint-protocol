package com.brainiactechne.restraintprotocol.data

import com.brainiactechne.restraintprotocol.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AuthApi {
    suspend fun authenticate(username: String?, email: String, password: String, register: Boolean): String =
        withContext(Dispatchers.IO) {
            val baseUrl = BuildConfig.RP_SYNC_BASE_URL.trim().trimEnd('/')
            require(baseUrl.startsWith("https://")) {
                "Configure an HTTPS API URL in the rpSyncBaseUrl Gradle property first."
            }
            val path = if (register) "/auth/register" else "/auth/login"
            val body = JSONObject().put("email", email).put("password", password)
            if (register) body.put("username", username.orEmpty())

            val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val response = runCatching { JSONObject(responseText) }.getOrNull()
                    ?: throw IllegalStateException("The server returned an unreadable response.")
                if (status !in 200..299 || !response.optBoolean("success")) {
                    throw IllegalStateException(response.optString("message").ifBlank { "Sign-in failed (HTTP $status)." })
                }
                response.optJSONObject("data")?.optString("token")
                    ?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("The server response did not include an access token.")
            } finally {
                connection.disconnect()
            }
        }
}
