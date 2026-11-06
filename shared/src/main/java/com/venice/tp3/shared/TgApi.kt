package com.venice.tp3.shared

import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object TgApi {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()

    private const val BASE = "https://api.telegram.org/bot"
    private val JSON_MT = "application/json".toMediaType()

    private fun post(method: String, token: String, body: RequestBody): JsonObject? {
        if (token.isBlank()) return null
        val req = Request.Builder().url(BASE + token + "/" + method).post(body).build()
        return runCatching {
            http.newCall(req).execute().use { r ->
                Json.parseToJsonElement(r.body?.string() ?: "{}").jsonObject
            }
        }.getOrNull()
    }

    fun sendMessage(token: String, chatId: String, text: String): JsonObject? {
        val payload = buildJsonObject {
            put("chat_id", chatId)
            put("text", text)
            put("disable_web_page_preview", true)
        }
        return post("sendMessage", token, payload.toString().toRequestBody(JSON_MT))
    }

    fun sendPhoto(token: String, chatId: String, bytes: ByteArray, caption: String): String? {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("caption", caption)
            .addFormDataPart("photo", "proof.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
            .build()
        val res = post("sendPhoto", token, body) ?: return null
        val ok = res["ok"]?.jsonPrimitive?.booleanOrNull ?: false
        if (!ok) return null
        return res["result"]?.jsonObject?.get("photo")?.jsonArray
            ?.lastOrNull()?.jsonObject?.get("file_id")?.jsonPrimitive?.contentOrNull
    }

    fun getUpdates(token: String, offset: Long, limit: Int = 100): JsonObject? {
        val url = BASE + token + "/getUpdates?offset=$offset&limit=$limit&timeout=0&allowed_updates=%5B%22message%22%5D"
        val req = Request.Builder().url(url).get().build()
        return runCatching {
            http.newCall(req).execute().use { r ->
                Json.parseToJsonElement(r.body?.string() ?: "{}").jsonObject
            }
        }.getOrNull()
    }

    fun getFileUrl(token: String, fileId: String): String? {
        val res = post("getFile", token, buildJsonObject { put("file_id", fileId) }.toString().toRequestBody(JSON_MT))
            ?: return null
        val path = res["result"]?.jsonObject?.get("file_path")?.jsonPrimitive?.contentOrNull ?: return null
        return "https://api.telegram.org/file/bot$token/$path"
    }

    fun ping(token: String): String {
        val res = post("getMe", token, "{}".toRequestBody(JSON_MT))
        val ok = res?.get("ok")?.jsonPrimitive?.booleanOrNull ?: false
        return if (ok) "OK @" + (res["result"]?.jsonObject?.get("username")?.jsonPrimitive?.contentOrNull ?: "bot")
        else "FAIL " + (res?.get("description")?.jsonPrimitive?.contentOrNull ?: "network")
    }
}
