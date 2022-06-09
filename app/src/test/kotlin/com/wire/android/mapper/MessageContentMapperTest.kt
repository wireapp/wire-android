package com.wire.android.mapper

import android.content.res.Resources
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestMessage
import com.wire.android.framework.TestUser
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import com.wire.android.ui.home.conversations.model.MessageContent as MessageViewContent

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MessageContentMapperTest {

    @Test
    fun givenMemberDetails_whenMappingToSystemMessageMemberName_thenCorrectValuesShouldBeReturned() = runTest {
        // Given
        val (arrangement, mapper) = Arrangement().arrange()
        val selfMemberDetails = TestUser.MEMBER_SELF
        val deletedMemberDetails = TestUser.MEMBER_OTHER.copy(TestUser.OTHER_USER.copy(name = null))
        val otherMemberDetails = TestUser.MEMBER_OTHER
        // When
        val selfName = mapper.toSystemMessageMemberName(selfMemberDetails, MessageContentMapper.SelfNameType.NameOrDeleted)
        val selfResLower = mapper.toSystemMessageMemberName(selfMemberDetails, MessageContentMapper.SelfNameType.ResourceLowercase)
        val selfResTitle = mapper.toSystemMessageMemberName(selfMemberDetails, MessageContentMapper.SelfNameType.ResourceTitlecase)
        val deleted = mapper.toSystemMessageMemberName(deletedMemberDetails, MessageContentMapper.SelfNameType.NameOrDeleted)
        val otherName = mapper.toSystemMessageMemberName(otherMemberDetails, MessageContentMapper.SelfNameType.NameOrDeleted)
        // Then
        assert(selfName is UIText.DynamicString && selfName.value == selfMemberDetails.selfUser.name)
        assert(selfResLower is UIText.StringResource && selfResLower.resId == arrangement.messageResourceProvider.memberNameYouLowercase)
        assert(selfResTitle is UIText.StringResource && selfResTitle.resId == arrangement.messageResourceProvider.memberNameYouTitlecase)
        assert(deleted is UIText.StringResource && deleted.resId == arrangement.messageResourceProvider.memberNameDeleted)
        assert(otherName is UIText.DynamicString && otherName.value == otherMemberDetails.otherUser.name)
    }

    @Test
    fun givenTextOrNullContent_whenMappingToTextMessageContent_thenCorrectValuesShouldBeReturned() = runTest {
        // Given
        val (arrangement, mapper) = Arrangement().arrange()
        val message = "text-message"
        // When
        val resultText = mapper.toTextMessageContent(message)
        val resultNonText = mapper.toTextMessageContent(null)
        assert(resultText is UIText.DynamicString && resultText.value == message)
        assert(resultNonText is UIText.StringResource && resultNonText.resId == arrangement.messageResourceProvider.contentNotAvailable)
    }

    @Test
    fun givenServerContent_whenMappingToUIMessageContent_thenCorrectValuesShouldBeReturned() = runTest {
        // Given
        val (arrangement, mapper) = Arrangement().arrange()
        val userId1 = UserId("user-id1", "user-domain")
        val userId2 = UserId("user-id2", "user-domain")
        val userId3 = UserId("user-id3", "user-domain")
        val contentLeft = MessageContent.MemberChange.Removed(listOf(Member(userId1)))
        val contentRemoved = MessageContent.MemberChange.Removed(listOf(Member(userId2)))
        val contentAdded = MessageContent.MemberChange.Added(listOf(Member(userId2), Member(userId3)))
        val member1 = TestUser.MEMBER_OTHER.copy(TestUser.OTHER_USER.copy(id = userId1))
        val member2 = TestUser.MEMBER_OTHER.copy(TestUser.OTHER_USER.copy(id = userId2))
        val member3 = TestUser.MEMBER_OTHER.copy(TestUser.OTHER_USER.copy(id = userId3))
        // When
        val resultContentLeft = mapper.toServer(contentLeft, userId1, listOf(member1))
        val resultContentRemoved = mapper.toServer(contentRemoved, userId1, listOf(member1, member2))
        val resultContentAdded = mapper.toServer(contentAdded, userId1, listOf(member1, member2, member3))
        // Then
        assert(
            resultContentLeft is MessageViewContent.ServerMessage.MemberLeft
                    && resultContentLeft.author.asString(arrangement.resources) == member1.otherUser.name
        )
        assert(
            resultContentRemoved is MessageViewContent.ServerMessage.MemberRemoved
                    && resultContentRemoved.author.asString(arrangement.resources) == member1.otherUser.name
                    && resultContentRemoved.memberNames.size == 1
                    && resultContentRemoved.memberNames[0].asString(arrangement.resources) == member2.otherUser.name
        )
        assert(
            resultContentAdded is MessageViewContent.ServerMessage.MemberAdded
                    && resultContentAdded.author.asString(arrangement.resources) == member1.otherUser.name
                    && resultContentAdded.memberNames.size == 2
                    && resultContentAdded.memberNames[0].asString(arrangement.resources) == member2.otherUser.name
                    && resultContentAdded.memberNames[1].asString(arrangement.resources) == member3.otherUser.name
        )
    }

    @Test
    fun givenAssetContent_whenMappingToUIMessageContent_thenCorrectValuesShouldBeReturned() = runTest {
        // Given
        val (arrangement, mapper) = Arrangement().arrange()
        val userId = UserId("user-id1", "user-domain")
        val contentOther = TestMessage.ASSET_IMAGE_CONTENT.copy(
            mimeType = "other",
            remoteData = TestMessage.ASSET_REMOTE_DATA.copy(assetId = "id")
        )
        val contentImage = TestMessage.ASSET_IMAGE_CONTENT.copy(
            remoteData = TestMessage.ASSET_REMOTE_DATA.copy(assetId = "image-id")
        )
        // When - Then
        val resultContentOther = mapper.toAsset(QualifiedID("id", "domain"), "message-id", contentOther)
        coVerify(exactly = 0) { arrangement.getMessageAssetUseCase.invoke(any(), any()) }
        assert(resultContentOther is MessageViewContent.AssetMessage && resultContentOther.assetId == contentOther.remoteData.assetId)
        // When - Then
        val resultContentImage = mapper.toAsset(QualifiedID("id", "domain"), "message-id", contentImage)
        coVerify(exactly = 1) { arrangement.getMessageAssetUseCase.invoke(any(), any()) }
        assert(resultContentImage is MessageViewContent.ImageMessage && resultContentImage.assetId == contentImage.remoteData.assetId)
    }

    @Test
    fun givenMessage_whenMappingToUIMessageContent_thenCorrectValuesShouldBeReturned() = runTest {
        // Given
        val (_, mapper) = Arrangement().arrange()
        val visibleMessage = TestMessage.TEXT_MESSAGE.copy(visibility = Message.Visibility.VISIBLE)
        val nonVisibleMessage = TestMessage.TEXT_MESSAGE.copy(
            visibility = Message.Visibility.DELETED,
            content = MessageContent.DeleteMessage("")
        )
        // When
        val resultContentVisible = mapper.fromMessage(visibleMessage, listOf())
        val resultContentNonVisible = mapper.fromMessage(nonVisibleMessage, listOf())
        // Then
        assert(resultContentVisible !is MessageViewContent.DeletedMessage)
        assert(resultContentNonVisible is MessageViewContent.DeletedMessage)
    }

    private class Arrangement {

        @MockK
        lateinit var getMessageAssetUseCase: GetMessageAssetUseCase

        @MockK
        lateinit var messageResourceProvider: MessageResourceProvider

        @MockK
        lateinit var resources: Resources

        private val messageContentMapper by lazy { MessageContentMapper(getMessageAssetUseCase, messageResourceProvider) }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { getMessageAssetUseCase.invoke(any(), any()) } returns MessageAssetResult.Success("mocked-image".toByteArray())
            every { messageResourceProvider.memberNameDeleted } returns 10584735
            every { messageResourceProvider.memberNameYouLowercase } returns 24153498
            every { messageResourceProvider.memberNameYouTitlecase } returns 38946214
            every { messageResourceProvider.contentNotAvailable } returns 45407124
        }

        fun arrange() = this to messageContentMapper
    }
}
