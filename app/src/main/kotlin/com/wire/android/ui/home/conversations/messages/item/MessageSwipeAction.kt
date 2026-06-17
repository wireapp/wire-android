/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.home.conversations.messages.item

import com.wire.android.ui.home.conversations.model.UIMessage

enum class MessageSwipeAction {
    REPLY,
    REACT,
    DETAILS;

    companion object {
        val DEFAULT_RIGHT = REPLY
        val DEFAULT_LEFT = REACT

        fun fromStoredValue(value: String?, defaultValue: MessageSwipeAction): MessageSwipeAction =
            entries.firstOrNull { it.name == value } ?: defaultValue
    }
}

internal fun MessageSwipeAction.toSwipeAction(
    message: UIMessage.Regular,
    onSwipedToReply: (UIMessage.Regular) -> Unit,
    onSwipedToReact: (UIMessage.Regular) -> Unit,
    onSwipedToDetails: (UIMessage.Regular) -> Unit,
): SwipeAction? =
    when (this) {
        MessageSwipeAction.REPLY -> SwipeAction(iconResId()) { onSwipedToReply(message) }
            .takeIf { message.isReplyable }

        MessageSwipeAction.REACT -> SwipeAction(iconResId()) { onSwipedToReact(message) }
            .takeIf { message.isReactionAllowed }

        MessageSwipeAction.DETAILS -> SwipeAction(iconResId()) { onSwipedToDetails(message) }
    }
