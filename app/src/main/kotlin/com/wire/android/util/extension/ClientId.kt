/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.util.extension

import com.wire.kalium.logic.data.conversation.ClientId

private const val REQUIRED_DISPLAY_LENGTH = 16

fun ClientId.formatAsString(): String {
    val actualLength = value.length

    val validatedValue = if (actualLength != REQUIRED_DISPLAY_LENGTH) {
        StringBuilder(value).insert(0, "0".repeat(REQUIRED_DISPLAY_LENGTH - actualLength)).toString()
    } else {
        value
    }

    return validatedValue.chunked(2).joinToString(separator = " ")
}
