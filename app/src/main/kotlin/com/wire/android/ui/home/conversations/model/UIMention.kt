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
package com.wire.android.ui.home.conversations.model

import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.data.user.UserId

data class UIMention(
    val start: Int,
    val length: Int,
    val userId: UserId,
    val handler: String // name that should be displayed in a message
) {
    fun intoMessageMention() = MessageMention(start, length, userId, false) // We can never send a self mention message
}

fun MessageMention.toUiMention(originalText: String): UIMention? =
    if (start + length <= originalText.length && originalText.elementAt(start) == '@') {
        UIMention(
            start = start,
            length = length,
            userId = userId,
            handler = originalText.substring(start, start + length)
        )
    } else {
        null
    }
