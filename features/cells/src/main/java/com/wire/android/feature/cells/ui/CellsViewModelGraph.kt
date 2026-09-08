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

@file:Suppress("TooManyFunctions")

package com.wire.android.feature.cells.ui

import androidx.compose.runtime.Composable
import com.wire.android.di.metro.wireAssistedMetroViewModel
import com.wire.android.feature.cells.ui.create.file.CreateFileScreenNavArgs
import com.wire.android.feature.cells.ui.create.file.CreateFileViewModel
import com.wire.android.feature.cells.ui.create.folder.CreateFolderViewModel
import com.wire.android.feature.cells.ui.create.folder.CreateFolderScreenNavArgs
import com.wire.android.feature.cells.ui.imageviewer.CellImageViewerNavArgs
import com.wire.android.feature.cells.ui.imageviewer.CellImageViewerViewModel
import com.wire.android.feature.cells.ui.movetofolder.MoveToFolderViewModel
import com.wire.android.feature.cells.ui.movetofolder.MoveToFolderNavArgs
import com.wire.android.feature.cells.ui.publiclink.PublicLinkViewModel
import com.wire.android.feature.cells.ui.publiclink.PublicLinkNavArgs
import com.wire.android.feature.cells.ui.publiclink.settings.expiration.PublicLinkExpirationScreenNavArgs
import com.wire.android.feature.cells.ui.publiclink.settings.expiration.PublicLinkExpirationScreenViewModel
import com.wire.android.feature.cells.ui.publiclink.settings.password.PublicLinkPasswordScreenViewModel
import com.wire.android.feature.cells.ui.publiclink.settings.password.PublicLinkPasswordNavArgs
import com.wire.android.feature.cells.ui.rename.RenameNodeNavArgs
import com.wire.android.feature.cells.ui.rename.RenameNodeViewModel
import com.wire.android.feature.cells.ui.search.SearchScreenViewModel
import com.wire.android.feature.cells.ui.search.SearchNavArgs
import com.wire.android.feature.cells.ui.tags.AddRemoveTagsNavArgs
import com.wire.android.feature.cells.ui.tags.AddRemoveTagsViewModel
import com.wire.android.feature.cells.ui.versioning.VersionHistoryViewModel
import com.wire.android.feature.cells.ui.versioning.VersionHistoryNavArgs

@Composable
fun cellViewModel(navArgs: CellFilesNavArgs): CellViewModel =
    wireAssistedMetroViewModel<CellViewModel, CellsManualViewModelFactory> {
        cell(navArgs)
    }

@Composable
internal fun searchCellViewModel(navArgs: SearchNavArgs): CellViewModel =
    wireAssistedMetroViewModel<CellViewModel, CellsManualViewModelFactory>(
        instanceKey = "search-cell",
    ) {
        searchCell(navArgs)
    }

@Composable
internal fun createFileViewModel(navArgs: CreateFileScreenNavArgs): CreateFileViewModel =
    wireAssistedMetroViewModel<CreateFileViewModel, CellsManualViewModelFactory> { createFile(navArgs) }

@Composable
internal fun createFolderViewModel(navArgs: CreateFolderScreenNavArgs): CreateFolderViewModel =
    wireAssistedMetroViewModel<CreateFolderViewModel, CellsManualViewModelFactory> { createFolder(navArgs) }

@Composable
internal fun moveToFolderViewModel(navArgs: MoveToFolderNavArgs): MoveToFolderViewModel =
    wireAssistedMetroViewModel<MoveToFolderViewModel, CellsManualViewModelFactory> { moveToFolder(navArgs) }

@Composable
internal fun publicLinkViewModel(navArgs: PublicLinkNavArgs): PublicLinkViewModel =
    wireAssistedMetroViewModel<PublicLinkViewModel, CellsManualViewModelFactory> { publicLink(navArgs) }

@Composable
internal fun publicLinkExpirationViewModel(
    navArgs: PublicLinkExpirationScreenNavArgs,
): PublicLinkExpirationScreenViewModel =
    wireAssistedMetroViewModel<PublicLinkExpirationScreenViewModel, CellsManualViewModelFactory> {
        publicLinkExpiration(navArgs)
    }

@Composable
internal fun publicLinkPasswordViewModel(navArgs: PublicLinkPasswordNavArgs): PublicLinkPasswordScreenViewModel =
    wireAssistedMetroViewModel<PublicLinkPasswordScreenViewModel, CellsManualViewModelFactory> {
        publicLinkPassword(navArgs)
    }

@Composable
internal fun renameNodeViewModel(navArgs: RenameNodeNavArgs): RenameNodeViewModel =
    wireAssistedMetroViewModel<RenameNodeViewModel, CellsManualViewModelFactory> { renameNode(navArgs) }

@Composable
internal fun searchScreenViewModel(navArgs: SearchNavArgs): SearchScreenViewModel =
    wireAssistedMetroViewModel<SearchScreenViewModel, CellsManualViewModelFactory> { search(navArgs) }

@Composable
internal fun addRemoveTagsViewModel(navArgs: AddRemoveTagsNavArgs): AddRemoveTagsViewModel =
    wireAssistedMetroViewModel<AddRemoveTagsViewModel, CellsManualViewModelFactory> { addRemoveTags(navArgs) }

@Composable
internal fun versionHistoryViewModel(navArgs: VersionHistoryNavArgs): VersionHistoryViewModel =
    wireAssistedMetroViewModel<VersionHistoryViewModel, CellsManualViewModelFactory> { versionHistory(navArgs) }

@Composable
internal fun cellImageViewerViewModel(navArgs: CellImageViewerNavArgs): CellImageViewerViewModel =
    wireAssistedMetroViewModel<CellImageViewerViewModel, CellsManualViewModelFactory> { imageViewer(navArgs) }
