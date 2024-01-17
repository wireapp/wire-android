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

import android.location.Location
import androidx.core.net.toUri
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogsState
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.messagecomposer.state.ComposableMessageBundle
import com.wire.android.ui.home.messagecomposer.state.Ping
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.failure.LegalHoldEnabledForConversationFailure
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCaseImpl.Companion.ASSET_SIZE_DEFAULT_LIMIT_BYTES
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
@Suppress("LargeClass")
class MessageComposerViewModelTest {

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageDialog is visible for my message`() =
        runTest {
            // Given
            val (_, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .arrange()

            // When
            viewModel.showDeleteMessageDialog("", true)

            // Then
            viewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
                forYourself = DeleteMessageDialogActiveState.Hidden,
                forEveryone = DeleteMessageDialogActiveState.Visible("", viewModel.conversationId)
            )
        }

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageDialog is visible for others message`() =
        runTest {
            // Given
            val (_, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .arrange()

            // When
            viewModel.showDeleteMessageDialog("", false)

            // Then
            viewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
                forYourself = DeleteMessageDialogActiveState.Visible("", viewModel.conversationId),
                forEveryone = DeleteMessageDialogActiveState.Hidden
            )
        }

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageForYourselfDialog is visible`() =
        runTest {
            // Given
            val (_, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .arrange()

            // When
            viewModel.deleteMessageHelper.showDeleteMessageForYourselfDialog("")

            // Then
            viewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
                forYourself = DeleteMessageDialogActiveState.Visible("", viewModel.conversationId),
                forEveryone = DeleteMessageDialogActiveState.Hidden
            )
        }

    @Test
    fun `validate deleteMessageDialogsState states when dialogs are dismissed`() {
        // Given
        val (_, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .arrange()

        // When
        viewModel.deleteMessageHelper.onDeleteDialogDismissed()

        // Then
        viewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Hidden,
            forEveryone = DeleteMessageDialogActiveState.Hidden
        )
    }

    @Test
    fun `given a failure, when deleting messages, then the error state is updated`() = runTest {
        // Given
        val (_, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withFailureOnDeletingMessages().arrange()

        viewModel.messageComposerViewState
        viewModel.infoMessage.test {

            // when
            expectNoEvents()
            viewModel.deleteMessageHelper.onDeleteMessage("messageId", true)

            // Then
            assertEquals(ConversationSnackbarMessages.ErrorDeletingMessage, awaitItem())
        }
    }

    @Test
    fun `given a failure, when deleting messages, then the delete dialog state is closed`() =
        runTest {
            // Given
            val (_, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withFailureOnDeletingMessages()
                .arrange()

            viewModel.messageComposerViewState
            // When
            viewModel.deleteMessageHelper.onDeleteMessage("messageId", true)

            // Then
            val expectedState = DeleteMessageDialogsState.States(
                DeleteMessageDialogActiveState.Hidden,
                DeleteMessageDialogActiveState.Hidden
            )
            assertEquals(expectedState, viewModel.deleteMessageDialogsState)
        }

    @Test
    fun `given the user sends an asset message, when invoked, then sendAssetMessageUseCase gets called`() =
        runTest {
            // Given
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .withGetAssetSizeLimitUseCase(false, limit)
                .arrange()
            val mockedAttachment = AssetBundle(
                "file/x-zip",
                "Mocked-data-path".toPath(),
                1L,
                "mocked_file.zip",
                AttachmentType.GENERIC_FILE
            )

            // When
            viewModel.sendAttachment(mockedAttachment)

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
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val assetPath = "mocked-asset-data-path".toPath()
            val assetContent = "some-dummy-image".toByteArray()
            val assetName = "mocked_image.jpeg"
            val assetSize = 1L
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withStoredAsset(assetPath, assetContent)
                .withSuccessfulSendAttachmentMessage()
                .withGetAssetSizeLimitUseCase(true, limit)
                .arrange()
            val mockedAttachment = AssetBundle(
                "image/jpeg", assetPath, assetSize, assetName, AttachmentType.IMAGE
            )

            // When
            viewModel.sendAttachment(mockedAttachment)

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
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .arrange()
            val mockedAttachment = null

            // When
            viewModel.sendAttachment(mockedAttachment)

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
    fun `given a user picks an image asset larger than 15MB, when invoked, then sendAssetMessageUseCase isn't called`() =
        runTest {
            // Given
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val mockedAttachment = AssetBundle(
                "image/jpeg",
                "some-data-path".toPath(),
                limit + 1L,
                "mocked_image.jpeg",
                AttachmentType.IMAGE
            )
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .withGetAssetSizeLimitUseCase(true, limit)
                .withGetAssetBundleFromUri(mockedAttachment)
                .arrange()
            val mockedMessageBundle = ComposableMessageBundle.AttachmentPickedBundle(
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
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val mockedAttachment = AssetBundle(
                "file/x-zip",
                "some-data-path".toPath(),
                limit + 1L,
                "mocked_asset.zip",
                AttachmentType.GENERIC_FILE
            )
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .withGetAssetSizeLimitUseCase(false, limit)
                .withGetAssetBundleFromUri(mockedAttachment)
                .arrange()
            val mockedMessageBundle = ComposableMessageBundle.AttachmentPickedBundle(
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
    fun `given that a user picks too large asset that needs saving if invalid, when invoked, then saveToExternalMediaStorage is called`() =
        runTest {
            // Given
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val mockedAttachment = AssetBundle(
                "file/x-zip",
                "some-data-path".toPath(),
                limit + 1L,
                "mocked_asset.zip",
                AttachmentType.GENERIC_FILE
            )
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .withGetAssetSizeLimitUseCase(false, limit)
                .withGetAssetBundleFromUri(mockedAttachment)
                .withSaveToExternalMediaStorage("mocked_image.jpeg")
                .arrange()
            val mockedMessageBundle = ComposableMessageBundle.AttachmentPickedBundle(
                attachmentUri = UriAsset("mocked_image.jpeg".toUri(), true)
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
            coVerify {
                arrangement.fileManager.saveToExternalMediaStorage(
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
    fun `given attachment picked and null when getting asset bundle from uri, then show message to user`() =
        runTest {
            // Given
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .withGetAssetSizeLimitUseCase(false, limit)
                .withGetAssetBundleFromUri(null)
                .withSaveToExternalMediaStorage("mocked_image.jpeg")
                .arrange()
            val mockedMessageBundle = ComposableMessageBundle.AttachmentPickedBundle(
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
    fun `given that a team user sends an asset message larger than 25MB, when invoked, then sendAssetMessageUseCase is called`() =
        runTest {
            // Given
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .withGetAssetSizeLimitUseCase(false, limit)
                .arrange()
            val mockedAttachment = AssetBundle(
                "file/x-zip",
                "some-data-path".toPath(),
                limit + 1,
                "mocked_asset.jpeg",
                AttachmentType.GENERIC_FILE
            )

            // When
            viewModel.sendAttachment(mockedAttachment)

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
            assert(viewModel.assetTooLargeDialogState is AssetTooLargeDialogState.Hidden)
        }

    @Test
    fun `given that a user sends an ping message, when invoked, then sendKnockUseCase and pingRinger are called`() =
        runTest {
            // Given
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .arrange()

            // When
            viewModel.trySendMessage(
                messageBundle = Ping
            )

            // Then
            coVerify(exactly = 1) { arrangement.sendKnockUseCase.invoke(any(), any()) }
            verify(exactly = 1) { arrangement.pingRinger.ping(any(), isReceivingPing = false) }
        }

    @Test
    fun `given that a user updates the self-deleting message timer, when invoked, then the timer gets successfully updated`() =
        runTest {
            // Given
            val expectedDuration = 1.toDuration(DurationUnit.HOURS)
            val expectedTimer = SelfDeletionTimer.Enabled(expectedDuration)
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withPersistSelfDeletionStatus()
                .arrange()

            // When
            viewModel.updateSelfDeletingMessages(expectedTimer)

            // Then
            coVerify(exactly = 1) {
                arrangement.persistSelfDeletionStatus.invoke(
                    arrangement.conversationId,
                    expectedTimer
                )
            }
            assertInstanceOf(SelfDeletionTimer.Enabled::class.java, viewModel.messageComposerViewState.value.selfDeletionTimer)
            assertEquals(expectedDuration, viewModel.messageComposerViewState.value.selfDeletionTimer.duration)
        }

    @Test
    fun `given a valid observed enforced self-deleting message timer, when invoked, then the timer gets successfully updated`() =
        runTest {
            // Given
            val expectedDuration = 1.toDuration(DurationUnit.DAYS)
            val expectedTimer = SelfDeletionTimer.Enabled(expectedDuration)
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withObserveSelfDeletingStatus(expectedTimer)
                .arrange()

            // When

            // Then
            coVerify(exactly = 1) {
                arrangement.observeConversationSelfDeletionStatus.invoke(
                    arrangement.conversationId,
                    true
                )
            }
            assertInstanceOf(SelfDeletionTimer.Enabled::class.java, viewModel.messageComposerViewState.value.selfDeletionTimer)
            assertEquals(expectedDuration, viewModel.messageComposerViewState.value.selfDeletionTimer.duration)
        }

    @Test
    fun `given the user sends an audio message, when invoked, then sendAssetMessageUseCase gets called`() =
        runTest {
            // Given
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val assetPath = "mocked-asset-data-path".toPath()
            val assetContent = "some-dummy-audio".toByteArray()
            val assetName = "mocked_audio.m4a"
            val assetSize = 1L
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withStoredAsset(assetPath, assetContent)
                .withSuccessfulSendAttachmentMessage()
                .withGetAssetSizeLimitUseCase(false, limit)
                .arrange()
            val mockedAttachment = AssetBundle(
                "audio/mp4", assetPath, assetSize, assetName, AttachmentType.AUDIO
            )

            // When
            viewModel.sendAttachment(mockedAttachment)

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
    fun `given that user sends a text message, when invoked, then send typing stopped event is called`() = runTest {
        // given
        val (arrangement, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendTextMessage()
            .arrange()

        // when
        viewModel.trySendMessage(ComposableMessageBundle.SendTextMessageBundle("mocked-text-message", emptyList()))

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
    }

    @Test
    fun `given that user sends an edited text message, when invoked, then send typing stopped event is called`() = runTest {
        // given
        val (arrangement, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendEditTextMessage()
            .arrange()

        // when
        viewModel.trySendMessage(ComposableMessageBundle.EditMessageBundle("mocked-text-message", "new-mocked-text-message", emptyList()))

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
    }

    @Test
    fun `given that user types a text message, when invoked typing invoked, then send typing event is called`() = runTest {
        // given
        val (arrangement, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .arrange()

        // when
        viewModel.sendTypingEvent(Conversation.TypingIndicatorMode.STARTED)

        // then
        coVerify(exactly = 1) {
            arrangement.sendTypingEvent.invoke(
                any(),
                eq(Conversation.TypingIndicatorMode.STARTED)
            )
        }
    }

    @Test
    fun `given that user need to be informed about verification, when invoked sending, then message is not sent and dialog shown`() =
        runTest {
            // given
            val messageBundle = ComposableMessageBundle.SendTextMessageBundle("mocked-text-message", emptyList())
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
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
            val messageBundle = ComposableMessageBundle.SendTextMessageBundle("mocked-text-message", emptyList())
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
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
            val messageBundle = ComposableMessageBundle.SendTextMessageBundle("mocked-text-message", emptyList())
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
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
            val messageBundle = ComposableMessageBundle.SendTextMessageBundle("mocked-text-message", emptyList())
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
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
            val messageBundle = ComposableMessageBundle.SendTextMessageBundle("mocked-text-message", emptyList())
            val messageId = "messageId"
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withFailedSendTextMessage(LegalHoldEnabledForConversationFailure(messageId))
                .arrange()
            // when
            viewModel.trySendMessage(messageBundle)
            // then
            coVerify(exactly = 0) { arrangement.retryFailedMessageUseCase.invoke(eq(messageId), any()) }
            assertEquals(
                SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold.AfterSending(messageId),
                viewModel.sureAboutMessagingDialogState
            )
        }

    @Test
    fun `given that user chose to dismiss when enabled legal hold when sending fails, then message is not resent and dialog hidden`() =
        runTest {
            // given
            val messageBundle = ComposableMessageBundle.SendTextMessageBundle("mocked-text-message", emptyList())
            val messageId = "messageId"
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
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
            val messageBundle = ComposableMessageBundle.SendTextMessageBundle("mocked-text-message", emptyList())
            val messageId = "messageId"
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
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
                "mocked-location-message",
                Location("mocked-provider")
            )
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
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
}
