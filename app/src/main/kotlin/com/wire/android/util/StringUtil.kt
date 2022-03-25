package com.wire.android.util

val String.Companion.EMPTY get() = ""

fun String?.orDefault(default: String) = this ?: default
