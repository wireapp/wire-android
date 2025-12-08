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

import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.ui.common.multipart.AssetSource
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.util.FileManager
import com.wire.kalium.cells.domain.usecase.DownloadCellFileUseCase
import com.wire.kalium.cells.domain.usecase.RefreshCellAssetStateUseCase
import com.wire.kalium.common.functional.right
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.message.CellAssetContent
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

typealias OpenImageCallback = (s: String) -> Unit

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
        coVerify(exactly = 1) { arrangement.refreshAsset(testAttachmentUi.uuid) }
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
        coVerify(exactly = 1) { arrangement.refreshAsset(testAttachmentUi.uuid) }
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

    // TODO: Refresh asset tests (part of refresh update PR)

    private class Arrangement {

        init {
            MockKAnnotations.init(this)
        }

        @MockK
        lateinit var refreshAsset: RefreshCellAssetStateUseCase

        @MockK
        lateinit var download: DownloadCellFileUseCase

        @MockK
        lateinit var fileManager: FileManager

        val kaliumFileSystem: KaliumFileSystem = FakeKaliumFileSystem()

        suspend fun arrange(): Pair<Arrangement, MultipartAttachmentsViewModel> {

            coEvery { refreshAsset(any()) } returns Unit.right()
            coEvery { fileManager.openWithExternalApp(any(), any(), any(), any()) } returns Unit
            coEvery { fileManager.openUrlWithExternalApp(any(), any(), any()) } returns Unit
            coEvery { download(any(), any(), any(), any(), any()) } returns Unit.right()

            return this to MultipartAttachmentsViewModelImpl(
                refreshAsset = refreshAsset,
                download = download,
                fileManager = fileManager,
                kaliumFileSystem = kaliumFileSystem,
            )
        }
    }

    private companion object {
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
