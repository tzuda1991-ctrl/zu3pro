package com.venice.tp3.shared

import kotlinx.serialization.json.*
import kotlin.concurrent.thread

/**
 * Admin bot = poora control panel Telegram ke andar.
 * Sirf locked admin chat (aur group) se commands lete hain.
 */
class AdminBot(private val prefs: Prefs, private val repo: Repo) {

    @Volatile
    private var running = false
    private var worker: Thread? = null

    private val token get() = prefs.config.adminToken
    private val group get() = prefs.config.chatId
    private val owner get() = prefs.adminChatId

    fun start() {
        if (running || token.isBlank()) return
        running = true
        worker = thread(name = "admin-bot", isDaemon = true) {
            var off = 0L
            while (running) {
                val res = TgApi.getUpdates(token, off, 50, "%5B%22message%22%2C%22callback_query%22%5D")
                if (res?.get("ok")?.jsonPrimitive?.booleanOrNull == true) {
                    val arr = res["result"]?.jsonArray
                    if (arr != null && arr.isNotEmpty()) {
                        for (e in arr) {
                            val o = e.jsonObject
                            o["update_id"]?.jsonPrimitive?.longOrNull?.let { off = maxOf(off, it + 1) }
                            runCatching {
                                o["message"]?.jsonObject?.let { onMessage(it) }
                                o["callback_query"]?.jsonObject?.let { onCallback(it) }
                            }
                        }
                        continue
                    }
                }
                Thread.sleep(1500)
            }
        }
    }

    fun stop() {
        running = false
        worker = null
    }

    // ---------------- helpers ----------------

    private fun say(chat: String, text: String, markup: JsonObject? = null) {
        TgApi.sendMessage(token, chat, text, markup)
    }

    private fun kb(rows: List<List<Pair<String, String>>>): JsonObject =
        buildJsonObject {
            put("inline_keyboard", buildJsonArray {
                rows.forEach { row ->
                    add(buildJsonArray {
                        row.forEach { (label, data) ->
                            add(buildJsonObject {
                                put("text", label)
                                put("callback_data", data)
                            })
                        }
                    })
                }
            })
        }

    private fun findUser(q: String): User? =
        repo.allUsers().firstOrNull {
            it.id == q || it.phone == q || it.refCode.equals(q, true)
        }

    private fun help(): String = """
ADMIN COMMANDS

/stats  — total users, balance, pending
/users  — user list
/find <phone|id>  — user card
/bal <phone> <₹>  — + / - (minus ke liye -500)
/set <phone> <₹>  — exact balance set
/ban <phone>  /unban <phone>
/deps  — pending deposits
/wdrs  — pending withdraws
/cfg  — table config dekho
/setcfg <key> <value>
  keys: boot minbuy maxbuy players comm upi payee mindep minwdr demo demotopup
/ping  — bot + db check
/whoami  — chat id
""".trimIndent()

    // ---------------- message ----------------

    private fun onMessage(m: JsonObject) {
        val chat = m["chat"]?.jsonObject ?: return
        val chatId = chat["id"]?.jsonPrimitive?.longOrNull?.toString() ?: return
        val text = m["text"]?.jsonPrimitive?.contentOrNull?.trim() ?: return

        if (owner.isBlank()) {
            if (!text.startsWith("/start")) {
                say(chatId, "Pehle /start bhejo")
                return
            }
            prefs.adminChatId = chatId
            say(chatId, "Locked. Ye chat ab admin control hai.\n\n" + help())
            return
        }
        if (chatId != owner && chatId != group) {
            say(chatId, "Ye chat authorized nahi hai.")
            return
        }
        if (!text.startsWith("/")) return

        val parts = text.removePrefix("/").split(" ").filter { it.isNotBlank() }
        val cmd = parts.firstOrNull()?.lowercase() ?: return
        val args = parts.drop(1)

        when (cmd) {
            "start", "help" -> say(chatId, help())
            "whoami" -> say(chatId, "chat id: $chatId")
            "ping" -> say(chatId, "BOT: ${TgApi.ping(token)} | DB: ${repo.db.pingDb()}")
            "stats" -> say(chatId, statsText())
            "users" -> say(chatId, usersText(0), kb(pageKb(0)))
            "find" -> {
                val u = args.firstOrNull()?.let { findUser(it) }
                if (u == null) say(chatId, "User nahi mila") else say(chatId, userText(u), kb(userKb(u)))
            }
            "bal" -> {
                if (args.size < 2) { say(chatId, "Use: /bal 9876543210 500"); return }
                val u = findUser(args[0]) ?: run { say(chatId, "User nahi mila"); return }
                val rupees = args[1].toLongOrNull() ?: run { say(chatId, "Amount galat"); return }
                val ok = repo.adjust(u.id, rupees * 100L, "tg")
                val now = repo.refreshUser(u.id)?.balance ?: 0L
                say(chatId, if (ok) "${u.name}: ${signed(rupees)} → ${money(now)}" else "Fail — DB check karo")
            }
            "set" -> {
                if (args.size < 2) { say(chatId, "Use: /set 9876543210 1000"); return }
                val u = findUser(args[0]) ?: run { say(chatId, "User nahi mila"); return }
                val rupees = args[1].toLongOrNull() ?: run { say(chatId, "Amount galat"); return }
                val ok = repo.setBalance(u.id, rupees * 100L)
                say(chatId, if (ok) "${u.name} balance = ${money(rupees * 100L)}" else "Fail")
            }
            "ban", "unban" -> {
                val u = args.firstOrNull()?.let { findUser(it) }
                if (u == null) { say(chatId, "User nahi mila"); return }
                repo.setStatus(u.id, if (cmd == "ban") "BANNED" else "ACTIVE")
                say(chatId, "${u.name} → ${if (cmd == "ban") "BANNED" else "ACTIVE"}")
            }
            "deps" -> sendDeps(chatId)
            "wdrs" -> sendWdrs(chatId)
            "cfg" -> say(chatId, cfgText())
            "setcfg" -> {
                if (args.size < 2) { say(chatId, "Use: /setcfg boot 1000"); return }
                say(chatId, setCfg(args[0], args[1]))
            }
            else -> say(chatId, "Unknown command. /help")
        }
    }

