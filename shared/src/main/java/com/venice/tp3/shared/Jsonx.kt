package com.venice.tp3.shared

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

val J = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

fun now() = System.currentTimeMillis()

fun JsonObject.str(k: String) = this[k]?.toString()?.trim('"') ?: ""
fun JsonObject.lng(k: String) = this[k]?.toString()?.toLongOrNull() ?: 0L
fun JsonObject.bln(k: String) = this[k]?.toString()?.toBooleanStrictOrNull() ?: false
