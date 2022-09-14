package com.wire.android.ui.home.conversations

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.home.conversations.MessageComposerViewModel.Companion.ASSET_SIZE_DEFAULT_LIMIT_BYTES
import com.wire.android.ui.home.conversations.MessageComposerViewModel.Companion.IMAGE_SIZE_LIMIT_BYTES
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogsState
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.kalium.logic.data.team.Team
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.internal.assertFalse
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
        // When
        viewModel.deleteMessageHelper.onDeleteMessage("messageId", true)

        // Then
        assert(viewModel.conversationViewState.snackbarMessage is ConversationSnackbarMessages.ErrorDeletingMessage)
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
            .arrange()
        val mockedAttachment = AttachmentBundle(
            "image/jpeg", "some-data-path".toPath(), IMAGE_SIZE_LIMIT_BYTES + 1L, "mocked_image.jpeg", AttachmentType.IMAGE
        )

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        // Then
        coVerify(inverse = true) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
        assert(viewModel.conversationViewState.snackbarMessage is ConversationSnackbarMessages.ErrorMaxImageSize)
    }

    @Test
    fun `given that a free user sends an asset message larger than 25MB, when invoked, then sendAssetMessageUseCase isn't called`() =
        runTest {
            // Given
            val (arrangement, viewModel) = ConversationsViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSuccessfulSendAttachmentMessage()
                .arrange()
            val mockedAttachment = AttachmentBundle(
                "file/x-zip",
                "some-data-path".toPath(),
                ASSET_SIZE_DEFAULT_LIMIT_BYTES + 1L,
                "mocked_asset.jpeg",
                AttachmentType.GENERIC_FILE
            )

            // When
            viewModel.sendAttachmentMessage(mockedAttachment)

            // Then
            coVerify(inverse = true) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
            assert(viewModel.conversationViewState.snackbarMessage is ConversationSnackbarMessages.ErrorMaxAssetSize)
        }

    @Test
    fun `given that a team user sends an asset message larger than 25MB, when invoked, then sendAssetMessageUseCase is called`() = runTest {
        // Given
        val userTeam = Team("mocked-team-id", "mocked-team-name")
        val (arrangement, viewModel) = ConversationsViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSuccessfulSendAttachmentMessage()
            .withTeamUser(userTeam)
            .arrange()
        val mockedAttachment = AttachmentBundle(
            "file/x-zip", "some-data-path".toPath(), ASSET_SIZE_DEFAULT_LIMIT_BYTES + 1L, "mocked_asset.jpeg", AttachmentType.GENERIC_FILE
        )

        // When
        viewModel.sendAttachmentMessage(mockedAttachment)

        // Then
        coVerify(exactly = 1) { arrangement.sendAssetMessage.invoke(any(), any(), any(), any(), any(), any(), any()) }
        assertFalse(viewModel.conversationViewState.snackbarMessage != null)
    }


}
