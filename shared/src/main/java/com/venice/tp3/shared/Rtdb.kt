package com.venice.tp3.shared

import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Firebase Realtime Database REST client. Bot API ki tarah bot-to-bot blind nahi. */
object Rtdb {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json".toMediaType()

    private fun url(root: String, path: String): String {
        val b = root.trim().trimEnd('/')
        val p = if (path.isBlank()) "" else "/" + path.trim('/')
        return b + p + ".json"
    }

    fun get(root: String, path: String = ""): JsonElement? {
        if (root.isBlank()) return null
        return runCatching {
            val req = Request.Builder().url(url(root, path)).get().build()
            http.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return null
                val body = r.body?.string() ?: "null"
                if (body.isBlank() || body == "null") null else Json.parseToJsonElement(body)
            }
        }.getOrNull()
    }

    fun patch(root: String, path: String, body: JsonObject): Boolean {
        if (root.isBlank() || path.isBlank()) return false
        return runCatching {
            val req = Request.Builder()
                .url(url(root, path))
                .patch(body.toString().toRequestBody(JSON))
                .build()
            http.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    fun delete(root: String, path: String): Boolean {
        if (root.isBlank() || path.isBlank()) return false
        return runCatching {
            val req = Request.Builder().url(url(root, path)).delete().build()
            http.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    fun ping(root: String): String {
        if (root.isBlank()) return "NO URL"
        val r = get(root, "")
        return if (r != null) "OK" else "FAIL"
    }
}
