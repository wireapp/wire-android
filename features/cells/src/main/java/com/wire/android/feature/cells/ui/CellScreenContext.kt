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

import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.search.DriveSearchScreenType
import com.wire.android.feature.cells.ui.search.SearchNavArgs
import com.wire.android.feature.cells.ui.search.sort.SortingCriteria
import com.wire.android.feature.cells.ui.search.sort.toSortingCriteria

/**
 * What a files screen shows, derived from its navigation arguments: All Files, the files of a conversation (shared
 * drive), or its recycle bin, possibly backing the search screen.
 */
class CellScreenContext(
    private val navArgs: CellFilesNavArgs,
    private val searchNavArgs: SearchNavArgs?,
) {
    /** Path of the folder being browsed: the conversation id, followed by the sub folders if any. */
    val currentPath: String? get() = navArgs.conversationId

    /** The conversation the browsed folder belongs to: the root segment of [currentPath]. */
    val rootConversationId: String? = navArgs.conversationId?.substringBefore("/")

    /** UUID of the top deleted folder, while browsing inside a deleted folder of the recycle bin. */
    val parentFolderUuid: String? get() = navArgs.parentFolderUuid

    val screenTitle: String? get() = navArgs.screenTitle
    val breadcrumbs: Array<String>? get() = navArgs.breadcrumbs
    val isOfflineOnlyRequested: Boolean get() = navArgs.isOfflineOnly

    val isRecycleBin: Boolean = navArgs.isRecycleBin ?: false

    /** The recycle bin root is opened with the conversation id alone, its sub folders with their full path. */
    val isRecycleBinRoot: Boolean = isRecycleBin && navArgs.conversationId?.contains('/') == false
    val isConversationFiles: Boolean = navArgs.conversationId != null && !isRecycleBin
    val isAllFiles: Boolean = navArgs.conversationId == null && !isRecycleBin

    /** Whether this instance backs the search screen rather than a plain listing. */
    val isSearch: Boolean = searchNavArgs != null

    /** A Drive or Shared Drive listing, which the search screen can be opened from. */
    val isSearchableListing: Boolean = !isSearch && !isRecycleBin

    /**
     * Bulk selection is offered in a shared drive and the recycle bin, and in All Files where only the offline actions
     * apply, so there only when [bulkOfflineActionsEnabled]. Search results keep the single-item flow.
     */
    fun offersBulkSelection(bulkOfflineActionsEnabled: Boolean): Boolean =
        !isSearch && (isConversationFiles || isRecycleBin || (isAllFiles && bulkOfflineActionsEnabled))

    val isSearchingDrive: Boolean = searchNavArgs?.screenType == DriveSearchScreenType.SHARED_DRIVE ||
            searchNavArgs?.screenType == DriveSearchScreenType.DRIVE

    // AllFiles context (no conversationId, not recycle bin) defaults to newest-first;
    // ConversationFiles and RecycleBin default to folders-first.
    val defaultSortingCriteria: SortingCriteria =
        if (isAllFiles) SortingCriteria.ByDate.NewestFirst else SortingCriteria.FoldersFirst

    // When backing the search screen, start from the sort order that was active in the parent listing so the
    // unfiltered list shown on entry matches what the user was looking at.
    val initialSortingCriteria: SortingCriteria =
        searchNavArgs?.initialSortingCriteria?.toSortingCriteria() ?: defaultSortingCriteria

    /** The path to open [folder] at, or null when it can't be resolved. */
    fun pathOf(folder: CellNodeUi.Folder): String? = when {
        isRecycleBin -> if (currentPath?.contains("recycle_bin") == true) {
            "$currentPath/${folder.name}"
        } else {
            "$currentPath/recycle_bin/${folder.name}"
        }

        // Use the folder's full remote path so results opened from a (recursive) search
        // resolve correctly at any depth. Fall back to the current folder + name when it is missing.
        isConversationFiles -> folder.remotePath ?: "$currentPath/${folder.name}"
        else -> folder.remotePath
    }

    /** UUID of the top parent folder for recycle bin items, used when restoring a folder. */
    fun parentFolderUuidOf(folder: CellNodeUi.Folder): String? =
        if (isRecycleBin) navArgs.parentFolderUuid ?: folder.uuid else null

    /**
     * Nodes listed inside a conversation don't carry their conversation id, and [currentPath] is the path of the folder
     * being browsed, so only its root segment is the conversation the file belongs to.
     */
    fun withConversation(file: CellNodeUi.File): CellNodeUi.File =
        file.copy(conversationId = file.conversationId ?: rootConversationId)
}
