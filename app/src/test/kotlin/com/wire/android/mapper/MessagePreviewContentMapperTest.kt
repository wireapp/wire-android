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
import com.wire.android.assertions.shouldBeEqualTo
import com.wire.android.assertions.shouldBeInstanceOf
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

        result.resId shouldBeEqualTo R.string.last_message_self_user_shared_audio
    }

    @Test
    fun givenLastAssetImageConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.Asset("admin", AssetType.IMAGE),
            isSelfMessage = true
        )

        val senderWithMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val result = senderWithMessage.message.shouldBeInstanceOf<UIText.StringResource>()

        result.resId shouldBeEqualTo R.string.last_message_self_user_shared_image

        val otherUserPreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.Asset("admin", AssetType.IMAGE),
            isSelfMessage = false
        )

        val otherUserWithMessage = otherUserPreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val otherSenderResult = otherUserWithMessage.message.shouldBeInstanceOf<UIText.StringResource>()

        otherSenderResult.resId shouldBeEqualTo R.string.last_message_other_user_shared_image
    }

    @Test
    fun givenSelfUserSharedLastAssetVideoConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() =
        runTest {
            val messagePreview = TestMessage.PREVIEW.copy(
                content = MessagePreviewContent.WithUser.Asset("admin", AssetType.VIDEO),
                isSelfMessage = true
            )

            val senderWithMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
            val result = senderWithMessage.message.shouldBeInstanceOf<UIText.StringResource>()

            result.resId shouldBeEqualTo R.string.last_message_self_user_shared_video
        }

    @Test
    fun givenLastConversationRenamedMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.ConversationNameChange("admin"),
        )

        val senderWithMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val result = senderWithMessage.message.shouldBeInstanceOf<UIText.StringResource>()

        result.resId shouldBeEqualTo R.string.last_message_other_changed_conversation_name
    }

    @Test
    fun givenLastConversationKnockMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val selfUserMessagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.Knock("admin"),
            isSelfMessage = true
        )
        val otherUserMessagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.Knock("admin"),
            isSelfMessage = false
        )

        val selfUserMessage = selfUserMessagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val otherUserMessage = otherUserMessagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val selfUserResult = selfUserMessage.message.shouldBeInstanceOf<UIText.StringResource>()
        val otherUserResult = otherUserMessage.message.shouldBeInstanceOf<UIText.StringResource>()

        selfUserResult.resId shouldBeEqualTo R.string.last_message_self_user_knock
        otherUserResult.resId shouldBeEqualTo R.string.last_message_other_user_knock
    }

    @Test
    fun givenUserLeftConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val selfUserMessagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.MemberLeft("user"),
            isSelfMessage = true
        )
        val otherUserMessagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.MemberLeft("user"),
            isSelfMessage = false
        )

        val selfSenderWithMessage =
            selfUserMessagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val otherSenderWithMessage =
            otherUserMessagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val result = selfSenderWithMessage.message.shouldBeInstanceOf<UIText.StringResource>()
        val otherUserResult = otherSenderWithMessage.message.shouldBeInstanceOf<UIText.StringResource>()

        result.resId shouldBeEqualTo R.string.last_message_self_user_left_conversation
        otherUserResult.resId shouldBeEqualTo R.string.last_message_other_user_left_conversation
    }

    @Test
    fun givenUserJoinedConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.MemberJoined("user"),
        )

        val senderWithMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val result = senderWithMessage.message.shouldBeInstanceOf<UIText.StringResource>()

        result.resId shouldBeEqualTo R.string.last_message_other_user_joined_conversation
    }

    @Test
    fun givenSelfUserWasRemovedFromConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.ConversationMembersRemoved("admin", isSelfUserRemoved = true, listOf()),
        )

        val uiPreviewMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.TextMessage>()
        val previewString = uiPreviewMessage.messageBody.message.shouldBeInstanceOf<UIText.StringResource>()
        previewString.resId shouldBeEqualTo R.string.last_message_other_removed_only_self_user
    }

    @Test
    fun givenSelfUserRemovedOtherUsersFromConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() =
        runTest {
            val otherRemovedUsers = listOf(UserId("otherValue", "a-domain"), UserId("otherValue2", "a-domain2"))
            val messagePreview = TestMessage.PREVIEW.copy(
                content = MessagePreviewContent.WithUser.ConversationMembersRemoved("admin", isSelfUserRemoved = false, otherRemovedUsers),
                isSelfMessage = true
            )

            val uiPreviewMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.TextMessage>()
            val previewString = uiPreviewMessage.messageBody.message.shouldBeInstanceOf<UIText.PluralResource>()
            previewString.count shouldBeEqualTo otherRemovedUsers.size
            previewString.resId shouldBeEqualTo R.plurals.last_message_self_removed_users
        }

    @Test
    fun givenSelfAndOtherUserWereRemovedFromConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() =
        runTest {
            val otherRemovedUsers = listOf(UserId("otherValue", "a-domain"), UserId("otherValue2", "a-domain2"))
            val messagePreview = TestMessage.PREVIEW.copy(
                content = MessagePreviewContent.WithUser.ConversationMembersRemoved(
                    username = "admin",
                    isSelfUserRemoved = true,
                    otherUserIdList = otherRemovedUsers
                )
            )

            val uiPreviewMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.TextMessage>()
            val previewString = uiPreviewMessage.messageBody.message.shouldBeInstanceOf<UIText.PluralResource>()
            previewString.count shouldBeEqualTo otherRemovedUsers.size
            previewString.resId shouldBeEqualTo R.plurals.last_message_other_removed_self_user_and_others
        }

    @Test
    fun givenSelfAndOtherUsersWereAddedToConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() =
        runTest {
            val otherUsersAdded = listOf(UserId("otherValue", "a-domain"), UserId("otherValue2", "a-domain2"))
            val messagePreview = TestMessage.PREVIEW.copy(
                content = MessagePreviewContent.WithUser.MembersAdded(
                    username = "admin",
                    isSelfUserAdded = true,
                    otherUserIdList = otherUsersAdded
                )
            )

            val uiPreviewMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.TextMessage>()
            val previewString = uiPreviewMessage.messageBody.message.shouldBeInstanceOf<UIText.PluralResource>()
            previewString.count shouldBeEqualTo otherUsersAdded.size
            previewString.resId shouldBeEqualTo R.plurals.last_message_other_added_self_user
        }

    @Test
    fun givenSelfAddedOtherUsersToConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val otherUsersAdded = listOf(UserId("otherValue", "a-domain"), UserId("otherValue2", "a-domain2"))
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.MembersAdded(
                username = "admin",
                isSelfUserAdded = false,
                otherUserIdList = otherUsersAdded
            ),
            isSelfMessage = true
        )

        val uiPreviewMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.TextMessage>()
        val previewString = uiPreviewMessage.messageBody.message.shouldBeInstanceOf<UIText.PluralResource>()
        previewString.count shouldBeEqualTo otherUsersAdded.size
        previewString.resId shouldBeEqualTo R.plurals.last_message_self_added_users
    }

    @Test
    fun givenOtherUsersWereAddedToConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val otherUsersAdded = listOf(UserId("otherValue", "a-domain"), UserId("otherValue2", "a-domain2"))
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.MembersAdded(
                username = "admin",
                isSelfUserAdded = false,
                otherUserIdList = otherUsersAdded
            )
        )

        val uiPreviewMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.TextMessage>()
        val previewString = uiPreviewMessage.messageBody.message.shouldBeInstanceOf<UIText.PluralResource>()
        previewString.count shouldBeEqualTo otherUsersAdded.size
        previewString.resId shouldBeEqualTo R.plurals.last_message_other_added_other_users
    }

    @Test
    fun givenFederatedUsersWereRemovedFromConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() =
        runTest {
            val otherRemovedUsers = listOf(UserId("otherValue", "a-domain"), UserId("otherValue2", "a-domain2"))
            val messagePreview = TestMessage.PREVIEW.copy(
                content = MessagePreviewContent.FederatedMembersRemoved(
                    isSelfUserRemoved = true,
                    otherUserIdList = otherRemovedUsers
                )
            )

            val uiPreviewMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.TextMessage>()
            val previewString = uiPreviewMessage.messageBody.message.shouldBeInstanceOf<UIText.PluralResource>()
            previewString.count shouldBeEqualTo otherRemovedUsers.size
            previewString.resId shouldBeEqualTo R.plurals.last_message_other_removed_self_user_and_others
        }

    @Test
    fun givenSelfAndFederatedUsersWereRemovedFromConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldReturn() =
        runTest {
            val otherRemovedUsers = listOf(UserId("otherValue", "a-domain"), UserId("otherValue2", "a-domain2"))
            val messagePreview = TestMessage.PREVIEW.copy(
                content = MessagePreviewContent.FederatedMembersRemoved(
                    isSelfUserRemoved = false,
                    otherUserIdList = otherRemovedUsers
                )
            )

            val uiPreviewMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.TextMessage>()
            val previewString = uiPreviewMessage.messageBody.message.shouldBeInstanceOf<UIText.PluralResource>()
            previewString.count shouldBeEqualTo otherRemovedUsers.size
            previewString.resId shouldBeEqualTo R.plurals.last_message_other_removed_other_users
        }

    @Test
    fun givenLastMessageIsDeleted_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.Deleted("admin"),
            isSelfMessage = false
        )

        val senderWithMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val result = senderWithMessage.message.shouldBeInstanceOf<UIText.StringResource>()

        result.resId shouldBeEqualTo R.string.deleted_message_text
    }
}
