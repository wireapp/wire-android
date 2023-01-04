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
    fun givenSelfUserLeftConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val selfUserId = UserId("selfValue", "selfDomain")

        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.MembersRemoved("admin", listOf(selfUserId)),
            selfUserId = selfUserId,
            senderUserId = selfUserId,
            isSelfMessage = true
        )

        val textMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.TextMessage>()
        val result = textMessage.messageBody.message.shouldBeInstanceOf<UIText.StringResource>()

        result.resId shouldBeEqualTo R.string.left_conversation_group_success
    }

    @Test
    fun givenSelfUserWasRemovedFromConversationMessage_whenMappingToUILastMessageContent_thenCorrectContentShouldBeReturned() = runTest {
        val selfUserId = UserId("selfValue", "selfDomain")
        val otherUserId = UserId("otherValue", "selfDomain")

        val messagePreview = TestMessage.PREVIEW.copy(
            content = MessagePreviewContent.WithUser.MembersRemoved("admin", listOf(selfUserId)),
            selfUserId = selfUserId,
            senderUserId = otherUserId,
        )

        val textMessage = messagePreview.uiLastMessageContent().shouldBeInstanceOf<UILastMessageContent.SenderWithMessage>()
        val result = textMessage.message.shouldBeInstanceOf<UIText.StringResource>()

        result.resId shouldBeEqualTo R.string.last_message_self_removed
    }

}
