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

package com.wire.android.ui.home.gallery

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.ui.home.conversations.MediaGallerySnackbarMessages
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogType
import com.wire.android.ui.navArgs
import com.wire.android.util.FileManager
import com.wire.kalium.cells.domain.usecase.GetCellFileUseCase
import com.wire.kalium.cells.domain.usecase.GetMessageAttachmentUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.right
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationDetails.OneOne
import com.wire.kalium.logic.data.conversation.MutedConversationStatus.AllAllowed
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.CellAssetContent
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.MessageOperationResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class MediaGalleryViewModelTest {

    @Test
    fun givenCurrentSetup_whenInitialisingViewModel_thenScreenTitleMatchesTheConversationName() = runTest {
        // Given
        val dummyConversationId = QualifiedID(
            "dummy-value",
            "dummy-domain"
        )
        val mockedConversation = mockedConversationDetails("dummyTitle", dummyConversationId)
        val (_, viewModel) = Arrangement().withConversationDetails(mockedConversation).arrange()

        // When
        val screenTitle = viewModel.mediaGalleryViewState.screenTitle

        // Then
        assertEquals("Test user", screenTitle)
    }

    @Test
    fun givenACorrectRequest_whenUserSavesAnImage_theUseCaseGetsInvokedCorrectlyAndASuccessValueIsReturned() = runTest {
        // Given
        val mockedConversation = mockedConversationDetails()
        val mockedImage = "mocked-image".toByteArray()
        val dummyDataPath = "dummy-path".toPath()
        val (arrangement, viewModel) = Arrangement()
            .withStoredData(mockedImage, dummyDataPath)
            .withConversationDetails(mockedConversation)
            .withSuccessfulImageData(dummyDataPath, mockedImage.size.toLong())
            .arrange()

        // When
        viewModel.saveImageToExternalStorage()

        // Then
        coVerify(exactly = 1) {
            arrangement.getImageData.invoke(mockedConversation.conversation.id, dummyMessageId)
            arrangement.fileManager.saveToExternalStorage(any(), dummyDataPath, mockedImage.size.toLong(), any(), any())
        }
    }

    @Test
    fun givenAFailedRequest_whenUserTriesToSaveAnImage_aFailureValueIsReturned() = runTest {
        // Given
        val mockedConversation = mockedConversationDetails()
        val (_, viewModel) = Arrangement()
            .withConversationDetails(mockedConversation)
            .withFailedImageDataRequest()
            .arrange()

        viewModel.snackbarMessage.test {
            // When
            viewModel.saveImageToExternalStorage()

            // Then
            assertEquals(MediaGallerySnackbarMessages.OnImageDownloadError, awaitItem())
        }
    }

    @Test
    fun givenACorrectSetup_whenUserTriesToDeleteAnImage_DeleteDialogIsShown() = runTest {
        // Given
        val mockedConversation = mockedConversationDetails()
        val mockedImage = "mocked-image".toByteArray()
        val imagePath = fakeKaliumFileSystem.providePersistentAssetPath("dummy-path")
        val (_, viewModel) = Arrangement()
            .withStoredData(mockedImage, imagePath)
            .withConversationDetails(mockedConversation)
            .withSuccessfulImageData(imagePath, mockedImage.size.toLong())
            .arrange()

        // When
        viewModel.deleteMessageDialogState.show(
            DeleteMessageDialogState(true, dummyMessageId, dummyConversationId)
        )

        // Then
        assertEquals(true, viewModel.deleteMessageDialogState.isVisible)
        assertNotNull(viewModel.deleteMessageDialogState.savedState)
        assertEquals(DeleteMessageDialogType.ForEveryone, viewModel.deleteMessageDialogState.savedState!!.type)
    }

    @Test
    fun givenACorrectSetup_whenUserDeletesAnImage_navigationBackIsCalled() = runTest {
        // Given
        val mockedConversation = mockedConversationDetails()
        val mockedImage = "mocked-image".toByteArray()
        val imagePath = fakeKaliumFileSystem.providePersistentAssetPath("dummy-path")
        val (arrangement, viewModel) = Arrangement()
            .withStoredData(mockedImage, imagePath)
            .withConversationDetails(mockedConversation)
            .withSuccessfulImageData(imagePath, mockedImage.size.toLong())
            .arrange()

        viewModel.actions.test {
            // When
            viewModel.deleteMessage("", true)

            assertTrue(awaitItem() is MediaGalleryAction.Close)
        }
    }

    @Test
    fun givenErrorWhileDelete_whenUserDeletesAnImage_errorIsShown() = runTest {
        // Given
        val mockedConversation = mockedConversationDetails()
        val mockedImage = "mocked-image".toByteArray()
        val imagePath = fakeKaliumFileSystem.providePersistentAssetPath("dummy-path")
        val (arrangement, viewModel) = Arrangement()
            .withStoredData(mockedImage, imagePath)
            .withConversationDetails(mockedConversation)
            .withSuccessfulImageData(imagePath, mockedImage.size.toLong())
            .withFailedMessageDeleting()
            .arrange()

        viewModel.snackbarMessage.test {
            // When
            viewModel.deleteMessage("", true)

            // Then
            assertEquals(MediaGallerySnackbarMessages.DeletingMessageError, awaitItem())
        }
    }

    @Test
    fun givenCellAssetWithLocalPath_whenInitialisingViewModel_thenAssetWithLocalPathReturned() = runTest {
        // Given
        val (_, viewModel) = Arrangement()
            .withNavArgs(cellAssetId = "cell-asset-id")
            .withConversationDetails(mockedConversationDetails())
            .withAssetContent(
                CellAssetContent(
                    id = "cell-asset-id",
                    versionId = "",
                    mimeType = "image/png",
                    localPath = "local/path",
                    assetPath = "asset/path",
                    assetSize = 1,
                    metadata = null,
                    transferStatus = AssetTransferStatus.SAVED_INTERNALLY
                )
            )
            .arrange()

        // When
        val state = viewModel.mediaGalleryViewState

        // Then
        assertTrue(state.imageAsset is MediaGalleryImage.LocalAsset)
        assertEquals("local/path", (state.imageAsset as MediaGalleryImage.LocalAsset).path)
    }

    @Test
    fun givenCellAssetWithUrl_whenInitialisingViewModel_thenAssetWithUrlReturned() = runTest {
        // Given
        val (_, viewModel) = Arrangement()
            .withNavArgs(cellAssetId = "cell-asset-id")
            .withConversationDetails(mockedConversationDetails())
            .withAssetContent(
                CellAssetContent(
                    id = "cell-asset-id",
                    versionId = "",
                    mimeType = "image/png",
                    localPath = null,
                    assetPath = "asset/path",
                    contentUrl = "content/url",
                    previewUrl = "preview/url",
                    assetSize = 1,
                    metadata = null,
                    transferStatus = AssetTransferStatus.SAVED_INTERNALLY
                )
            )
            .arrange()

        // When
        val state = viewModel.mediaGalleryViewState

        // Then
        assertTrue(state.imageAsset is MediaGalleryImage.UrlAsset)
        assertEquals("content/url", (state.imageAsset as MediaGalleryImage.UrlAsset).url)
        assertEquals("preview/url", state.imageAsset.placeholder)
    }

    @Test
    fun givenMessageMenuOptionsDisabled_whenShowingMenu_thenCorrectMenuItemsShown() = runTest {
        val (_, viewModel) = Arrangement()
            .withNavArgs(messageOptionsEnabled = false, isEphemeral = false)
            .withConversationDetails(mockedConversationDetails())
            .arrange()

        viewModel.onOptionsClick()

        val state = viewModel.mediaGalleryViewState

        assertEquals(
            listOf(
                MediaGalleryMenuItem.DOWNLOAD,
                MediaGalleryMenuItem.SHARE,
                MediaGalleryMenuItem.DELETE,
            ),
            state.menuItems
        )
    }

    @Test
    fun givenMessageMenuOptionsDisabledAndEphemeral_whenShowingMenu_thenCorrectMenuItemsShown() = runTest {
        val (_, viewModel) = Arrangement()
            .withNavArgs(messageOptionsEnabled = false, isEphemeral = true)
            .withConversationDetails(mockedConversationDetails())
            .arrange()

        viewModel.onOptionsClick()

        val state = viewModel.mediaGalleryViewState

        assertEquals(
            listOf(
                MediaGalleryMenuItem.DOWNLOAD,
                MediaGalleryMenuItem.DELETE,
            ),
            state.menuItems
        )
    }

    @Test
    fun givenMessageMenuOptionsEnabledAndEphemeral_whenShowingMenu_thenCorrectMenuItemsShown() = runTest {
        val (_, viewModel) = Arrangement()
            .withNavArgs(messageOptionsEnabled = true, isEphemeral = true)
            .withConversationDetails(mockedConversationDetails())
            .arrange()

        viewModel.onOptionsClick()

        val state = viewModel.mediaGalleryViewState

        assertEquals(
            listOf(
                MediaGalleryMenuItem.SHOW_DETAILS,
                MediaGalleryMenuItem.DOWNLOAD,
                MediaGalleryMenuItem.DELETE,
            ),
            state.menuItems
        )
    }

    @Test
    fun givenMessageMenuOptionsEnabledAndCellAsset_whenShowingMenu_thenCorrectMenuItemsShown() = runTest {
        val (_, viewModel) = Arrangement()
            .withNavArgs(messageOptionsEnabled = true, isEphemeral = false, cellAssetId = "cell-asset-id")
            .withConversationDetails(mockedConversationDetails())
            .withAssetContent(
                CellAssetContent(
                    id = "cell-asset-id",
                    versionId = "",
                    mimeType = "image/png",
                    localPath = null,
                    assetPath = "asset/path",
                    contentUrl = "content/url",
                    previewUrl = "preview/url",
                    assetSize = 1,
                    metadata = null,
                    transferStatus = AssetTransferStatus.SAVED_INTERNALLY
                )
            )
            .arrange()

        viewModel.onOptionsClick()

        val state = viewModel.mediaGalleryViewState

        assertEquals(
            listOf(
                MediaGalleryMenuItem.REACT,
                MediaGalleryMenuItem.SHOW_DETAILS,
                MediaGalleryMenuItem.REPLY,
                MediaGalleryMenuItem.SHARE_PUBLIC_LINK,
            ),
            state.menuItems
        )
    }

    @Test
    fun givenMessageMenuOptionsEnabled_whenShowingMenu_thenCorrectMenuItemsShown() = runTest {
        val (_, viewModel) = Arrangement()
            .withNavArgs(messageOptionsEnabled = true, isEphemeral = false, cellAssetId = null)
            .withConversationDetails(mockedConversationDetails())
            .arrange()

        viewModel.onOptionsClick()

        val state = viewModel.mediaGalleryViewState

        assertEquals(
            listOf(
                MediaGalleryMenuItem.REACT,
                MediaGalleryMenuItem.SHOW_DETAILS,
                MediaGalleryMenuItem.REPLY,
                MediaGalleryMenuItem.DOWNLOAD,
                MediaGalleryMenuItem.SHARE,
                MediaGalleryMenuItem.DELETE,
            ),
            state.menuItems
        )
    }

    private class Arrangement {
        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var getConversationDetails: ObserveConversationDetailsUseCase

        @MockK
        lateinit var getImageData: GetMessageAssetUseCase

        @MockK
        lateinit var fileManager: FileManager

        @MockK
        lateinit var deleteMessage: DeleteMessageUseCase

        @MockK
        lateinit var getAttachment: GetMessageAttachmentUseCase

        @MockK
        lateinit var getCellFile: GetCellFileUseCase

        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)

            every { savedStateHandle.navArgs<MediaGalleryNavArgs>() } returns MediaGalleryNavArgs(
                conversationId = dummyConversationId,
                messageId = dummyMessageId,
                isSelfAsset = true,
                isEphemeral = false,
                messageOptionsEnabled = true,
                cellAssetId = null,
            )

            coEvery { deleteMessage(any(), any(), any()) } returns MessageOperationResult.Success
        }

        fun withNavArgs(messageOptionsEnabled: Boolean = true, isEphemeral: Boolean = false, cellAssetId: String? = null) = apply {
            every { savedStateHandle.navArgs<MediaGalleryNavArgs>() } returns MediaGalleryNavArgs(
                conversationId = dummyConversationId,
                messageId = dummyMessageId,
                isSelfAsset = true,
                isEphemeral = isEphemeral,
                messageOptionsEnabled = messageOptionsEnabled,
                cellAssetId = cellAssetId,
            )
        }

        fun withAssetContent(cellAssetContent: CellAssetContent) = apply {
            coEvery { getAttachment(any()) } returns cellAssetContent.right()
        }

        fun withStoredData(assetData: ByteArray, assetPath: Path): Arrangement {
            fakeKaliumFileSystem.sink(assetPath).buffer().use {
                assetData
            }

            return this
        }

        fun withConversationDetails(conversationDetails: ConversationDetails): Arrangement {
            coEvery {
                getConversationDetails(any())
            } returns flowOf(ObserveConversationDetailsUseCase.Result.Success(conversationDetails))
            return this
        }

        fun withSuccessfulImageData(imageDataPath: Path, imageSize: Long, assetName: String = "name"): Arrangement {
            coEvery { getImageData(any(), any()) } returns CompletableDeferred(
                MessageAssetResult.Success(
                    imageDataPath,
                    imageSize,
                    assetName
                )
            )
            return this
        }

        fun withFailedImageDataRequest(): Arrangement {
            coEvery {
                getImageData(
                    any(),
                    any()
                )
            } returns CompletableDeferred(
                MessageAssetResult.Failure(
                    CoreFailure.Unknown(RuntimeException()),
                    isRetryNeeded = true
                )
            )
            return this
        }

        fun withFailedMessageDeleting(): Arrangement {
            coEvery { deleteMessage(any(), any(), any()) } returns MessageOperationResult.Failure(CoreFailure.Unknown(null))
            return this
        }

        fun arrange() = this to MediaGalleryViewModel(
            savedStateHandle,
            getConversationDetails,
            TestDispatcherProvider(),
            getImageData,
            fileManager,
            deleteMessage,
            getAttachment,
            getCellFile,
        )
    }

    private fun mockedConversationDetails(
        mockedConversationTitle: String = "Dummy Screen Title",
        mockedConversationId: QualifiedID = dummyConversationId
    ): ConversationDetails =
        OneOne(
            conversation = Conversation(
                id = mockedConversationId,
                name = mockedConversationTitle,
                type = Conversation.Type.OneOnOne,
                teamId = null,
                protocol = Conversation.ProtocolInfo.Proteus,
                mutedStatus = AllAllowed,
                removedBy = null,
                lastNotificationDate = null,
                lastModifiedDate = null,
                lastReadDate = Instant.parse("2022-04-04T16:11:28.388Z"),
                access = listOf(Conversation.Access.INVITE),
                accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER),
                creatorId = null,
                receiptMode = Conversation.ReceiptMode.ENABLED,
                messageTimer = null,
                userMessageTimer = null,
                archived = false,
                archivedDateTime = null,
                mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
                proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
                legalHoldStatus = Conversation.LegalHoldStatus.DISABLED
            ),
            otherUser = OtherUser(
                QualifiedID("other-user-id", "domain-id"),
                "Test user", null, null, null,
                1, null, ConnectionState.ACCEPTED, null, null,
                UserTypeInfo.Regular(UserType.INTERNAL),
                UserAvailabilityStatus.AVAILABLE,
                setOf(SupportedProtocol.PROTEUS),
                null,
                false,
                defederated = false,
                isProteusVerified = false
            ),
            userType = UserTypeInfo.Regular(UserType.INTERNAL),
        )

    companion object {
        val fakeKaliumFileSystem = FakeKaliumFileSystem()
        val dummyConversationId = QualifiedID("a-value", "a-domain")
        const val dummyMessageId = "some-conversationId:some-message-id:true:true"
    }
}
