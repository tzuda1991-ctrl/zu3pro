package com.venice.tp3.shared

import kotlinx.serialization.json.*

data class Record(val tag: String, val body: JsonObject)

/**
 * Data Firebase Realtime DB me. Telegram sirf log + proof photo hosting —
 * kyunki bot dusre bot ka message getUpdates me nahi dekhta.
 */
class TgDb(private val prefs: Prefs, private val admin: Boolean) {

    private fun cfg() = prefs.config

    private fun pathOf(tag: String): String = when (tag) {
        "user" -> "users"
        "dep" -> "deps"
        "wdr" -> "wdrs"
        "cfg" -> "cfg"
        "hist" -> "hist"
        "ping" -> "ping"
        else -> tag
    }

    private fun tagOf(path: String): String? = when (path) {
        "users" -> "user"
        "deps" -> "dep"
        "wdrs" -> "wdr"
        "cfg" -> "cfg"
        "hist" -> "hist"
        "ping" -> "ping"
        else -> null
    }

    private fun log(tag: String, body: JsonObject) {
        val c = cfg()
        if (c.chatId.isBlank() || c.adminToken.isBlank()) return
        runCatching {
            Thread {
                runCatching { TgApi.sendMessage(c.adminToken, c.chatId, "#DB:$tag\n" + body.toString()) }
            }.start()
        }
    }

    fun save(tag: String, body: JsonObject): Boolean {
        cachePut(tag, body)
        val c = cfg()
        if (c.dbUrl.isBlank()) return false
        val id = body["id"]?.jsonPrimitive?.contentOrNull ?: return false
        val ok = Rtdb.patch(c.dbUrl, "${pathOf(tag)}/$id", body)
        log(tag, body)
        return ok
    }

    fun save(tag: String, body: JsonObject, @Suppress("UNUSED_PARAMETER") admin: Boolean): Boolean =
        save(tag, body)

    fun savePhoto(bytes: ByteArray, caption: String): String? {
        val c = cfg()
        return runCatching { TgApi.sendPhoto(c.adminToken, c.chatId, bytes, caption) }.getOrNull()
    }

    fun proofUrl(fileId: String): String? {
        val c = cfg()
        return TgApi.getFileUrl(c.adminToken, fileId)
            ?: TgApi.getFileUrl(c.publicToken, fileId)
    }

    private fun readCache(): LinkedHashMap<String, LinkedHashMap<String, JsonObject>> {
        val out = LinkedHashMap<String, LinkedHashMap<String, JsonObject>>()
        val root = runCatching { Json.parseToJsonElement(prefs.cache).jsonObject }.getOrNull()
            ?: return out
        for ((tag, v) in root) {
            val bucket = LinkedHashMap<String, JsonObject>()
            runCatching { v.jsonObject }.getOrNull()?.forEach { (id, b) ->
                runCatching { b.jsonObject }.getOrNull()?.let { bucket[id] = it }
            }
            out[tag] = bucket
        }
        return out
    }

    private fun writeCache(map: Map<String, Map<String, JsonObject>>) {
        prefs.cache = buildJsonObject {
            for ((tag, bucket) in map) {
                put(tag, buildJsonObject { for ((id, b) in bucket) put(id, b) })
            }
        }.toString()
    }

    private fun cachePut(tag: String, body: JsonObject) {
        val id = body["id"]?.jsonPrimitive?.contentOrNull ?: return
        val c = readCache()
        c.getOrPut(tag) { LinkedHashMap() }[id] = body
        writeCache(c)
    }

    fun load(): Map<String, Map<String, JsonObject>> {
        val map = readCache()
        val c = cfg()
        if (c.dbUrl.isBlank()) return map

        val root = runCatching { Rtdb.get(c.dbUrl, "")?.jsonObject }.getOrNull()
        if (root != null) {
            for ((p, v) in root) {
                val tag = tagOf(p) ?: continue
                if (tag == "cfg") {
                    runCatching { v.jsonObject }.getOrNull()?.let {
                        map.getOrPut("cfg") { LinkedHashMap() }["cfg"] = it
                    }
                } else {
                    runCatching { v.jsonObject }.getOrNull()?.forEach { (id, b) ->
                        runCatching { b.jsonObject }.getOrNull()?.let {
                            map.getOrPut(tag) { LinkedHashMap() }[id] = it
                        }
                    }
                }
            }
            writeCache(map)
        }

        for (b in map.values) {
            b.entries.removeAll { it.value["del"]?.jsonPrimitive?.booleanOrNull == true }
        }
        return map
    }

    fun load(@Suppress("UNUSED_PARAMETER") admin: Boolean): Map<String, Map<String, JsonObject>> = load()

    fun cached(tag: String, id: String): JsonObject? = readCache()[tag]?.get(id)

    fun resetOffsets() {
        prefs.offAdmin = 0L
        prefs.offPublic = 0L
    }

    fun pingDb(): String = Rtdb.ping(cfg().dbUrl)
}
