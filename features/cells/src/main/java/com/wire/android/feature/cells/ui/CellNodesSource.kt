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

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.OpenLoadState
import com.wire.android.feature.cells.ui.model.toUiModel
import com.wire.android.feature.cells.ui.offline.SessionOfflineDownloads
import com.wire.android.feature.cells.ui.search.sort.SortingCriteria
import com.wire.android.feature.cells.ui.search.sort.toKaliumCriteria
import com.wire.kalium.cells.data.FileFilters
import com.wire.kalium.cells.data.SortingSpec
import com.wire.kalium.cells.domain.model.Node
import com.wire.kalium.cells.domain.usecase.GetConversationNameUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.cells.domain.usecase.GetUserNameUseCase
import com.wire.kalium.cells.domain.usecase.IsAtLeastOneCellAvailableUseCase
import com.wire.kalium.cells.domain.usecase.offline.ObserveOfflineFilesUseCase
import com.wire.kalium.cells.domain.usecase.offline.OfflineFileInfo
import com.wire.kalium.common.functional.fold
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Where the nodes listed on the files screens come from: the server, or the files saved for offline use. Either way
 * they come along with their local state (open download, offline download, saved for offline use).
 */
@Suppress("LongParameterList")
class CellNodesSource @Inject constructor(
    private val getCellFilesPaged: GetPaginatedFilesFlowUseCase,
    private val isAtLeastOneCellAvailable: IsAtLeastOneCellAvailableUseCase,
    private val observeOfflineFiles: ObserveOfflineFilesUseCase,
    private val sharedPathCache: CellFileLocalPathCache,
    private val offlineDownloads: SessionOfflineDownloads,
    private val getConversationName: GetConversationNameUseCase,
    private val getUserName: GetUserNameUseCase,
) {

    suspend fun isCellAvailable(): Boolean = isAtLeastOneCellAvailable().fold({ false }, { it })

    /** The nodes of the folder at [currentPath] on the server, the paging data being cached in [scope]. */
    internal suspend fun onlineNodes(
        currentPath: String?,
        isRecycleBin: Boolean,
        sortingCriteria: SortingCriteria,
        scope: CoroutineScope,
    ): Flow<PagingData<CellNodeUi>> = combine(
        getCellFilesPaged(
            conversationId = currentPath,
            fileFilters = FileFilters(onlyDeleted = isRecycleBin),
            sortingSpec = SortingSpec(
                criteria = sortingCriteria.toKaliumCriteria(),
                descending = sortingCriteria.isDescending,
            ),
        ).cachedIn(scope),
        sharedPathCache.openLoadStates,
        observeOfflineFiles(),
        offlineDownloads.states,
    ) { pagingData, openLoadStates, offlineFiles, downloadStates ->
        val offlineUuids = offlineFiles.map { it.id }.toSet()
        pagingData.map { node ->
            when (node) {
                is Node.Folder -> node.toUiModel()
                is Node.File -> node.toUiModel(
                    openLoadState = openLoadStates[node.uuid],
                    downloadProgress = downloadStates.progresses.downloadProgressOf(node.uuid),
                    isAvailableOffline = node.uuid in offlineUuids,
                    isDownloadQueued = node.uuid in downloadStates.queued,
                    pausedDownloadProgress = downloadStates.paused[node.uuid],
                )
            }
        }
    }

    /**
     * Files saved for offline use, listed folder by folder so browsing a conversation without network shows the same
     * structure as online: only the nodes of the folder at [currentPath].
     */
    internal fun offlineNodes(currentPath: String?): Flow<PagingData<CellNodeUi>> = combine(
        observeOfflineFiles(),
        sharedPathCache.openLoadStates,
        offlineDownloads.states,
    ) { offlineFiles, openLoadStates, downloadStates ->
        val entries = buildOfflineListing(currentPath = currentPath, offlineFiles = offlineFiles)
        PagingData.from(
            data = entries.map { entry ->
                when (entry) {
                    is OfflineListingEntry.FolderEntry -> entry.toCellNodeUi()
                    is OfflineListingEntry.FileEntry -> entry.info.toOfflineUiModel(
                        openLoadState = openLoadStates[entry.info.id],
                        downloadProgress = downloadStates.progresses.downloadProgressOf(entry.info.id),
                    )
                }
            },
            sourceLoadStates = notLoadingStates,
        )
    }

    /** The files saved for offline use matching [search], see [matching]. */
    internal fun offlineSearchResults(search: OfflineSearch): Flow<List<CellNodeUi.File>> =
        observeOfflineFiles().map { files -> files.matching(search).map { it.toOfflineUiModel() } }

    private suspend fun OfflineFileInfo.toOfflineUiModel(
        openLoadState: OpenLoadState? = null,
        downloadProgress: Float? = null,
    ): CellNodeUi.File = toCellNodeUi(
        conversationName = conversationId?.let { getConversationName(it) },
        userName = owner.ifEmpty { null }?.let { getUserName(it) },
        openLoadState = openLoadState,
        downloadProgress = downloadProgress,
    )

    /** Whether something in the folder at [currentPath] is saved for offline use, see [buildOfflineListing]. */
    internal fun hasOfflineFiles(currentPath: String?): Flow<Boolean> =
        observeOfflineFiles().map { files -> buildOfflineListing(currentPath, files).isNotEmpty() }

    internal companion object {
        private val notLoadingStates = LoadStates(
            refresh = LoadState.NotLoading(true),
            prepend = LoadState.NotLoading(true),
            append = LoadState.NotLoading(true),
        )

        val emptyData: PagingData<CellNodeUi> = PagingData.empty(notLoadingStates)
    }
}
