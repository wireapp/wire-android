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
package com.wire.android.ui.home.conversations.usecase

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.util.FileManager
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase.AssetSizeLimits.ASSET_SIZE_DEFAULT_LIMIT_BYTES
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HandleUriAssetUseCaseTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `given an unsupported URI scheme, when invoked, then result should not succeed`() =
        runTest(dispatcher) {
            // Given
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val (arrangement, useCase) = Arrangement()
                .withGetAssetSizeLimitUseCase(true, limit)
                .withGetAssetBundleFromUri(null)
                .withUnsupportedUriScheme()
                .arrange()

            // When
            val result = useCase.invoke(Uri.Builder().scheme("file").path("/data/asdasdasd.txt").build(), false)

            // Then
            assert(result is HandleUriAssetUseCase.Result.Failure.Unknown)
            coVerify(exactly = 0) {
                arrangement.fileManager.getAssetBundleFromUri(any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `given a user picks an image asset less than limit, when invoked, then result should succeed`() =
        runTest(dispatcher) {
            // Given
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val mockedAttachment = AssetBundle(
                "key",
                "image/jpeg",
                "some-data-path".toPath(),
                limit - 1L,
                "mocked_image.jpeg",
                AttachmentType.IMAGE
            )
            val (_, useCase) = Arrangement()
                .withGetAssetSizeLimitUseCase(true, limit)
                .withGetAssetBundleFromUri(mockedAttachment)
                .arrange()

            // When
            val result = useCase.invoke(CONTENT_URI, false)

            // Then
            assert(result is HandleUriAssetUseCase.Result.Success)
        }

    @Test
    fun `given a user picks an image asset larger than limit, when invoked, then result is asset too large failure`() =
        runTest(dispatcher) {
            // Given
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val mockedAttachment = AssetBundle(
                "key",
                "image/jpeg",
                "some-data-path".toPath(),
                limit + 1L,
                "mocked_image.jpeg",
                AttachmentType.IMAGE
            )
            val (_, useCase) = Arrangement()
                .withGetAssetSizeLimitUseCase(true, limit)
                .withGetAssetBundleFromUri(mockedAttachment)
                .arrange()

            // When
            val result = useCase.invoke(CONTENT_URI, false)

            // Then
            assert(result is HandleUriAssetUseCase.Result.Failure.AssetTooLarge)
        }

    @Test
    fun `given that a user picks too large asset that needs saving if invalid, when invoked, then saveToExternalMediaStorage is called`() =
        runTest(dispatcher) {
            // Given
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val mockedAttachment = AssetBundle(
                "key",
                "file/x-zip",
                "some-data-path".toPath(),
                limit + 1L,
                "mocked_asset.zip",
                AttachmentType.GENERIC_FILE
            )
            val (arrangement, useCase) = Arrangement()
                .withGetAssetBundleFromUri(mockedAttachment)
                .withGetAssetSizeLimitUseCase(false, limit)
                .withSaveToExternalMediaStorage("mocked_image.jpeg")
                .arrange()

            // When
            val result = useCase.invoke(CONTENT_URI, true)

            // Then
            coVerify {
                arrangement.fileManager.saveToExternalMediaStorage(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
            assert(result is HandleUriAssetUseCase.Result.Failure.AssetTooLarge)
        }

    @Test
    fun `given that a user picks asset, when getting uri returns null, then it should return error`() =
        runTest(dispatcher) {
            // Given
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val (_, useCase) = Arrangement()
                .withGetAssetBundleFromUri(null)
                .withGetAssetSizeLimitUseCase(false, limit)
                .withSaveToExternalMediaStorage("mocked_image.jpeg")
                .arrange()

            // When
            val result = useCase.invoke(CONTENT_URI, false)

            // Then
            assert(result is HandleUriAssetUseCase.Result.Failure.Unknown)
        }

    @Test
    fun `given a user picks an asset larger than limit, when invoked, then the temp copy is deleted`() =
        runTest(dispatcher) {
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val (arrangement, useCase) = Arrangement()
                .withGetAssetSizeLimitUseCase(false, limit)
                .withCopiedAsset(limit + 1L)
                .arrange()

            val result = useCase.invoke(CONTENT_URI, false)

            assert(result is HandleUriAssetUseCase.Result.Failure.AssetTooLarge)
            assert(!arrangement.fileSystem.exists(arrangement.copiedPath!!))
        }

    @Test
    fun `given a user picks an asset larger than limit that needs saving, when invoked, then it is saved to device before the temp copy is deleted`() =
        runTest(dispatcher) {
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val (arrangement, useCase) = Arrangement()
                .withGetAssetSizeLimitUseCase(false, limit)
                .withCopiedAsset(limit + 1L)
                .withSaveToExternalMediaStorageRequiringCopy("saved.zip")
                .arrange()

            val result = useCase.invoke(CONTENT_URI, true)

            assert(result is HandleUriAssetUseCase.Result.Failure.AssetTooLarge)
            assert(arrangement.copyExistedWhenSaving == true)
            assert(!arrangement.fileSystem.exists(arrangement.copiedPath!!))
        }

    @Test
    fun `given a user picks an asset larger than limit and the caller keeps oversized copies, when invoked, then result is still too large and the temp copy is kept`() =
        runTest(dispatcher) {
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val (arrangement, useCase) = Arrangement()
                .withGetAssetSizeLimitUseCase(false, limit)
                .withCopiedAsset(limit + 1L)
                .arrange()

            val result = useCase.invoke(CONTENT_URI, saveToDeviceIfInvalid = false, keepOversizedCopy = true)

            assert(result is HandleUriAssetUseCase.Result.Failure.AssetTooLarge)
            assert(arrangement.fileSystem.exists(arrangement.copiedPath!!))
        }

    @Test
    fun `given a partial temp copy, when getting the asset bundle returns null, then the temp copy is deleted`() =
        runTest(dispatcher) {
            val (arrangement, useCase) = Arrangement()
                .withPartialCopyAndNullBundle()
                .arrange()

            val result = useCase.invoke(CONTENT_URI, false)

            assert(result is HandleUriAssetUseCase.Result.Failure.Unknown)
            assert(!arrangement.fileSystem.exists(arrangement.copiedPath!!))
        }

    @Test
    fun `given a user picks an asset within limit, when invoked, then the temp copy is kept`() =
        runTest(dispatcher) {
            val limit = ASSET_SIZE_DEFAULT_LIMIT_BYTES
            val (arrangement, useCase) = Arrangement()
                .withGetAssetSizeLimitUseCase(false, limit)
                .withCopiedAsset(limit - 1L)
                .arrange()

            val result = useCase.invoke(CONTENT_URI, false)

            assert(result is HandleUriAssetUseCase.Result.Success)
            assert(arrangement.fileSystem.exists(arrangement.copiedPath!!))
        }

    private class Arrangement {

        @MockK
        lateinit var getAssetSizeLimitUseCase: GetAssetSizeLimitUseCase

        @MockK
        lateinit var fileManager: FileManager

        private val fakeKaliumFileSystem = FakeKaliumFileSystem()
        val fileSystem: KaliumFileSystem get() = fakeKaliumFileSystem
        var copiedPath: Path? = null
        var copyExistedWhenSaving: Boolean? = null

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withGetAssetBundleFromUri(assetBundle: AssetBundle?) = apply {
            coEvery { fileManager.getAssetBundleFromUri(any(), any(), any(), any(), any()) } returns assetBundle
        }

        fun withCopiedAsset(size: Long) = apply {
            coEvery { fileManager.getAssetBundleFromUri(any(), any(), any(), any(), any()) } coAnswers {
                val destination = secondArg<Path>()
                fakeKaliumFileSystem.sink(destination, false).buffer().use { it.writeUtf8("content") }
                copiedPath = destination
                AssetBundle("key", "file/x-zip", destination, size, "mocked_asset.zip", AttachmentType.GENERIC_FILE)
            }
        }

        fun withPartialCopyAndNullBundle() = apply {
            coEvery { fileManager.getAssetBundleFromUri(any(), any(), any(), any(), any()) } coAnswers {
                val destination = secondArg<Path>()
                fakeKaliumFileSystem.sink(destination, false).buffer().use { it.writeUtf8("partial") }
                copiedPath = destination
                null
            }
        }

        fun withSaveToExternalMediaStorageRequiringCopy(resultFileName: String?) = apply {
            coEvery { fileManager.saveToExternalMediaStorage(any(), any(), any(), any(), any()) } coAnswers {
                copyExistedWhenSaving = fakeKaliumFileSystem.exists(secondArg<Path>())
                resultFileName
            }
        }

        fun withUnsupportedUriScheme() = apply {
            every { fileManager.checkValidSchema(any()) } throws IllegalArgumentException("Unsupported URI scheme")
        }

        fun withGetAssetSizeLimitUseCase(isImage: Boolean, assetSizeLimit: Long) = apply {
            coEvery { getAssetSizeLimitUseCase(eq(isImage)) } returns assetSizeLimit
            return this
        }

        fun withSaveToExternalMediaStorage(resultFileName: String?) = apply {
            coEvery { fileManager.saveToExternalMediaStorage(any(), any(), any(), any(), any()) } returns resultFileName
        }

        fun arrange() = this to HandleUriAssetUseCase(
            getAssetSizeLimitUseCase,
            fileManager,
            fakeKaliumFileSystem,
            TestDispatcherProvider(),
        )
    }

    companion object {
        private val CONTENT_URI = "content://example.provider/mocked_image.jpeg".toUri()
    }
}
