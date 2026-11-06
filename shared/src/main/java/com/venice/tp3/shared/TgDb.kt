package com.venice.tp3.shared

import kotlinx.serialization.json.*

data class Record(val tag: String, val body: JsonObject)

/**
 * Telegram group = append-only DB.
 *
 *  1. Ek bot = ek poller. Admin app sirf adminToken poll karta hai, user app sirf
 *     publicToken. getUpdates ka offset destructive hai, isliye ek hi bot ko do app
 *     poll karein to ek dusre ka data kha jaate hain.
 *  2. Jo record dusri app ko dikhna chahiye wo dono bots pe mirror hota hai.
 *     Sirf apna bot padha jaata hai, isliye offset confirm safe hai.
 *  3. Har record local cache me bhi jaata hai, isliye khaali poll / 24h retention se
 *     UI blank nahi hota.
 */
class TgDb(private val prefs: Prefs, private val admin: Boolean) {

    private fun cfg() = prefs.config

    /** ye tags dono app ko chahiye — inhe mirror karte hain */
    private val shared = setOf("user", "dep", "wdr", "cfg")

    // ---------------- write ----------------

    private fun write(token: String, tag: String, body: JsonObject): Boolean {
        if (token.isBlank()) return false
        return runCatching {
            val res = TgApi.sendMessage(token, cfg().chatId, "#DB:$tag\n" + body.toString()) ?: return false
            res["ok"]?.jsonPrimitive?.booleanOrNull == true
        }.getOrDefault(false)
    }

    fun save(tag: String, body: JsonObject): Boolean {
        cachePut(tag, body)
        val c = cfg()
        if (!c.ready) return false
        val a = write(c.adminToken, tag, body)
        val p = if (tag in shared && c.publicToken.isNotBlank())
            write(c.publicToken, tag, body) else false
        return a || p
    }

    /** purane call sites ke liye — flag ignore hota hai, bot class-level decide hai */
    fun save(tag: String, body: JsonObject, @Suppress("UNUSED_PARAMETER") admin: Boolean): Boolean =
        save(tag, body)

    fun savePhoto(bytes: ByteArray, caption: String): String? {
        val c = cfg()
        return runCatching { TgApi.sendPhoto(c.adminToken, c.chatId, bytes, caption) }.getOrNull()
            ?: runCatching { TgApi.sendPhoto(c.publicToken, c.chatId, bytes, caption) }.getOrNull()
    }

    fun proofUrl(fileId: String): String? {
        val c = cfg()
        return TgApi.getFileUrl(c.adminToken, fileId) ?: TgApi.getFileUrl(c.publicToken, fileId)
    }

    // ---------------- cache ----------------

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

    // ---------------- read ----------------

    fun poll(): List<Record> {
        val c = cfg()
        if (!c.ready) return emptyList()
        val token = if (admin) c.adminToken else c.publicToken
        var off = if (admin) prefs.offAdmin else prefs.offPublic
        val out = mutableListOf<Record>()
        repeat(4) {
            val res = TgApi.getUpdates(token, off) ?: return out
            if (res["ok"]?.jsonPrimitive?.booleanOrNull != true) return out
            val arr = res["result"]?.jsonArray ?: return out
            if (arr.isEmpty()) return out
            for (e in arr) {
                val o = e.jsonObject
                val uid = o["update_id"]?.jsonPrimitive?.longOrNull ?: continue
                off = maxOf(off, uid + 1)
                val msg = o["message"]?.jsonObject ?: continue
                val chat = msg["chat"]?.jsonObject
                val cid = chat?.get("id")?.jsonPrimitive?.longOrNull?.toString()
                val uname = chat?.get("username")?.jsonPrimitive?.contentOrNull
                val want = c.chatId.trim()
                if (cid != want && (uname == null || "@$uname" != want)) continue
                val text = msg["text"]?.jsonPrimitive?.contentOrNull
                    ?: msg["caption"]?.jsonPrimitive?.contentOrNull ?: continue
                if (!text.startsWith("#DB:")) continue
                val nl = text.indexOf('\n')
                if (nl < 0) continue
                val tag = text.substring(4, nl).trim()
                val parsed = runCatching {
                    Json.parseToJsonElement(text.substring(nl + 1)).jsonObject
                }.getOrNull() ?: continue
                out.add(Record(tag, parsed))
            }
            if (admin) prefs.offAdmin = off else prefs.offPublic = off
            if (arr.size < 100) return out
        }
        return out
    }

    /** tag -> (id -> latest body). Cache + naye records merge hote hain. */
    fun load(): Map<String, Map<String, JsonObject>> {
        val map = readCache()
        var changed = false
        for (r in poll()) {
            val id = r.body["id"]?.jsonPrimitive?.contentOrNull ?: continue
            map.getOrPut(r.tag) { LinkedHashMap() }[id] = r.body
            changed = true
        }
        for (b in map.values) {
            if (b.entries.removeAll { it.value["del"]?.jsonPrimitive?.booleanOrNull == true }) changed = true
        }
        if (changed) writeCache(map)
        return map
    }

    /** purane call sites ke liye — flag ignore hota hai */
    fun load(@Suppress("UNUSED_PARAMETER") admin: Boolean): Map<String, Map<String, JsonObject>> = load()

    /** network ke bina, seedha cache se — balance edit ke liye */
    fun cached(tag: String, id: String): JsonObject? = readCache()[tag]?.get(id)

    fun resetOffsets() {
        prefs.offAdmin = 0L
        prefs.offPublic = 0L
    }
}
