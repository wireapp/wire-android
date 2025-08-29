/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.common.topappbar

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.kalium.logic.data.conversation.Filter
import dev.ahmedmourad.bundlizer.Bundlizer
import kotlinx.serialization.builtins.ListSerializer

@Composable
fun rememberConversationFilterState(): ConversationFilterState = rememberSaveable(saver = ConversationFilterState.saver()) {
    ConversationFilterState()
}

class ConversationFilterState(initialValue: Filter.Conversation = Filter.Conversation.All) {
    var filter: Filter.Conversation by mutableStateOf(initialValue)
        private set

    fun changeFilter(newFilter: Filter.Conversation) {
        filter = newFilter
    }

    companion object {
        fun saver(): Saver<ConversationFilterState, Bundle> = Saver(
            save = {
                Bundlizer.bundle(Filter.serializer(), it.filter)
            },
            restore = {
                ConversationFilterState(Bundlizer.unbundle(Filter.Conversation.serializer(), it))
            }
        )
    }
}


@Composable
fun rememberCellsFilterState(): CellsFilterState =
    rememberSaveable(saver = CellsFilterState.saver()) {
        CellsFilterState()
    }

class CellsFilterState(initialFilters: Set<Filter.Cells> = emptySet()) {
    var filters: Set<Filter.Cells> by mutableStateOf(initialFilters)
        private set

    fun updateFilters(newFilters: Set<Filter.Cells>) {
        filters = newFilters
    }

    fun clearFilters() {
        filters = emptySet()
    }

    fun isSelected(filter: Filter.Cells): Boolean = filters.contains(filter)

    companion object {
        fun saver(): Saver<CellsFilterState, Bundle> = Saver(
            save = {
                val list = it.filters.toList()
                Bundlizer.bundle(ListSerializer(Filter.Cells.serializer()), list)
            },
            restore = {
                val list = Bundlizer.unbundle(ListSerializer(Filter.Cells.serializer()), it)
                CellsFilterState(list.toSet())
            }
        )
    }
}
