package com.wire.android.util

val String.Companion.EMPTY get() = ""

fun String?.orDefault(default: String) = this ?: default

public inline fun String.ifNotEmpty(transform: () -> String): String = if (!isEmpty()) transform() else this
