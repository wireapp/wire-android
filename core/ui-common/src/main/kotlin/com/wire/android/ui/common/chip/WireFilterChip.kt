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
package com.wire.android.ui.common.chip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.wire.android.ui.common.button.wireChipColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun WireFilterChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    count: Int? = null,
    isEnabled: Boolean = true,
    onClick: (String) -> Unit = {},
    trailingIconResource: Int? = null
) {

    FilterChip(
        modifier = modifier.wrapContentSize(),
        onClick = { onClick(label) },
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.wireTypography.button02,
                    maxLines = 1
                )

                if (count != null && count > 0) {
                    CountBadge(count = count)
                }
            }
        },
        enabled = isEnabled,
        selected = isSelected,
        colors = wireChipColors(),
        trailingIcon = {
            trailingIconResource ?.let {
                Icon(
                    modifier = Modifier.width(dimensions().spacing14x),
                    painter = painterResource(id = it),
                    contentDescription = null,
                )
            }
        },
        border = FilterChipDefaults.filterChipBorder(
            enabled = isEnabled,
            selected = isSelected,
            borderColor = colorsScheme().outline,
            selectedBorderColor = colorsScheme().primary,
        )
    )
}

@Composable
fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(36)
            )
            .padding(horizontal = dimensions().spacing4x, vertical = dimensions().spacing1x),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.wireTypography.label02,
            color = colorsScheme().onPrimary
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewFilterChip() {
    WireTheme {
        WireFilterChip(label = "Preview", isSelected = false)
    }
}

@MultipleThemePreviews
@Composable
fun PreviewSelectedFilterChip() {
    WireTheme {
        WireFilterChip(label = "Selected", count = 4, isSelected = true)
    }
}

@MultipleThemePreviews
@Composable
fun PreviewDisabledFilterChip() {
    WireTheme {
        WireFilterChip(label = "Disabled items", isSelected = true, isEnabled = false)
    }
}
