package com.wire.android.util.ui

import android.content.res.Resources
import coil.ImageLoader
import coil.fetch.FetchResult
import com.wire.android.model.ImageAsset
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AssetImageFetcherTest {

    @Test
    fun givenAUserAvatarAssetData_WhenCallingFetch_ThenGetPublicAssetUseCaseGetsInvoked() = runTest {
        // Given
        val someUserAssetId = "some-user-asset-id"
        val data = ImageAsset.UserAvatarAsset(someUserAssetId)
        val (arrangement, assetImageFetcher) = Arrangement().withSuccessfulImageData(data).arrange()

        // When
        assetImageFetcher.fetch()

        // Then
        coVerify(exactly = 1) { arrangement.getPublicAsset(data.userAssetId) }
    }

    @Test
    fun givenAPrivateAssetImageData_WhenCallingFetch_ThenGetPrivateAssetUseCaseGetsInvoked() = runTest {
        // Given
        val someConversationId = ConversationId("some-value", "some-domain")
        val someMessageId = "some-message-id"
        val data = ImageAsset.PrivateAsset(someConversationId, someMessageId)
        val (arrangement, assetImageFetcher) = Arrangement().withSuccessfulImageData(data).arrange()

        // When
        assetImageFetcher.fetch()

        // Then
        coVerify(exactly = 1) { arrangement.getPrivateAsset(data.conversationId, data.messageId) }
        coVerify(exactly = 1) { arrangement.drawableResultWrapper.toFetchResult(any()) }
    }

    @Test
    fun givenAUserAvatarAssetData_WhenCallingFetchUnsuccessfully_ThenFetchResultIsNotReturned() = runTest {
        // Given
        val someUserAssetId = "some-user-asset-id"
        val data = ImageAsset.UserAvatarAsset(someUserAssetId)
        val (arrangement, assetImageFetcher) = Arrangement().withErrorResponse(data).arrange()

        // When
        assetImageFetcher.fetch()

        // Then
        coVerify(inverse = true) { arrangement.drawableResultWrapper.toFetchResult(any()) }
    }

    @Test
    fun givenAPrivateAssetImageData_WhenCallingFetchUnsuccessfully_ThenFetchResultIsNotReturned() = runTest {
        // Given
        val someConversationId = ConversationId("some-value", "some-domain")
        val someMessageId = "some-message-id"
        val data = ImageAsset.PrivateAsset(someConversationId, someMessageId)
        val (arrangement, assetImageFetcher) = Arrangement().withErrorResponse(data).arrange()

        // When
        assetImageFetcher.fetch()

        // Then
        coVerify(inverse = true) { arrangement.drawableResultWrapper.toFetchResult(decodedAsset = any()) }
    }

    private class Arrangement {
        val getPublicAsset = mockk<GetAvatarAssetUseCase>()
        val getPrivateAsset = mockk<GetMessageAssetUseCase>()
        val resources = mockk<Resources>()
        val imageLoader = mockk<ImageLoader>()
        val mockDecodedAsset = "mocked-asset".toByteArray()
        val drawableResultWrapper = mockk<DrawableResultWrapper>()
        val mockFetchResult = mockk<FetchResult>()
        lateinit var imageData: ImageAsset

        fun withSuccessfulImageData(data: ImageAsset): Arrangement {
            imageData = data
            coEvery { getPublicAsset.invoke((any())) }.returns(PublicAssetResult.Success(mockDecodedAsset))
            coEvery { getPrivateAsset.invoke(any(), any()) }.returns(MessageAssetResult.Success(mockDecodedAsset))
            coEvery { drawableResultWrapper.toFetchResult(any()) }.returns(mockFetchResult)

            return this
        }

        fun withErrorResponse(data: ImageAsset): Arrangement {
            imageData = data
            coEvery { getPublicAsset.invoke((any())) }.returns(PublicAssetResult.Failure(CoreFailure.Unknown(null)))
            coEvery { getPrivateAsset.invoke(any(), any()) }.returns(MessageAssetResult.Failure(CoreFailure.Unknown(null)))

            return this
        }

        fun arrange() = this to AssetImageFetcher(
            data = imageData,
            getPublicAsset = getPublicAsset,
            getPrivateAsset = getPrivateAsset,
            resources = resources,
            drawableResultWrapper = drawableResultWrapper,
            imageLoader = imageLoader
        )
    }
}
