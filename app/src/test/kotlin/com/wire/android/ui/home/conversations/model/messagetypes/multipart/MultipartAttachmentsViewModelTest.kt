/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.model.messagetypes.multipart

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.ui.CellFileLocalPathCache
import com.wire.android.feature.cells.ui.OpenFileDownloadController
import com.wire.android.feature.cells.ui.edit.OnlineEditor
import com.wire.android.feature.cells.ui.model.OpenLoadState
import com.wire.android.ui.common.multipart.AssetSource
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.common.multipart.MultipartAttachmentOpenLoadState
import com.wire.android.util.FileManager
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.GetWireCellConfigurationUseCase
import com.wire.kalium.cells.domain.usecase.offline.ObserveOfflineFilesUseCase
import com.wire.kalium.cells.domain.usecase.offline.OfflineFileInfo
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.CellAssetContent
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

typealias OpenImageCallback = (s: String) -> Unit

@ExtendWith(CoroutineTestExtension::class)
class MultipartAttachmentsViewModelTest {

    @Test
    fun `with multiple media attachments when mapped the attachments are grouped correctly`() = runTest {
        val (_, viewModel) = Arrangement()
            .arrange()

        val result = viewModel.mapAttachments(
            listOf(
                testAssetContent.copy(id = "asset_1"),
                testAssetContent.copy(id = "asset_2"),
                testAssetContent.copy(id = "asset_3"),
            )
        )

        assertEquals(
            listOf(
                MultipartAttachmentsViewModel.MultipartAttachmentGroup.Media(
                    attachments = listOf(
                        testAttachmentUi.copy(uuid = "asset_1"),
                        testAttachmentUi.copy(uuid = "asset_2"),
                        testAttachmentUi.copy(uuid = "asset_3"),
                    )
                )
            ),
            result
        )
    }

    @Test
    fun `with multiple file attachments when mapped the attachments are grouped correctly`() = runTest {
        val (_, viewModel) = Arrangement()
            .arrange()

        val result = viewModel.mapAttachments(
            listOf(
                testAssetContent.copy(id = "asset_1", mimeType = "application/pdf"),
                testAssetContent.copy(id = "asset_2", mimeType = "application/pdf"),
                testAssetContent.copy(id = "asset_3", mimeType = "application/pdf"),
            )
        )

        assertEquals(
            listOf(
                MultipartAttachmentsViewModel.MultipartAttachmentGroup.Files(
                    attachments = listOf(
                        testAttachmentUi.copy(uuid = "asset_1", mimeType = "application/pdf", assetType = AttachmentFileType.PDF),
                        testAttachmentUi.copy(uuid = "asset_2", mimeType = "application/pdf", assetType = AttachmentFileType.PDF),
                        testAttachmentUi.copy(uuid = "asset_3", mimeType = "application/pdf", assetType = AttachmentFileType.PDF),
                    )
                )
            ),
            result
        )
    }

    @Test
    fun `with mixed media attachments when mapped the attachments are grouped correctly`() = runTest {
        val (_, viewModel) = Arrangement()
            .arrange()

        val result = viewModel.mapAttachments(
            listOf(
                testAssetContent.copy(id = "asset_1"),
                testAssetContent.copy(id = "asset_2"),
                testAssetContent.copy(id = "asset_3"),
                testAssetContent.copy(id = "asset_4", mimeType = "application/pdf"),
                testAssetContent.copy(id = "asset_5"),
            )
        )

        assertEquals(
            listOf(
                MultipartAttachmentsViewModel.MultipartAttachmentGroup.Media(
                    attachments = listOf(
                        testAttachmentUi.copy(uuid = "asset_1"),
                        testAttachmentUi.copy(uuid = "asset_2"),
                        testAttachmentUi.copy(uuid = "asset_3"),
                    )
                ),
                MultipartAttachmentsViewModel.MultipartAttachmentGroup.Files(
                    attachments = listOf(
                        testAttachmentUi.copy(uuid = "asset_4", mimeType = "application/pdf", assetType = AttachmentFileType.PDF),
                    )
                ),
                MultipartAttachmentsViewModel.MultipartAttachmentGroup.Media(
                    attachments = listOf(
                        testAttachmentUi.copy(uuid = "asset_5"),
                    )
                ),
            ),
            result
        )
    }

