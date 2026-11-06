package com.venice.tp3.shared

import kotlinx.serialization.json.*
import java.security.MessageDigest
import java.util.UUID

/** @param admin true = admin app (admin bot poll karta hai), false = user app */
class Repo(val prefs: Prefs, private val admin: Boolean = true) {
    val db = TgDb(prefs, admin)

    fun sha(s: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(("tp3$s").toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun allUsers(): List<User> =
        db.load()["user"]?.values?.mapNotNull { runCatching { User.from(it) }.getOrNull() } ?: emptyList()

    fun allDeposits(): List<Deposit> =
        db.load()["dep"]?.values?.mapNotNull { runCatching { Deposit.from(it) }.getOrNull() } ?: emptyList()

    fun allWithdraws(): List<Withdraw> =
        db.load()["wdr"]?.values?.mapNotNull { runCatching { Withdraw.from(it) }.getOrNull() } ?: emptyList()

    fun tableCfg(): TableCfg =
        db.load()["cfg"]?.values?.firstOrNull()?.let { runCatching { TableCfg.from(it) }.getOrNull() } ?: TableCfg()

    /** pehle cache, phir network */
    fun refreshUser(id: String): User? {
        val o = db.cached("user", id) ?: db.load()["user"]?.get(id) ?: return null
        return runCatching { User.from(o) }.getOrNull()
    }

    fun register(phone: String, name: String, pin: String): Result<User> {
        if (allUsers().any { it.phone == phone }) return Result.failure(Exception("Phone already registered"))
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

    fun saveUser(u: User): Boolean = db.save("user", u.toObj())

    // ---------------- admin balance control ----------------

    /** delta paise me. true = save hua. */
    fun adjust(id: String, delta: Long, note: String = ""): Boolean {
        val fresh = refreshUser(id) ?: return false
        fresh.balance = (fresh.balance + delta).coerceAtLeast(0)
        val ok = saveUser(fresh)
        if (ok) pushHistory(id, "ADMIN ${if (delta >= 0) "+" else "-"}$note", delta)
        return ok
    }

    fun adjust(u: User, delta: Long): Boolean = adjust(u.id, delta)

    /** seedha exact balance set karo (paise me) */
    fun setBalance(id: String, exact: Long): Boolean {
        val fresh = refreshUser(id) ?: return false
        val delta = exact - fresh.balance
        fresh.balance = exact.coerceAtLeast(0)
        val ok = saveUser(fresh)
        if (ok) pushHistory(id, "ADMIN SET", delta)
        return ok
    }

    fun setStatus(id: String, status: String): Boolean {
        val fresh = refreshUser(id) ?: return false
        fresh.status = status
        return saveUser(fresh)
    }

    // ---------------- deposits / withdraws ----------------

    fun requestDeposit(u: User, amount: Long, utr: String, proof: ByteArray?): Result<Unit> {
        val id = UUID.randomUUID().toString().take(12)
        var fileId = ""
        if (proof != null) fileId = db.savePhoto(proof, "#DB:proof $id") ?: ""
        val d = Deposit(id = id, userId = u.id, userName = u.name, amount = amount, utr = utr, proofFileId = fileId)
        return if (db.save("dep", d.toObj())) Result.success(Unit)
        else Result.failure(Exception("Deposit save fail"))
    }

    fun setDeposit(d: Deposit, status: String, note: String): Boolean {
        val fresh = allDeposits().firstOrNull { it.id == d.id } ?: d
        val was = fresh.status
        fresh.status = status
        fresh.note = note
        val ok = db.save("dep", fresh.toObj())
        // sirf pehli baar approve hone par credit — double credit nahi
        if (ok && status == "APPROVED" && was != "APPROVED") adjust(fresh.userId, fresh.amount, "dep ${fresh.id}")
        return ok
    }

    fun requestWithdraw(u: User, amount: Long, upi: String): Result<Unit> {
        if (u.balance < amount) return Result.failure(Exception("Balance kam hai"))
        val w = Withdraw(
            id = UUID.randomUUID().toString().take(12),
            userId = u.id, userName = u.name, amount = amount, upi = upi
        )
        adjust(u.id, -amount)
        return if (db.save("wdr", w.toObj())) Result.success(Unit)
        else Result.failure(Exception("Withdraw save fail"))
    }

    fun setWithdraw(w: Withdraw, status: String, note: String): Boolean {
        val fresh = allWithdraws().firstOrNull { it.id == w.id } ?: w
        val was = fresh.status
        fresh.status = status
        fresh.note = note
        val ok = db.save("wdr", fresh.toObj())
        // reject par refund, sirf ek baar
        if (ok && status == "REJECTED" && was != "REJECTED") adjust(fresh.userId, fresh.amount, "wdr ${fresh.id}")
        return ok
    }

    fun pushHistory(userId: String, tableId: String, delta: Long) {
        db.save("hist", History(UUID.randomUUID().toString().take(10), userId, tableId, delta).toObj())
    }

    fun saveCfg(c: TableCfg): Boolean = db.save("cfg", c.toObj())

    /** poora state dobara push — purana data / naya phone install ke baad */
    fun snapshotAll(): Boolean {
        var ok = false
        allUsers().forEach { if (db.save("user", it.toObj())) ok = true }
        if (db.save("cfg", tableCfg().toObj())) ok = true
        return ok
    }

    fun resync(): Boolean {
        db.resetOffsets()
        return snapshotAll()
    }

    fun pingAdmin(): String = TgApi.ping(prefs.config.adminToken)
    fun pingPublic(): String = TgApi.ping(prefs.config.publicToken)

    fun sendTest() {
        db.save("ping", buildJsonObject {
            put("id", UUID.randomUUID().toString())
            put("ts", now())
        })
    }
}
