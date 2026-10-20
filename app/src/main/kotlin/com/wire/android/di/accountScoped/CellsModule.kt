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
package com.wire.android.di.accountScoped

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.CellAssetRefreshHelper
import com.wire.android.pdfviewer.PdfPreSignedLoader
import com.wire.android.pdfviewer.PdfRemoteLoader
import com.wire.kalium.cells.KaliumCellsApi
import com.wire.kalium.cells.domain.CellUploadCoordinator
import com.wire.kalium.cells.domain.CellUploadManager
import com.wire.kalium.cells.domain.usecase.AddAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.DeleteCellAssetUseCase
import com.wire.kalium.cells.domain.usecase.GetAllTagsUseCase
import com.wire.kalium.cells.domain.usecase.GetCellFileUseCase
import com.wire.kalium.cells.domain.usecase.GetConversationNameUseCase
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.GetPdfPreviewUrlUseCase
import com.wire.kalium.cells.domain.usecase.GetFoldersUseCase
import com.wire.kalium.cells.domain.usecase.GetMessageAttachmentUseCase
import com.wire.kalium.cells.domain.usecase.GetOwnersUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedCellConversationsFlowUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedNodesUseCase
import com.wire.kalium.cells.domain.usecase.GetUserNameUseCase
import com.wire.kalium.cells.domain.usecase.GetWireCellConfigurationUseCase
import com.wire.kalium.cells.domain.usecase.IsAtLeastOneCellAvailableUseCase
import com.wire.kalium.cells.domain.usecase.MoveNodeUseCase
import com.wire.kalium.cells.domain.usecase.ObserveAttachmentDraftsUseCase
import com.wire.kalium.cells.domain.usecase.ObserveIsAtLeastOneCellAvailableUseCase
import com.wire.kalium.cells.domain.usecase.PublishAttachmentsUseCase
import com.wire.kalium.cells.domain.usecase.RefreshCellAssetStateUseCase
import com.wire.kalium.cells.domain.usecase.RemoveAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.RemoveAttachmentDraftsUseCase
import com.wire.kalium.cells.domain.usecase.RemoveNodeTagsUseCase
import com.wire.kalium.cells.domain.usecase.RenameNodeUseCase
import com.wire.kalium.cells.domain.usecase.RestoreNodeFromRecycleBinUseCase
import com.wire.kalium.cells.domain.usecase.RetryAttachmentUploadUseCase
import com.wire.kalium.cells.domain.usecase.UpdateNodeTagsUseCase
import com.wire.kalium.cells.domain.usecase.create.CreateDocumentFileUseCase
import com.wire.kalium.cells.domain.usecase.create.CreateFolderUseCase
import com.wire.kalium.cells.domain.usecase.create.CreatePresentationFileUseCase
import com.wire.kalium.cells.domain.usecase.create.CreateSpreadsheetFileUseCase
import com.wire.kalium.cells.domain.usecase.download.DownloadCellFileUseCase
import com.wire.kalium.cells.domain.usecase.download.DownloadCellVersionUseCase
import com.wire.kalium.cells.domain.usecase.offline.DeleteOfflineFileUseCase
import com.wire.kalium.cells.domain.usecase.offline.GetOfflineFileUseCase
import com.wire.kalium.cells.domain.usecase.offline.ObserveOfflineFilesByConversationUseCase
import com.wire.kalium.cells.domain.usecase.offline.ObserveOfflineFilesUseCase
import com.wire.kalium.cells.domain.usecase.offline.SaveOfflineFileUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.CreatePublicLinkPasswordUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.CreatePublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.DeletePublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.GetPublicLinkPasswordUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.GetPublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.SetPublicLinkExpirationUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.UpdatePublicLinkPasswordUseCase
import com.wire.kalium.cells.domain.usecase.versioning.GetNodeVersionsUseCase
import com.wire.kalium.cells.domain.usecase.versioning.RestoreNodeVersionUseCase
import com.wire.kalium.cells.paginatedConversationsFlowUseCase
import com.wire.kalium.cells.paginatedFilesFlowUseCase
import com.wire.kalium.common.functional.fold
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import java.io.IOException
import okio.Path.Companion.toOkioPath
import okio.sink

@Suppress("TooManyFunctions")
@BindingContainer
class CellsModule {

    @Provides
    fun provideCellsApi(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount accountId: UserId,
    ): KaliumCellsApi = coreLogic.getSessionScope(accountId).cells.individualApi

    @Provides
    fun provideAddAttachmentUseCase(api: KaliumCellsApi): AddAttachmentDraftUseCase = api.cellsAddAttachment

    @Provides
    fun provideRemoveAttachmentUseCase(api: KaliumCellsApi): RemoveAttachmentDraftUseCase = api.cellsRemoveAttachment

