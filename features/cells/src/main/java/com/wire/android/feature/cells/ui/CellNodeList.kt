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

import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.search.sort.SortBy
import com.wire.android.feature.cells.ui.search.sort.SortingCriteria
import com.wire.android.feature.cells.ui.search.defaultCriteriaFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * The list of nodes of a files screen: online from the server, or the offline listing while offline or when only the
 * files saved for offline use are asked for. Also holds how it is sorted and the nodes taken off it ahead of the server.
 *
 * @param offlineOnlyFilterEnabled whether the screen offers to list only the files saved for offline use, see
 * [isOfflineOnly].
 */
@Suppress("LongParameterList")
internal class CellNodeList(
    /** Where the list is loaded and cached: it lives as long as the screen that owns it. */
    val scope: CoroutineScope,
    private val context: CellScreenContext,
    private val source: CellNodesSource,
    private val isOnline: StateFlow<Boolean>,
    private val offlineFilesEnabled: Boolean,
    val offlineOnlyFilterEnabled: Boolean,
) : CellNodeListUpdater {

    private val isCellAvailable = MutableStateFlow(false)
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    private val refreshDone = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val hiddenUuids = MutableStateFlow<List<String>>(emptyList())

    // Filled as pages load, possibly off the main thread.
    private val loadedUuids: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // Changes made to loaded nodes since, applied on top of them until they are loaded again.
    private val nodeUpdates = MutableStateFlow<List<CellNodeUpdate>>(emptyList())

    private val _isPullToRefresh = MutableStateFlow(false)
    val isPullToRefresh: StateFlow<Boolean> = _isPullToRefresh.asStateFlow()

    private val _sortingCriteria = MutableStateFlow(context.initialSortingCriteria)
    val sortingCriteria: StateFlow<SortingCriteria> = _sortingCriteria.asStateFlow()

    private val _isOfflineOnly = MutableStateFlow(offlineOnlyFilterEnabled && context.isOfflineOnlyRequested)

    /**
     * Lists just the files saved for offline use, like when there is no connection, without having to go offline.
     * While offline the offline listing shows regardless.
     */
    val isOfflineOnly: StateFlow<Boolean> = _isOfflineOnly.asStateFlow()

    /**
     * Whether to offer switching to the offline listing: only when something here is saved for offline use. Kept while
     * the offline listing is shown, so that removing the last saved file there doesn't take away the way back.
     */
    val showOfflineOnlyToggle: StateFlow<Boolean> = if (offlineOnlyFilterEnabled) {
        combine(source.hasOfflineFiles(context.currentPath), isOfflineOnly) { hasOfflineFiles, offlineOnly ->
            hasOfflineFiles || offlineOnly
        }.stateIn(scope, SharingStarted.Eagerly, _isOfflineOnly.value)
    } else {
        MutableStateFlow(false).asStateFlow()
    }

    private val onlineNodes: Flow<PagingData<CellNodeUi>> = isCellAvailable.flatMapLatest { cellAvailable ->
        if (!cellAvailable) return@flatMapLatest flowOf(CellNodesSource.emptyData)
        refreshTrigger.flatMapLatest {
            _sortingCriteria.flatMapLatest { sortingCriteria ->
                // Loaded again from the server: the nodes come with the changes made so far.
                nodeUpdates.value = emptyList()
                combine(
                    source.onlineNodes(context.currentPath, context.isRecycleBin, sortingCriteria, scope),
                    hiddenUuids,
                    nodeUpdates,
                ) { pagingData, hidden, updates ->
                    pagingData.filter { it.uuid !in hidden }.map { updates.applyTo(it) }.trackLoaded()
                }
            }
        }
    }.shareIn(scope, started = SharingStarted.Eagerly, replay = 1)

    private val offlineNodes: Flow<PagingData<CellNodeUi>> = source.offlineNodes(context.currentPath)

    val nodes: Flow<PagingData<CellNodeUi>> =
        combine(isCellAvailable, isOnline, isOfflineOnly) { cellAvailable, online, offlineOnly ->
            Triple(cellAvailable, online, offlineOnly)
        }.flatMapLatest { (cellAvailable, online, offlineOnly) ->
            when {
                !cellAvailable -> flowOf(CellNodesSource.emptyData)
                offlineFilesEnabled && (!online || offlineOnly) -> offlineNodes
                else -> onlineNodes
            }
        }

    /** Checks a cell is available at all before loading anything from the server. */
    fun load() = scope.launch {
        val cellAvailable = source.isCellAvailable()
        isCellAvailable.value = cellAvailable
        if (cellAvailable) refresh()
    }

    fun onPullToRefresh() {
        // The offline listing follows the local database on its own: there is nothing to refresh.
        if (!isOnline.value || _isOfflineOnly.value) return
        _isPullToRefresh.value = true
        refresh()
    }

    /** @return whether the listing changed. */
    fun setOfflineOnly(offlineOnly: Boolean): Boolean {
        if (!offlineOnlyFilterEnabled || _isOfflineOnly.value == offlineOnly) return false
        _isOfflineOnly.value = offlineOnly
        return true
    }

    fun setSortBy(by: SortBy) = _sortingCriteria.update { current ->
        if (current.by == by) current else defaultCriteriaFor(by)
    }

    fun setSorting(criteria: SortingCriteria) {
        _sortingCriteria.value = criteria
    }

    fun clearHidden() = hiddenUuids.update { emptyList() }

    override fun refresh() {
        scope.launch { refreshTrigger.tryEmit(Unit) }
    }

    override fun hide(node: CellNodeUi) = hiddenUuids.update { it + node.uuid }

    override fun show(node: CellNodeUi) = hiddenUuids.update { it - node.uuid }

    override fun applyUpdate(update: CellNodeUpdate) {
        // Nothing to update otherwise, and no need to present the list again for that.
        if (update.uuid in loadedUuids) nodeUpdates.update { it + update }
    }

    override suspend fun awaitRefreshDone() {
        refreshDone.first()
    }

    private fun onRefreshDone() {
        if (_isPullToRefresh.value) _isPullToRefresh.value = false
        refreshDone.tryEmit(Unit)
    }

    /** Records the nodes loaded, and that the list got (re)loaded once the first of them is. */
    private fun PagingData<CellNodeUi>.trackLoaded(): PagingData<CellNodeUi> {
        var isFirst = true
        return map { node ->
            if (isFirst) {
                isFirst = false
                onRefreshDone()
            }
            loadedUuids.add(node.uuid)
            node
        }
    }
}
