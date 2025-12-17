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

import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.versioning.VersionHistoryViewModel.Companion.DATE_PATTERN
import com.wire.android.util.FileSizeFormatter
import com.wire.android.util.ui.UIText
import com.wire.kalium.cells.domain.model.NodeVersion
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class VersionGroupHelper @OptIn(ExperimentalTime::class) constructor(
    private val fileSizeFormatter: FileSizeFormatter,
    private val currentTime: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {

    fun groupByDay(versions: List<NodeVersion>): List<VersionGroup> {
        val today = LocalDate.ofInstant(Instant.ofEpochMilli(currentTime()), ZoneId.systemDefault())
        val yesterday = today.minusDays(1)

        val grouped = versions.groupBy { item ->
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
                        isCurrentVersion = groupIndex == 0 && itemIndex == 0
                    )
                }
                VersionGroup(dateLabel, uiItems)
            }
    }
}
