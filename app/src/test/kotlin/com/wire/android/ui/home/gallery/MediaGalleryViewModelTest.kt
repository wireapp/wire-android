package com.wire.android.ui.home.gallery

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.navigation.EXTRA_MESSAGE_TO_DELETE_ID
import com.wire.android.navigation.EXTRA_MESSAGE_TO_DELETE_IS_SELF
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationDetails.OneOne
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.conversation.MutedConversationStatus.AllAllowed
import com.wire.kalium.logic.data.conversation.UserType
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MediaGalleryViewModelTest {

    @Test
    fun givenCurrentSetup_whenInitialisingViewModel_thenScreenTitleMatchesTheConversationName() = runTest {
        // Given
        val dummyTitle = "Test title"
        val dummyConversationId = QualifiedID(
            "dummy-value",
            "dummy-domain"
        )
        val mockedConversation = mockedConversationDetails(dummyTitle, dummyConversationId)
        val (_, viewModel) = Arrangement().withConversationDetails(mockedConversation).arrange()

        // When
        val screenTitle = viewModel.mediaGalleryViewState.screenTitle

        // Then
        assertEquals(dummyTitle, screenTitle)
    }

    @Test
    fun givenACorrectRequest_whenUserSavesAnImage_theUseCaseGetsInvokedCorrectlyAndASuccessValueIsReturned() = runTest {
        // Given
        val mockedConversation = mockedConversationDetails()
        val mockedImage = "mocked-image".toByteArray()
        val (arrangement, viewModel) = Arrangement()
            .withConversationDetails(mockedConversation)
            .withSuccessfulImageData(mockedImage)
            .arrange()

        // When
        viewModel.saveImageToExternalStorage()

        // Then
        coVerify(exactly = 1) {
            arrangement.getImageData.invoke(mockedConversation.conversation.id, viewModel.imageAssetId.messageId)
        }
        coVerify(exactly = 1) {
            arrangement.fileManager.saveToExternalStorage(any(), mockedImage, any())
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

        // When
        viewModel.saveImageToExternalStorage()

        // Then
        coVerify(exactly = 1) {
            viewModel.onSaveError()
        }
    }

    @Test
    fun givenACorrectSetup_whenUserTriesNavigateBack_navigateBackGetsInvokedOnNavigationManager() = runTest {
        // Given
        val mockedConversation = mockedConversationDetails()
        val mockedImage = "mocked-image".toByteArray()
        val (arrangement, viewModel) = Arrangement()
            .withConversationDetails(mockedConversation)
            .withSuccessfulImageData(mockedImage)
            .arrange()

        // When
        viewModel.navigateBack()

        // Then
        coVerify(exactly = 1) {
            arrangement.navigationManager.navigateBack()
        }
    }

    @Test
    fun givenACorrectSetup_whenUserTriesToDeleteAnImage_navigateBackGetsInvokedWithImageInfo() = runTest {
        // Given
        val mockedConversation = mockedConversationDetails()
        val mockedImage = "mocked-image".toByteArray()
        val (arrangement, viewModel) = Arrangement()
            .withConversationDetails(mockedConversation)
            .withSuccessfulImageData(mockedImage)
            .arrange()

        // When
        viewModel.deleteCurrentImageMessage()

        // Then
        coVerify(exactly = 1) {
            arrangement.navigationManager.navigateBack(
                mapOf(
                    EXTRA_MESSAGE_TO_DELETE_ID to viewModel.imageAssetId.messageId,
                    EXTRA_MESSAGE_TO_DELETE_IS_SELF to viewModel.imageAssetId.isSelfAsset
                )
            )
        }
    }

    private class Arrangement {
        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var navigationManager: NavigationManager

        @MockK
        private lateinit var wireSessionImageLoader: WireSessionImageLoader

        @MockK
        private lateinit var getConversationDetails: ObserveConversationDetailsUseCase

        @MockK
        private lateinit var dispatchers: DispatcherProvider

        @MockK
        lateinit var getImageData: GetMessageAssetUseCase

        @MockK
        lateinit var fileManager: FileManager

        lateinit var conversationDetails: ConversationDetails

        init {
            // Tests setup
            val dummyPrivateAsset = "some-conversationId:some-message-id:true"
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.get<String>(any()) } returns dummyPrivateAsset

            // Default empty values
            coEvery { getConversationDetails(any()) } returns flowOf(conversationDetails)
        }

        fun withConversationDetails(mockedConversationDetails: ConversationDetails): Arrangement {
            conversationDetails = mockedConversationDetails
            return this
        }

        fun withSuccessfulImageData(imageData: ByteArray): Arrangement {
            coEvery { getImageData(any(), any()) } returns MessageAssetResult.Success(imageData)
            return this
        }

        fun withFailedImageDataRequest(): Arrangement {
            coEvery { getImageData(any(), any()) } returns MessageAssetResult.Failure(CoreFailure.Unknown(java.lang.RuntimeException()))
            return this
        }

        fun arrange() = this to MediaGalleryViewModel(
            savedStateHandle,
            wireSessionImageLoader,
            navigationManager,
            getConversationDetails,
            dispatchers,
            getImageData,
            fileManager
        )

    }

    private fun mockedConversationDetails(
        mockedConversationTitle: String = "Dummy Screen Title",
        dummyConversationId: QualifiedID = QualifiedID("a-value", "a-domain")
    ): ConversationDetails =
        OneOne(
            Conversation(
                dummyConversationId,
                mockedConversationTitle,
                Conversation.Type.ONE_ON_ONE,
                null,
                AllAllowed,
                null, null
            ),
            OtherUser(
                QualifiedID("other-user-id", "domain-id"),
                null, null, null, null,
                1, null, ConnectionState.ACCEPTED, null, null
            ),
            ConnectionState.ACCEPTED,
            LegalHoldStatus.DISABLED,
            UserType.INTERNAL
        )
}