    // ---------------- callback ----------------

    private fun onCallback(c: JsonObject) {
        val id = c["id"]?.jsonPrimitive?.contentOrNull ?: return
        val data = c["data"]?.jsonPrimitive?.contentOrNull ?: return
        val msg = c["message"]?.jsonObject ?: return
        val chatId = msg["chat"]?.jsonObject?.get("id")?.jsonPrimitive?.longOrNull?.toString() ?: return
        val mid = msg["message_id"]?.jsonPrimitive?.longOrNull ?: 0L
        if (chatId != owner && chatId != group) {
            TgApi.answerCallback(token, id, "Unauthorized")
            return
        }

        val p = data.split(":")
        when (p.getOrNull(0)) {
            "p" -> {
                val page = p.getOrNull(1)?.toIntOrNull() ?: 0
                TgApi.answerCallback(token, id)
                TgApi.editMessage(token, chatId, mid, usersText(page), kb(pageKb(page)))
            }
            "u" -> {
                val u = p.getOrNull(1)?.let { findUser(it) }
                TgApi.answerCallback(token, id)
                if (u == null) TgApi.editMessage(token, chatId, mid, "User nahi mila")
                else TgApi.editMessage(token, chatId, mid, userText(u), kb(userKb(u)))
            }
            "b" -> {
                val steps = p.getOrNull(1)?.toIntOrNull() ?: 1
                val u = p.getOrNull(2)?.let { findUser(it) }
                if (u == null) { TgApi.answerCallback(token, id, "User nahi mila"); return }
                val delta = 10000L * steps
                val ok = repo.adjust(u.id, delta, "tg")
                TgApi.answerCallback(token, id, if (ok) signed(delta / 100) else "Fail")
                val f = repo.refreshUser(u.id) ?: u
                TgApi.editMessage(token, chatId, mid, userText(f), kb(userKb(f)))
            }
            "k" -> {
                val u = p.getOrNull(1)?.let { findUser(it) }
                if (u == null) { TgApi.answerCallback(token, id, "User nahi mila"); return }
                val wasActive = u.active
                repo.setStatus(u.id, if (wasActive) "BANNED" else "ACTIVE")
                TgApi.answerCallback(token, id, if (wasActive) "Banned" else "Unbanned")
                val f = repo.refreshUser(u.id) ?: u
                TgApi.editMessage(token, chatId, mid, userText(f), kb(userKb(f)))
            }
            "d" -> {
                val act = p.getOrNull(1)
                val dep = repo.allDeposits().firstOrNull { it.id == p.getOrNull(2) }
                if (dep == null) { TgApi.answerCallback(token, id, "Nahi mila"); return }
                val status = if (act == "a") "APPROVED" else "REJECTED"
                val ok = repo.setDeposit(dep, status, "tg")
                TgApi.answerCallback(token, id, if (ok) status else "Fail")
                val f = repo.allDeposits().firstOrNull { it.id == dep.id } ?: dep
                TgApi.editMessage(token, chatId, mid, depText(f))
            }
            "w" -> {
                val act = p.getOrNull(1)
                val w = repo.allWithdraws().firstOrNull { it.id == p.getOrNull(2) }
                if (w == null) { TgApi.answerCallback(token, id, "Nahi mila"); return }
                val status = if (act == "p") "APPROVED" else "REJECTED"
                val ok = repo.setWithdraw(w, status, "tg")
                TgApi.answerCallback(token, id, if (ok) status else "Fail")
                val f = repo.allWithdraws().firstOrNull { it.id == w.id } ?: w
                TgApi.editMessage(token, chatId, mid, wdrText(f))
            }
            else -> TgApi.answerCallback(token, id)
        }
    }

    // ---------------- render ----------------

    private fun signed(rupees: Long): String = if (rupees >= 0) "+₹$rupees" else "-₹${-rupees}"

