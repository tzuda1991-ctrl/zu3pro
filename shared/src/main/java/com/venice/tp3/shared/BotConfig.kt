package com.venice.tp3.shared

import android.content.Context
import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BotConfig(
    val adminToken: String = "",
    val publicToken: String = "",
    val chatId: String = ""
) {
    val ready: Boolean
        get() = adminToken.isNotBlank() && publicToken.isNotBlank() && chatId.isNotBlank()

    fun encode(): String = Base64.encodeToString(
        json.encodeToString(serializer(), this).toByteArray(),
        Base64.NO_WRAP
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun decode(s: String): BotConfig? = runCatching {
            json.decodeFromString(serializer(), String(Base64.decode(s.trim(), Base64.DEFAULT)))
        }.getOrNull()
    }
}

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("tp3", Context.MODE_PRIVATE)

    var config: BotConfig
        get() = sp.getString("cfg", null)?.let { BotConfig.decode(it) } ?: BotConfig()
        set(v) { sp.edit().putString("cfg", v.encode()).apply() }

    var offAdmin: Long
        get() = sp.getLong("offA", 0L)
        set(v) { sp.edit().putLong("offA", v).apply() }

    var offPublic: Long
        get() = sp.getLong("offP", 0L)
        set(v) { sp.edit().putLong("offP", v).apply() }

    var meId: String
        get() = sp.getString("me", "") ?: ""
        set(v) { sp.edit().putString("me", v).apply() }

    var adminOk: Boolean
        get() = sp.getBoolean("adm", false)
        set(v) { sp.edit().putBoolean("adm", v).apply() }

    /** local mirror of the Telegram log — network down ho to bhi data rehta hai */
    var cache: String
        get() = sp.getString("cache", "{}") ?: "{}"
        set(v) { sp.edit().putString("cache", v).apply() }

    fun clearCache() {
        sp.edit().putString("cache", "{}").apply()
    }
}
