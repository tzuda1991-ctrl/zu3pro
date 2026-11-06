package com.venice.tp3.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class User(
    val id: String,
    val phone: String,
    val name: String,
    val pinHash: String,
    val createdAt: Long = now(),
    var balance: Long = 0L,
    var status: String = "ACTIVE",
    var refCode: String = "",
    var referredBy: String = "",
    var color: Int = 0,
    var del: Boolean = false
) {
    val active get() = status == "ACTIVE"
    fun toObj(): JsonObject = J.encodeToJsonElement(serializer(), this).jsonObject
    companion object {
        fun from(o: JsonObject) = J.decodeFromJsonElement(serializer(), o)
    }
}

@Serializable
data class Deposit(
    val id: String,
    val userId: String,
    val userName: String,
    var amount: Long,
    var utr: String,
    var proofFileId: String = "",
    var status: String = "PENDING",
    var note: String = "",
    val ts: Long = now(),
    var del: Boolean = false
) {
    fun toObj(): JsonObject = J.encodeToJsonElement(serializer(), this).jsonObject
    companion object {
        fun from(o: JsonObject) = J.decodeFromJsonElement(serializer(), o)
    }
}

@Serializable
data class Withdraw(
    val id: String,
    val userId: String,
    val userName: String,
    var amount: Long,
    var upi: String,
    var status: String = "PENDING",
    var note: String = "",
    val ts: Long = now(),
    var del: Boolean = false
) {
    fun toObj(): JsonObject = J.encodeToJsonElement(serializer(), this).jsonObject
    companion object {
        fun from(o: JsonObject) = J.decodeFromJsonElement(serializer(), o)
    }
}

@Serializable
data class History(
    val id: String,
    val userId: String,
    val tableId: String,
    val delta: Long,
    val ts: Long = now(),
    var del: Boolean = false
) {
    fun toObj(): JsonObject = J.encodeToJsonElement(serializer(), this).jsonObject
    companion object {
        fun from(o: JsonObject) = J.decodeFromJsonElement(serializer(), o)
    }
}

@Serializable
data class TableCfg(
    val id: String = "cfg",
    var boot: Long = 1000L,
    var minBuyIn: Long = 10000L,
    var maxBuyIn: Long = 500000L,
    var players: Int = 5,
    var commissionPct: Int = 5,
    var upi: String = "paytm@upi",
    var payeeName: String = "Teen Patti 3",
    var minDeposit: Long = 10000L,
    var minWithdraw: Long = 50000L,
    var del: Boolean = false
) {
    fun toObj(): JsonObject = J.encodeToJsonElement(serializer(), this).jsonObject
    companion object {
        fun from(o: JsonObject) = J.decodeFromJsonElement(serializer(), o)
    }
}
