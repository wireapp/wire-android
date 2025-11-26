/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.cells.ui.versionhistory

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.cells.ui.navArgs
import com.wire.kalium.cells.domain.usecase.versioning.GetNodeVersionsUseCase
import com.wire.kalium.common.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VersionHistoryViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    val getNodeVersionsUseCase: GetNodeVersionsUseCase,
) : ViewModel() {

    private val navArgs: VersionHistoryNavArgs = savedStateHandle.navArgs()

    var versionsGroupedByTime: Map<String, List<CellVersion>> = mapOf()
        private set

    init {

        viewModelScope.launch {
            getNodeVersionsGroupedByDate()
        }
    }
    suspend fun getNodeVersionsGroupedByDate(): Map<String, List<CellVersion>> {
        navArgs.uuid?.let {
            val result = getNodeVersionsUseCase(navArgs.uuid)
            result.onSuccess {
                Log.d("Versions", "getNodeVersionsGroupedByDate: $result")
            }
        }
        // todo: implement real logic to get versions in next PR
        return mapOf("1 Dec 2025" to listOf())
    }
}
