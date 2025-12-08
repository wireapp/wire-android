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
import com.wire.android.feature.cells.ui.versioning.restore.RestoreDialogState
import com.wire.android.feature.cells.ui.versioning.restore.RestoreVersionState
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.util.FileSizeFormatter
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.cells.domain.model.NodeVersion
import com.wire.kalium.cells.domain.usecase.DownloadCellVersionUseCase
import com.wire.kalium.cells.domain.usecase.versioning.GetNodeVersionsUseCase
import com.wire.kalium.cells.domain.usecase.versioning.RestoreNodeVersionUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.buffer
import okio.sink
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val navArgs: VersionHistoryNavArgs = savedStateHandle.navArgs()

    val fileName = navArgs.fileName

    var isFetchingContent: MutableState<Boolean> = mutableStateOf(true)
        private set

    var versionsGroupedByTime: MutableState<List<VersionGroup>> = mutableStateOf(listOf())
        private set

    var restoreDialogState: MutableState<RestoreDialogState> =
        mutableStateOf(RestoreDialogState())
        private set

    init {
        fetchNodeVersionsGroupedByDate()
    }

    fun fetchNodeVersionsGroupedByDate() =
        viewModelScope.launch {
            isFetchingContent.value = true
            getNodeVersionsUseCase(navArgs.uuid).onSuccess {
                versionsGroupedByTime.value = it.groupByDay()
            }
            isFetchingContent.value = false
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

                    val formattedTime = Instant.ofEpochSecond(apiItem.modifiedTime?.toLong() ?: 0L)
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime()
                        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

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
                // simulating progress
                val progressJob = launch {
                    while (value.restoreProgress < 0.95f && value.restoreVersionState == RestoreVersionState.Restoring) {
                        delay(100)
                        restoreDialogState.value = value.copy(
                            restoreProgress = value.restoreProgress + 0.03f
                        )
                    }
                }

                restoreNodeVersionUseCase(navArgs.uuid, value.versionId)
                    .onSuccess {
                        delay(500) // delay since server takes some time to restore the version
                        val fetchJob = fetchNodeVersionsGroupedByDate()
                        fetchJob.start()
                        fetchJob.join()
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

    private fun addBeforeExtension(fileName: String, insert: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex != -1) {
            val name = fileName.take(dotIndex)
            val ext = fileName.substring(dotIndex)
            "${name}_$insert$ext"
        } else {
            fileName + insert
        }
    }

    fun downloadVersion(versionId: String, versionDate: String, onDownloadCompleted: (CellVersion, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch(dispatchers.io()) {
            val cellVersion = findVersionById(versionId)

            cellVersion?.let {
                val cellVersion = findVersionById(versionId)
                val newFileName = addBeforeExtension(fileName, "${versionDate}_${cellVersion?.modifiedAt}")
                val bufferedSink = fileHelper.createDownloadFileStream(newFileName)?.sink()?.buffer()
                if (cellVersion?.presignedUrl != null && bufferedSink != null) {
                    downloadCellVersionUseCase.invoke(
                        bufferedSink = bufferedSink,
                        preSignedUrl = cellVersion.presignedUrl,
                        onProgressUpdate = {},
                        onCompleted = {
                            onDownloadCompleted(cellVersion, newFileName)
                        }
                    )
                }
            }
        }
    }

    fun findVersionById(versionId: String): CellVersion? {
        return versionsGroupedByTime.value
            .flatMap { it.versions }
            .find { it.versionId == versionId }
    }

    fun openOnlineEditor() {
        val cellVersion = findVersionById(restoreDialogState.value.versionId)
        cellVersion?.presignedUrl?.let {
            onlineEditor.open(it)
        }
    }

    companion object {
        const val DATE_PATTERN = "d MMM yyyy"
    }
}
