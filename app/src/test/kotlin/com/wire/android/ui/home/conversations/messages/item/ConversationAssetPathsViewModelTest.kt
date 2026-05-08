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
class ConversationAssetPathsViewModelTest {

    @Test
    fun givenUploadedStatus_whenResolveIfNeededWithDownloadIfNeeded_thenPathIsResolved() = runTest {
        // given
        val expectedPath = "/local/path/image.jpg"
        val (arrangement, viewModel) = Arrangement()
            .withGetMessageAssetSuccess(expectedPath)
            .arrange()

        // when
        viewModel.resolveIfNeeded(
            conversationId = arrangement.conversationId,
            messageId = arrangement.messageId,
            transferStatus = AssetTransferStatus.UPLOADED,
            downloadIfNeeded = true
        )

        // then
        assertEquals(expectedPath, viewModel.localAssetPath(arrangement.messageId))
        coVerify(exactly = 1) { arrangement.getMessageAsset(any(), any()) }
    }

    @Test
    fun givenNoCachedPath_whenLocalAssetPathCalledWithDownloadIfNeeded_thenPathIsResolved() = runTest {
        // given
        val expectedPath = "/local/path/image.jpg"
        val (arrangement, viewModel) = Arrangement()
            .withGetMessageAssetSuccess(expectedPath)
            .arrange()

        // when
        val path = viewModel.localAssetPath(
            conversationId = arrangement.conversationId,
            messageId = arrangement.messageId,
            assetStatus = AssetTransferStatus.UPLOADED,
            downloadIfNeeded = true
        )

        // then
        assertNull(path)
        assertEquals(expectedPath, viewModel.localAssetPath(arrangement.messageId))
        coVerify(exactly = 1) { arrangement.getMessageAsset(any(), any()) }
    }

    @Test
    fun givenCachedPath_whenLocalAssetPathCalled_thenGetAssetIsNotCalledAgain() = runTest {
        // given
        val expectedPath = "/local/path/image.jpg"
        val (arrangement, viewModel) = Arrangement()
            .withGetMessageAssetSuccess(expectedPath)
            .arrange()

        viewModel.resolveIfNeeded(
            conversationId = arrangement.conversationId,
            messageId = arrangement.messageId,
            transferStatus = AssetTransferStatus.UPLOADED,
            downloadIfNeeded = true
        )

        // when
        val path = viewModel.localAssetPath(
            conversationId = arrangement.conversationId,
            messageId = arrangement.messageId,
            assetStatus = AssetTransferStatus.UPLOADED,
            downloadIfNeeded = true
        )

        // then
        assertEquals(expectedPath, path)
        coVerify(exactly = 1) { arrangement.getMessageAsset(any(), any()) }
    }

    @Test
    fun givenDownloadInProgressStatus_whenResolveIfNeededWithDownloadIfNeeded_thenPathIsResolved() = runTest {
        // given
        val expectedPath = "/local/path/image.jpg"
        val (arrangement, viewModel) = Arrangement()
            .withGetMessageAssetSuccess(expectedPath)
            .arrange()

        // when
        viewModel.resolveIfNeeded(
            conversationId = arrangement.conversationId,
            messageId = arrangement.messageId,
            transferStatus = AssetTransferStatus.DOWNLOAD_IN_PROGRESS,
            downloadIfNeeded = true
        )

        // then
        assertEquals(expectedPath, viewModel.localAssetPath(arrangement.messageId))
        coVerify(exactly = 1) { arrangement.getMessageAsset(any(), any()) }
    }

    @Test
    fun givenPathAlreadyResolved_whenResolveIfNeededCalledAgain_thenGetAssetIsNotCalledAgain() = runTest {
        // given
        val expectedPath = "/local/path/image.jpg"
        val (arrangement, viewModel) = Arrangement()
            .withGetMessageAssetSuccess(expectedPath)
            .arrange()

        viewModel.localAssetPath(
            conversationId = arrangement.conversationId,
            messageId = arrangement.messageId,
            assetStatus = AssetTransferStatus.UPLOADED,
            downloadIfNeeded = true
        )
        assertEquals(expectedPath, viewModel.localAssetPath(arrangement.messageId))

        // when
        viewModel.localAssetPath(
            conversationId = arrangement.conversationId,
            messageId = arrangement.messageId,
            assetStatus = AssetTransferStatus.UPLOADED,
            downloadIfNeeded = true
        )

        // then
        coVerify(exactly = 1) { arrangement.getMessageAsset(any(), any()) }
    }

    @Test
    fun givenTwoMessageIds_whenResolveIfNeeded_thenPathsAreResolvedSeparately() = runTest {
        // given
        val firstMessageId = "message-id-1"
        val secondMessageId = "message-id-2"
        val firstPath = "/local/path/first.jpg"
        val secondPath = "/local/path/second.jpg"
        val (arrangement, viewModel) = Arrangement()
            .withGetMessageAssetSuccessForMessage(firstMessageId, firstPath)
            .withGetMessageAssetSuccessForMessage(secondMessageId, secondPath)
            .arrange()

        // when
        viewModel.resolveIfNeeded(
            conversationId = arrangement.conversationId,
            messageId = firstMessageId,
            transferStatus = AssetTransferStatus.UPLOADED,
            downloadIfNeeded = true
        )
        viewModel.resolveIfNeeded(
            conversationId = arrangement.conversationId,
            messageId = secondMessageId,
            transferStatus = AssetTransferStatus.UPLOADED,
            downloadIfNeeded = true
        )

        // then
        assertEquals(firstPath, viewModel.localAssetPath(firstMessageId))
        assertEquals(secondPath, viewModel.localAssetPath(secondMessageId))
    }

    @Test
    fun givenGetAssetFails_whenResolveIfNeeded_thenLocalPathRemainsNull() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withGetMessageAssetFailure()
            .arrange()

        // when
        viewModel.resolveIfNeeded(
            conversationId = arrangement.conversationId,
            messageId = arrangement.messageId,
            transferStatus = AssetTransferStatus.UPLOADED,
            downloadIfNeeded = true
        )

        // then
        assertNull(viewModel.localAssetPath(arrangement.messageId))
    }

    @Test
    fun givenUploadInProgressStatus_whenResolveIfNeeded_thenPathIsNotResolved() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        // when
        viewModel.resolveIfNeeded(
            conversationId = arrangement.conversationId,
            messageId = arrangement.messageId,
            transferStatus = AssetTransferStatus.UPLOAD_IN_PROGRESS,
            downloadIfNeeded = true
        )

        // then
        assertNull(viewModel.localAssetPath(arrangement.messageId))
        coVerify(exactly = 0) { arrangement.getMessageAsset(any(), any()) }
    }

    private class Arrangement {

        @MockK
        lateinit var getMessageAsset: GetMessageAssetUseCase

        val conversationId = ConversationId("conv-value", "conv-domain")
        val messageId = "test-message-id"
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

        fun withGetMessageAssetSuccessForMessage(messageId: String, path: String) = apply {
            coEvery { getMessageAsset(any(), messageId) } returns CompletableDeferred(
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

        fun arrange(): Pair<Arrangement, ConversationAssetPathsViewModelImpl> {
            val viewModel = ConversationAssetPathsViewModelImpl(
                getMessageAsset = getMessageAsset,
                dispatchers = dispatchers
            )
            return this to viewModel
        }
    }
}