    @Provides
    fun provideRemoveAttachmentsUseCase(api: KaliumCellsApi): RemoveAttachmentDraftsUseCase = api.cellsRemoveAttachments

    @Provides
    fun provideObserveAttachmentsUseCase(api: KaliumCellsApi): ObserveAttachmentDraftsUseCase = api.cellsObserveAttachments

    @Provides
    fun providePublishAttachmentsUseCase(api: KaliumCellsApi): PublishAttachmentsUseCase = api.cellsPublishAttachments

    @Provides
    fun provideCellUploadManager(api: KaliumCellsApi): CellUploadManager = api.cellsUploadManager

    @Provides
    fun provideCellUploadCoordinator(api: KaliumCellsApi): CellUploadCoordinator = api.cellsUploadCoordinator

    @Provides
    fun provideObserveFilesUseCase(api: KaliumCellsApi): GetPaginatedNodesUseCase = api.cellsObserveFiles

    @Provides
    fun provideObservePagedFilesUseCase(api: KaliumCellsApi): GetPaginatedFilesFlowUseCase = api.paginatedFilesFlowUseCase

    @Provides
    fun provideDownloadUseCase(api: KaliumCellsApi): DownloadCellFileUseCase = api.cellsDownloadCellFile

    @Provides
    fun provideRefreshAssetUseCase(api: KaliumCellsApi): RefreshCellAssetStateUseCase = api.cellsRefreshAsset

    @Provides
    fun provideDeleteCellAssetUseCase(api: KaliumCellsApi): DeleteCellAssetUseCase = api.cellsDeleteCellAssetUseCase

    @Provides
    fun provideCreatePublicUrlUseCase(api: KaliumCellsApi): CreatePublicLinkUseCase = api.cellsCreatePublicLinkUseCase

    @Provides
    fun provideGetPublicUrlUseCase(api: KaliumCellsApi): GetPublicLinkUseCase = api.cellsGetPublicLinkUseCase

    @Provides
    fun provideDeletePublicUrlUseCase(api: KaliumCellsApi): DeletePublicLinkUseCase = api.cellsDeletePublicLinkUseCase

    @Provides
    fun provideRetryAttachmentUploadUseCase(api: KaliumCellsApi): RetryAttachmentUploadUseCase = api.cellsRetryAttachmentUpload

    @Provides
    fun provideCreateFolderUseCase(api: KaliumCellsApi): CreateFolderUseCase = api.cellsCreateFolderUseCase

    @Provides
    fun provideCreateSpreadsheetFileUseCase(api: KaliumCellsApi): CreateSpreadsheetFileUseCase = api.cellsCreateSpreadsheetFileUseCase

    @Provides
    fun provideCreateDocumentFileUseCase(api: KaliumCellsApi): CreateDocumentFileUseCase = api.cellsCreateDocumentFileUseCase

    @Provides
    fun provideCreatePresentationFileUseCase(api: KaliumCellsApi): CreatePresentationFileUseCase =
        api.cellsCreatePresentationFileUseCase

    @Provides
    fun provideMoveNodeUseCase(api: KaliumCellsApi): MoveNodeUseCase = api.cellsMoveNodeUseCase

    @Provides
    fun provideGetFoldersUseCase(api: KaliumCellsApi): GetFoldersUseCase = api.cellsGetFoldersUseCase

    @Provides
    fun provideRestoreNodeFromRecycleBinUseCase(api: KaliumCellsApi): RestoreNodeFromRecycleBinUseCase =
        api.cellsRestoreNodeFromRecycleBin

    @Provides
    fun provideRenameNodeUseCase(api: KaliumCellsApi): RenameNodeUseCase =
        api.cellsRenameNodeUseCase

    @Provides
    fun provideGetAllTagsUseCase(api: KaliumCellsApi): GetAllTagsUseCase = api.cellsGetAllTags

    @Provides
    fun provideUpdateNodeTagsUseCase(api: KaliumCellsApi): UpdateNodeTagsUseCase = api.cellsUpdateNodeTagsUseCase

    @Provides
    fun provideRemoveNodeTagsUseCase(api: KaliumCellsApi): RemoveNodeTagsUseCase = api.cellsRemoveNodeTagsUseCase

    @Provides
    fun provideCellAvailableUseCase(api: KaliumCellsApi): IsAtLeastOneCellAvailableUseCase = api.cellsIsCellAvailable

    @Provides
    fun provideObserveCellAvailableUseCase(api: KaliumCellsApi): ObserveIsAtLeastOneCellAvailableUseCase =
        api.cellsObserveIsCellAvailable

    @Provides
    fun provideGetAttachmentUseCase(api: KaliumCellsApi): GetMessageAttachmentUseCase = api.cellsGetMessageAttachmentUseCase

    @Provides
    fun provideGetOwnersUseCase(api: KaliumCellsApi): GetOwnersUseCase = api.cellsGetOwnersUseCase

