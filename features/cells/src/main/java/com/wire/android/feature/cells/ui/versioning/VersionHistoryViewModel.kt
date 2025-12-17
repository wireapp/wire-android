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
package com.wire.android.feature.cells.ui.versioning

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.cells.ui.navArgs
import com.wire.android.feature.cells.ui.versioning.restore.RestoreDialogState
import com.wire.android.feature.cells.ui.versioning.restore.RestoreVersionState
import com.wire.kalium.cells.domain.usecase.versioning.GetNodeVersionsUseCase
import com.wire.kalium.cells.domain.usecase.versioning.RestoreNodeVersionUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VersionHistoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getNodeVersionsUseCase: GetNodeVersionsUseCase,
    private val restoreNodeVersionUseCase: RestoreNodeVersionUseCase,
    private val versionGroupHelper: VersionGroupHelper,
) : ViewModel() {

    private val navArgs: VersionHistoryNavArgs = savedStateHandle.navArgs()

    val fileName = navArgs.fileName

    var versionHistoryState: MutableState<VersionHistoryState> = mutableStateOf(VersionHistoryState.Idle)
        private set

    var versionsGroupedByTime: MutableState<List<VersionGroup>> = mutableStateOf(listOf())
        private set

    var restoreDialogState: MutableState<RestoreDialogState> =
        mutableStateOf(RestoreDialogState())

    init {
        viewModelScope.launch {
            versionHistoryState.value = VersionHistoryState.Loading
            fetchNodeVersionsGroupedByDate()
        }
    }

    suspend fun refreshVersions() {
        versionHistoryState.value = VersionHistoryState.Refreshing
        fetchNodeVersionsGroupedByDate()
    }

    private suspend fun fetchNodeVersionsGroupedByDate() =
        navArgs.uuid?.let {
            getNodeVersionsUseCase(navArgs.uuid)
                .onSuccess {
                    versionHistoryState.value = VersionHistoryState.Success
                    versionsGroupedByTime.value = versionGroupHelper.groupByDay(it)
                }
                // TODO: Handle error on UI
                .onFailure {
                    versionHistoryState.value = VersionHistoryState.Failed
                }
        }

    // TODO: Unit test coming in another PR
    fun showRestoreConfirmationDialog(versionId: String) {
        restoreDialogState.value = restoreDialogState.value.copy(
            visible = true,
            versionId = versionId,
            restoreVersionState = RestoreVersionState.Idle,
            restoreProgress = 0f
        )
    }

    // TODO: Unit test coming in another PR
    fun hideRestoreConfirmationDialog() {
        restoreDialogState.value = restoreDialogState.value.copy(
            restoreVersionState = RestoreVersionState.Idle,
            visible = false,
            versionId = ""
        )
    }

    // TODO: Unit test coming in another PR
    fun restoreVersion() {
        with(restoreDialogState) {
            restoreDialogState.value = value.copy(
                restoreVersionState = RestoreVersionState.Restoring
            )

            viewModelScope.launch {
                val progressJob = simulateRestoreProgress()

                restoreNodeVersionUseCase(navArgs.uuid ?: "", value.versionId)
                    .onSuccess {
                        delay(DELAY_500_MS) // delay since server takes some time to restore the version
                        refreshVersions()
                        progressJob.cancel()
                        restoreDialogState.value = value.copy(
                            restoreVersionState = RestoreVersionState.Completed,
                            restoreProgress = 1f
                        )
                    }
                    .onFailure {
                        progressJob.cancel()
                        restoreDialogState.value = value.copy(
                            restoreVersionState = RestoreVersionState.Failed
                        )
                    }
            }
        }
    }

    @Suppress("MagicNumber")
    private fun simulateRestoreProgress() = viewModelScope.launch {
        with(restoreDialogState) {
            while (value.restoreProgress < 0.95f && value.restoreVersionState == RestoreVersionState.Restoring) {
                delay(DELAY_100_MS)
                restoreDialogState.value = value.copy(
                    restoreProgress = value.restoreProgress + 0.03f
                )
            }
        }
    }

    companion object {
        const val DATE_PATTERN = "d MMM yyyy"
        const val DELAY_100_MS = 100L
        const val DELAY_500_MS = 500L
    }
}
