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

import com.wire.android.framework.TestMessage
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.util.DateTimeUtil
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class AuthorHeaderHelperTest {

    @Test
    fun givenOneRegularMessage_thenShouldShowHeaderForRecentMessage() {
        // given
        val messages = listOf(testRegularMessage())
        // when
        val result = AuthorHeaderHelper.shouldShowHeader(0, messages, messages[0])
        // then
        assertEquals(true, result)
    }

    @Test
    fun givenOneSystemMessage_thenShouldNotShowHeaderForRecentMessage() {
        // given
        val messages = listOf(testSystemMessage())
        // when
        val result = AuthorHeaderHelper.shouldShowHeader(0, messages, messages[0])
        // then
        assertEquals(false, result)
    }

    @Test
    fun givenOnePingMessage_thenShouldNotShowHeaderForRecentMessage() {
        // given
        val messages = listOf(testPingMessage())
        // when
        val result = AuthorHeaderHelper.shouldShowHeader(0, messages, messages[0])
        // then
        assertEquals(false, result)
    }

    @Test
    fun givenTwoRegularMessagesFromSameUser_thenShouldNotShowHeaderForRecentMessage() {
        // given
        val messages = listOf( // more recent message is first on list
            testRegularMessage(userId = SELF_USER_ID, timestamp = "2021-01-01T00:00:01.000Z"),
            testRegularMessage(userId = SELF_USER_ID, timestamp = "2021-01-01T00:00:00.000Z")
        )
        // when
        val result = AuthorHeaderHelper.shouldShowHeader(0, messages, messages[0])
        // then
        assertEquals(false, result)
    }

    @Test
    fun givenTwoRegularMessagesFromDifferentUser_thenShouldShowHeaderForRecentMessage() {
        // given
        val messages = listOf( // more recent message is first on list
            testRegularMessage(userId = SELF_USER_ID, timestamp = "2021-01-01T00:00:01.000Z"),
            testRegularMessage(userId = OTHER_USER_ID, timestamp = "2021-01-01T00:00:00.000Z")
        )
        // when
        val result = AuthorHeaderHelper.shouldShowHeader(0, messages, messages[0])
        // then
        assertEquals(true, result)
    }

    @Test
    fun givenSystemAndThenRegularMessageFromSameUser_thenShouldShowHeaderForRecentMessage() {
        // given
        val messages = listOf( // more recent message is first on list
            testRegularMessage(userId = SELF_USER_ID, timestamp = "2021-01-01T00:00:01.000Z"),
            testSystemMessage(userId = SELF_USER_ID, timestamp = "2021-01-01T00:00:00.000Z")
        )
        // when
        val result = AuthorHeaderHelper.shouldShowHeader(0, messages, messages[0])
        // then
        assertEquals(true, result)
    }

    @Test
    fun givenRegularAndThenSystemMessagFromSameUsere_thenShouldNotShowHeaderForRecentMessage() {
        // given
        val messages = listOf( // more recent message is first on list
            testSystemMessage(userId = SELF_USER_ID, timestamp = "2021-01-01T00:00:01.000Z"),
            testRegularMessage(userId = SELF_USER_ID, timestamp = "2021-01-01T00:00:00.000Z")
        )
        // when
        val result = AuthorHeaderHelper.shouldShowHeader(0, messages, messages[0])
        // then
        assertEquals(false, result)
    }

    @Test
    fun givenPingAndThenRegularMessageFromSameUser_thenShouldShowHeaderForRecentMessage() {
        // given
        val messages = listOf( // more recent message is first on list
            testRegularMessage(userId = SELF_USER_ID, timestamp = "2021-01-01T00:00:01.000Z"),
            testPingMessage(userId = SELF_USER_ID, timestamp = "2021-01-01T00:00:00.000Z")
        )
        // when
        val result = AuthorHeaderHelper.shouldShowHeader(0, messages, messages[0])
        // then
        assertEquals(true, result)
    }

    @Test
    fun givenRegularAndThenPingMessageFromSameUser_thenShouldNotShowHeaderForRecentMessage() {
        // given
        val messages = listOf( // more recent message is first on list
            testPingMessage(userId = SELF_USER_ID, timestamp = "2021-01-01T00:00:01.000Z"),
            testRegularMessage(userId = SELF_USER_ID, timestamp = "2021-01-01T00:00:00.000Z")
        )
        // when
        val result = AuthorHeaderHelper.shouldShowHeader(0, messages, messages[0])
        // then
        assertEquals(false, result)
    }

    @Test
    fun givenTwoRegularMessagesFromSameUserAndTimestampsWithinThreshold_thenShouldNotShowHeaderForRecentMessage() {
        // given
        val timestamp = "2021-01-01T00:00:00.000Z"
        val timestampMinusLessThanThreshold = DateTimeUtil.minusMilliseconds(timestamp, AuthorHeaderHelper.AGGREGATION_TIME_WINDOW - 1L)
        val messages = listOf(
            testRegularMessage(userId = SELF_USER_ID, timestamp = timestamp),
            testRegularMessage(userId = SELF_USER_ID, timestamp = timestampMinusLessThanThreshold)
        )
        // when
        val result = AuthorHeaderHelper.shouldShowHeader(0, messages, messages[0])
        // then
        assertEquals(false, result)
    }

    @Test
    fun givenTwoRegularMessagesFromSameUserAndTimestampsBeyondThreshold_thenShouldShowHeaderForRecentMessage() {
        // given
        val timestamp = "2021-01-01T00:00:00.000Z"
        val timestampMinusMoreThanThreshold = DateTimeUtil.minusMilliseconds(timestamp, AuthorHeaderHelper.AGGREGATION_TIME_WINDOW + 1L)
        val messages = listOf(
            testRegularMessage(userId = SELF_USER_ID, timestamp = timestamp),
            testRegularMessage(userId = SELF_USER_ID, timestamp = timestampMinusMoreThanThreshold)
        )
        // when
        val result = AuthorHeaderHelper.shouldShowHeader(0, messages, messages[0])
        // then
        assertEquals(true, result)
    }

    companion object {
        private val SELF_USER_ID = UserId("self", "domain")
        private val OTHER_USER_ID = UserId("other", "domain")

        private fun testSystemMessage(
            userId: UserId? = null,
            timestamp: String = "2021-01-01T00:00:00.000Z"
        ) = UIMessage.System(
            header = TestMessage.UI_MESSAGE_HEADER.copy(
                messageTime = TestMessage.UI_MESSAGE_HEADER.messageTime.copy(
                    utcISO = timestamp
                ),
                messageId = UUID.randomUUID().toString(),
                userId = userId
            ),
            source = MessageSource.OtherUser,
            messageContent = UIMessageContent.SystemMessage.HistoryLost
        )

        private fun testPingMessage(
            userId: UserId? = null,
            timestamp: String = "2021-01-01T00:00:00.000Z"
        ) = UIMessage.System(
            header = TestMessage.UI_MESSAGE_HEADER.copy(
                messageTime = TestMessage.UI_MESSAGE_HEADER.messageTime.copy(
                    utcISO = timestamp
                ),
                messageId = UUID.randomUUID().toString(),
                userId = userId
            ),
            source = MessageSource.OtherUser,
            messageContent = UIMessageContent.SystemMessage.Knock(UIText.DynamicString("pinged")),
        )

        private fun testRegularMessage(
            userId: UserId? = null,
            timestamp: String = "2021-01-01T00:00:00.000Z"
        ) = UIMessage.Regular(
            userAvatarData = UserAvatarData(asset = null, availabilityStatus = UserAvailabilityStatus.NONE),
            source = if (userId == SELF_USER_ID) MessageSource.Self else MessageSource.OtherUser,
            header = TestMessage.UI_MESSAGE_HEADER.copy(
                messageTime = TestMessage.UI_MESSAGE_HEADER.messageTime.copy(
                    utcISO = timestamp
                ),
                messageId = UUID.randomUUID().toString(),
                userId = userId
            ),
            messageContent = UIMessageContent.TextMessage(MessageBody(UIText.DynamicString("Some Text Message"))),
            messageFooter = com.wire.android.ui.home.conversations.model.MessageFooter(TestMessage.UI_MESSAGE_HEADER.messageId)
        )
    }
}
