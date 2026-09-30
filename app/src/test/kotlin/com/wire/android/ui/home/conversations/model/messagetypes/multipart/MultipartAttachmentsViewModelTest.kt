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
import com.wire.kalium.cells.domain.model.WireCellsConfig
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.GetPdfPreviewUrlUseCase
import com.wire.kalium.cells.domain.usecase.GetWireCellConfigurationUseCase
import com.wire.kalium.cells.domain.usecase.offline.ObserveOfflineFilesUseCase
import com.wire.kalium.cells.domain.usecase.offline.OfflineFileInfo
import com.wire.kalium.common.functional.Either
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.featureConfig.CollaboraEdition
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.CellAssetContent
import com.wire.kalium.logic.feature.conversation.IsSelfUserViewerOnConversationUseCase
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
typealias OpenAttachmentCallback = (attachment: MultipartAttachmentUi) -> Unit
typealias OpenPdfCallback = (attachment: MultipartAttachmentUi, pdfPreviewUrl: String?) -> Unit

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

        viewModel.onClick(testAttachmentUi, callback, {}, {}, { _, _ -> })

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
            openInImageViewer = callback,
            openInVideoPlayer = { },
            openInAudioPlayer = { },
            openInPdfViewer = { _, _ -> },
        )

        coVerify(exactly = 0) { callback.invoke(testAttachmentUi.uuid) }
        coVerify(exactly = 1) { arrangement.refreshHelper.refresh(testAttachmentUi.uuid, any()) }
    }

    @Test
    fun `with file attachment with not found status when clicked then refresh is called`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        val callback = mockk<OpenImageCallback>(relaxed = true)

        viewModel.onClick(
            attachment = testAttachmentUi.copy(
                mimeType = "application/pdf",
                assetType = AttachmentFileType.PDF,
                transferStatus = AssetTransferStatus.NOT_FOUND,
            ),
            openInImageViewer = callback,
            openInVideoPlayer = { },
            openInAudioPlayer = { },
            openInPdfViewer = { _, _ -> },
        )

        coVerify(exactly = 0) { callback.invoke(testAttachmentUi.uuid) }
        coVerify(exactly = 1) { arrangement.refreshHelper.refresh(testAttachmentUi.uuid, any()) }
    }

    @Test
    fun `with file attachment with local file available when clicked then file is opened locally`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        val callback = mockk<OpenImageCallback>(relaxed = true)

        viewModel.onClick(
            attachment = testAttachmentUi.copy(
                mimeType = "application/zip",
                assetType = AttachmentFileType.ARCHIVE,
                localPath = "local/path",
            ),
            openInImageViewer = callback,
            openInVideoPlayer = { },
            openInAudioPlayer = { },
            openInPdfViewer = { _, _ -> },
        )

        coVerify(exactly = 1) { arrangement.fileManager.openWithExternalApp(any(), any(), any(), any()) }
    }

    @Test
    fun `with file attachment that cannot be opened from its url when clicked then it is downloaded`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        val callback = mockk<OpenImageCallback>(relaxed = true)

        // Only image, video and audio are opened straight from the content url.
        viewModel.onClick(
            attachment = testAttachmentUi.copy(
                mimeType = "application/zip",
                assetType = AttachmentFileType.ARCHIVE,
                contentUrl = "content/url",
            ),
            openInImageViewer = callback,
            openInVideoPlayer = { },
            openInAudioPlayer = { },
            openInPdfViewer = { _, _ -> },
        )

        coVerify(exactly = 0) { arrangement.fileManager.openUrlWithExternalApp(any(), any(), any()) }
        verify(exactly = 1) { arrangement.openFileDownloadController.start(any(), any(), any(), any()) }
    }

    @Test
    fun `with pdf attachment with local file available when clicked then pdf opened in internal viewer`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        val callback = mockk<OpenPdfCallback>(relaxed = true)
        val attachment = testAttachmentUi.copy(
            mimeType = "application/pdf",
            assetType = AttachmentFileType.PDF,
            localPath = "local/path",
        )

        viewModel.onClick(
            attachment = attachment,
            openInImageViewer = { },
            openInVideoPlayer = { },
            openInAudioPlayer = { },
            openInPdfViewer = callback,
        )

        coVerify(exactly = 1) { callback.invoke(attachment, null) }
        coVerify(exactly = 0) { arrangement.fileManager.openWithExternalApp(any(), any(), any(), any()) }
    }

    @Test
    fun `with pdf attachment openable via remote path when clicked then pdf opened in internal viewer`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        val callback = mockk<OpenPdfCallback>(relaxed = true)
        val attachment = testAttachmentUi.copy(
            mimeType = "application/pdf",
            assetType = AttachmentFileType.PDF,
            remotePath = "/cells/path/doc.pdf",
        )

        viewModel.onClick(
            attachment = attachment,
            openInImageViewer = { },
            openInVideoPlayer = { },
            openInAudioPlayer = { },
            openInPdfViewer = callback,
        )

        coVerify(exactly = 1) { callback.invoke(attachment, null) }
        coVerify(exactly = 0) { arrangement.fileManager.openUrlWithExternalApp(any(), any(), any()) }
    }

    @Test
    fun `with pdf attachment not downloaded yet when clicked then the viewer is not opened`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        val callback = mockk<OpenPdfCallback>(relaxed = true)

        viewModel.onClick(
            attachment = testAttachmentUi.copy(
                mimeType = "application/pdf",
                assetType = AttachmentFileType.PDF,
                remotePath = null,
            ),
            openInImageViewer = { },
            openInVideoPlayer = { },
            openInAudioPlayer = { },
            openInPdfViewer = callback,
        )

        coVerify(exactly = 0) { callback.invoke(any(), any()) }
        coVerify(exactly = 0) { arrangement.fileManager.openWithExternalApp(any(), any(), any(), any()) }
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
        viewModel.onClick(pdfAttachment.copy(openLoadState = null), mockk(), mockk(), mockk(), mockk())

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
        viewModel.onClick(pdfAttachment.copy(openLoadState = null), mockk(), mockk(), mockk(), mockk())

        // File must open directly — no new download triggered
        verify(exactly = 0) { arrangement.openFileDownloadController.start(any(), any(), any(), any()) }
        coVerify(exactly = 1) { arrangement.fileManager.openWithExternalApp(any(), any(), any(), any()) }
    }

    @Test
    fun `givenNoActiveState_whenClicked_thenControllerStartIsCalled`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        // Not a pdf - pdfs are handed over to the internal viewer instead of being downloaded here.
        val attachment = testAttachmentUi.copy(
            mimeType = "application/zip",
            assetType = AttachmentFileType.ARCHIVE,
        )

        // No state in cache — VM delegates to the download controller
        viewModel.onClick(attachment, mockk(), mockk(), mockk(), mockk())

        verify(exactly = 1) { arrangement.openFileDownloadController.start(any(), any(), any(), any()) }
    }

    @Test
    fun `givenVideoAttachmentWithContentUrl_whenClicked_thenVideoIsStreamedInPlayerInsteadOfOpeningUrl`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        val videoAttachment = testAttachmentUi.copy(
            mimeType = "video/mp4",
            assetType = AttachmentFileType.VIDEO,
            contentUrl = "content/url",
        )
        val openInVideoPlayer = mockk<OpenAttachmentCallback>(relaxed = true)

        // A cell video always carries a pre-signed contentUrl, which the in-app video player streams.
        // It must not be downloaded first, nor handed over to an external app.
        viewModel.onClick(videoAttachment, mockk(), openInVideoPlayer, mockk(), mockk())

        verify(exactly = 1) { openInVideoPlayer.invoke(videoAttachment) }
        verify(exactly = 0) { arrangement.openFileDownloadController.start(any(), any(), any(), any()) }
        coVerify(exactly = 0) { arrangement.fileManager.openUrlWithExternalApp(any(), any(), any()) }
    }

    @Test
    fun `givenVideoAttachmentWithLocalPath_whenClicked_thenVideoIsPlayedFromLocalFile`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        val videoAttachment = testAttachmentUi.copy(
            mimeType = "video/mp4",
            assetType = AttachmentFileType.VIDEO,
            contentUrl = "content/url",
            localPath = "local/path",
        )
        val openInVideoPlayer = mockk<OpenAttachmentCallback>(relaxed = true)

        viewModel.onClick(videoAttachment, mockk(), openInVideoPlayer, mockk(), mockk())

        verify(exactly = 1) { openInVideoPlayer.invoke(videoAttachment) }
        verify(exactly = 0) { arrangement.openFileDownloadController.start(any(), any(), any(), any()) }
        coVerify(exactly = 0) { arrangement.fileManager.openWithExternalApp(any(), any(), any(), any()) }
        coVerify(exactly = 0) { arrangement.fileManager.openUrlWithExternalApp(any(), any(), any()) }
    }

    @Test
    fun `givenEditableAttachmentAndEditorAccess_whenClicked_thenOnlineEditorIsOpened`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withCollaboraEnabled()
            .arrange()
        val openInPdfViewer = mockk<OpenPdfCallback>(relaxed = true)

        viewModel.onClick(testEditableAttachmentUi, mockk(), mockk(), mockk(), openInPdfViewer)

        coVerify(exactly = 1) { arrangement.getEditorUrl(testEditableAttachmentUi.uuid) }
        verify(exactly = 1) { arrangement.onlineEditor.open(EDITOR_URL) }
        coVerify(exactly = 0) { arrangement.getPdfPreviewUrl(any()) }
        verify(exactly = 0) { openInPdfViewer.invoke(any(), any()) }
    }

    @Test
    fun `givenEditableAttachmentAndViewerAccess_whenClicked_thenPdfRenditionIsOpenedInsteadOfEditor`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withCollaboraEnabled()
            .withViewerOnlyAccess()
            .arrange()
        val openInPdfViewer = mockk<OpenPdfCallback>(relaxed = true)

        viewModel.onClick(testEditableAttachmentUi, mockk(), mockk(), mockk(), openInPdfViewer)

        verify(exactly = 1) { openInPdfViewer.invoke(testEditableAttachmentUi, PDF_RENDITION_URL) }
        verify(exactly = 0) { arrangement.onlineEditor.open(any()) }
        coVerify(exactly = 0) { arrangement.getEditorUrl(any()) }
        verify(exactly = 0) { arrangement.openFileDownloadController.start(any(), any(), any(), any()) }
    }

    @Test
    fun `givenEditableAttachmentWithoutPdfRenditionAndViewerAccess_whenClicked_thenNothingIsOpened`() = runTest {
        // The backend has no rendition yet: still processing, failed, or the type is not convertible.
        val (arrangement, viewModel) = Arrangement()
            .withCollaboraEnabled()
            .withViewerOnlyAccess()
            .withoutPdfRendition()
            .arrange()
        val openInPdfViewer = mockk<OpenPdfCallback>(relaxed = true)

        viewModel.onClick(testEditableAttachmentUi, mockk(), mockk(), mockk(), openInPdfViewer)

        // A viewer has nothing to read, and must not be handed the original file either.
        verify(exactly = 0) { openInPdfViewer.invoke(any(), any()) }
        verify(exactly = 0) { arrangement.onlineEditor.open(any()) }
        verify(exactly = 0) { arrangement.openFileDownloadController.start(any(), any(), any(), any()) }
    }

    @Test
    fun `givenEditableAttachmentAndDrivePermissionsDisabled_whenClicked_thenOnlineEditorIsOpened`() = runTest {
        // Viewer restrictions only apply while the drive permissions feature is on.
        val (arrangement, viewModel) = Arrangement()
            .withCollaboraEnabled()
            .withViewerOnlyAccess()
            .withDrivePermissionsDisabled()
            .arrange()
        val openInPdfViewer = mockk<OpenPdfCallback>(relaxed = true)

        viewModel.onClick(testEditableAttachmentUi, mockk(), mockk(), mockk(), openInPdfViewer)

        verify(exactly = 1) { arrangement.onlineEditor.open(EDITOR_URL) }
        verify(exactly = 0) { openInPdfViewer.invoke(any(), any()) }
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
        lateinit var getPdfPreviewUrl: GetPdfPreviewUrlUseCase

        @MockK
        lateinit var isSelfUserViewerOnConversation: IsSelfUserViewerOnConversationUseCase

        @MockK
        lateinit var observeOfflineFiles: ObserveOfflineFilesUseCase

        private var collaboraEdition: CollaboraEdition = CollaboraEdition.NO
        private var viewerOnly: Boolean = false
        private var drivePermissionsEnabled: Boolean = true
        private var pdfRenditionUrl: String? = PDF_RENDITION_URL

        fun withCollaboraEnabled() = apply {
            collaboraEdition = CollaboraEdition.CODE
        }

        fun withViewerOnlyAccess() = apply {
            viewerOnly = true
        }

        fun withDrivePermissionsDisabled() = apply {
            drivePermissionsEnabled = false
        }

        /** The backend has no PDF rendition for the document — still processing, failed or unsupported. */
        fun withoutPdfRendition() = apply {
            pdfRenditionUrl = null
        }

        fun arrange(): Pair<Arrangement, MultipartAttachmentsViewModel> {
            coEvery { refreshHelper.refresh(any(), any()) } returns Unit
            coEvery { fileManager.openWithExternalApp(any(), any(), any(), any()) } returns Unit
            coEvery { fileManager.openUrlWithExternalApp(any(), any(), any()) } returns Unit
            coEvery { getWireCellsConfig() } returns WireCellsConfig(
                backendUrl = null,
                collabora = collaboraEdition,
                teamQuotaBytes = null,
            )
            every { kaliumConfigs.collaboraIntegration } returns true
            every { kaliumConfigs.drivePermissionsEnabled } returns drivePermissionsEnabled
            // The use case returns `true` when the self user has full access to the conversation.
            coEvery { isSelfUserViewerOnConversation(testConversationId) } returns !viewerOnly
            coEvery { getEditorUrl(any()) } returns Either.Right(EDITOR_URL)
            coEvery { getPdfPreviewUrl(any()) } returns Either.Right(pdfRenditionUrl)
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
                getPdfPreviewUrl = getPdfPreviewUrl,
                isSelfUserViewerOnConversation = isSelfUserViewerOnConversation,
                observeOfflineFiles = observeOfflineFiles,
            )
        }
    }

    private companion object {
        const val EDITOR_URL = "https://collabora.wire.com/edit"
        const val PDF_RENDITION_URL = "https://cells.wire.com/previews/document.pdf?presigned=true"
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
            // Mirrors testAssetContent.assetPath, which toUiModel() maps onto remotePath.
            remotePath = "/filename",
            mimeType = "image/png",
            assetType = AttachmentFileType.IMAGE,
            assetSize = 0,
            transferStatus = AssetTransferStatus.NOT_DOWNLOADED,
        )
        val testEditableAttachmentUi = testAttachmentUi.copy(
            uuid = "asset_doc",
            fileName = "document.docx",
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            assetType = AttachmentFileType.DOC,
            isEditSupported = true,
            contentUrl = "https://cells.wire.com/document.docx?presigned=true",
        )
    }
}
