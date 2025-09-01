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
import com.wire.android.feature.cells.domain.model.CellsFilter
import com.wire.kalium.logic.data.conversation.ConversationFilter
import dev.ahmedmourad.bundlizer.Bundlizer
import kotlinx.serialization.builtins.ListSerializer

@Composable
fun rememberConversationFilterState(): ConversationFilterState = rememberSaveable(saver = ConversationFilterState.saver()) {
    ConversationFilterState()
}

class ConversationFilterState(initialValue: ConversationFilter = ConversationFilter.All) {
    var filter: ConversationFilter by mutableStateOf(initialValue)
        private set

    fun changeFilter(newFilter: ConversationFilter) {
        filter = newFilter
    }

    companion object {
        fun saver(): Saver<ConversationFilterState, Bundle> = Saver(
            save = {
                Bundlizer.bundle(ConversationFilter.serializer(), it.filter)
            },
            restore = {
                ConversationFilterState(Bundlizer.unbundle(ConversationFilter.serializer(), it))
            }
        )
    }
}

@Composable
fun rememberCellsFilterState(): CellsFilterState =
    rememberSaveable(saver = CellsFilterState.saver()) {
        CellsFilterState()
    }

class CellsFilterState(initialFilters: Set<CellsFilter> = emptySet()) {
    var filters: Set<CellsFilter> by mutableStateOf(initialFilters)
        private set

    fun updateFilters(newFilters: Set<CellsFilter>) {
        filters = newFilters
    }

    fun isSelected(filter: CellsFilter): Boolean = filters.contains(filter)

    companion object {
        fun saver(): Saver<CellsFilterState, Bundle> = Saver(
            save = {
                val list = it.filters.toList()
                Bundlizer.bundle(ListSerializer(CellsFilter.serializer()), list)
            },
            restore = {
                val list = Bundlizer.unbundle(ListSerializer(CellsFilter.serializer()), it)
                CellsFilterState(list.toSet())
            }
        )
    }
}