    private fun statsText(): String {
        val users = repo.allUsers()
        val deps = repo.allDeposits()
        val wdrs = repo.allWithdraws()
        return """
STATS
Users: ${users.size}
Total balance: ${money(users.sumOf { it.balance })}
Pending deposits: ${deps.count { it.status == "PENDING" }}
Pending withdraws: ${wdrs.count { it.status == "PENDING" }}
Dep approved: ${money(deps.filter { it.status == "APPROVED" }.sumOf { it.amount })}
""".trimIndent()
    }

    private fun usersText(page: Int): String {
        val all = repo.allUsers().sortedByDescending { it.createdAt }
        if (all.isEmpty()) return "Koi user nahi"
        val slice = all.drop(page * 8).take(8)
        val pages = (all.size + 7) / 8
        return "USERS (${page + 1}/$pages)\n\n" + slice.joinToString("\n") {
            "${it.name} · ${it.phone} · ${money(it.balance)} · ${it.status}"
        }
    }

    private fun pageKb(page: Int): List<List<Pair<String, String>>> {
        val all = repo.allUsers()
        val pages = (all.size + 7) / 8
        val rows = mutableListOf<List<Pair<String, String>>>()
        all.drop(page * 8).take(8).forEach { u ->
            rows.add(listOf("${u.name} · ${money(u.balance)}" to "u:${u.id}"))
        }
        val nav = mutableListOf<Pair<String, String>>()
        if (page > 0) nav.add("<" to "p:${page - 1}")
        if (page < pages - 1) nav.add(">" to "p:${page + 1}")
        if (nav.isNotEmpty()) rows.add(nav)
        return rows
    }

    private fun userText(u: User): String = """
${u.name}
${u.phone}
ID: ${u.id}
Code: ${u.refCode}
Balance: ${money(u.balance)}
Status: ${u.status}
Demo: ${if (u.demoGiven) "diya" else "nahi"}
""".trimIndent()

    private fun userKb(u: User): List<List<Pair<String, String>>> = listOf(
        listOf("+₹100" to "b:1:${u.id}", "+₹500" to "b:5:${u.id}"),
        listOf("-₹100" to "b:-1:${u.id}", "-₹500" to "b:-5:${u.id}"),
        listOf((if (u.active) "BAN" else "UNBAN") to "k:${u.id}")
    )

    private fun depText(d: Deposit): String =
        "DEPOSIT ${d.status}\n${d.userName}\n${money(d.amount)}\nUTR ${d.utr}"

    private fun wdrText(w: Withdraw): String =
        "WITHDRAW ${w.status}\n${w.userName}\n${money(w.amount)}\n${w.upi}"

    private fun sendDeps(chat: String) {
        val list = repo.allDeposits().filter { it.status == "PENDING" }
        if (list.isEmpty()) { say(chat, "Koi pending deposit nahi"); return }
        list.take(10).forEach { d ->
            say(chat, depText(d), kb(listOf(listOf("APPROVE" to "d:a:${d.id}", "REJECT" to "d:r:${d.id}"))))
        }
    }

    private fun sendWdrs(chat: String) {
        val list = repo.allWithdraws().filter { it.status == "PENDING" }
        if (list.isEmpty()) { say(chat, "Koi pending withdraw nahi"); return }
        list.take(10).forEach { w ->
            say(chat, wdrText(w), kb(listOf(listOf("PAID" to "w:p:${w.id}", "REFUND" to "w:r:${w.id}"))))
        }
    }

    private fun cfgText(): String {
        val c = repo.tableCfg()
        return """
CFG
boot=${c.boot} minbuy=${c.minBuyIn} maxbuy=${c.maxBuyIn}
players=${c.players} comm=${c.commissionPct}
upi=${c.upi} payee=${c.payeeName}
mindep=${c.minDeposit} minwdr=${c.minWithdraw}
demo=${c.demoOn} demoTopUp=${c.demoTopUp}
""".trimIndent()
    }

    private fun setCfg(key: String, value: String): String {
        val c = repo.tableCfg()
        val n = value.toLongOrNull()
        when (key.lowercase()) {
            "boot" -> c.boot = n ?: return "Number galat"
            "minbuy" -> c.minBuyIn = n ?: return "Number galat"
            "maxbuy" -> c.maxBuyIn = n ?: return "Number galat"
            "players" -> c.players = n?.toInt() ?: return "Number galat"
            "comm" -> c.commissionPct = n?.toInt() ?: return "Number galat"
            "upi" -> c.upi = value
            "payee" -> c.payeeName = value
            "mindep" -> c.minDeposit = n ?: return "Number galat"
            "minwdr" -> c.minWithdraw = n ?: return "Number galat"
            "demo" -> c.demoOn = value == "1" || value.equals("on", true)
            "demotopup" -> c.demoTopUp = n ?: return "Number galat"
            else -> return "Unknown key"
        }
        return if (repo.saveCfg(c)) "Set: $key = $value" else "Fail"
    }
}
