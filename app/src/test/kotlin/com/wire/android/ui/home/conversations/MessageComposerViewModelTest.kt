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

import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.home.conversations.MessageComposerViewModel.Companion.ASSET_SIZE_DEFAULT_LIMIT_BYTES
import com.wire.android.ui.home.conversations.MessageComposerViewModel.Companion.IMAGE_SIZE_LIMIT_BYTES
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogsState
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.kalium.logic.data.team.Team
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MessageComposerViewModelTest {

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageDialog is visible for my message`() = runTest {
        // Given
        val (_, viewModel) = ConversationsViewModelArrangement()
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
        val (_, viewModel) = ConversationsViewModelArrangement()
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
        val (_, viewModel) = ConversationsViewModelArrangement()
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
        val (_, viewModel) = ConversationsViewModelArrangement().arrange()

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
        val (_, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withFailureOnDeletingMessages().arrange()

        viewModel.conversationViewState
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
        val (_, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withFailureOnDeletingMessages()
            .arrange()

        viewModel.conversationViewState
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
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendAttachmentMessage()
            .withGetAssetSizeLimitUseCase(false, 25000000)
            .arrange()
        val mockedAttachment = AttachmentBundle(
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
        val assetPath = "mocked-asset-data-path".toPath()
        val assetContent = "some-dummy-image".toByteArray()
        val assetName = "mocked_image.jpeg"
        val assetSize = assetContent.size.toLong()
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withStoredAsset(assetPath, assetContent)
            .withSuccessfulSendAttachmentMessage()
            .withGetAssetSizeLimitUseCase(true, 15000000)
            .arrange()
        val mockedAttachment = AttachmentBundle(
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
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendAttachmentMessage()
            .arrange()
        val mockedAttachment = null

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        coVerify(inverse = true) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given a user sends an image message larger than 15MB, when invoked, then sendAssetMessageUseCase isn't called`() = runTest {
        // Given
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendAttachmentMessage()
            .withGetAssetSizeLimitUseCase(true, 15000000)
            .arrange()
        val mockedAttachment = AttachmentBundle(
            "image/jpeg", "some-data-path".toPath(), IMAGE_SIZE_LIMIT_BYTES + 1L, "mocked_image.jpeg", AttachmentType.IMAGE
        )

        // When
        viewModel.infoMessage.test {
            viewModel.sendAttachmentMessage(mockedAttachment)

            // Then
            coVerify(inverse = true) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
            assertEquals(ConversationSnackbarMessages.ErrorMaxImageSize, awaitItem())
        }
    }

    @Test
    fun `given that a free user sends an asset message larger than 25MB, when invoked, then sendAssetMessageUseCase isn't called`() =
        runTest {
            // Given
            val (arrangement, viewModel) = ConversationsViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .withGetAssetSizeLimitUseCase(false, 15000000)
                .arrange()
            val mockedAttachment = AttachmentBundle(
                "file/x-zip",
                "some-data-path".toPath(),
                ASSET_SIZE_DEFAULT_LIMIT_BYTES + 1L,
                "mocked_asset.jpeg",
                AttachmentType.GENERIC_FILE
            )

            // When
            viewModel.infoMessage.test {
                viewModel.sendAttachmentMessage(mockedAttachment)

                // Then
                coVerify(inverse = true) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
                assert(awaitItem() is ConversationSnackbarMessages.ErrorMaxAssetSize)
            }
        }

    @Test
    fun `given that a team user sends an asset message larger than 25MB, when invoked, then sendAssetMessageUseCase is called`() = runTest {
        // Given
        val userTeam = Team("mocked-team-id", "mocked-team-name", "icon")
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendAttachmentMessage()
            .withGetAssetSizeLimitUseCase(false, 100000000)
            .withTeamUser(userTeam)
            .arrange()
        val mockedAttachment = AttachmentBundle(
            "file/x-zip", "some-data-path".toPath(), ASSET_SIZE_DEFAULT_LIMIT_BYTES + 1L, "mocked_asset.jpeg", AttachmentType.GENERIC_FILE
        )

        // When
        viewModel.infoMessage.test {
            viewModel.sendAttachmentMessage(mockedAttachment)

            // Then
            coVerify(exactly = 1) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
            expectNoEvents()
        }
    }

    @Test
    fun `given that a user sends an ping message, when invoked, then sendKnockUseCase and pingRinger are called`() = runTest {
        // Given
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .arrange()

        // When
        viewModel.sendPing()

        // Then
        coVerify(exactly = 1) { arrangement.sendKnockUseCase.invoke(any(), any()) }
        verify(exactly = 1) { arrangement.pingRinger.ping(any(), isReceivingPing = false) }
    }
}
