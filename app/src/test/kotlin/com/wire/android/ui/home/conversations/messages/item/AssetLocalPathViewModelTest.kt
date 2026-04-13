/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.home.conversations.messages.item

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class AssetLocalPathViewModelTest {

    @Test
    fun givenUploadedStatus_whenResolveIfNeededWithDownloadIfNeeded_thenPathIsResolved() = runTest {
        // given
        val expectedPath = "/local/path/image.jpg"
        val (arrangement, viewModel) = Arrangement()
            .withGetMessageAssetSuccess(expectedPath)
            .arrange()

        // when
        viewModel.resolveIfNeeded(transferStatus = AssetTransferStatus.UPLOADED, downloadIfNeeded = true)

        // then
        assertEquals(expectedPath, viewModel.localAssetPath)
        coVerify(exactly = 1) { arrangement.getMessageAsset(any(), any()) }
    }

    @Test
    fun givenNotDownloadedStatus_whenResolveIfNeededWithDownloadIfNeeded_thenPathIsResolved() = runTest {
        // given
        val expectedPath = "/local/path/image.jpg"
        val (arrangement, viewModel) = Arrangement()
            .withGetMessageAssetSuccess(expectedPath)
            .arrange()

        // when
        viewModel.resolveIfNeeded(transferStatus = AssetTransferStatus.NOT_DOWNLOADED, downloadIfNeeded = true)

        // then
        assertEquals(expectedPath, viewModel.localAssetPath)
        coVerify(exactly = 1) { arrangement.getMessageAsset(any(), any()) }
    }

    @Test
    fun givenSavedInternallyStatus_whenResolveIfNeeded_thenPathIsResolved() = runTest {
        // given
        val expectedPath = "/local/path/image.jpg"
        val (arrangement, viewModel) = Arrangement()
            .withGetMessageAssetSuccess(expectedPath)
            .arrange()

        // when
        viewModel.resolveIfNeeded(transferStatus = AssetTransferStatus.SAVED_INTERNALLY, downloadIfNeeded = false)

        // then
        assertEquals(expectedPath, viewModel.localAssetPath)
        coVerify(exactly = 1) { arrangement.getMessageAsset(any(), any()) }
    }

    @Test
    fun givenUploadInProgressStatus_whenResolveIfNeededWithDownloadIfNeeded_thenPathIsNotResolved() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        // when
        viewModel.resolveIfNeeded(transferStatus = AssetTransferStatus.UPLOAD_IN_PROGRESS, downloadIfNeeded = true)

        // then
        assertNull(viewModel.localAssetPath)
        coVerify(exactly = 0) { arrangement.getMessageAsset(any(), any()) }
    }

    @Test
    fun givenUploadedStatusButDownloadIfNeededFalse_whenResolveIfNeeded_thenPathIsNotResolved() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        // when
        viewModel.resolveIfNeeded(transferStatus = AssetTransferStatus.UPLOADED, downloadIfNeeded = false)

        // then
        assertNull(viewModel.localAssetPath)
        coVerify(exactly = 0) { arrangement.getMessageAsset(any(), any()) }
    }

    @Test
    fun givenPathAlreadyResolved_whenResolveIfNeededCalledAgain_thenGetAssetIsNotCalledAgain() = runTest {
        // given
        val expectedPath = "/local/path/image.jpg"
        val (arrangement, viewModel) = Arrangement()
            .withGetMessageAssetSuccess(expectedPath)
            .arrange()

        viewModel.resolveIfNeeded(transferStatus = AssetTransferStatus.UPLOADED, downloadIfNeeded = true)
        assertEquals(expectedPath, viewModel.localAssetPath)

        // when - call again
        viewModel.resolveIfNeeded(transferStatus = AssetTransferStatus.UPLOADED, downloadIfNeeded = true)

        // then - use case is only called once
        coVerify(exactly = 1) { arrangement.getMessageAsset(any(), any()) }
    }

    @Test
    fun givenGetAssetFails_whenResolveIfNeeded_thenLocalPathRemainsNull() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withGetMessageAssetFailure()
            .arrange()

        // when
        viewModel.resolveIfNeeded(transferStatus = AssetTransferStatus.UPLOADED, downloadIfNeeded = true)

        // then
        assertNull(viewModel.localAssetPath)
    }

    private class Arrangement {

        @MockK
        lateinit var getMessageAsset: GetMessageAssetUseCase

        private val conversationId = ConversationId("conv-value", "conv-domain")
        private val messageId = "test-message-id"
        private val dispatchers = TestDispatcherProvider()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withGetMessageAssetSuccess(path: String) = apply {
            coEvery { getMessageAsset(any(), any()) } returns CompletableDeferred(
                MessageAssetResult.Success(
                    decodedAssetPath = path.toPath(normalize = true),
                    assetSize = 1024L,
                    assetName = "image.jpg"
                )
            )
        }

        fun withGetMessageAssetFailure() = apply {
            coEvery { getMessageAsset(any(), any()) } returns CompletableDeferred(
                MessageAssetResult.Failure(
                    coreFailure = com.wire.kalium.common.error.CoreFailure.Unknown(null),
                    isRetryNeeded = false
                )
            )
        }

        fun arrange(): Pair<Arrangement, AssetLocalPathViewModelImpl> {
            val viewModel = AssetLocalPathViewModelImpl(
                getMessageAsset = getMessageAsset,
                dispatchers = dispatchers,
                args = AssetLocalPathArgs(
                    conversationId = conversationId,
                    messageId = messageId,
                )
            )
            return this to viewModel
        }
    }
}

