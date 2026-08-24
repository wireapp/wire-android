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

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestConversation
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIMessageContent.AssetMessage
import com.wire.android.util.time.ISOFormatter
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.AssetContent.AssetMetadata
import com.wire.kalium.logic.data.message.DeliveryStatus
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.message.MessageEncryptionAlgorithm
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class RegularMessageContentMapperTest {

    @Test
    fun givenTextOrNullContent_whenMappingToTextMessageContent_thenCorrectValuesShouldBeReturned() = runTest {
        // Given
        val (arrangement, mapper) = Arrangement().arrange()
        val textContent = MessageContent.Text("Some Text Message")
        val nonTextContent = unknownMessage
        // When
        val resultText = mapper.toText(TestConversation.ID, textMessage.content, userMembers, DeliveryStatus.CompleteDelivery)
        val resultNonText = mapper.toText(TestConversation.ID, nonTextContent.content, userMembers, DeliveryStatus.CompleteDelivery)
        with(resultText) {
            assertTrue(
                messageBody.message is UIText.DynamicString &&
                        (messageBody.message as UIText.DynamicString).value == textContent.value
            )
        }
        with(resultNonText) {
            assertTrue(
                messageBody.message is UIText.StringResource &&
                        (messageBody.message as UIText.StringResource).resId == arrangement.messageResourceProvider.sentAMessageWithContent
            )
        }
    }

    @Test
    fun givenTextContent_whenMappingToTextMessageContent_thenMarkdownDocumentShouldBeParsedByUi() = runTest {
        // Given
        val (_, mapper) = Arrangement().arrange()

        // When
        val result = mapper.toText(TestConversation.ID, textMessage.content, userMembers, DeliveryStatus.CompleteDelivery)

        // Then
        assertNull(result.messageBody.markdownDocument)
    }

    @Test
    fun givenAssetContent_whenMappingToUIMessageContent_thenCorrectValuesShouldBeReturned() = runTest {
        // Given
        val (_, mapper) = Arrangement().arrange()
        val unknownImageMessageContent = AssetContent(
            0L,
            "name1",
            "image/xrz",
            AssetMetadata.Image(100, 100),
            assetRemoteData.copy(assetId = "image-id"),
        )
        val correctJPGImage = AssetContent(
            0L,
            "name2",
            "image/jpg",
            AssetMetadata.Image(100, 100),
            assetRemoteData.copy(assetId = "image-id2"),
        )

        val testMessage1 = buildAssetMessage(unknownImageMessageContent)
        val testMessage2 = buildAssetMessage(correctJPGImage)

        // When - Then
        val resultContentOther = mapper.toUIMessageContent(
            AssetMessageContentMetadata(unknownImageMessageContent),
            testMessage1,
            sender,
            userMembers,
            DeliveryStatus.CompleteDelivery
        )
        assertTrue(
            resultContentOther is AssetMessage &&
                    resultContentOther.assetId.value == unknownImageMessageContent.remoteData.assetId
        )

        // When - Then
        val resultContentImage = mapper.toUIMessageContent(
            AssetMessageContentMetadata(correctJPGImage),
            testMessage2,
            sender,
            userMembers,
            DeliveryStatus.CompleteDelivery
        )
        assertTrue(resultContentImage is UIMessageContent.IncompleteAssetMessage)
    }

    @Test
    fun givenSVGImageAssetContent_whenMappingToUIMessageContent_thenIsMappedToAssetMessage() = runTest {
        // Given
        val (_, mapper) = Arrangement().arrange()
        val contentImage = AssetContent(
            0L,
            "name",
            "image/svg",
            AssetMetadata.Image(100, 100),
            assetRemoteData.copy(assetId = "image-id"),
        )
        val testMessage = buildAssetMessage(contentImage)

        // When
        val resultContentImage = mapper.toUIMessageContent(
            AssetMessageContentMetadata(contentImage),
            testMessage,
            sender,
            userMembers,
            DeliveryStatus.CompleteDelivery
        )

        // Then
        assertTrue(resultContentImage is AssetMessage)
    }

    @Test
    fun givenPNGImageAssetContentWith0Width_whenMappingToUIMessageContent_thenIsMappedToAssetMessage() = runTest {
        // Given
        val (_, mapper) = Arrangement().arrange()
        val contentImage1 = AssetContent(
            0L,
            "name1",
            "image/png",
            AssetMetadata.Image(0, 0),
            assetRemoteData.copy(assetId = "image-id"),
        )
        val contentImage2 = AssetContent(
            0L,
            "name2",
            "image/png",
            AssetMetadata.Image(100, 100),
            assetRemoteData.copy(assetId = "image-id2"),
        )
        val testMessage1 = buildAssetMessage(contentImage1)
        val testMessage2 = buildAssetMessage(contentImage2)

        // When
        val resultContentImage1 = mapper.toUIMessageContent(
            AssetMessageContentMetadata(contentImage1),
            testMessage1,
            sender,
            userMembers,
            DeliveryStatus.CompleteDelivery
        )
        val resultContentImage2 = mapper.toUIMessageContent(
            AssetMessageContentMetadata(contentImage2),
            testMessage2,
            sender,
            userMembers,
            DeliveryStatus.CompleteDelivery
        )

        // Then
        assertTrue(resultContentImage1 is AssetMessage)
        assertTrue(resultContentImage2 is UIMessageContent.IncompleteAssetMessage)
    }

    private class Arrangement {

        @MockK
        lateinit var messageResourceProvider: MessageResourceProvider

        private val messageContentMapper by lazy {
            RegularMessageMapper(messageResourceProvider, ISOFormatter())
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { messageResourceProvider.memberNameDeleted } returns 10584735
            every { messageResourceProvider.memberNameYouLowercase } returns 24153498
            every { messageResourceProvider.memberNameYouTitlecase } returns 38946214
            every { messageResourceProvider.sentAMessageWithContent } returns 45407124
        }

        fun arrange() = this to messageContentMapper
    }

    companion object {
        private val assetRemoteData = AssetContent.RemoteData(
            otrKey = ByteArray(0),
            sha256 = ByteArray(16),
            assetId = "asset-id",
            assetToken = "==some-asset-token",
            assetDomain = "some-asset-domain.com",
            encryptionAlgorithm = MessageEncryptionAlgorithm.AES_GCM,
        )

        private val textMessage = regularMessage(MessageContent.Text("Some Text Message"))
        private val unknownMessage = regularMessage(MessageContent.Unknown("some-unhandled-message"))

        private fun buildAssetMessage(assetContent: AssetContent) = regularMessage(MessageContent.Asset(assetContent))

        private fun regularMessage(content: MessageContent.Regular) = Message.Regular(
            id = "messageID",
            content = content,
            conversationId = TestConversation.ID,
            date = Instant.parse("2022-03-30T15:36:00.000Z"),
            senderUserId = UserId("user-id", "domain"),
            senderClientId = ClientId("client-id"),
            status = Message.Status.Sent,
            editStatus = Message.EditStatus.NotEdited,
            isSelfMessage = false,
        )

        val sender = OtherUser(
            id = QualifiedID(
                value = "someSearchQuery",
                domain = "wire.com"
            ),
            name = null,
            handle = null,
            email = null,
            phone = null,
            accentId = 0,
            teamId = null,
            connectionStatus = ConnectionState.ACCEPTED,
            previewPicture = null,
            completePicture = null,
            availabilityStatus = UserAvailabilityStatus.NONE,
            userType = UserTypeInfo.Regular(UserType.FEDERATED),
            botService = null,
            deleted = false,
            defederated = false,
            isProteusVerified = false,
            supportedProtocols = setOf(SupportedProtocol.PROTEUS)
        )

        val userMembers = listOf(TestUser.MEMBER_SELF.user, TestUser.MEMBER_OTHER.user)
    }
}
