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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the nodes picked for a bulk action. Selection mode is active while [selectedNodes] is not empty.
 *
 * @param isSupported whether the screen offers bulk selection at all (feature flag and screen type).
 * Even then, selection is only [isEnabled] while online and for a user with more than viewer access;
 * the selection is cleared as soon as that stops holding.
 */
internal class CellBulkSelection(
    isSupported: Boolean,
    isOnline: Flow<Boolean>,
    isSelfUserViewerOnly: Flow<Boolean>,
    scope: CoroutineScope,
) {

    // Keyed by uuid in selection order.
    private val _selectedNodes = MutableStateFlow<Map<String, CellNodeUi>>(emptyMap())
    val selectedNodes: StateFlow<Map<String, CellNodeUi>> = _selectedNodes.asStateFlow()

    val isEnabled: StateFlow<Boolean> = combine(isOnline, isSelfUserViewerOnly) { online, viewerOnly ->
        isSupported && online && !viewerOnly
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    val isActive: Boolean get() = _selectedNodes.value.isNotEmpty()

    init {
        scope.launch {
            isEnabled.collect { enabled ->
                if (!enabled) clear()
            }
        }
    }

    fun onLongClick(node: CellNodeUi) {
        if (isEnabled.value) toggle(node)
    }

    fun toggle(node: CellNodeUi) = _selectedNodes.update { selected ->
        if (node.uuid in selected) selected - node.uuid else selected + (node.uuid to node)
    }

    fun clear() = _selectedNodes.update { emptyMap() }
}
