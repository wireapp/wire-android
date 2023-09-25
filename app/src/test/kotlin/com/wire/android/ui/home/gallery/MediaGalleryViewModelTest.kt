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

package com.wire.android.ui.home.gallery

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.config.NavigationTestExtension
import com.wire.android.ui.home.conversations.MediaGallerySnackbarMessages
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogsState
import com.wire.android.ui.navArgs
import com.wire.android.util.FileManager
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationDetails.OneOne
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.conversation.MutedConversationStatus.AllAllowed
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.amshove.kluent.internal.assertEquals
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
            arrangement.getImageData.invoke(mockedConversation.conversation.id, viewModel.imageAsset.messageId)
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
        viewModel.deleteCurrentImage()

        // Then
        val deleteMessageDialogsState = viewModel.mediaGalleryViewState.deleteMessageDialogsState
        assert(deleteMessageDialogsState is DeleteMessageDialogsState.States)
        assert((deleteMessageDialogsState as DeleteMessageDialogsState.States).forEveryone is DeleteMessageDialogActiveState.Visible)
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

        // When
        viewModel.deleteMessageHelper.onDeleteMessage("", true, arrangement.onDeleted)

        // Then
        coVerify(exactly = 1) { arrangement.onDeleted() }
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
            viewModel.deleteMessageHelper.onDeleteMessage("", true, arrangement.onDeleted)

            // Then
            coVerify(exactly = 0) { arrangement.onDeleted() }
            assertEquals(MediaGallerySnackbarMessages.DeletingMessageError, awaitItem())
        }
    }

    private class Arrangement {
        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        private lateinit var wireSessionImageLoader: WireSessionImageLoader

        @MockK
        lateinit var getConversationDetails: ObserveConversationDetailsUseCase

        @MockK
        lateinit var getImageData: GetMessageAssetUseCase

        @MockK
        lateinit var fileManager: FileManager

        @MockK
        lateinit var deleteMessage: DeleteMessageUseCase

        @MockK(relaxed = true)
        lateinit var onDeleted: () -> Unit

        init {
            // Tests setup
            val dummyPrivateAsset = "some-conversationId:some-message-id:true:true"
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.navArgs<MediaGalleryNavArgs>() } returns MediaGalleryNavArgs(
                conversationId = dummyConversationId,
                messageId = dummyPrivateAsset,
                isSelfAsset = true,
                isEphemeral = false
            )

            coEvery { deleteMessage(any(), any(), any()) } returns Either.Right(Unit)
        }

        fun withStoredData(assetData: ByteArray, assetPath: Path): Arrangement {
            fakeKaliumFileSystem.sink(assetPath).buffer().use {
                assetData
            }

            return this
        }

        fun withConversationDetails(conversationDetails: ConversationDetails): Arrangement {
            coEvery { getConversationDetails(any()) } returns flowOf(ObserveConversationDetailsUseCase.Result.Success(conversationDetails))
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
                    CoreFailure.Unknown(java.lang.RuntimeException()),
                    isRetryNeeded = true
                )
            )
            return this
        }

        fun withFailedMessageDeleting(): Arrangement {
            coEvery { deleteMessage(any(), any(), any()) } returns Either.Left(CoreFailure.Unknown(null))
            return this
        }

        fun arrange() = this to MediaGalleryViewModel(
            savedStateHandle,
            wireSessionImageLoader,
            getConversationDetails,
            TestDispatcherProvider(),
            getImageData,
            fileManager,
            deleteMessage
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
                type = Conversation.Type.ONE_ON_ONE,
                teamId = null,
                protocol = Conversation.ProtocolInfo.Proteus,
                mutedStatus = AllAllowed,
                removedBy = null,
                lastNotificationDate = null,
                lastModifiedDate = null,
                lastReadDate = "2022-04-04T16:11:28.388Z",
                access = listOf(Conversation.Access.INVITE),
                accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER),
                creatorId = null,
                receiptMode = Conversation.ReceiptMode.ENABLED,
                messageTimer = null,
                userMessageTimer = null,
                archived = false,
                archivedDateTime = null
            ),
            otherUser = OtherUser(
                QualifiedID("other-user-id", "domain-id"),
                "Test user", null, null, null,
                1, null, ConnectionState.ACCEPTED, null, null,
                UserType.INTERNAL,
                UserAvailabilityStatus.AVAILABLE,
                null,
                false,
                defederated = false
            ),
            legalHoldStatus = LegalHoldStatus.DISABLED,
            userType = UserType.INTERNAL,
            lastMessage = null,
            unreadEventCount = emptyMap()
        )

    companion object {
        val fakeKaliumFileSystem = FakeKaliumFileSystem()
        val dummyConversationId = QualifiedID("a-value", "a-domain")
    }
}
