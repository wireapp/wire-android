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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import com.wire.android.ui.common.R
import com.wire.android.ui.common.button.wireChipColors
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun WireFilterChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onSelectChip: (String) -> Unit = {}
) {

    val rotationAngle by animateFloatAsState(
        targetValue = if (isSelected) 0f else 45f,
    )

    FilterChip(
        modifier = modifier.wrapContentSize(),
        onClick = { onSelectChip(label) },
        label = {
            Text(
                text = label,
                style = MaterialTheme.wireTypography.button02
            )
        },
        enabled = isEnabled,
        selected = isSelected,
        colors = wireChipColors(),
        trailingIcon = {
            Icon(
                modifier = Modifier
                    .size(dimensions().spacing12x)
                    .rotate(rotationAngle),
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = null,
            )
        },
    )
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
        WireFilterChip(label = "Selected", isSelected = true)
    }
}

@MultipleThemePreviews
@Composable
fun PreviewDisabledFilterChip() {
    WireTheme {
        WireFilterChip(label = "Disabled items", isSelected = true, isEnabled = false)
    }
}
