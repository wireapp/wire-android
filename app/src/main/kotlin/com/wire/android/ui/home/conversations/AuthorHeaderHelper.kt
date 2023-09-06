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
 */
package com.wire.android.ui.home.conversations

import androidx.annotation.VisibleForTesting
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.kalium.util.DateTimeUtil

object AuthorHeaderHelper {

    @VisibleForTesting
    internal const val AGGREGATION_TIME_WINDOW: Int = 30_000 // millis

    internal fun shouldShowHeader(index: Int, messages: List<UIMessage>, currentMessage: UIMessage): Boolean {
        var showHeader = currentMessage is UIMessage.Regular
        val nextIndex = index + 1
        if (nextIndex < messages.size) {
            val nextUiMessage = messages[nextIndex]
            if (currentMessage.header.userId == nextUiMessage.header.userId
                && currentMessage is UIMessage.Regular
                && nextUiMessage is UIMessage.Regular
            ) {
                val difference = DateTimeUtil.calculateMillisDifference(
                    nextUiMessage.header.messageTime.utcISO,
                    currentMessage.header.messageTime.utcISO,
                )
                showHeader = difference > AGGREGATION_TIME_WINDOW
            }
        }
        return showHeader
    }
}
