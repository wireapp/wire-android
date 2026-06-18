/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography

/**
 * A compact, fully-rounded selectable pill (WhatsApp-style filter pill).
 *
 * Compared to [WireFilterChip] this renders at a smaller, tighter size: it is built directly on
 * [Surface] instead of Material3's `FilterChip`, so it is not subject to the chip's fixed height or
 * the 48dp minimum interactive touch target. Use it for dense filter rows where the standard chip
 * feels too large.
 */
@Composable
fun WireSelectablePill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    count: Int? = null,
    onClick: () -> Unit = {},
) {
    // Suppress the 48dp minimum interactive touch target that the clickable Surface reserves,
    // otherwise the pill renders with a large empty area around it.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Surface(
            modifier = modifier,
            onClick = onClick,
            shape = CircleShape,
            color = if (isSelected) colorsScheme().primaryVariant else colorsScheme().surface,
            contentColor = if (isSelected) colorsScheme().primary else colorsScheme().onBackground,
            border = BorderStroke(
                width = dimensions().spacing1x,
                color = if (isSelected) colorsScheme().primary else colorsScheme().outline,
            ),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = dimensions().spacing12x,
                    vertical = dimensions().spacing6x,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions().spacing6x),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.wireTypography.button03,
                    maxLines = 1,
                )
                if (count != null && count > 0) {
                    CountBadge(count = count)
                }
            }
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewWireSelectablePillUnselected() {
    WireTheme {
        WireSelectablePill(label = "Conversations", isSelected = false)
    }
}

@MultipleThemePreviews
@Composable
fun PreviewWireSelectablePillSelectedWithCount() {
    WireTheme {
        WireSelectablePill(label = "Conversations", isSelected = true, count = 5)
    }
}
