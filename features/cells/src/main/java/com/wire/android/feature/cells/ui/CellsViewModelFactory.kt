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
package com.wire.android.feature.cells.ui

import androidx.lifecycle.SavedStateHandle
import com.wire.android.feature.cells.ui.create.file.CreateFileViewModel
import com.wire.android.feature.cells.ui.create.folder.CreateFolderViewModel
import com.wire.android.feature.cells.ui.edit.OnlineEditor
import com.wire.android.feature.cells.ui.movetofolder.MoveToFolderViewModel
import com.wire.android.feature.cells.ui.publiclink.PublicLinkViewModel
import com.wire.android.feature.cells.ui.publiclink.settings.expiration.PublicLinkExpirationScreenViewModel
import com.wire.android.feature.cells.ui.publiclink.settings.password.PublicLinkPasswordScreenViewModel
import com.wire.android.feature.cells.ui.rename.RenameNodeViewModel
import com.wire.android.feature.cells.ui.search.SearchScreenViewModel
import com.wire.android.feature.cells.ui.tags.AddRemoveTagsViewModel
import com.wire.android.feature.cells.ui.versioning.VersionHistoryViewModel
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.util.FileSizeFormatter
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.cells.domain.usecase.DeleteCellAssetUseCase
import com.wire.kalium.cells.domain.usecase.GetAllTagsUseCase
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.GetFoldersUseCase
import com.wire.kalium.cells.domain.usecase.GetOwnersUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedCellConversationsFlowUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.cells.domain.usecase.GetWireCellConfigurationUseCase
import com.wire.kalium.cells.domain.usecase.IsAtLeastOneCellAvailableUseCase
import com.wire.kalium.cells.domain.usecase.MoveNodeUseCase
import com.wire.kalium.cells.domain.usecase.RemoveNodeTagsUseCase
import com.wire.kalium.cells.domain.usecase.RenameNodeUseCase
import com.wire.kalium.cells.domain.usecase.RestoreNodeFromRecycleBinUseCase
import com.wire.kalium.cells.domain.usecase.UpdateNodeTagsUseCase
import com.wire.kalium.cells.domain.usecase.create.CreateDocumentFileUseCase
import com.wire.kalium.cells.domain.usecase.create.CreateFolderUseCase
import com.wire.kalium.cells.domain.usecase.create.CreatePresentationFileUseCase
import com.wire.kalium.cells.domain.usecase.create.CreateSpreadsheetFileUseCase
import com.wire.kalium.cells.domain.usecase.download.DownloadCellVersionUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.CreatePublicLinkPasswordUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.CreatePublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.DeletePublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.GetPublicLinkPasswordUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.GetPublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.SetPublicLinkExpirationUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.UpdatePublicLinkPasswordUseCase
import com.wire.kalium.cells.domain.usecase.versioning.GetNodeVersionsUseCase
import com.wire.kalium.cells.domain.usecase.versioning.RestoreNodeVersionUseCase
import com.wire.kalium.logic.util.RandomPassword
import javax.inject.Inject

