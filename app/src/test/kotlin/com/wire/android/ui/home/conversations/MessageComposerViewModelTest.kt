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

package com.wire.android.ui.home.conversations

import androidx.core.net.toUri
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogsState
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCaseImpl.Companion.ASSET_SIZE_DEFAULT_LIMIT_BYTES
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MessageComposerViewModelTest {

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageDialog is visible for my message`() = runTest {
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
    fun `validate deleteMessageDialogsState states when deleteMessageDialog is visible for others message`() = runTest {
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
    fun `validate deleteMessageDialogsState states when deleteMessageForYourselfDialog is visible`() = runTest {
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
        val (_, viewModel) = MessageComposerViewModelArrangement().arrange()

        // When
        viewModel.deleteMessageHelper.onDeleteDialogDismissed()

        // Then
        viewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Hidden, forEveryone = DeleteMessageDialogActiveState.Hidden
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
    fun `given a failure, when deleting messages, then the delete dialog state is closed`() = runTest {
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
    fun `given the user sends an asset message, when invoked, then sendAssetMessageUseCase gets called`() = runTest {
        // Given
        val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
        val (arrangement, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendAttachmentMessage()
            .withGetAssetSizeLimitUseCase(false, limit)
            .arrange()
        val mockedAttachment = AssetBundle(
            "file/x-zip", "Mocked-data-path".toPath(), 1L, "mocked_file.zip", AttachmentType.GENERIC_FILE
        )

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        // Then
        coVerify(exactly = 1) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given the user sends an image message, when invoked, then sendAssetMessageUseCase gets called`() = runTest {
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
        viewModel.sendAttachmentMessage(mockedAttachment)

        // Then
        coVerify(exactly = 1) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given the user picks a null attachment, when invoking sendAttachmentMessage, no use case gets called`() = runTest {
        // Given
        val (arrangement, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendAttachmentMessage()
            .arrange()
        val mockedAttachment = null

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        coVerify(inverse = true) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given a user picks an image asset larger than 15MB, when invoked, then sendAssetMessageUseCase isn't called`() = runTest {
        // Given
        val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
        val mockedAttachment = AssetBundle(
            "image/jpeg", "some-data-path".toPath(), limit + 1L, "mocked_image.jpeg", AttachmentType.IMAGE
        )
        val (arrangement, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendAttachmentMessage()
            .withGetAssetSizeLimitUseCase(true, limit)
            .withGetAssetBundleFromUri(mockedAttachment)
            .arrange()
        val mockedUri = UriAsset("mocked_image.jpeg".toUri(), false)

        // When
        viewModel.attachmentPicked(mockedUri)

        // Then
        coVerify(inverse = true) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
        assert(viewModel.messageComposerViewState.assetTooLargeDialogState is AssetTooLargeDialogState.Visible)
    }

    @Test
    fun `given that a free user picks an asset larger than 25MB, when invoked, then sendAssetMessageUseCase isn't called`() =
        runTest {
            // Given
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val mockedAttachment = AssetBundle(
                "file/x-zip", "some-data-path".toPath(), limit + 1L, "mocked_asset.zip", AttachmentType.GENERIC_FILE
            )
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .withGetAssetSizeLimitUseCase(false, limit)
                .withGetAssetBundleFromUri(mockedAttachment)
                .arrange()
            val mockedUri = UriAsset("mocked_image.jpeg".toUri(), false)

            // When
            viewModel.attachmentPicked(mockedUri)

            // Then
            coVerify(inverse = true) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
            assert(viewModel.messageComposerViewState.assetTooLargeDialogState is AssetTooLargeDialogState.Visible)
        }

    @Test
    fun `given that a user picks too large asset that heeds saving if invalid, when invoked, then saveToExternalMediaStorage is called`() =
        runTest {
            // Given
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val mockedAttachment = AssetBundle(
                "file/x-zip", "some-data-path".toPath(), limit + 1L, "mocked_asset.zip", AttachmentType.GENERIC_FILE
            )
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .withGetAssetSizeLimitUseCase(false, limit)
                .withGetAssetBundleFromUri(mockedAttachment)
                .withSaveToExternalMediaStorage("mocked_image.jpeg")
                .arrange()
            val mockedUri = UriAsset("mocked_image.jpeg".toUri(), true)

            // When
            viewModel.attachmentPicked(mockedUri)

            // Then
            coVerify(inverse = true) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
            coVerify { arrangement.fileManager.saveToExternalMediaStorage(any(), any(), any(), any(), any()) }
            assert(viewModel.messageComposerViewState.assetTooLargeDialogState is AssetTooLargeDialogState.Visible)
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
            val mockedUri = UriAsset("mocked_image.jpeg".toUri(), true)

            // When
            viewModel.infoMessage.test {
                viewModel.attachmentPicked(mockedUri)

                // Then
                coVerify(inverse = true) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
                assertEquals(ConversationSnackbarMessages.ErrorPickingAttachment, awaitItem())
            }
        }

    @Test
    fun `given that a team user sends an asset message larger than 25MB, when invoked, then sendAssetMessageUseCase is called`() = runTest {
        // Given
        val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
        val (arrangement, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendAttachmentMessage()
            .withGetAssetSizeLimitUseCase(false, limit)
            .arrange()
        val mockedAttachment = AssetBundle(
            "file/x-zip", "some-data-path".toPath(), limit + 1, "mocked_asset.jpeg", AttachmentType.GENERIC_FILE
        )

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        // Then
        coVerify(exactly = 1) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
        assert(viewModel.messageComposerViewState.assetTooLargeDialogState is AssetTooLargeDialogState.Hidden)
    }

    @Test
    fun `given that a user sends an ping message, when invoked, then sendKnockUseCase and pingRinger are called`() = runTest {
        // Given
        val (arrangement, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .arrange()

        // When
        viewModel.sendPing()

        // Then
        coVerify(exactly = 1) { arrangement.sendKnockUseCase.invoke(any(), any()) }
        verify(exactly = 1) { arrangement.pingRinger.ping(any(), isReceivingPing = false) }
    }

    @Test
    fun `given that a user updates the self-deleting message timer, when invoked, then the timer gets successfully updated`() = runTest {
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
        coVerify(exactly = 1) { arrangement.persistSelfDeletionStatus.invoke(arrangement.conversationId, expectedTimer) }
        assert(viewModel.messageComposerViewState.selfDeletionTimer is SelfDeletionTimer.Enabled)
        assert(viewModel.messageComposerViewState.selfDeletionTimer.toDuration() == expectedDuration)
    }

    @Test
    fun `given a valid observed enforced self-deleting message timer, when invoked, then the timer gets successfully updated`() = runTest {
        // Given
        val expectedDuration = 1.toDuration(DurationUnit.DAYS)
        val expectedTimer = SelfDeletionTimer.Enabled(expectedDuration)
        val (arrangement, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withObserveSelfDeletingStatus(expectedTimer)
            .arrange()

        // When

        // Then
        coVerify(exactly = 1) { arrangement.observeConversationSelfDeletionStatus.invoke(arrangement.conversationId, true) }
        assert(viewModel.messageComposerViewState.selfDeletionTimer is SelfDeletionTimer.Enabled)
        assert(viewModel.messageComposerViewState.selfDeletionTimer.toDuration() == expectedDuration)
    }
}
