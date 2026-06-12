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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
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
import com.wire.android.feature.cells.ui.videoplayer.VideoPlayerViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactoryKey

@BindingContainer
object CellsMetroViewModelBindings {

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CellViewModel::class)
    fun cellViewModel(factory: CellsViewModelFactory): ViewModelAssistedFactory =
        savedStateViewModel { factory.cellViewModel(it.createSavedStateHandle()) }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateFileViewModel::class)
    fun createFileViewModel(factory: CellsViewModelFactory): ViewModelAssistedFactory =
        savedStateViewModel { factory.createFileViewModel(it.createSavedStateHandle()) }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateFolderViewModel::class)
    fun createFolderViewModel(factory: CellsViewModelFactory): ViewModelAssistedFactory =
        savedStateViewModel { factory.createFolderViewModel(it.createSavedStateHandle()) }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(MoveToFolderViewModel::class)
    fun moveToFolderViewModel(factory: CellsViewModelFactory): ViewModelAssistedFactory =
        savedStateViewModel { factory.moveToFolderViewModel(it.createSavedStateHandle()) }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(PublicLinkViewModel::class)
    fun publicLinkViewModel(factory: CellsViewModelFactory): ViewModelAssistedFactory =
        savedStateViewModel { factory.publicLinkViewModel(it.createSavedStateHandle()) }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(PublicLinkExpirationScreenViewModel::class)
    fun publicLinkExpirationScreenViewModel(factory: CellsViewModelFactory): ViewModelAssistedFactory =
        savedStateViewModel { factory.publicLinkExpirationScreenViewModel(it.createSavedStateHandle()) }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(PublicLinkPasswordScreenViewModel::class)
    fun publicLinkPasswordScreenViewModel(factory: CellsViewModelFactory): ViewModelAssistedFactory =
        savedStateViewModel { factory.publicLinkPasswordScreenViewModel(it.createSavedStateHandle()) }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(RenameNodeViewModel::class)
    fun renameNodeViewModel(factory: CellsViewModelFactory): ViewModelAssistedFactory =
        savedStateViewModel { factory.renameNodeViewModel(it.createSavedStateHandle()) }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(SearchScreenViewModel::class)
    fun searchScreenViewModel(factory: CellsViewModelFactory): ViewModelAssistedFactory =
        savedStateViewModel { factory.searchScreenViewModel(it.createSavedStateHandle()) }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(AddRemoveTagsViewModel::class)
    fun addRemoveTagsViewModel(factory: CellsViewModelFactory): ViewModelAssistedFactory =
        savedStateViewModel { factory.addRemoveTagsViewModel(it.createSavedStateHandle()) }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(VersionHistoryViewModel::class)
    fun versionHistoryViewModel(factory: CellsViewModelFactory): ViewModelAssistedFactory =
        savedStateViewModel { factory.versionHistoryViewModel(it.createSavedStateHandle()) }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CellImageViewerViewModel::class)
    fun imageViewerViewModel(factory: CellsViewModelFactory): ViewModelAssistedFactory =
        savedStateViewModel { factory.cellImageViewerViewModel(it.createSavedStateHandle()) }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(VideoPlayerViewModel::class)
    fun videoViewerViewModel(factory: CellsViewModelFactory): ViewModelAssistedFactory =
        savedStateViewModel {
            factory.cellVideoViewerViewModel(
                context = checkNotNull(it[APPLICATION_KEY]) {
                    "No Application was provided via CreationExtras"
                },
                savedStateHandle = it.createSavedStateHandle(),
            )
        }

    private fun savedStateViewModel(create: (CreationExtras) -> ViewModel): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel = create(extras)
        }
}
