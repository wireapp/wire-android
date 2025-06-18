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
package com.wire.android.feature.cells.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.wireTypography

@Composable
fun Breadcrumbs(
    items: Array<String>
) {
    Row {
        items.forEachIndexed { index, item ->
            if (index != items.lastIndex) {
                Text(
                    text = item,
                    style = MaterialTheme.wireTypography.button02.copy(
                        color = colorsScheme().secondaryText
                    ),
                )
                Text(
                    text = " > ",
                    style = MaterialTheme.wireTypography.button02.copy(
                        color = colorsScheme().onBackground
                    ),
                )
            } else {
                Text(
                    text = item,
                    style = MaterialTheme.wireTypography.button02.copy(
                        color = colorsScheme().onBackground
                    )
                )
            }
        }
    }
}

@MultipleThemePreviews
@Composable
fun Breadcrumbs(
    items: List<String>
) {
    Breadcrumbs(items.toTypedArray())
}
