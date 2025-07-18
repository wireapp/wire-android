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
import com.wire.kalium.cells.CellsScope
import com.wire.kalium.cells.domain.CellUploadManager
import com.wire.kalium.cells.domain.usecase.AddAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.CreateFolderUseCase
import com.wire.kalium.cells.domain.usecase.DeleteCellAssetUseCase
import com.wire.kalium.cells.domain.usecase.DownloadCellFileUseCase
import com.wire.kalium.cells.domain.usecase.GetAllTagsUseCase
import com.wire.kalium.cells.domain.usecase.GetFoldersUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedNodesUseCase
import com.wire.kalium.cells.domain.usecase.MoveNodeUseCase
import com.wire.kalium.cells.domain.usecase.ObserveAttachmentDraftsUseCase
import com.wire.kalium.cells.domain.usecase.PublishAttachmentsUseCase
import com.wire.kalium.cells.domain.usecase.RefreshCellAssetStateUseCase
import com.wire.kalium.cells.domain.usecase.RemoveAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.RemoveAttachmentDraftsUseCase
import com.wire.kalium.cells.domain.usecase.RemoveNodeTagsUseCase
import com.wire.kalium.cells.domain.usecase.RenameNodeUseCase
import com.wire.kalium.cells.domain.usecase.RestoreNodeFromRecycleBinUseCase
import com.wire.kalium.cells.domain.usecase.RetryAttachmentUploadUseCase
import com.wire.kalium.cells.domain.usecase.SetWireCellForConversationUseCase
import com.wire.kalium.cells.domain.usecase.UpdateNodeTagsUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.CreatePublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.DeletePublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.GetPublicLinkUseCase
import com.wire.kalium.cells.paginatedFilesFlowUseCase
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Suppress("TooManyFunctions")
@Module
@InstallIn(ViewModelComponent::class)
class CellsModule {

    @ViewModelScoped
    @Provides
    fun provideCellsScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount accountId: UserId,
    ): CellsScope = coreLogic.getSessionScope(accountId).cells

    @ViewModelScoped
    @Provides
    fun provideAddAttachmentUseCase(cellsScope: CellsScope): AddAttachmentDraftUseCase = cellsScope.addAttachment

    @ViewModelScoped
    @Provides
    fun provideRemoveAttachmentUseCase(cellsScope: CellsScope): RemoveAttachmentDraftUseCase = cellsScope.removeAttachment

    @ViewModelScoped
    @Provides
    fun provideRemoveAttachmentsUseCase(cellsScope: CellsScope): RemoveAttachmentDraftsUseCase = cellsScope.removeAttachments

    @ViewModelScoped
    @Provides
    fun provideObserveAttachmentsUseCase(cellsScope: CellsScope): ObserveAttachmentDraftsUseCase = cellsScope.observeAttachments

    @ViewModelScoped
    @Provides
    fun providePublishAttachmentsUseCase(cellsScope: CellsScope): PublishAttachmentsUseCase = cellsScope.publishAttachments

    @ViewModelScoped
    @Provides
    fun provideCellUploadManager(cellsScope: CellsScope): CellUploadManager = cellsScope.uploadManager

    @ViewModelScoped
    @Provides
    fun provideObserveFilesUseCase(cellsScope: CellsScope): GetPaginatedNodesUseCase = cellsScope.observeFiles

    @ViewModelScoped
    @Provides
    fun provideObservePagedFilesUseCase(cellsScope: CellsScope): GetPaginatedFilesFlowUseCase = cellsScope.paginatedFilesFlowUseCase

    @ViewModelScoped
    @Provides
    fun provideEnableCellUseCase(cellsScope: CellsScope): SetWireCellForConversationUseCase = cellsScope.enableWireCell

    @ViewModelScoped
    @Provides
    fun provideDownloadUseCase(cellsScope: CellsScope): DownloadCellFileUseCase = cellsScope.downloadFile

    @ViewModelScoped
    @Provides
    fun provideRefreshAssetUseCase(cellsScope: CellsScope): RefreshCellAssetStateUseCase = cellsScope.refreshAsset

    @ViewModelScoped
    @Provides
    fun provideDeleteCellAssetUseCase(cellsScope: CellsScope): DeleteCellAssetUseCase = cellsScope.deleteCellAssetUseCase

    @ViewModelScoped
    @Provides
    fun provideCreatePublicUrlUseCase(cellsScope: CellsScope): CreatePublicLinkUseCase = cellsScope.createPublicLinkUseCase

    @ViewModelScoped
    @Provides
    fun provideGetPublicUrlUseCase(cellsScope: CellsScope): GetPublicLinkUseCase = cellsScope.getPublicLinkUseCase

    @ViewModelScoped
    @Provides
    fun provideDeletePublicUrlUseCase(cellsScope: CellsScope): DeletePublicLinkUseCase = cellsScope.deletePublicLinkUseCase

    @ViewModelScoped
    @Provides
    fun provideRetryAttachmentUploadUseCase(cellsScope: CellsScope): RetryAttachmentUploadUseCase = cellsScope.retryAttachmentUpload

    @ViewModelScoped
    @Provides
    fun provideCreateFolderUseCase(cellsScope: CellsScope): CreateFolderUseCase = cellsScope.createFolderUseCase

    @ViewModelScoped
    @Provides
    fun provideMoveNodeUseCase(cellsScope: CellsScope): MoveNodeUseCase = cellsScope.moveNodeUseCase

    @ViewModelScoped
    @Provides
    fun provideGetFoldersUseCase(cellsScope: CellsScope): GetFoldersUseCase = cellsScope.getFoldersUseCase

    @ViewModelScoped
    @Provides
    fun provideRestoreNodeFromRecycleBinUseCase(cellsScope: CellsScope): RestoreNodeFromRecycleBinUseCase =
        cellsScope.restoreNodeFromRecycleBin

    @ViewModelScoped
    @Provides
    fun provideRenameNodeUseCase(cellsScope: CellsScope): RenameNodeUseCase =
        cellsScope.renameNodeUseCase

    @ViewModelScoped
    @Provides
    fun provideGetAllTagsUseCase(cellsScope: CellsScope): GetAllTagsUseCase = cellsScope.getAllTags

    @ViewModelScoped
    @Provides
    fun provideUpdateNodeTagsUseCase(cellsScope: CellsScope): UpdateNodeTagsUseCase = cellsScope.updateNodeTagsUseCase

    @ViewModelScoped
    @Provides
    fun provideRemoveNodeTagsUseCase(cellsScope: CellsScope): RemoveNodeTagsUseCase = cellsScope.removeNodeTagsUseCase
}