@Suppress("LongParameterList")
class CellsViewModelFactory @Inject constructor(
    private val getCellFilesPaged: GetPaginatedFilesFlowUseCase,
    private val deleteCellAsset: DeleteCellAssetUseCase,
    private val restoreNodeFromRecycleBinUseCase: RestoreNodeFromRecycleBinUseCase,
    private val isCellAvailable: IsAtLeastOneCellAvailableUseCase,
    private val fileHelper: FileHelper,
    private val getEditorUrl: GetEditorUrlUseCase,
    private val onlineEditor: OnlineEditor,
    private val cellFileActionsMenu: CellFileActionsMenu,
    private val getWireCellsConfig: GetWireCellConfigurationUseCase,
    private val sharedPathCache: CellFileLocalPathCache,
    private val openFileDownloadController: OpenFileDownloadController,
    private val createPresentationFileUseCase: CreatePresentationFileUseCase,
    private val createDocumentFileUseCase: CreateDocumentFileUseCase,
    private val createSpreadsheetFileUseCase: CreateSpreadsheetFileUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val moveNodeUseCase: MoveNodeUseCase,
    private val renameNodeUseCase: RenameNodeUseCase,
    private val createPublicLink: CreatePublicLinkUseCase,
    private val getPublicLinkUseCase: GetPublicLinkUseCase,
    private val deletePublicLinkUseCase: DeletePublicLinkUseCase,
    private val setPublicLinkExpiration: SetPublicLinkExpirationUseCase,
    private val generateRandomPassword: RandomPassword,
    private val createPublicLinkPassword: CreatePublicLinkPasswordUseCase,
    private val updatePublicLinkPassword: UpdatePublicLinkPasswordUseCase,
    private val getPublicLinkPassword: GetPublicLinkPasswordUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val updateNodeTagsUseCase: UpdateNodeTagsUseCase,
    private val removeNodeTagsUseCase: RemoveNodeTagsUseCase,
    private val getOwners: GetOwnersUseCase,
    private val getPaginatedConversations: GetPaginatedCellConversationsFlowUseCase,
    private val getNodeVersionsUseCase: GetNodeVersionsUseCase,
    private val fileSizeFormatter: FileSizeFormatter,
    private val restoreNodeVersionUseCase: RestoreNodeVersionUseCase,
    private val downloadCellVersionUseCase: DownloadCellVersionUseCase,
    private val dispatchers: DispatcherProvider,
) {
    internal fun cellViewModel(savedStateHandle: SavedStateHandle) = CellViewModel(
        savedStateHandle = savedStateHandle,
        getCellFilesPaged = getCellFilesPaged,
        deleteCellAsset = deleteCellAsset,
        restoreNodeFromRecycleBinUseCase = restoreNodeFromRecycleBinUseCase,
        isCellAvailable = isCellAvailable,
        fileHelper = fileHelper,
        getEditorUrl = getEditorUrl,
        onlineEditor = onlineEditor,
        cellFileActionsMenu = cellFileActionsMenu,
        getWireCellsConfig = getWireCellsConfig,
        sharedPathCache = sharedPathCache,
        openFileDownloadController = openFileDownloadController,
    )

    internal fun createFileViewModel(savedStateHandle: SavedStateHandle) = CreateFileViewModel(
        savedStateHandle = savedStateHandle,
        createPresentationFileUseCase = createPresentationFileUseCase,
        createDocumentFileUseCase = createDocumentFileUseCase,
        createSpreadsheetFileUseCase = createSpreadsheetFileUseCase,
    )

    internal fun createFolderViewModel(savedStateHandle: SavedStateHandle) = CreateFolderViewModel(
        savedStateHandle = savedStateHandle,
        createFolderUseCase = createFolderUseCase,
    )

    internal fun moveToFolderViewModel(savedStateHandle: SavedStateHandle) = MoveToFolderViewModel(
        savedStateHandle = savedStateHandle,
        getFoldersUseCase = getFoldersUseCase,
        moveNodeUseCase = moveNodeUseCase,
    )

    internal fun renameNodeViewModel(savedStateHandle: SavedStateHandle) = RenameNodeViewModel(
        savedStateHandle = savedStateHandle,
        renameNodeUseCase = renameNodeUseCase,
    )

    internal fun publicLinkViewModel(savedStateHandle: SavedStateHandle) = PublicLinkViewModel(
        savedStateHandle = savedStateHandle,
        createPublicLink = createPublicLink,
        getPublicLinkUseCase = getPublicLinkUseCase,
        deletePublicLinkUseCase = deletePublicLinkUseCase,
        fileHelper = fileHelper,
    )

    internal fun publicLinkExpirationScreenViewModel(savedStateHandle: SavedStateHandle) = PublicLinkExpirationScreenViewModel(
        setExpiration = setPublicLinkExpiration,
        savedStateHandle = savedStateHandle,
    )

    internal fun publicLinkPasswordScreenViewModel(savedStateHandle: SavedStateHandle) = PublicLinkPasswordScreenViewModel(
        generateRandomPassword = generateRandomPassword,
        createPassword = createPublicLinkPassword,
        updatePassword = updatePublicLinkPassword,
        getPublicLinkPassword = getPublicLinkPassword,
        savedStateHandle = savedStateHandle,
    )

    internal fun searchScreenViewModel(savedStateHandle: SavedStateHandle) = SearchScreenViewModel(
        savedStateHandle = savedStateHandle,
        getAllTagsUseCase = getAllTagsUseCase,
        getCellFilesPaged = getCellFilesPaged,
        getOwners = getOwners,
        getPaginatedConversations = getPaginatedConversations,
        sharedPathCache = sharedPathCache,
    )

    internal fun addRemoveTagsViewModel(savedStateHandle: SavedStateHandle) = AddRemoveTagsViewModel(
        savedStateHandle = savedStateHandle,
        getAllTagsUseCase = getAllTagsUseCase,
        updateNodeTagsUseCase = updateNodeTagsUseCase,
        removeNodeTagsUseCase = removeNodeTagsUseCase,
    )

    internal fun versionHistoryViewModel(savedStateHandle: SavedStateHandle) = VersionHistoryViewModel(
        savedStateHandle = savedStateHandle,
        getNodeVersionsUseCase = getNodeVersionsUseCase,
        fileSizeFormatter = fileSizeFormatter,
        restoreNodeVersionUseCase = restoreNodeVersionUseCase,
        downloadCellVersionUseCase = downloadCellVersionUseCase,
        fileHelper = fileHelper,
        onlineEditor = onlineEditor,
        getEditorUrl = getEditorUrl,
        dispatchers = dispatchers,
    )
}
