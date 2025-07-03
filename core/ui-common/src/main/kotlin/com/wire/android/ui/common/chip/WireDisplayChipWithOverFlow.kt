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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme

@Composable
fun WireDisplayChipWithOverFlow(
    label: String,
    modifier: Modifier = Modifier,
    chipsCount: Int = 5
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .height(dimensions().spacing16x)
                .background(color = colorsScheme().primaryVariant, shape = RoundedCornerShape(size = dimensions().spacing4x)),
        ) {
            Text(
                modifier = Modifier.padding(
                    start = dimensions().spacing4x,
                    end = dimensions().spacing4x,
                    top = dimensions().spacing2x,
                    bottom = dimensions().spacing2x
                ),
                text = label,
                textAlign = TextAlign.Left,
                overflow = TextOverflow.Ellipsis,
                style = typography().label03,
                color = colorsScheme().onPrimaryVariant,
                maxLines = 1,
            )
        }
        if (chipsCount > 0) {
            Text(
                modifier = Modifier.padding(start = dimensions().spacing4x),
                text = "+$chipsCount",
                style = typography().label03,
                color = colorsScheme().onPrimaryVariant,
                maxLines = 1,
            )
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewDisplayChip() {
    WireTheme {
        WireDisplayChipWithOverFlow(
            label = "Marketing",
            modifier = Modifier
        )
    }
}
