package com.venice.tp3.shared

import kotlinx.serialization.json.*

data class Record(val tag: String, val body: JsonObject)

class TgDb(private val prefs: Prefs) {

    private fun cfg() = prefs.config

    fun save(tag: String, body: JsonObject, admin: Boolean = false): Boolean {
        val c = cfg()
        val token = if (admin) c.adminToken else c.publicToken
        val text = "#DB:$tag\n" + body.toString()
        return runCatching {
            val res = TgApi.sendMessage(token, c.chatId, text) ?: return false
            res["ok"]?.jsonPrimitive?.booleanOrNull == true
        }.getOrDefault(false)
    }

    fun savePhoto(bytes: ByteArray, caption: String): String? =
        TgApi.sendPhoto(cfg().publicToken, cfg().chatId, bytes, caption)

    fun proofUrl(fileId: String): String? = TgApi.getFileUrl(cfg().adminToken, fileId)

    fun poll(admin: Boolean): List<Record> {
        val c = cfg()
        if (!c.ready) return emptyList()
        val token = if (admin) c.adminToken else c.publicToken
        var off = if (admin) prefs.offAdmin else prefs.offPublic
        val out = mutableListOf<Record>()
        repeat(3) {
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
                val parsed = runCatching { Json.parseToJsonElement(text.substring(nl + 1)).jsonObject }.getOrNull()
                    ?: continue
                out.add(Record(tag, parsed))
            }
            if (admin) prefs.offAdmin = off else prefs.offPublic = off
            if (arr.size() < 100) return out
        }
        return out
    }

    /** tag -> (id -> latest body). del=true hataya jaata hai. */
    fun load(admin: Boolean = false): Map<String, Map<String, JsonObject>> {
        val map = LinkedHashMap<String, LinkedHashMap<String, JsonObject>>()
        for (r in poll(admin)) {
            val id = r.body["id"]?.jsonPrimitive?.contentOrNull ?: continue
            val bucket = map.getOrPut(r.tag) { LinkedHashMap() }
            bucket[id] = r.body
        }
        for (b in map.values) b.entries.removeAll { it.value["del"]?.jsonPrimitive?.booleanOrNull == true }
        return map
    }

    fun snapshot(users: List<JsonObject>, cfgJson: JsonObject) {
        users.forEach { save("user", it, true) }
        save("cfg", cfgJson, true)
    }
}
