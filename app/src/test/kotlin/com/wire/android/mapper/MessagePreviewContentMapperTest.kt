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

package com.wire.android.mapper

import com.wire.android.R
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestMessage
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.AssetType
import com.wire.kalium.logic.data.message.MessagePreviewContent
import com.wire.kalium.logic.data.message.UnreadEventType
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.`should contain`
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MessagePreviewContentMapperTest {

    @Test
    fun givenMultipleUnreadEvents_whenMappingToUIPreview_thenCorrectSortedUILastMessageContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.Text("admin", "Hello"),
        )
        val mentionCount = 2
        val missedCallCount = 3

        val unreadEventCount = mapOf(UnreadEventType.MENTION to mentionCount, UnreadEventType.MISSED_CALL to missedCallCount)

        val multipleMessage = messagePreview.toUIPreview(unreadEventCount).shouldBeInstanceOf<UILastMessageContent.MultipleMessage>()
        val results = multipleMessage.messages.filterIsInstance<UIText.PluralResource>()

        val sortedEventCount = unreadEventCount.toSortedMap()

        results.first().count shouldBeEqualTo sortedEventCount.values.first()
        results.last().count shouldBeEqualTo sortedEventCount.values.last()
    }

    @Test
    fun givenMissedCalls_whenMappingToUIPreview_thenCorrectUILastMessageContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.MissedCall("admin"),
        )
        val unreadCount = 2
        val unreadEventCount = mapOf(UnreadEventType.MISSED_CALL to unreadCount)

        val textMessage = messagePreview.toUIPreview(unreadEventCount).shouldBeInstanceOf<UILastMessageContent.TextMessage>()
        val result = textMessage.messageBody.message.shouldBeInstanceOf<UIText.PluralResource>()

        result.resId shouldBeEqualTo R.plurals.unread_event_call
        result.count shouldBeEqualTo unreadCount
    }

    @Test
    fun givenUnreadMentions_whenMappingToUIPreview_thenCorrectUILastMessageContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.MentionedSelf("admin"),
        )
        val unreadCount = 2
        val unreadEventCount = mapOf(UnreadEventType.MENTION to unreadCount)

        val textMessage = messagePreview.toUIPreview(unreadEventCount).shouldBeInstanceOf<UILastMessageContent.TextMessage>()
        val result = textMessage.messageBody.message.shouldBeInstanceOf<UIText.PluralResource>()

        result.resId shouldBeEqualTo R.plurals.unread_event_mention
        result.count shouldBeEqualTo unreadCount
    }

    @Test
    fun givenUnreadReplies_whenMappingToUIPreview_thenCorrectUILastMessageContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.MentionedSelf("admin"),
        )
        val unreadCount = 2
        val unreadEventCount = mapOf(UnreadEventType.REPLY to unreadCount)

        val textMessage = messagePreview.toUIPreview(unreadEventCount).shouldBeInstanceOf<UILastMessageContent.TextMessage>()
        val result = textMessage.messageBody.message.shouldBeInstanceOf<UIText.PluralResource>()

        result.resId shouldBeEqualTo R.plurals.unread_event_reply
        result.count shouldBeEqualTo unreadCount
    }

    @Test
    fun givenUnreadPings_whenMappingToUIPreview_thenCorrectUILastMessageContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.Knock("admin"),
        )
        val unreadCount = 2
        val unreadEventCount = mapOf(UnreadEventType.KNOCK to unreadCount)

        val textMessage = messagePreview.toUIPreview(unreadEventCount).shouldBeInstanceOf<UILastMessageContent.TextMessage>()
        val result = textMessage.messageBody.message.shouldBeInstanceOf<UIText.PluralResource>()

        result.resId shouldBeEqualTo R.plurals.unread_event_knock
        result.count shouldBeEqualTo unreadCount
    }

    @Test
    fun givenUnreadMessages_whenMappingToUIPreview_thenLastTextMessageContentShouldBeReturned() = runTest {
        val lastMessage = "See ya"
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.Text("admin", lastMessage),
        )

        val unreadCount = 2
        val unreadEventCount = mapOf(UnreadEventType.MESSAGE to unreadCount)

        val senderWithMessage = messagePreview.toUIPreview(unreadEventCount).shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val result = senderWithMessage.message.shouldBeInstanceOf<UIText.DynamicString>()

        result.value shouldBeEqualTo lastMessage
    }

    @Test
    fun givenLastAssetAudioConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.Asset("admin", AssetType.AUDIO),
        )

        val senderWithMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val result = senderWithMessage.message.shouldBeInstanceOf<UIText.StringResource>()

        result.resId shouldBeEqualTo R.string.last_message_audio
    }

    @Test
    fun givenLastAssetImageConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.Asset("admin", AssetType.IMAGE),
        )

        val senderWithMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val result = senderWithMessage.message.shouldBeInstanceOf<UIText.StringResource>()

        result.resId shouldBeEqualTo R.string.last_message_image
    }

    @Test
    fun givenLastAssetVideoConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.Asset("admin", AssetType.VIDEO),
        )

        val senderWithMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val result = senderWithMessage.message.shouldBeInstanceOf<UIText.StringResource>()

        result.resId shouldBeEqualTo R.string.last_message_video
    }

    @Test
    fun givenLastConversationRenamedMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.ConversationNameChange("admin"),
        )

        val senderWithMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val result = senderWithMessage.message.shouldBeInstanceOf<UIText.StringResource>()

        result.resId shouldBeEqualTo R.string.last_message_change_conversation_name
    }

    @Test
    fun givenLastConversationKnockMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.Knock("admin"),
        )

        val senderWithMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val result = senderWithMessage.message.shouldBeInstanceOf<UIText.StringResource>()

        result.resId shouldBeEqualTo R.string.last_message_knock
    }

    @Test
    fun givenUserLeftConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.MemberLeft("user"),
        )

        val senderWithMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val result = senderWithMessage.message.shouldBeInstanceOf<UIText.StringResource>()

        result.resId shouldBeEqualTo R.string.last_message_left_conversation
    }

    @Test
    fun givenUserJoinedConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.MemberJoined("user"),
        )

        val senderWithMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val result = senderWithMessage.message.shouldBeInstanceOf<UIText.StringResource>()

        result.resId shouldBeEqualTo R.string.last_message_joined_conversation
    }

    @Test
    fun givenSelfUserWasRemovedFromConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val otherUserId = UserId("otherValue", "selfDomain")

        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.MembersRemoved("admin", isSelfUserRemoved = true, listOf(otherUserId)),
        )

        val multipleMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.MultipleMessage>()
        val resources = multipleMessage.messages.filterIsInstance<UIText.StringResource>().map { it.resId }

        resources `should contain` R.string.last_message_removed
        resources `should contain` R.string.member_name_you_label_lowercase
    }

    @Test
    fun givenSelfAndOtherUserWasRemovedFromConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() =
        runTest {
            val otherUserId = UserId("otherValue", "selfDomain")

            val messagePreview = TestMessage.PREVIEW.copy(
                content = MessagePreviewContent.WithUser.MembersRemoved(
                    "admin",
                    isSelfUserRemoved = true,
                    listOf(otherUserId)
                )
            )

            val multipleMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.MultipleMessage>()
            val resources = multipleMessage.messages.filterIsInstance<UIText.StringResource>().map { it.resId }
            val plurals = multipleMessage.messages.filterIsInstance<UIText.PluralResource>().map { it.resId }

            resources `should contain` R.string.last_message_removed
            resources `should contain` R.string.member_name_you_label_lowercase
            plurals `should contain` R.plurals.last_message_people
        }
}
