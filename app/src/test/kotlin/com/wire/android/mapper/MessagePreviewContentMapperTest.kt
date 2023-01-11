package com.wire.android.mapper

import com.wire.android.R
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestMessage
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.AssetType
import com.wire.kalium.logic.data.message.MessagePreviewContent
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
                content = MessagePreviewContent.WithUser.MembersRemoved("admin", isSelfUserRemoved = true, listOf(otherUserId)),
            )

            val multipleMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.MultipleMessage>()
            val resources = multipleMessage.messages.filterIsInstance<UIText.StringResource>().map { it.resId }
            val plurals = multipleMessage.messages.filterIsInstance<UIText.PluralResource>().map { it.resId }

            resources `should contain` R.string.last_message_removed
            resources `should contain` R.string.member_name_you_label_lowercase
            plurals `should contain` R.plurals.last_message_people
        }

}
