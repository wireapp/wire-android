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

package com.wire.android.util.extension

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.withStyle
import com.wire.kalium.logic.data.conversation.ClientId

private const val REQUIRED_DISPLAY_LENGTH = 16

@Stable
fun ClientId.formatAsString(): String {
    val validatedValue = value.padStart(REQUIRED_DISPLAY_LENGTH, '0')
    return validatedValue.uppercase().chunked(2).joinToString(separator = " ")
}

@Stable
fun String.formatAsFingerPrint(): AnnotatedString =
    buildAnnotatedString {
        this@formatAsFingerPrint
            .uppercase()
            .chunked(2)
            .forEachIndexed { index, str ->
                if (index % 2 == 0) {
                    withStyle(style = SpanStyle(fontWeight = Bold, shadow = null)) {
                        append(str)
                    }
                } else {
                    append(str)
                }
                if (index != this.length - 1) {
                    append(" ")
                }
            }
    }
