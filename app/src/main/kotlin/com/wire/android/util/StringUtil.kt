package com.wire.android.util

import java.math.BigInteger
import java.security.MessageDigest

val String.Companion.EMPTY get() = ""

fun String?.orDefault(default: String) = this ?: default

public inline fun String.ifNotEmpty(transform: () -> String): String = if (!isEmpty()) transform() else this

@Suppress("MagicNumber")
fun String.sha256(): String {
    val md = MessageDigest.getInstance("SHA-256")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}
