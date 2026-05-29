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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.metro.metroSavedStateViewModel
import com.wire.android.feature.cells.ui.create.file.CreateFileViewModel
import com.wire.android.feature.cells.ui.create.folder.CreateFolderViewModel
import com.wire.android.feature.cells.ui.movetofolder.MoveToFolderViewModel
import com.wire.android.feature.cells.ui.publiclink.PublicLinkViewModel
import com.wire.android.feature.cells.ui.publiclink.settings.expiration.PublicLinkExpirationScreenViewModel
import com.wire.android.feature.cells.ui.publiclink.settings.password.PublicLinkPasswordScreenViewModel
import com.wire.android.feature.cells.ui.rename.RenameNodeViewModel
import com.wire.android.feature.cells.ui.search.SearchScreenViewModel
import com.wire.android.feature.cells.ui.tags.AddRemoveTagsViewModel
import com.wire.android.feature.cells.ui.versioning.VersionHistoryViewModel

interface CellsViewModelGraph : MetroViewModelGraph {
    val cellsViewModelFactory: CellsViewModelFactory
}

@Composable
inline fun <reified VM> cellsViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
    crossinline create: CellsViewModelFactory.(SavedStateHandle) -> VM,
): VM where VM : ViewModel =
    metroSavedStateViewModel<CellsViewModelGraph, VM>(
        viewModelStoreOwner = viewModelStoreOwner,
        key = key,
    ) { savedStateHandle ->
        cellsViewModelFactory.create(savedStateHandle)
    }

@Composable
fun cellViewModel(): CellViewModel = cellsViewModel { cellViewModel(it) }

@Composable
fun createFileViewModel(): CreateFileViewModel = cellsViewModel { createFileViewModel(it) }

@Composable
fun createFolderViewModel(): CreateFolderViewModel = cellsViewModel { createFolderViewModel(it) }

@Composable
fun moveToFolderViewModel(): MoveToFolderViewModel = cellsViewModel { moveToFolderViewModel(it) }

@Composable
fun publicLinkViewModel(): PublicLinkViewModel = cellsViewModel { publicLinkViewModel(it) }

@Composable
internal fun publicLinkExpirationScreenViewModel(): PublicLinkExpirationScreenViewModel =
    cellsViewModel { publicLinkExpirationScreenViewModel(it) }

@Composable
internal fun publicLinkPasswordScreenViewModel(): PublicLinkPasswordScreenViewModel =
    cellsViewModel { publicLinkPasswordScreenViewModel(it) }

@Composable
fun renameNodeViewModel(): RenameNodeViewModel = cellsViewModel { renameNodeViewModel(it) }

@Composable
fun searchScreenViewModel(): SearchScreenViewModel = cellsViewModel { searchScreenViewModel(it) }

@Composable
fun addRemoveTagsViewModel(): AddRemoveTagsViewModel = cellsViewModel { addRemoveTagsViewModel(it) }

@Composable
fun versionHistoryViewModel(): VersionHistoryViewModel = cellsViewModel { versionHistoryViewModel(it) }