    @Test
    fun `with offline attachment id when mapped then attachment is marked as available offline`() = runTest {
        val (_, viewModel) = Arrangement()
            .arrange()

        val result = viewModel.mapAttachments(
            listOf(testAssetContent.copy(id = "asset_1", mimeType = "application/pdf")),
            offlineAttachmentIds = setOf("asset_1")
        )

        assertEquals(
            listOf(
                MultipartAttachmentsViewModel.MultipartAttachmentGroup.Files(
                    attachments = listOf(
                        testAttachmentUi.copy(
                            uuid = "asset_1",
                            mimeType = "application/pdf",
                            assetType = AttachmentFileType.PDF,
                            isAvailableOffline = true,
                        ),
                    )
                )
            ),
            result
        )
    }

    @Test
    fun `with loading state when mapped then progress and open load state are exposed`() = runTest {
        val (_, viewModel) = Arrangement()
            .arrange()

        val result = viewModel.mapAttachments(
            attachments = listOf(testAssetContent.copy(id = "asset_1", mimeType = "application/pdf")),
            openLoadStates = mapOf("asset_1" to MultipartAttachmentOpenLoadState.Loading(progress = 0.5f)),
        )

        assertEquals(
            MultipartAttachmentOpenLoadState.Loading(progress = 0.5f),
            (result.first() as MultipartAttachmentsViewModel.MultipartAttachmentGroup.Files).attachments.first().openLoadState,
        )
        assertEquals(
            0.5f,
            (result.first() as MultipartAttachmentsViewModel.MultipartAttachmentGroup.Files).attachments.first().progress,
        )
    }

    @Test
    fun `with image attachment when clicked then image opened in internal viewer`() = runTest {
        val (_, viewModel) = Arrangement()
            .arrange()

        val callback = mockk<OpenImageCallback>(relaxed = true)

        viewModel.onClick(testAttachmentUi, callback)

        coVerify(exactly = 1) { callback.invoke(testAttachmentUi.uuid) }
    }

    @Test
    fun `with image attachment with not found status when clicked then image is not opened`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        val callback = mockk<OpenImageCallback>(relaxed = true)

        viewModel.onClick(
            attachment = testAttachmentUi.copy(
                transferStatus = AssetTransferStatus.NOT_FOUND,
            ),
            openInImageViewer = callback
        )

