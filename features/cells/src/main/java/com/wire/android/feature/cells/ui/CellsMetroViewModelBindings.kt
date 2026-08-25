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

import com.wire.android.feature.cells.ui.audioplayer.AudioPlayerViewModel
import com.wire.android.feature.cells.ui.audioplayer.AudioPlayerNavArgs
import com.wire.android.feature.cells.ui.create.file.CreateFileViewModel
import com.wire.android.feature.cells.ui.create.file.CreateFileScreenNavArgs
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
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey

internal interface CellsManualViewModelFactory : ManualViewModelAssistedFactory {
    fun cell(navArgs: CellFilesNavArgs): CellViewModel
    fun searchCell(navArgs: SearchNavArgs): CellViewModel
    fun createFile(navArgs: CreateFileScreenNavArgs): CreateFileViewModel
    fun createFolder(navArgs: CreateFolderScreenNavArgs): CreateFolderViewModel
    fun moveToFolder(navArgs: MoveToFolderNavArgs): MoveToFolderViewModel
    fun publicLink(navArgs: PublicLinkNavArgs): PublicLinkViewModel
    fun publicLinkExpiration(navArgs: PublicLinkExpirationScreenNavArgs): PublicLinkExpirationScreenViewModel
    fun publicLinkPassword(navArgs: PublicLinkPasswordNavArgs): PublicLinkPasswordScreenViewModel
    fun renameNode(navArgs: RenameNodeNavArgs): RenameNodeViewModel
    fun search(navArgs: SearchNavArgs): SearchScreenViewModel
    fun addRemoveTags(navArgs: AddRemoveTagsNavArgs): AddRemoveTagsViewModel
    fun versionHistory(navArgs: VersionHistoryNavArgs): VersionHistoryViewModel
    fun imageViewer(navArgs: CellImageViewerNavArgs): CellImageViewerViewModel
    fun audioPlayer(context: android.content.Context, navArgs: AudioPlayerNavArgs): AudioPlayerViewModel
}

@BindingContainer
object CellsMetroViewModelBindings {

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(CellsManualViewModelFactory::class)
    @Suppress("LongParameterList")
    internal fun manualViewModelFactory(
        cellFactory: CellViewModel.Factory,
        createFileFactory: CreateFileViewModel.Factory,
        createFolderFactory: CreateFolderViewModel.Factory,
        moveToFolderFactory: MoveToFolderViewModel.Factory,
        publicLinkFactory: PublicLinkViewModel.Factory,
        publicLinkExpirationFactory: PublicLinkExpirationScreenViewModel.Factory,
        publicLinkPasswordFactory: PublicLinkPasswordScreenViewModel.Factory,
        renameNodeFactory: RenameNodeViewModel.Factory,
        searchFactory: SearchScreenViewModel.Factory,
        addRemoveTagsFactory: AddRemoveTagsViewModel.Factory,
        versionHistoryFactory: VersionHistoryViewModel.Factory,
        imageViewerFactory: CellImageViewerViewModel.Factory,
        audioPlayerFactory: AudioPlayerViewModel.Factory,
    ): ManualViewModelAssistedFactory =
        object : CellsManualViewModelFactory {
            override fun cell(navArgs: CellFilesNavArgs) = cellFactory.create(navArgs, null)
            override fun searchCell(navArgs: SearchNavArgs) = cellFactory.create(navArgs.toCellFilesNavArgs(), navArgs)
            override fun createFile(navArgs: CreateFileScreenNavArgs) = createFileFactory.create(navArgs)
            override fun createFolder(navArgs: CreateFolderScreenNavArgs) = createFolderFactory.create(navArgs)
            override fun moveToFolder(navArgs: MoveToFolderNavArgs) = moveToFolderFactory.create(navArgs)
            override fun publicLink(navArgs: PublicLinkNavArgs) = publicLinkFactory.create(navArgs)
            override fun publicLinkExpiration(navArgs: PublicLinkExpirationScreenNavArgs) =
                publicLinkExpirationFactory.create(navArgs)
            override fun publicLinkPassword(navArgs: PublicLinkPasswordNavArgs) =
                publicLinkPasswordFactory.create(navArgs)
            override fun renameNode(navArgs: RenameNodeNavArgs) = renameNodeFactory.create(navArgs)
            override fun search(navArgs: SearchNavArgs) = searchFactory.create(navArgs)
            override fun addRemoveTags(navArgs: AddRemoveTagsNavArgs) = addRemoveTagsFactory.create(navArgs)
            override fun versionHistory(navArgs: VersionHistoryNavArgs) = versionHistoryFactory.create(navArgs)
            override fun imageViewer(navArgs: CellImageViewerNavArgs) = imageViewerFactory.create(navArgs)
            override fun audioPlayer(context: android.content.Context, navArgs: AudioPlayerNavArgs) =
                audioPlayerFactory.create(context, navArgs.localPath, navArgs.contentUrl, navArgs.fileName)
        }
}
