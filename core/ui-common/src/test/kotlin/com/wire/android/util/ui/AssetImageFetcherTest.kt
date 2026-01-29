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

package com.wire.android.util.ui

import android.content.Context
import coil3.Extras
import coil3.fetch.FetchResult
import coil3.request.Options
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.model.ImageAsset
import com.wire.android.util.ui.AssetImageFetcher.Companion.OPTION_PARAMETER_RETRY_KEY
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.AssetId
import com.wire.kalium.logic.feature.asset.DeleteAssetUseCase
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.buffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AssetImageFetcherTest {

    @Test
    fun givenAUserAvatarAssetData_WhenCallingFetch_ThenGetPublicAssetUseCaseGetsInvoked() = runTest {
        // Given
        val someUserAssetId = AssetId("value", "domain")
        val someDummyData = "some-dummy-data".toByteArray()
        val someDummyName = "some-dummy-name"
        val data = ImageAsset.UserAvatarAsset(someUserAssetId)
        val avatarPath = fakeKaliumFileSystem.selfUserAvatarPath()
        val (arrangement, assetImageFetcher) = Arrangement()
            .withSuccessfulImageData(data, avatarPath, someDummyData.size.toLong(), someDummyName)
            .withSuccessFullAssetDelete()
            .withStoredData(someDummyData, avatarPath)
            .arrange()

        // When
        assetImageFetcher.fetch()

        // Then
        coVerify(exactly = 1) { arrangement.getPublicAsset(data.userAssetId) }
    }

    @Test
    fun givenAUserAvatarAssetData_WhenCallingFetchOnRetry_ThenAssetGetsDeletedGetPublicAssetUseCaseGetsInvoked() = runTest {
        // Given
        val someUserAssetId = AssetId("value", "domain")
        val someDummyData = "some-dummy-data".toByteArray()
        val someDummyName = "some-dummy-name"
        val data = ImageAsset.UserAvatarAsset(someUserAssetId)
        val avatarPath = fakeKaliumFileSystem.selfUserAvatarPath()
        val (arrangement, assetImageFetcher) = Arrangement()
            .withSuccessfulImageData(data, avatarPath, someDummyData.size.toLong(), someDummyName, 1)
            .withSuccessFullAssetDelete()
            .withStoredData(someDummyData, avatarPath)
            .arrange()

        // When
        assetImageFetcher.fetch()

        // Then
        coVerify(exactly = 1) { arrangement.deleteAsset(data.userAssetId) }
        coVerify(exactly = 1) { arrangement.getPublicAsset(data.userAssetId) }
    }

    @Test
    fun givenAPrivateAssetImageData_WhenCallingFetch_ThenGetPrivateAssetUseCaseGetsInvoked() = runTest {
        // Given
        val someConversationId = ConversationId("some-value", "some-domain")
        val someMessageId = "some-message-id"
        val someDummyData = "some-dummy-data".toByteArray()
        val someDummyName = "some-dummy-name"
        val data = ImageAsset.PrivateAsset(someConversationId, someMessageId, true)
        val avatarPath = fakeKaliumFileSystem.selfUserAvatarPath()
        val (arrangement, assetImageFetcher) = Arrangement()
            .withSuccessfulImageData(data, avatarPath, 1, someDummyName)
            .withSuccessFullAssetDelete()
            .withStoredData(someDummyData, avatarPath)
            .arrange()

        // When
        assetImageFetcher.fetch()

        // Then
        coVerify(exactly = 1) { arrangement.getPrivateAsset(data.conversationId, data.messageId) }
        coVerify(exactly = 1) { arrangement.drawableResultWrapper.toFetchResult(any()) }
    }

    @Test
    fun givenAUserAvatarAssetData_WhenCallingFetchUnsuccessfully_ThenFetchResultIsNotReturned() = runTest {
        // Given
        val someUserAssetId = AssetId("value", "domain")
        val data = ImageAsset.UserAvatarAsset(someUserAssetId)
        val (arrangement, assetImageFetcher) = Arrangement().withErrorResponse(data).arrange()

        // When
        assertThrows<AssetImageException> { assetImageFetcher.fetch() }

        // Then
        coVerify(inverse = true) { arrangement.drawableResultWrapper.toFetchResult(any()) }
    }

    @Test
    fun givenAPrivateAssetImageData_WhenCallingFetchUnsuccessfully_ThenFetchResultIsNotReturned() = runTest {
        // Given
        val someConversationId = ConversationId("some-value", "some-domain")
        val someMessageId = "some-message-id"
        val data = ImageAsset.PrivateAsset(someConversationId, someMessageId, true)
        val (arrangement, assetImageFetcher) = Arrangement().withErrorResponse(data).arrange()

        // When
        assertThrows<AssetImageException> { assetImageFetcher.fetch() }

        // Then
        coVerify(inverse = true) { arrangement.drawableResultWrapper.toFetchResult(any()) }
    }

    @Test
    fun givenAUserAvatarAssetData_WhenCallingFetchReturnsFailureWithRetryNotNeeded_ThenThrowExceptionWithDoNotRetryPolicy() =
        runTest {
            // Given
            val someUserAssetId = AssetId("value", "domain")
            val data = ImageAsset.UserAvatarAsset(someUserAssetId)
            val (arrangement, assetImageFetcher) = Arrangement()
                .withErrorResponse(
                    data = data,
                    isRetryNeeded = false,
                    coreFailure = CoreFailure.Unknown(null)
                )
                .arrange()

            // When
            val exception = assertThrows<AssetImageException> { assetImageFetcher.fetch() }

            // Then
            assertEquals(AssetImageRetryPolicy.DO_NOT_RETRY, exception.retryPolicy)
            coVerify(inverse = true) { arrangement.drawableResultWrapper.toFetchResult(any()) }
        }

    @Test
    fun givenAUserAvatarAssetData_WhenCallingFetchReturnsNoConnectionFailure_ThenThrowExceptionWithRetryPolicy() =
        runTest {
            // Given
            val someUserAssetId = AssetId("value", "domain")
            val data = ImageAsset.UserAvatarAsset(someUserAssetId)
            val (arrangement, assetImageFetcher) = Arrangement()
                .withErrorResponse(
                    data = data,
                    isRetryNeeded = true,
                    coreFailure = NetworkFailure.NoNetworkConnection(null)
                )
                .arrange()

            // When
            val exception = assertThrows<AssetImageException> { assetImageFetcher.fetch() }

            // Then
            assertEquals(AssetImageRetryPolicy.RETRY_WHEN_CONNECTED, exception.retryPolicy)
            coVerify(inverse = true) { arrangement.drawableResultWrapper.toFetchResult(any()) }
        }

    @Test
    fun givenAUserAvatarAssetData_WhenCallingFetchReturnsFailureWithRetryNeeded_ThenThrowExceptionWithExponentialRetryPolicy() =
        runTest {
            // Given
            val someUserAssetId = AssetId("value", "domain")
            val data = ImageAsset.UserAvatarAsset(someUserAssetId)
            val (arrangement, assetImageFetcher) = Arrangement()
                .withErrorResponse(
                    data = data,
                    isRetryNeeded = true,
                    coreFailure = CoreFailure.Unknown(null)
                )
                .arrange()

            // When
            val exception = assertThrows<AssetImageException> { assetImageFetcher.fetch() }

            // Then
            assertEquals(AssetImageRetryPolicy.EXPONENTIAL_RETRY_WHEN_CONNECTED, exception.retryPolicy)
            coVerify(inverse = true) { arrangement.drawableResultWrapper.toFetchResult(any()) }
        }

    @Test
    fun givenAPrivateAssetImageData_WhenCallingFetchReturnsFailureWithRetryNotNeeded_ThenThrowExceptionWithDoNotRetryPolicy() =
        runTest {
            // Given
            val someConversationId = ConversationId("some-value", "some-domain")
            val someMessageId = "some-message-id"
            val data = ImageAsset.PrivateAsset(someConversationId, someMessageId, true)
            val (arrangement, assetImageFetcher) = Arrangement()
                .withErrorResponse(
                    data = data,
                    isRetryNeeded = false,
                    coreFailure = CoreFailure.Unknown(null)
                )
                .arrange()

            // When
            val exception = assertThrows<AssetImageException> { assetImageFetcher.fetch() }

            // Then
            assertEquals(AssetImageRetryPolicy.DO_NOT_RETRY, exception.retryPolicy)
            coVerify(inverse = true) { arrangement.drawableResultWrapper.toFetchResult(any()) }
        }

    @Test
    fun givenAPrivateAssetImageData_WhenCallingFetchReturnsNoConnectionFailure_ThenThrowExceptionWithRetryPolicy() =
        runTest {
            // Given
            val someConversationId = ConversationId("some-value", "some-domain")
            val someMessageId = "some-message-id"
            val data = ImageAsset.PrivateAsset(someConversationId, someMessageId, true)
            val (arrangement, assetImageFetcher) = Arrangement()
                .withErrorResponse(
                    data = data,
                    isRetryNeeded = true,
                    coreFailure = NetworkFailure.NoNetworkConnection(null)
                )
                .arrange()

            // When
            val exception = assertThrows<AssetImageException> { assetImageFetcher.fetch() }

            // Then
            assertEquals(AssetImageRetryPolicy.RETRY_WHEN_CONNECTED, exception.retryPolicy)
            coVerify(inverse = true) { arrangement.drawableResultWrapper.toFetchResult(any()) }
        }

    @Test
    fun givenAPrivateAssetImageData_WhenCallingFetchReturnsFailureWithRetryNeeded_ThenThrowExceptionWithExponentialRetryPolicy() =
        runTest {
            // Given
            val someConversationId = ConversationId("some-value", "some-domain")
            val someMessageId = "some-message-id"
            val data = ImageAsset.PrivateAsset(someConversationId, someMessageId, true)
            val (arrangement, assetImageFetcher) = Arrangement()
                .withErrorResponse(
                    data = data,
                    isRetryNeeded = true,
                    coreFailure = CoreFailure.Unknown(null)
                )
                .arrange()

            // When
            val exception = assertThrows<AssetImageException> { assetImageFetcher.fetch() }

            // Then
            assertEquals(AssetImageRetryPolicy.EXPONENTIAL_RETRY_WHEN_CONNECTED, exception.retryPolicy)
            coVerify(inverse = true) { arrangement.drawableResultWrapper.toFetchResult(any()) }
        }

    private class Arrangement {
        val getPublicAsset = mockk<GetAvatarAssetUseCase>()
        val getPrivateAsset = mockk<GetMessageAssetUseCase>()
        val deleteAsset = mockk<DeleteAssetUseCase>()
        val drawableResultWrapper = mockk<DrawableResultWrapper>()
        val mockFetchResult = mockk<FetchResult>()
        val mockContext = mockk<Context>()

        lateinit var imageData: ImageAsset.Remote
        private var options: Options? = null

        fun withSuccessfulImageData(
            data: ImageAsset.Remote,
            expectedAssetPath: Path,
            expectedAssetSize: Long,
            assetName: String = "name",
            retryAttempt: Int = 0
        ): Arrangement {
            imageData = data
            options = Options(
                context = mockContext,
                extras = Extras.Builder().set(OPTION_PARAMETER_RETRY_KEY, retryAttempt).build(),
            )

            coEvery { getPublicAsset.invoke((any())) }.returns(PublicAssetResult.Success(expectedAssetPath))
            coEvery { getPrivateAsset.invoke(any(), any()) }.returns(
                CompletableDeferred(
                    MessageAssetResult.Success(
                        expectedAssetPath,
                        expectedAssetSize,
                        assetName
                    )
                )
            )
            coEvery { drawableResultWrapper.toFetchResult(any()) }.returns(mockFetchResult)
            coEvery { deleteAsset.invoke(any()) }.returns(Unit)

            return this
        }

        fun withStoredData(assetData: ByteArray, assetPath: Path): Arrangement {
            fakeKaliumFileSystem.sink(assetPath).buffer().use {
                assetData
            }

            return this
        }

        fun withSuccessFullAssetDelete(): Arrangement {
            coEvery { deleteAsset.invoke(any()) }.returns(Unit)

            return this
        }

        fun withErrorResponse(
            data: ImageAsset.Remote,
            isRetryNeeded: Boolean = false,
            coreFailure: CoreFailure = CoreFailure.Unknown(null)
        ): Arrangement {
            imageData = data
            coEvery { getPublicAsset.invoke((any())) }.returns(
                PublicAssetResult.Failure(
                    coreFailure,
                    isRetryNeeded
                )
            )
            coEvery {
                getPrivateAsset.invoke(
                    any(),
                    any()
                )
            }.returns(
                CompletableDeferred(
                    MessageAssetResult.Failure(
                        coreFailure,
                        isRetryNeeded
                    )
                )
            )

            return this
        }

        fun arrange() = this to AssetImageFetcher(
            assetFetcherParameters = AssetFetcherParameters(
                data = imageData,
                options ?: Options(
                    context = mockContext,
                    extras = Extras.Builder().set(OPTION_PARAMETER_RETRY_KEY, 0).build(),
                )
            ),
            getPublicAsset = getPublicAsset,
            getPrivateAsset = getPrivateAsset,
            deleteAsset = deleteAsset,
            drawableResultWrapper = drawableResultWrapper
        )
    }

    companion object {
        val fakeKaliumFileSystem = FakeKaliumFileSystem()
    }
}