        coVerify(exactly = 0) { callback.invoke(testAttachmentUi.uuid) }
        coVerify(exactly = 1) { arrangement.refreshHelper.refresh(testAttachmentUi.uuid) }
    }

    @Test
    fun `with file attachment with not found status when clicked then refresh is called`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        val callback = mockk<OpenImageCallback>(relaxed = true)

        viewModel.onClick(
            attachment = testAttachmentUi.copy(
                mimeType = "application/pdf",
                transferStatus = AssetTransferStatus.NOT_FOUND,
            ),
            openInImageViewer = callback
        )

        coVerify(exactly = 0) { callback.invoke(testAttachmentUi.uuid) }
        coVerify(exactly = 1) { arrangement.refreshHelper.refresh(testAttachmentUi.uuid) }
    }

    @Test
    fun `with file attachment with local file available when clicked then file is opened locally`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        val callback = mockk<OpenImageCallback>(relaxed = true)

        viewModel.onClick(
            attachment = testAttachmentUi.copy(
                mimeType = "application/pdf",
                localPath = "local/path",
            ),
            openInImageViewer = callback
        )

        coVerify(exactly = 1) { arrangement.fileManager.openWithExternalApp(any(), any(), any(), any()) }
    }

    @Test
    fun `with file attachment openable via url when clicked then file is opened via url`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        val callback = mockk<OpenImageCallback>(relaxed = true)

        viewModel.onClick(
            attachment = testAttachmentUi.copy(
                mimeType = "application/pdf",
                contentUrl = "content/url",
            ),
            openInImageViewer = callback
        )

        coVerify(exactly = 1) { arrangement.fileManager.openUrlWithExternalApp(any(), any(), any()) }
    }

    @Test
    fun `givenFileActivelyDownloading_whenClickedAgain_thenDownloadIsCancelled`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        val pdfAttachment = testAttachmentUi.copy(
            mimeType = "application/pdf",
            assetType = AttachmentFileType.PDF,
        )

        // Put the file into Loading state in the shared cache (as the controller would after start())
        arrangement.sharedPathCache.setOpenLoadState(pdfAttachment.uuid, OpenLoadState.Loading())

        // Loading state is reflected in the VM
        assertTrue(viewModel.openLoadStates.value[pdfAttachment.uuid] is MultipartAttachmentOpenLoadState.Loading)

        // Click again with a stale attachment snapshot (no openLoadState set).
        // The VM must use its own authoritative cache state — not the stale UI snapshot.
        viewModel.onClick(pdfAttachment.copy(openLoadState = null), mockk())

        // Controller.cancel() should have been called
        verify(exactly = 1) { arrangement.openFileDownloadController.cancel(pdfAttachment.uuid, any()) }
        // Loading state is cleared (by the cancel mock)
        assertNull(viewModel.openLoadStates.value[pdfAttachment.uuid])
    }

    @Test
    fun `givenDownloadCompleted_whenClickedDuringReadyState_thenFileIsOpenedImmediately`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        val pdfAttachment = testAttachmentUi.copy(
            mimeType = "application/pdf",
            assetType = AttachmentFileType.PDF,
        )

        // Simulate controller having finished download and set Ready state
        val downloadedPath = "/downloads/test.pdf"
        arrangement.sharedPathCache.setOpenLoadState(
            pdfAttachment.uuid,
            OpenLoadState.Ready(downloadedPath.toPath())
        )

        // Verify VM reflects Ready state
        assertTrue(viewModel.openLoadStates.value[pdfAttachment.uuid] is MultipartAttachmentOpenLoadState.Ready)

        // Tap while in Ready state (stale attachment without openLoadState)
        viewModel.onClick(pdfAttachment.copy(openLoadState = null), mockk())

        // File must open directly — no new download triggered
        verify(exactly = 0) { arrangement.openFileDownloadController.start(any(), any(), any(), any()) }
        coVerify(exactly = 1) { arrangement.fileManager.openWithExternalApp(any(), any(), any(), any()) }
    }

    @Test
    fun `givenNoActiveState_whenClicked_thenControllerStartIsCalled`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        val pdfAttachment = testAttachmentUi.copy(
            mimeType = "application/pdf",
            assetType = AttachmentFileType.PDF,
        )

        // No state in cache — VM delegates to the download controller
        viewModel.onClick(pdfAttachment, mockk())

        verify(exactly = 1) { arrangement.openFileDownloadController.start(any(), any(), any(), any()) }
    }

    @Test
    fun `givenVideoAttachmentWithContentUrl_whenClicked_thenDownloadStartsInsteadOfOpeningUrl`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        val videoAttachment = testAttachmentUi.copy(
            mimeType = "video/mp4",
            assetType = AttachmentFileType.VIDEO,
            contentUrl = "content/url",
        )

        // A cell video always carries a pre-signed contentUrl, but it must still go through the
        // download/loading flow so the spinner/progress is shown — not be opened via the URL.
        viewModel.onClick(videoAttachment, mockk())

        verify(exactly = 1) { arrangement.openFileDownloadController.start(any(), any(), any(), any()) }
        coVerify(exactly = 0) { arrangement.fileManager.openUrlWithExternalApp(any(), any(), any()) }
    }

    @Test
    fun `givenVideoAttachmentWithLocalPath_whenClicked_thenFileIsOpenedLocally`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        val videoAttachment = testAttachmentUi.copy(
            mimeType = "video/mp4",
            assetType = AttachmentFileType.VIDEO,
            contentUrl = "content/url",
            localPath = "local/path",
        )

        viewModel.onClick(videoAttachment, mockk())

        coVerify(exactly = 1) { arrangement.fileManager.openWithExternalApp(any(), any(), any(), any()) }
        verify(exactly = 0) { arrangement.openFileDownloadController.start(any(), any(), any(), any()) }
        coVerify(exactly = 0) { arrangement.fileManager.openUrlWithExternalApp(any(), any(), any()) }
    }
    // TODO: Refresh asset tests (part of refresh update PR)

    private class Arrangement {

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        @MockK
        lateinit var refreshHelper: CellAssetRefreshHelper

        @MockK
        lateinit var openFileDownloadController: OpenFileDownloadController

        val sharedPathCache = CellFileLocalPathCache()

        @MockK
        lateinit var getEditorUrl: GetEditorUrlUseCase

        @MockK
        lateinit var onlineEditor: OnlineEditor

        @MockK
        lateinit var fileManager: FileManager

        @MockK
        lateinit var kaliumConfigs: KaliumConfigs

        @MockK
        lateinit var getWireCellsConfig: GetWireCellConfigurationUseCase

        @MockK
        lateinit var observeOfflineFiles: ObserveOfflineFilesUseCase

        fun withSlowDownload() = apply {
            every { openFileDownloadController.start(any(), any(), any(), any()) } answers {
                // Simulate slow download — just set Loading, don't call onOpenFile
                val cellNode = secondArg<com.wire.android.feature.cells.ui.model.CellNodeUi.File>()
                sharedPathCache.setOpenLoadState(cellNode.uuid, OpenLoadState.Loading())
            }
        }

        fun arrange(): Pair<Arrangement, MultipartAttachmentsViewModel> {
            coEvery { refreshHelper.refresh(any()) } returns Unit
            coEvery { fileManager.openWithExternalApp(any(), any(), any(), any()) } returns Unit
            coEvery { fileManager.openUrlWithExternalApp(any(), any(), any()) } returns Unit
            coEvery { getWireCellsConfig() } returns null
            every { observeOfflineFiles() } returns flowOf(emptyList<OfflineFileInfo>())

            // Default: controller.cancel() clears state from the shared cache
            every { openFileDownloadController.cancel(any(), any()) } answers {
                val uuid = firstArg<String>()
                sharedPathCache.clearOpenLoadState(uuid)
            }

            // Default: controller.start() does nothing (no-op — tests that need specific behaviour
            // can override via withSlowDownload() or by pre-seeding sharedPathCache)
            every { openFileDownloadController.start(any(), any(), any(), any()) } returns Unit

            return this to MultipartAttachmentsViewModelImpl(
                conversationId = testConversationId,
                refreshHelper = refreshHelper,
                openFileDownloadController = openFileDownloadController,
                sharedPathCache = sharedPathCache,
                getEditorUrl = getEditorUrl,
                onlineEditor = onlineEditor,
                fileManager = fileManager,
                featureFlags = kaliumConfigs,
                getWireCellsConfig = getWireCellsConfig,
                observeOfflineFiles = observeOfflineFiles,
            )
        }
    }

    private companion object {
        val testConversationId = ConversationId("test-conversation-id", "test-domain")

        val testAssetContent = CellAssetContent(
            id = "assetId1",
            versionId = "1",
            mimeType = "image/png",
            assetPath = "/filename",
            assetSize = 0,
            metadata = null,
            transferStatus = AssetTransferStatus.NOT_DOWNLOADED,
        )
        val testAttachmentUi = MultipartAttachmentUi(
            uuid = "asset_1",
            source = AssetSource.CELL,
            fileName = "filename",
            localPath = null,
            mimeType = "image/png",
            assetType = AttachmentFileType.IMAGE,
            assetSize = 0,
            transferStatus = AssetTransferStatus.NOT_DOWNLOADED,
        )
    }
}
