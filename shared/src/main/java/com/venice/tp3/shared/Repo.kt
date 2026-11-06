package com.venice.tp3.shared

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.util.UUID

class Repo(val prefs: Prefs) {
    val db = TgDb(prefs)

    fun sha(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(("tp3$s").toByteArray()).joinToString("") { "%02x".format(it) }

    fun allUsers(): List<User> =
        db.load()["user"]?.values?.mapNotNull { runCatching { User.from(it) }.getOrNull() } ?: emptyList()

    fun allDeposits(): List<Deposit> =
        db.load(true)["dep"]?.values?.mapNotNull { runCatching { Deposit.from(it) }.getOrNull() } ?: emptyList()

    fun allWithdraws(): List<Withdraw> =
        db.load(true)["wdr"]?.values?.mapNotNull { runCatching { Withdraw.from(it) }.getOrNull() } ?: emptyList()

    fun tableCfg(): TableCfg =
        db.load()["cfg"]?.values?.firstOrNull()?.let { runCatching { TableCfg.from(it) }.getOrNull() } ?: TableCfg()

    fun refreshUser(id: String): User? {
        val o = db.load()["user"]?.get(id) ?: return null
        return runCatching { User.from(o) }.getOrNull()
    }

    fun register(phone: String, name: String, pin: String): Result<User> {
        val users = allUsers()
        if (users.any { it.phone == phone }) return Result.failure(Exception("Phone already registered"))
        val u = User(
            id = UUID.randomUUID().toString().take(12),
            phone = phone,
            name = name,
            pinHash = sha(phone + pin),
            refCode = "TP" + (100000..999999).random(),
            color = (0..7).random()
        )
        return if (db.save("user", u.toObj())) Result.success(u)
        else Result.failure(Exception("Telegram write failed"))
    }

    fun login(phone: String, pin: String): Result<User> {
        val u = allUsers().firstOrNull { it.phone == phone }
            ?: return Result.failure(Exception("User nahi mila"))
        if (u.pinHash != sha(phone + pin)) return Result.failure(Exception("Galat PIN"))
        if (!u.active) return Result.failure(Exception("Account ban hai"))
        return Result.success(u)
    }

    fun saveUser(u: User) = db.save("user", u.toObj())

    fun adjust(u: User, delta: Long) {
        val fresh = refreshUser(u.id) ?: return
        fresh.balance = (fresh.balance + delta).coerceAtLeast(0)
        saveUser(fresh)
    }

    fun requestDeposit(u: User, amount: Long, utr: String, proof: ByteArray?): Result<Unit> {
        val id = UUID.randomUUID().toString().take(12)
        var fileId = ""
        if (proof != null) {
            fileId = db.savePhoto(proof, "#DB:proof $id") ?: ""
        }
        val d = Deposit(id = id, userId = u.id, userName = u.name, amount = amount, utr = utr, proofFileId = fileId)
        return if (db.save("dep", d.toObj())) Result.success(Unit)
        else Result.failure(Exception("Deposit save fail"))
    }

    fun setDeposit(d: Deposit, status: String, note: String) {
        val fresh = allDeposits().firstOrNull { it.id == d.id } ?: d
        fresh.status = status
        fresh.note = note
        db.save("dep", fresh.toObj(), true)
        if (status == "APPROVED") {
            val u = refreshUser(fresh.userId)
            if (u != null) adjust(u, fresh.amount)
        }
    }

    fun requestWithdraw(u: User, amount: Long, upi: String): Result<Unit> {
        if (u.balance < amount) return Result.failure(Exception("Balance kam hai"))
        val w = Withdraw(
            id = UUID.randomUUID().toString().take(12),
            userId = u.id, userName = u.name, amount = amount, upi = upi
        )
        adjust(u, -amount)
        return if (db.save("wdr", w.toObj())) Result.success(Unit)
        else Result.failure(Exception("Withdraw save fail"))
    }

    fun setWithdraw(w: Withdraw, status: String, note: String) {
        val fresh = allWithdraws().firstOrNull { it.id == w.id } ?: w
        fresh.status = status
        fresh.note = note
        db.save("wdr", fresh.toObj(), true)
        if (status == "REJECTED") {
            refreshUser(fresh.userId)?.let { adjust(it, fresh.amount) }
        }
    }

    fun pushHistory(userId: String, tableId: String, delta: Long) {
        db.save("hist", History(UUID.randomUUID().toString().take(10), userId, tableId, delta).toObj())
    }

    fun saveCfg(c: TableCfg) = db.save("cfg", c.toObj(), true)

    fun snapshotAll() {
        val cfg = tableCfg()
        db.snapshot(allUsers().map { it.toObj() }, cfg.toObj())
    }

    fun pingAdmin(): String = TgApi.ping(prefs.config.adminToken)
    fun pingPublic(): String = TgApi.ping(prefs.config.publicToken)

    fun sendTest() {
        db.save("ping", buildJsonObject { put("id", UUID.randomUUID().toString()); put("ts", now()) }, true)
    }
}
