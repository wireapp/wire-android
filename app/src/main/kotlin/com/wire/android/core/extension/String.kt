package com.wire.android.core.extension

import java.text.Normalizer

val String.Companion.EMPTY get() = ""

fun String.removeAccents(): String {
    val normalised = Normalizer.normalize((this), Normalizer.Form.NFD)
    val accentRegex = "[\\p{InCombiningDiacriticalMarks}]".toRegex()
    return normalised.replace(accentRegex, String.EMPTY)
}
