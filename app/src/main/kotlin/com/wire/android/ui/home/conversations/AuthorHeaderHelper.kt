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
package com.wire.android.ui.home.conversations

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.paging.compose.LazyPagingItems
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.kalium.util.DateTimeUtil

object AuthorHeaderHelper {

    @VisibleForTesting
    internal const val AGGREGATION_TIME_WINDOW: Int = 30_000 // in millis

    private fun LazyPagingItems<UIMessage>.peekOrNull(index: Int) =
        if (index in 0 until this.itemCount) this.peek(index) else null

    @Suppress("ComplexCondition")
    internal fun shouldShowHeader(currentMessage: UIMessage, messageAbove: UIMessage?): Boolean =
        if (messageAbove != null
            && currentMessage.header.userId == messageAbove.header.userId
            && currentMessage is UIMessage.Regular
            && messageAbove is UIMessage.Regular
        ) {
            val difference = DateTimeUtil.calculateMillisDifference(
                messageAbove.header.messageTime.utcISO,
                currentMessage.header.messageTime.utcISO,
            )
            difference > AGGREGATION_TIME_WINDOW
        } else {
            currentMessage is UIMessage.Regular
        }

    @Composable
    internal fun rememberShouldShowHeader(
        currentIndex: Int,
        currentMessage: UIMessage,
        messages: LazyPagingItems<UIMessage>
    ): Boolean {
        val messageAbove = messages.peekOrNull(currentIndex + 1) // order of messages is reversed (from bottom to top)
        val state by remember(currentIndex, currentMessage, messageAbove) {
            derivedStateOf {
                shouldShowHeader(currentMessage, messageAbove)
            }
        }
        return state
    }

    @Suppress("ComplexCondition")
    internal fun shouldHaveSmallBottomPadding(currentMessage: UIMessage, messageBelow: UIMessage?): Boolean =
        if (messageBelow != null
            && currentMessage.header.userId == messageBelow.header.userId
            && currentMessage is UIMessage.Regular
            && messageBelow is UIMessage.Regular
        ) {
            val difference = DateTimeUtil.calculateMillisDifference(
                currentMessage.header.messageTime.utcISO,
                messageBelow.header.messageTime.utcISO
            )
            difference < AGGREGATION_TIME_WINDOW
        } else {
            false
        }

    @Composable
    internal fun rememberShouldHaveSmallBottomPadding(
        currentIndex: Int,
        currentMessage: UIMessage,
        messages: LazyPagingItems<UIMessage>
    ): Boolean {
        val messageBelow = messages.peekOrNull(currentIndex - 1) // order of messages is reversed (from bottom to top)
        val state by remember(currentIndex, currentMessage, messageBelow) {
            derivedStateOf {
                shouldHaveSmallBottomPadding(currentMessage, messageBelow)
            }
        }
        return state
    }
}
