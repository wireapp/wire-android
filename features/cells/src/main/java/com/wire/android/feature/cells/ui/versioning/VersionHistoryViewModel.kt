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
import com.wire.android.feature.cells.ui.navArgs
import com.wire.android.navigation.di.ResourceProvider
import com.wire.kalium.cells.domain.model.NodeVersion
import com.wire.kalium.cells.domain.usecase.versioning.GetNodeVersionsUseCase
import com.wire.kalium.common.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class VersionHistoryViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    val resourceProvider: ResourceProvider,
    val getNodeVersionsUseCase: GetNodeVersionsUseCase
) : ViewModel() {

    private val navArgs: VersionHistoryNavArgs = savedStateHandle.navArgs()

    var versionsGroupedByTime: MutableState<List<VersionGroup>> = mutableStateOf(listOf())
        private set

    var isFetchingContent: MutableState<Boolean> = mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            fetchNodeVersionsGroupedByDate()
        }
    }

    suspend fun fetchNodeVersionsGroupedByDate() {
        isFetchingContent.value = true
        navArgs.uuid?.let {
            getNodeVersionsUseCase(navArgs.uuid).onSuccess {
                versionsGroupedByTime.value = it.groupByDay()
            }
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
            .map { (date, items) ->
                val dateLabel = when (date) {
                    today -> "${resourceProvider.getString(R.string.today_label)}, " +
                            "${date.format(DateTimeFormatter.ofPattern(DATE_PATTERN))}"

                    yesterday -> "${resourceProvider.getString(R.string.yesterday_label)}, " +
                            "${date.format(DateTimeFormatter.ofPattern(DATE_PATTERN))}"

                    else -> date.format(DateTimeFormatter.ofPattern(DATE_PATTERN))
                }

                val uiItems = items.map { apiItem ->
                    CellVersion(
                        modifiedBy = apiItem.ownerName ?: "",
                        fileSize = formatSize(apiItem.size?.toLong() ?: 0),
                        modifiedAt = Instant.ofEpochSecond(apiItem.modifiedTime?.toLong() ?: 0L)
                            .atZone(ZoneId.systemDefault())
                            .toLocalTime()
                            .format(DateTimeFormatter.ofPattern(TIME_PATTERN))
                    )
                }
                VersionGroup(dateLabel, uiItems)
            }
    }

    @Suppress("MagicNumber")
    private fun formatSize(bytes: Long): String {
        val units = arrayOf(
            resourceProvider.getString(R.string.unit_b),
            resourceProvider.getString(R.string.unit_kb),
            resourceProvider.getString(R.string.unit_mb),
            resourceProvider.getString(R.string.unit_gb),
            resourceProvider.getString(R.string.unit_tb)
        )
        var size = bytes.toDouble()
        var index = 0

        while (size >= 1024 && index < units.size - 1) {
            size /= 1024
            index++
        }
        return String.format("%.2f %s", size, units[index])
    }

    companion object {
        const val DATE_PATTERN = "d MMM yyyy"
        const val TIME_PATTERN = "HH:mm"
    }
}
