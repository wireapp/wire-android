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

import com.wire.android.framework.TestMessage
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.AuthorHeaderHelper.shouldHaveSmallBottomPadding
import com.wire.android.ui.home.conversations.AuthorHeaderHelper.shouldShowHeader
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import kotlin.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class AuthorHeaderHelperTest {

    private data class Messages(val currentMessage: UIMessage, val messageAbove: UIMessage?, val messageBelow: UIMessage?)

    private fun List<UIMessage>.forIndex(index: Int, action: (Messages) -> Boolean): Boolean =
        action(Messages(this[index], this.getOrNull(index + 1), this.getOrNull(index - 1)))

    // shouldShowHeader tests
    @Test
    fun givenOneRegularMessage_thenShouldShowHeaderForRecentMessage() {
        // given
        val messages = listOf(testRegularMessage())
        // when
        val result = messages.forIndex(0) { shouldShowHeader(it.currentMessage, it.messageAbove) }
        // then
        assertEquals(true, result)
    }

    @Test
    fun givenOneSystemMessage_thenShouldNotShowHeaderForRecentMessage() {
        // given
        val messages = listOf(testSystemMessage())
        // when
        val result = messages.forIndex(0) { shouldShowHeader(it.currentMessage, it.messageAbove) }
        // then
        assertEquals(false, result)
    }

    @Test
    fun givenOnePingMessage_thenShouldNotShowHeaderForRecentMessage() {
        // given
        val messages = listOf(testPingMessage())
        // when
        val result = messages.forIndex(0) { shouldShowHeader(it.currentMessage, it.messageAbove) }
        // then
        assertEquals(false, result)
    }

    @Test
    fun givenTwoRegularMessagesFromSameUser_thenShouldNotShowHeaderForRecentMessage() {
        // given
        val messages = listOf( // more recent message is first on list
            testRegularMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:01.000Z")),
            testRegularMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:00.000Z"))
        )
        // when
        val result = messages.forIndex(0) { shouldShowHeader(it.currentMessage, it.messageAbove) }
        // then
        assertEquals(false, result)
    }

    @Test
    fun givenTwoRegularMessagesFromDifferentUser_thenShouldShowHeaderForRecentMessage() {
        // given
        val messages = listOf( // more recent message is first on list
            testRegularMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:01.000Z")),
            testRegularMessage(userId = OTHER_USER_ID, instant = Instant.parse("2021-01-01T00:00:00.000Z"))
        )
        // when
        val result = messages.forIndex(0) { shouldShowHeader(it.currentMessage, it.messageAbove) }
        // then
        assertEquals(true, result)
    }

    @Test
    fun givenSystemAndThenRegularMessageFromSameUser_thenShouldShowHeaderForRecentMessage() {
        // given
        val messages = listOf( // more recent message is first on list
            testRegularMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:01.000Z")),
            testSystemMessage(userId = SELF_USER_ID, timestamp = Instant.parse("2021-01-01T00:00:00.000Z"))
        )
        // when
        val result = messages.forIndex(0) { shouldShowHeader(it.currentMessage, it.messageAbove) }
        // then
        assertEquals(true, result)
    }

    @Test
    fun givenRegularAndThenSystemMessagFromSameUsere_thenShouldNotShowHeaderForRecentMessage() {
        // given
        val messages = listOf( // more recent message is first on list
            testSystemMessage(userId = SELF_USER_ID, timestamp = Instant.parse("2021-01-01T00:00:01.000Z")),
            testRegularMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:00.000Z"))
        )
        // when
        val result = messages.forIndex(0) { shouldShowHeader(it.currentMessage, it.messageAbove) }
        // then
        assertEquals(false, result)
    }

    @Test
    fun givenPingAndThenRegularMessageFromSameUser_thenShouldShowHeaderForRecentMessage() {
        // given
        val messages = listOf( // more recent message is first on list
            testRegularMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:01.000Z")),
            testPingMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:00.000Z"))
        )
        // when
        val result = messages.forIndex(0) { shouldShowHeader(it.currentMessage, it.messageAbove) }
        // then
        assertEquals(true, result)
    }

    @Test
    fun givenRegularAndThenPingMessageFromSameUser_thenShouldNotShowHeaderForRecentMessage() {
        // given
        val messages = listOf( // more recent message is first on list
            testPingMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:01.000Z")),
            testRegularMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:00.000Z"))
        )
        // when
        val result = messages.forIndex(0) { shouldShowHeader(it.currentMessage, it.messageAbove) }
        // then
        assertEquals(false, result)
    }

    @Test
    fun givenTwoRegularMessagesFromSameUserAndTimestampsWithinThreshold_thenShouldNotShowHeaderForRecentMessage() {
        // given
        val timestamp = Instant.parse("2021-01-01T00:00:00.000Z")
        val timestampMinusLessThanThreshold = timestamp - (AuthorHeaderHelper.AGGREGATION_TIME_WINDOW - 1).milliseconds
        val messages = listOf(
            testRegularMessage(userId = SELF_USER_ID, instant = timestamp),
            testRegularMessage(userId = SELF_USER_ID, instant = timestampMinusLessThanThreshold)
        )
        // when
        val result = messages.forIndex(0) { shouldShowHeader(it.currentMessage, it.messageAbove) }
        // then
        assertEquals(false, result)
    }

    @Test
    fun givenTwoRegularMessagesFromSameUserAndTimestampsBeyondThreshold_thenShouldShowHeaderForRecentMessage() {
        // given
        val timestamp = Instant.parse("2021-01-01T00:00:00.000Z")
        val timestampMinusMoreThanThreshold = timestamp - (AuthorHeaderHelper.AGGREGATION_TIME_WINDOW + 1).milliseconds
        val messages = listOf(
            testRegularMessage(userId = SELF_USER_ID, instant = timestamp),
            testRegularMessage(userId = SELF_USER_ID, instant = timestampMinusMoreThanThreshold)
        )
        // when
        val result = messages.forIndex(0) { shouldShowHeader(it.currentMessage, it.messageAbove) }
        // then
        assertEquals(true, result)
    }

    // shouldHaveSmallBottomPadding tests
    @Test
    fun givenOneRegularMessage_thenShouldNotHaveSmallBottomPadding() {
        // given
        val messages = listOf(testRegularMessage())
        // when
        val result = messages.forIndex(0) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        // then
        assertEquals(false, result)
    }

    @Test
    fun givenOneSystemMessage_thenShouldNotHaveSmallBottomPadding() {
        // given
        val messages = listOf(testSystemMessage())
        // when
        val result = messages.forIndex(0) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        // then
        assertEquals(false, result)
    }

    @Test
    fun givenOnePingMessage_thenShouldNotHaveSmallBottomPadding() {
        // given
        val messages = listOf(testPingMessage())
        // when
        val result = messages.forIndex(0) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        // then
        assertEquals(false, result)
    }

    @Test
    fun givenTwoRegularMessagesFromSameUser_thenPreviousShouldHaveSmallBottomPaddingAndRecentShouldNot() {
        // given
        val messages = listOf( // more recent message is first on list
            testRegularMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:01.000Z")),
            testRegularMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:00.000Z"))
        )
        // when
        val resultPrevious = messages.forIndex(1) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        val resultRecent = messages.forIndex(0) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        // then
        assertEquals(true, resultPrevious)
        assertEquals(false, resultRecent)
    }

    @Test
    fun givenTwoRegularMessagesFromDifferentUser_thenBothShouldNotHaveSmallBottomPadding() {
        // given
        val messages = listOf( // more recent message is first on list
            testRegularMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:01.000Z")),
            testRegularMessage(userId = OTHER_USER_ID, instant = Instant.parse("2021-01-01T00:00:00.000Z"))
        )
        // when
        val resultPrevious = messages.forIndex(1) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        val resultRecent = messages.forIndex(0) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        // then
        assertEquals(false, resultPrevious)
        assertEquals(false, resultRecent)
    }

    @Test
    fun givenSystemAndThenRegularMessageFromSameUser_thenBothShouldNotHaveSmallBottomPadding() {
        // given
        val messages = listOf( // more recent message is first on list
            testRegularMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:01.000Z")),
            testSystemMessage(userId = SELF_USER_ID, timestamp = Instant.parse("2021-01-01T00:00:00.000Z"))
        )
        // when
        val resultPrevious = messages.forIndex(1) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        val resultRecent = messages.forIndex(0) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        // then
        assertEquals(false, resultPrevious)
        assertEquals(false, resultRecent)
    }

    @Test
    fun givenRegularAndThenSystemMessagFromSameUsere_thenBothShouldNotHaveSmallBottomPadding() {
        // given
        val messages = listOf( // more recent message is first on list
            testSystemMessage(userId = SELF_USER_ID, timestamp = Instant.parse("2021-01-01T00:00:01.000Z")),
            testRegularMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:00.000Z"))
        )
        // when
        val resultPrevious = messages.forIndex(1) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        val resultRecent = messages.forIndex(0) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        // then
        assertEquals(false, resultPrevious)
        assertEquals(false, resultRecent)
    }

    @Test
    fun givenPingAndThenRegularMessageFromSameUser_thenBothShouldNotHaveSmallBottomPadding() {
        // given
        val messages = listOf( // more recent message is first on list
            testRegularMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:01.000Z")),
            testPingMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:00.000Z"))
        )
        // when
        val resultPrevious = messages.forIndex(1) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        val resultRecent = messages.forIndex(0) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        // then
        assertEquals(false, resultPrevious)
        assertEquals(false, resultRecent)
    }

    @Test
    fun givenRegularAndThenPingMessageFromSameUser_thenBothShouldNotHaveSmallBottomPadding() {
        // given
        val messages = listOf( // more recent message is first on list
            testPingMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:01.000Z")),
            testRegularMessage(userId = SELF_USER_ID, instant = Instant.parse("2021-01-01T00:00:00.000Z"))
        )
        // when
        val resultPrevious = messages.forIndex(1) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        val resultRecent = messages.forIndex(0) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        // then
        assertEquals(false, resultPrevious)
        assertEquals(false, resultRecent)
    }

    @Test
    fun givenTwoRegularMessagesFromSameUserAndTimestampsWithinThreshold_thenPreviousShouldHaveSmallBottomPaddingAndRecentShouldNot() {
        // given
        val timestamp = Instant.parse("2021-01-01T00:00:00.000Z")
        val timestampMinusLessThanThreshold = timestamp - (AuthorHeaderHelper.AGGREGATION_TIME_WINDOW - 1L).milliseconds
        val messages = listOf(
            testRegularMessage(userId = SELF_USER_ID, instant = timestamp),
            testRegularMessage(userId = SELF_USER_ID, instant = timestampMinusLessThanThreshold)
        )
        // when
        val resultPrevious = messages.forIndex(1) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        val resultRecent = messages.forIndex(0) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        // then
        assertEquals(true, resultPrevious)
        assertEquals(false, resultRecent)
    }

    @Test
    fun givenTwoRegularMessagesFromSameUserAndTimestampsBeyondThreshold_thenBothShouldNotHaveSmallBottomPadding() {
        // given
        val timestamp = Instant.parse("2021-01-01T00:00:00.000Z")
        val timestampMinusMoreThanThreshold = timestamp - (AuthorHeaderHelper.AGGREGATION_TIME_WINDOW + 1L).milliseconds
        val messages = listOf(
            testRegularMessage(userId = SELF_USER_ID, instant = timestamp),
            testRegularMessage(userId = SELF_USER_ID, instant = timestampMinusMoreThanThreshold)
        )
        // when
        val resultPrevious = messages.forIndex(1) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        val resultRecent = messages.forIndex(0) { shouldHaveSmallBottomPadding(it.currentMessage, it.messageBelow) }
        // then
        assertEquals(false, resultPrevious)
        assertEquals(false, resultRecent)
    }

    companion object {
        private val SELF_USER_ID = UserId("self", "domain")
        private val OTHER_USER_ID = UserId("other", "domain")
        private val CONVERSATION_ID = ConversationId("value", "domain")

        private fun testSystemMessage(
            userId: UserId? = null,
            timestamp: Instant = Instant.parse("2021-01-01T00:00:00.000Z")
        ) = UIMessage.System(
            conversationId = CONVERSATION_ID,
            header = TestMessage.UI_MESSAGE_HEADER.copy(
                messageTime = TestMessage.UI_MESSAGE_HEADER.messageTime.copy(
                    instant = timestamp
                ),
                messageId = UUID.randomUUID().toString(),
                userId = userId
            ),
            source = MessageSource.OtherUser,
            messageContent = UIMessageContent.SystemMessage.HistoryLost
        )

        private fun testPingMessage(
            userId: UserId? = null,
            instant: Instant = Instant.parse("2021-01-01T00:00:00.000Z")
        ) = UIMessage.System(
            conversationId = CONVERSATION_ID,
            header = TestMessage.UI_MESSAGE_HEADER.copy(
                messageTime = TestMessage.UI_MESSAGE_HEADER.messageTime.copy(
                    instant = instant
                ),
                messageId = UUID.randomUUID().toString(),
                userId = userId
            ),
            source = MessageSource.OtherUser,
            messageContent = UIMessageContent.SystemMessage.Knock(UIText.DynamicString("pinged"), false),
        )

        private fun testRegularMessage(
            userId: UserId? = null,
            instant: Instant = Instant.parse("2021-01-01T00:00:00.000Z")
        ) = UIMessage.Regular(
            conversationId = CONVERSATION_ID,
            userAvatarData = UserAvatarData(asset = null, availabilityStatus = UserAvailabilityStatus.NONE),
            source = if (userId == SELF_USER_ID) MessageSource.Self else MessageSource.OtherUser,
            header = TestMessage.UI_MESSAGE_HEADER.copy(
                messageTime = TestMessage.UI_MESSAGE_HEADER.messageTime.copy(
                    instant = instant
                ),
                messageId = UUID.randomUUID().toString(),
                userId = userId
            ),
            messageContent = UIMessageContent.TextMessage(MessageBody(UIText.DynamicString("Some Text Message"))),
            messageFooter = com.wire.android.ui.home.conversations.model.MessageFooter(TestMessage.UI_MESSAGE_HEADER.messageId)
        )
    }
}
