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
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.edit.OnlineEditor
import com.wire.android.feature.cells.ui.navArgs
import com.wire.android.feature.cells.ui.versioning.download.DownloadState
import com.wire.android.feature.cells.ui.versioning.restore.RestoreDialogState
import com.wire.android.feature.cells.ui.versioning.restore.RestoreVersionState
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.util.FileSizeFormatter
import com.wire.android.util.addBeforeExtension
import com.wire.android.util.cellFileTime
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.cells.domain.model.NodeVersion
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.download.DownloadCellVersionUseCase
import com.wire.kalium.cells.domain.usecase.versioning.GetNodeVersionsUseCase
import com.wire.kalium.cells.domain.usecase.versioning.RestoreNodeVersionUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.sink
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class VersionHistoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getNodeVersionsUseCase: GetNodeVersionsUseCase,
    private val fileSizeFormatter: FileSizeFormatter,
    private val restoreNodeVersionUseCase: RestoreNodeVersionUseCase,
    private val downloadCellVersionUseCase: DownloadCellVersionUseCase,
    private val fileHelper: FileHelper,
    private val onlineEditor: OnlineEditor,
    private val getEditorUrl: GetEditorUrlUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val navArgs: VersionHistoryNavArgs = savedStateHandle.navArgs()

    val fileName = navArgs.fileName

    var versionHistoryState: MutableState<VersionHistoryState> = mutableStateOf(VersionHistoryState.Idle)
        private set

    var versionsGroupedByTime: MutableState<List<VersionGroup>> = mutableStateOf(listOf())
        private set

    var restoreDialogState: MutableState<RestoreDialogState> = mutableStateOf(RestoreDialogState())
        private set

    var downloadState: MutableState<DownloadState> = mutableStateOf(DownloadState.Idle)
        private set

    init {
        initVersions()
    }

    fun initVersions() {
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
        getNodeVersionsUseCase(navArgs.uuid)
            .onSuccess {
                versionHistoryState.value = VersionHistoryState.Success
                versionsGroupedByTime.value = it.groupByDay()
            }
            // TODO: Handle error on UI
            .onFailure {
                versionHistoryState.value = VersionHistoryState.Failed
            }

    private fun List<NodeVersion>.groupByDay(): List<VersionGroup> {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val grouped = this.groupBy { item ->
            Instant.ofEpochSecond(item.modifiedTime?.toLong() ?: 0L)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }

        return grouped.entries
            .sortedByDescending { it.key }
            .mapIndexed { groupIndex, (date, items) ->
                val dateFormat = DateTimeFormatter.ofPattern(DATE_PATTERN)
                val formattedDate = date.format(dateFormat)

                val dateLabel: UIText = when (date) {
                    today -> UIText.StringResource(
                        R.string.date_label_today,
                        formattedDate
                    )

                    yesterday -> UIText.StringResource(
                        R.string.date_label_yesterday,
                        formattedDate
                    )

                    else -> UIText.DynamicString(formattedDate)
                }

                val uiItems = items.mapIndexed { itemIndex, apiItem ->

                    val formattedTime = apiItem.modifiedTime?.toLong()?.let {
                        kotlinx.datetime.Instant.fromEpochSeconds(it).cellFileTime()
                    } ?: ""

                    CellVersion(
                        versionId = apiItem.id,
                        modifiedBy = apiItem.ownerName ?: "",
                        fileSize = fileSizeFormatter.formatSize(apiItem.size?.toLong() ?: 0),
                        modifiedAt = formattedTime,
                        isCurrentVersion = groupIndex == 0 && itemIndex == 0,
                        presignedUrl = apiItem.getUrl?.url
                    )
                }
                VersionGroup(dateLabel, uiItems)
            }
    }

    fun showRestoreConfirmationDialog(versionId: String) {
        restoreDialogState.value = restoreDialogState.value.copy(
            visible = true,
            versionId = versionId,
            restoreVersionState = RestoreVersionState.Idle,
            restoreProgress = 0f
        )
    }

    fun hideRestoreConfirmationDialog() {
        restoreDialogState.value = restoreDialogState.value.copy(
            restoreVersionState = RestoreVersionState.Idle,
            visible = false,
            versionId = ""
        )
    }

    fun restoreVersion() {
        with(restoreDialogState) {
            restoreDialogState.value = value.copy(
                restoreVersionState = RestoreVersionState.Restoring
            )

            viewModelScope.launch {
                val progressJob = simulateRestoreProgress()

                restoreNodeVersionUseCase(navArgs.uuid, value.versionId)
                    .onSuccess {
                        delay(DELAY_500_MS) // delay since server takes some time to restore the version
                        initVersions()
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

    fun downloadVersion(versionId: String, versionDate: String) {
        viewModelScope.launch {
            downloadState.value = DownloadState.Downloading(0, 0)

            val cellVersion = findVersionById(versionId)
                ?: return@launch run { downloadState.value = DownloadState.Failed }

            val newFileName = fileName.addBeforeExtension("${versionDate}_${cellVersion.modifiedAt}")

            val outputStream = withContext(dispatchers.io()) {
                fileHelper.createDownloadFileStream(newFileName)
            } ?: run {
                downloadState.value = DownloadState.Failed
                return@launch
            }

            val presignedUrl = cellVersion.presignedUrl
                ?: return@launch run { downloadState.value = DownloadState.Failed }

            outputStream.sink().use { sink ->
                downloadCellVersionUseCase(
                    bufferedSink = sink,
                    preSignedUrl = presignedUrl,
                    onProgressUpdate = { progress, total ->
                        downloadState.value = DownloadState.Downloading(progress.toInt(), total)
                    }
                )
                    .onSuccess {
                        downloadState.value = DownloadState.Downloaded(newFileName)
                    }
                    .onFailure {
                        downloadState.value = DownloadState.Failed
                    }
            }
        }
    }

    fun openOnlineEditor() {
        viewModelScope.launch {
            getEditorUrl(navArgs.uuid).onSuccess { editorUrl ->
                editorUrl?.let {
                    onlineEditor.open(it)
                }
            }
        }
    }

    private fun findVersionById(versionId: String): CellVersion? {
        return versionsGroupedByTime.value
            .flatMap { it.versions }
            .find { it.versionId == versionId }
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
