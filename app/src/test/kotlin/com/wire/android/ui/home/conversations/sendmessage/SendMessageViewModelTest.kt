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

package com.wire.android.ui.home.conversations.sendmessage

import android.location.Location
import androidx.core.net.toUri
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.ui.home.conversations.AssetTooLargeDialogState
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages
import com.wire.android.ui.home.conversations.SureAboutMessagingDialogState
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.ui.home.messagecomposer.model.ComposableMessageBundle
import com.wire.android.ui.home.messagecomposer.model.Ping
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.failure.LegalHoldEnabledForConversationFailure
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
@Suppress("LargeClass")
class SendMessageViewModelTest {

    @Test
    fun `given the user sends an asset message, when invoked, then sendAssetMessageUseCase gets called`() =
        runTest {
            // Given
            val mockedAttachment = AssetBundle(
                "key",
                "file/x-zip",
                "Mocked-data-path".toPath(),
                1L,
                "mocked_file.zip",
                AttachmentType.GENERIC_FILE
            )
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .withHandleUriAsset(HandleUriAssetUseCase.Result.Success(mockedAttachment))
                .arrange()

            // When
            viewModel.sendAttachment(mockedAttachment, conversationId)

            // Then
            coVerify(exactly = 1) {
                arrangement.sendAssetMessage.invoke(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
        }

    @Test
    fun `given the user sends an image message, when invoked, then sendAssetMessageUseCase gets called`() =
        runTest {
            // Given
            val assetPath = "mocked-asset-data-path".toPath()
            val assetContent = "some-dummy-image".toByteArray()
            val assetName = "mocked_image.jpeg"
            val assetSize = 1L
            val mockedAttachment = AssetBundle(
                "key",
                "image/jpeg", assetPath, assetSize, assetName, AttachmentType.IMAGE
            )
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withStoredAsset(assetPath, assetContent)
                .withSuccessfulSendAttachmentMessage()
                .withHandleUriAsset(HandleUriAssetUseCase.Result.Success(mockedAttachment))
                .arrange()

            // When
            viewModel.sendAttachment(mockedAttachment, conversationId)

            // Then
            coVerify(exactly = 1) {
                arrangement.sendAssetMessage.invoke(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
        }

    @Test
    fun `given the user picks a null attachment, when invoking sendAttachmentMessage, no use case gets called`() =
        runTest {
            // Given
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .arrange()
            val mockedAttachment = null

            // When
            viewModel.sendAttachment(mockedAttachment, conversationId)

            coVerify(inverse = true) {
                arrangement.sendAssetMessage.invoke(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
        }

    @Test
    fun `given a user picks an too large image asset, when invoked, then sendAssetMessageUseCase isn't called`() =
        runTest {
            // Given
            val limit = 25
            val mockedAttachment = AssetBundle(
                "key",
                "image/jpeg",
                "some-data-path".toPath(),
                limit + 1L,
                "mocked_image.jpeg",
                AttachmentType.IMAGE
            )
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .withHandleUriAsset(HandleUriAssetUseCase.Result.Failure.AssetTooLarge(mockedAttachment, 25))
                .arrange()
            val mockedMessageBundle = ComposableMessageBundle.AttachmentPickedBundle(
                conversationId = conversationId,
                attachmentUri = UriAsset("mocked_image.jpeg".toUri(), false)
            )

            // When
            viewModel.trySendMessage(mockedMessageBundle)

            // Then
            coVerify(inverse = true) {
                arrangement.sendAssetMessage.invoke(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
            assert(viewModel.assetTooLargeDialogState is AssetTooLargeDialogState.Visible)
        }

    @Test
    fun `given that a free user picks an asset larger than 25MB, when invoked, then sendAssetMessageUseCase isn't called`() =
        runTest {
            // Given
            val limit = 25
            val mockedAttachment = AssetBundle(
                "key",
                "file/x-zip",
                "some-data-path".toPath(),
                limit + 1L,
                "mocked_asset.zip",
                AttachmentType.GENERIC_FILE
            )
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .withHandleUriAsset(HandleUriAssetUseCase.Result.Failure.AssetTooLarge(mockedAttachment, limit))
                .arrange()
            val mockedMessageBundle = ComposableMessageBundle.AttachmentPickedBundle(
                conversationId = conversationId,
                attachmentUri = UriAsset("mocked_image.jpeg".toUri(), false)
            )

            // When
            viewModel.trySendMessage(mockedMessageBundle)

            // Then
            coVerify(inverse = true) {
                arrangement.sendAssetMessage.invoke(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
            assert(viewModel.assetTooLargeDialogState is AssetTooLargeDialogState.Visible)
        }

    @Test
    fun `given attachment picked and error when handling asset from uri, then show message to user`() =
        runTest {
            // Given
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .withHandleUriAsset(HandleUriAssetUseCase.Result.Failure.Unknown)
                .arrange()
            val mockedMessageBundle = ComposableMessageBundle.AttachmentPickedBundle(
                conversationId = conversationId,
                attachmentUri = UriAsset("mocked_image.jpeg".toUri(), false)
            )

            // When
            viewModel.infoMessage.test {
                viewModel.trySendMessage(mockedMessageBundle)

                // Then
                coVerify(inverse = true) {
                    arrangement.sendAssetMessage.invoke(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                    )
                }
                assertEquals(ConversationSnackbarMessages.ErrorPickingAttachment, awaitItem())
            }
        }

    @Test
    fun `given that a user sends an ping message, when invoked, then sendKnockUseCase and pingRinger are called`() =
        runTest {
            // Given
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .arrange()

            // When
            viewModel.trySendMessage(
                messageBundle = Ping(conversationId)
            )

            // Then
            coVerify(exactly = 1) { arrangement.sendKnockUseCase.invoke(any(), any()) }
            verify(exactly = 1) { arrangement.pingRinger.ping(any(), isReceivingPing = false) }
        }

    @Test
    fun `given the user sends an audio message, when invoked, then sendAssetMessageUseCase gets called`() =
        runTest {
            // Given
            val assetPath = "mocked-asset-data-path".toPath()
            val assetContent = "some-dummy-audio".toByteArray()
            val assetName = "mocked_audio.m4a"
            val assetSize = 1L
            val mockedAttachment = AssetBundle(
                "key",
                "audio/mp4", assetPath, assetSize, assetName, AttachmentType.AUDIO
            )
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withStoredAsset(assetPath, assetContent)
                .withSuccessfulSendAttachmentMessage()
                .withHandleUriAsset(HandleUriAssetUseCase.Result.Success(mockedAttachment))
                .arrange()

            // When
            viewModel.sendAttachment(mockedAttachment, conversationId)

            // Then
            coVerify(exactly = 1) {
                arrangement.sendAssetMessage.invoke(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
        }

    @Test
    fun `given that user sends a text message, when invoked, then send typing stopped event and remove draft are called`() = runTest {
        // given
        val (arrangement, viewModel) = SendMessageViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendTextMessage()
            .arrange()

        // when
        viewModel.trySendMessage(ComposableMessageBundle.SendTextMessageBundle(conversationId, "mocked-text-message", emptyList()))

        // then
        coVerify(exactly = 1) {
            arrangement.sendTextMessage.invoke(
                any(),
                any(),
                any(),
                any()
            )
        }
        coVerify(exactly = 1) {
            arrangement.sendTypingEvent.invoke(
                any(),
                eq(Conversation.TypingIndicatorMode.STOPPED)
            )
        }
        coVerify(exactly = 1) {
            arrangement.removeMessageDraftUseCase.invoke(any())
        }
    }

    @Test
    fun `given that user sends an edited text message, when invoked, then send typing stopped event and remove draft are called`() =
        runTest {
            // given
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendEditTextMessage()
                .arrange()

            // when
            viewModel.trySendMessage(
                ComposableMessageBundle.EditMessageBundle(
                    conversationId,
                    "mocked-text-message",
                    "new-mocked-text-message",
                    emptyList()
                )
            )

            // then
            coVerify(exactly = 1) {
                arrangement.sendEditTextMessage.invoke(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
            coVerify(exactly = 1) {
                arrangement.sendTypingEvent.invoke(
                    any(),
                    eq(Conversation.TypingIndicatorMode.STOPPED)
                )
            }
            coVerify(exactly = 1) {
                arrangement.removeMessageDraftUseCase.invoke(any())
            }
        }

    @Test
    fun `given that user need to be informed about verification, when invoked sending, then message is not sent and dialog shown`() =
        runTest {
            // given
            val messageBundle = ComposableMessageBundle.SendTextMessageBundle(conversationId, "mocked-text-message", emptyList())
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withInformAboutVerificationBeforeMessagingFlag(false)
                .arrange()

            // when
            viewModel.trySendMessage(messageBundle)

            // then
            coVerify(exactly = 0) {
                arrangement.sendTextMessage.invoke(
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
            assertEquals(
                SureAboutMessagingDialogState.Visible.ConversationVerificationDegraded(messageBundle),
                viewModel.sureAboutMessagingDialogState
            )
        }

    @Test
    fun `given that user needs to be informed about enabled legal hold when sending, then message is not sent and dialog shown`() =
        runTest {
            // given
            val messageBundle = ComposableMessageBundle.SendTextMessageBundle(conversationId, "mocked-text-message", emptyList())
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withObserveConversationUnderLegalHoldNotified(false)
                .arrange()
            // when
            viewModel.trySendMessage(messageBundle)
            // then
            coVerify(exactly = 0) { arrangement.sendTextMessage.invoke(any(), any(), any(), any()) }
            assertEquals(
                SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold.BeforeSending(messageBundle),
                viewModel.sureAboutMessagingDialogState
            )
        }

    @Test
    fun `given that user chose to dismiss when enabled legal hold before sending, then message is not sent and dialog hidden`() =
        runTest {
            // given
            val messageBundle = ComposableMessageBundle.SendTextMessageBundle(conversationId, "mocked-text-message", emptyList())
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withObserveConversationUnderLegalHoldNotified(false)
                .arrange()
            viewModel.trySendMessage(messageBundle)
            // when
            arrangement.withObserveConversationUnderLegalHoldNotified(true)
            viewModel.dismissSureAboutSendingMessage()
            advanceUntilIdle()
            // then
            coVerify(exactly = 0) { arrangement.sendTextMessage.invoke(any(), any(), any(), any()) }
            assertEquals(SureAboutMessagingDialogState.Hidden, viewModel.sureAboutMessagingDialogState)
        }

    @Test
    fun `given that user chose to send anyway when enabled legal hold before sending, then message is sent and dialog hidden`() =
        runTest {
            // given
            val messageBundle = ComposableMessageBundle.SendTextMessageBundle(conversationId, "mocked-text-message", emptyList())
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withObserveConversationUnderLegalHoldNotified(false)
                .withSuccessfulSendTextMessage()
                .arrange()
            viewModel.trySendMessage(messageBundle)
            // when
            arrangement.withObserveConversationUnderLegalHoldNotified(true)
            viewModel.acceptSureAboutSendingMessage()
            advanceUntilIdle()
            // then
            coVerify(exactly = 1) { arrangement.sendTextMessage.invoke(any(), any(), any(), any()) }
            assertEquals(SureAboutMessagingDialogState.Hidden, viewModel.sureAboutMessagingDialogState)
        }

    @Test
    fun `given that user needs to be informed about enabled legal hold when sending fails, then message is not resent and dialog shown`() =
        runTest {
            // given
            val messageBundle = ComposableMessageBundle.SendTextMessageBundle(conversationId, "mocked-text-message", emptyList())
            val messageId = "messageId"
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withFailedSendTextMessage(LegalHoldEnabledForConversationFailure(messageId))
                .arrange()
            // when
            viewModel.trySendMessage(messageBundle)
            // then
            coVerify(exactly = 0) { arrangement.retryFailedMessageUseCase.invoke(eq(messageId), any()) }
            assertEquals(
                SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold.AfterSending(messageId, conversationId),
                viewModel.sureAboutMessagingDialogState
            )
        }

    @Test
    fun `given that user chose to dismiss when enabled legal hold when sending fails, then message is not resent and dialog hidden`() =
        runTest {
            // given
            val messageBundle = ComposableMessageBundle.SendTextMessageBundle(conversationId, "mocked-text-message", emptyList())
            val messageId = "messageId"
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withObserveConversationUnderLegalHoldNotified(true)
                .withFailedSendTextMessage(LegalHoldEnabledForConversationFailure(messageId))
                .arrange()
            viewModel.trySendMessage(messageBundle)
            // when
            viewModel.dismissSureAboutSendingMessage()
            advanceUntilIdle()
            // then
            coVerify(exactly = 0) { arrangement.retryFailedMessageUseCase.invoke(any(), any()) }
            assertEquals(SureAboutMessagingDialogState.Hidden, viewModel.sureAboutMessagingDialogState)
        }

    @Test
    fun `given that user chose to send anyway when enabled legal hold when sending fails, then message is resent and dialog hidden`() =
        runTest {
            // given
            val messageBundle = ComposableMessageBundle.SendTextMessageBundle(conversationId, "mocked-text-message", emptyList())
            val messageId = "messageId"
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withFailedSendTextMessage(LegalHoldEnabledForConversationFailure(messageId))
                .withSuccessfulRetryFailedMessage()
                .arrange()
            viewModel.trySendMessage(messageBundle)
            // when
            viewModel.acceptSureAboutSendingMessage()
            advanceUntilIdle()
            // then
            coVerify(exactly = 1) { arrangement.retryFailedMessageUseCase.invoke(eq(messageId), any()) }
            assertEquals(SureAboutMessagingDialogState.Hidden, viewModel.sureAboutMessagingDialogState)
        }

    @Test
    fun `given that user sends a location message and valid, then message is sent to use case`() =
        runTest {
            // given
            val messageBundle = ComposableMessageBundle.LocationBundle(
                conversationId,
                "mocked-location-message",
                Location("mocked-provider")
            )
            val (arrangement, viewModel) = SendMessageViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendLocationMessage()
                .arrange()
            viewModel.trySendMessage(messageBundle)

            // when
            advanceUntilIdle()
            // then
            coVerify(exactly = 1) { arrangement.sendLocation.invoke(any(), any(), any(), any(), any()) }
            assertEquals(SureAboutMessagingDialogState.Hidden, viewModel.sureAboutMessagingDialogState)
        }

    companion object {
        val conversationId: ConversationId = ConversationId("some-dummy-value", "some.dummy.domain")
    }
}
