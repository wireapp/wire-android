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

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.wire.android.di.metro.sessionKeyedMetroViewModel
import com.wire.android.feature.cells.ui.audioplayer.CellAudioPlayerViewModel
import com.wire.android.feature.cells.ui.create.file.CreateFileViewModel
import com.wire.android.feature.cells.ui.create.folder.CreateFolderViewModel
import com.wire.android.feature.cells.ui.imageviewer.CellImageViewerViewModel
import com.wire.android.feature.cells.ui.movetofolder.MoveToFolderViewModel
import com.wire.android.feature.cells.ui.publiclink.PublicLinkViewModel
import com.wire.android.feature.cells.ui.publiclink.settings.expiration.PublicLinkExpirationScreenViewModel
import com.wire.android.feature.cells.ui.publiclink.settings.password.PublicLinkPasswordScreenViewModel
import com.wire.android.feature.cells.ui.rename.RenameNodeViewModel
import com.wire.android.feature.cells.ui.search.SearchScreenViewModel
import com.wire.android.feature.cells.ui.tags.AddRemoveTagsViewModel
import com.wire.android.feature.cells.ui.versioning.VersionHistoryViewModel
import com.wire.android.feature.cells.ui.videoviewer.CellVideoViewerViewModel

@Composable
inline fun <reified VM> cellsViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
): VM where VM : ViewModel =
    sessionKeyedMetroViewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        key = key,
    )

@Composable
fun cellViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
): CellViewModel = cellsViewModel(viewModelStoreOwner = viewModelStoreOwner)

@Composable
fun createFileViewModel(): CreateFileViewModel = cellsViewModel()

@Composable
fun createFolderViewModel(): CreateFolderViewModel = cellsViewModel()

@Composable
fun moveToFolderViewModel(): MoveToFolderViewModel = cellsViewModel()

@Composable
fun publicLinkViewModel(): PublicLinkViewModel = cellsViewModel()

@Composable
internal fun publicLinkExpirationScreenViewModel(): PublicLinkExpirationScreenViewModel =
    cellsViewModel()

@Composable
internal fun publicLinkPasswordScreenViewModel(): PublicLinkPasswordScreenViewModel =
    cellsViewModel()

@Composable
fun renameNodeViewModel(): RenameNodeViewModel = cellsViewModel()

@Composable
fun searchScreenViewModel(): SearchScreenViewModel = cellsViewModel()

@Composable
fun addRemoveTagsViewModel(): AddRemoveTagsViewModel = cellsViewModel()

@Composable
fun versionHistoryViewModel(): VersionHistoryViewModel = cellsViewModel()

@Composable
fun cellImageViewerViewModel(): CellImageViewerViewModel = cellsViewModel()

@Composable
fun cellVideoViewerViewModel(): CellVideoViewerViewModel = cellsViewModel()

@Composable
fun cellAudioPlayerViewModel(): CellAudioPlayerViewModel = cellsViewModel()