    @Provides
    fun provideGetCellNodeUseCase(api: KaliumCellsApi): GetCellFileUseCase = api.cellsGetCellFileUseCase

    @Provides
    fun provideCreatePublicLinkPasswordUseCase(api: KaliumCellsApi): CreatePublicLinkPasswordUseCase =
        api.cellsCreatePublicLinkPasswordUseCase

    @Provides
    fun provideUpdatePublicLinkPasswordUseCase(api: KaliumCellsApi): UpdatePublicLinkPasswordUseCase =
        api.cellsUpdatePublicLinkPasswordUseCase

    @Provides
    fun provideGetPublicLinkPasswordUseCase(api: KaliumCellsApi): GetPublicLinkPasswordUseCase =
        api.cellsGetPublicLinkPassword

    @Provides
    fun provideSetPublicLinkExpirationUseCase(api: KaliumCellsApi): SetPublicLinkExpirationUseCase =
        api.cellsSetPublicLinkExpiration

    @Provides
    fun provideEditorUrlUseCase(api: KaliumCellsApi): GetEditorUrlUseCase = api.cellsGetEditorUrl

    @Provides
    fun provideGetPdfPreviewUrlUseCase(api: KaliumCellsApi): GetPdfPreviewUrlUseCase = api.cellsGetPdfPreviewUrl

    @Provides
    fun provideGetNodeVersionsUseCase(api: KaliumCellsApi): GetNodeVersionsUseCase =
        api.cellsGetNodeVersions

    @Provides
    fun provideRestoreNodeVersionUseCase(api: KaliumCellsApi): RestoreNodeVersionUseCase =
        api.cellsRestoreNodeVersion

    @Provides
    fun provideDownloadCellVersionUseCase(api: KaliumCellsApi): DownloadCellVersionUseCase =
        api.cellsDownloadCellVersion

    @Provides
    fun provideRefreshHelper(api: KaliumCellsApi, kaliumConfigs: KaliumConfigs): CellAssetRefreshHelper = CellAssetRefreshHelper(
        refreshAsset = api.cellsRefreshAsset,
        featureFlags = kaliumConfigs
    )

    @Provides
    fun provideGetCellsConfigUseCase(api: KaliumCellsApi): GetWireCellConfigurationUseCase = api.cellsGetCellConfig

    @Provides
    fun provideGetPaginatedConversationsFlowUseCase(api: KaliumCellsApi): GetPaginatedCellConversationsFlowUseCase =
        api.paginatedConversationsFlowUseCase

    @Provides
    fun provideSaveOfflineFileUseCase(api: KaliumCellsApi): SaveOfflineFileUseCase = api.cellsSaveOfflineFile

    @Provides
    fun provideDeleteOfflineFileUseCase(api: KaliumCellsApi): DeleteOfflineFileUseCase = api.cellsDeleteOfflineFile

    @Provides
    fun provideObserveOfflineFilesUseCase(api: KaliumCellsApi): ObserveOfflineFilesUseCase = api.cellsObserveOfflineFiles

    @Provides
    fun provideObserveOfflineFilesByConversationUseCase(api: KaliumCellsApi): ObserveOfflineFilesByConversationUseCase =
        api.cellsObserveOfflineFilesByConversation

    @Provides
    fun provideGetOfflineFileUseCase(api: KaliumCellsApi): GetOfflineFileUseCase = api.cellsGetOfflineFile

    @Provides
    fun provideGetConversationNamesUseCase(api: KaliumCellsApi): GetConversationNameUseCase = api.cellsGetConversationName

    @Provides
    fun provideGetUserNamesUseCase(api: KaliumCellsApi): GetUserNameUseCase = api.cellsGetUserName

    @Provides
    fun providePdfPreSignedLoader(download: DownloadCellVersionUseCase): PdfPreSignedLoader =
        PdfPreSignedLoader { url, outFile ->
            outFile.sink().use { sink ->
                download(
                    bufferedSink = sink,
                    preSignedUrl = url,
                    onProgressUpdate = { _, _ -> },
                ).fold(
                    { failure -> Result.failure(IOException("PDF rendition download failed: $failure")) },
                    { Result.success(Unit) },
                )
            }
        }

    @Provides
    fun providePdfRemoteLoader(download: DownloadCellFileUseCase): PdfRemoteLoader =
        PdfRemoteLoader { assetId, remotePath, conversationId, assetSize, outFile ->
            download(
                assetId = assetId,
                conversationId = conversationId,
                outFilePath = outFile.toPath().toOkioPath(),
                assetSize = assetSize,
                remoteFilePath = remotePath,
                onProgressUpdate = {},
            ).fold(
                { failure -> Result.failure(IOException("PDF download failed: $failure")) },
                { Result.success(Unit) },
            )
        }
}
