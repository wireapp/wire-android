/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.util

import java.math.BigInteger
import java.security.MessageDigest

val String.Companion.EMPTY get() = ""

val String.Companion.WHITE_SPACE get() = " "

val String.Companion.MENTION_SYMBOL get() = "@"

val String.Companion.NEW_LINE_SYMBOL get() = "\n"

fun String?.orDefault(default: String) = this ?: default

inline fun String.ifNotEmpty(transform: () -> String): String = if (!isEmpty()) transform() else this

@Suppress("MagicNumber")
fun String.sha256(): String {
    val md = MessageDigest.getInstance("SHA-256")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}

fun String.toTitleCase(delimiter: String = " ", separator: String = " "): String =
    this.split(delimiter).joinToString(separator = separator) {
        it.lowercase().replaceFirstChar(Char::titlecaseChar)
    }

fun String.capitalizeFirstLetter(): String = lowercase().replaceFirstChar(Char::titlecaseChar)

fun String.normalizeFileName(): String = this.replace("/", "")

fun String.addBeforeExtension(insert: String): String {
    val lastDotIndex = this.lastIndexOf('.')
    if (lastDotIndex <= 0) {
        return this + insert
    }

    val extensionBlockIndex = this.lastIndexOf('.', lastDotIndex - 1).let {
        if (it == -1) lastDotIndex else it
    }

    val name = this.take(extensionBlockIndex)
    val ext = this.substring(extensionBlockIndex)

    return "${name}_$insert$ext"
}

