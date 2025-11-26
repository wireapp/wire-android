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
package com.wire.android.feature.cells.ui.publiclink.settings.expiration

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.feature.cells.R

@Composable
fun DateTimeView(
    date: String?,
    time: String?,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions().spacing48x)
            .background(
                color = colorsScheme().surfaceVariant,
                shape = RoundedCornerShape(dimensions().spacing12x)
            )
            .border(
                width = dimensions().spacing1x,
                color = colorsScheme().outline,
                shape = RoundedCornerShape(dimensions().spacing12x)
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateView(date, onDateClick)
        VerticalDivider(
            thickness = dimensions().spacing1x,
            color = colorsScheme().divider
        )
        TimeView(time, onTimeClick)
    }
}

@Composable
private fun RowScope.DateView(
    date: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .weight(2.2f)
            .clip(
                RoundedCornerShape(
                    topStart = dimensions().spacing12x,
                    bottomStart = dimensions().spacing12x
                ),
            )
            .clickable { onClick() }
            .padding(horizontal = dimensions().spacing16x),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = date ?: stringResource(R.string.label_date),
            color = if (date != null) colorsScheme().onSurface else colorsScheme().secondaryText
        )
        Icon(
            modifier = Modifier.size(dimensions().spacing16x),
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = null,
        )
    }
}

@Composable
private fun RowScope.TimeView(
    time: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .clip(
                RoundedCornerShape(
                    topEnd = dimensions().spacing12x,
                    bottomEnd = dimensions().spacing12x,
                )
            )
            .clickable { onClick() }
            .padding(horizontal = dimensions().spacing16x),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = time ?: stringResource(R.string.label_time),
            color = if (time != null) colorsScheme().onSurface else colorsScheme().secondaryText
        )
        Icon(
            modifier = Modifier.size(dimensions().spacing16x),
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewDateTimeView() {
    WireTheme {
        Column(
            modifier = Modifier.padding(dimensions().spacing16x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing16x)
        ) {
            DateTimeView(
                date = null,
                time = null,
                onDateClick = {},
                onTimeClick = {},
            )
            DateTimeView(
                date = "Tuesday, March 27",
                time = "11:30 AM",
                onDateClick = {},
                onTimeClick = {},
            )
        }
    }
}
