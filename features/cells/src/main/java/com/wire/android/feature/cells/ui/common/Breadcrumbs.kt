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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.wireTypography

@Composable
fun Breadcrumbs(
    pathSegments: Array<String>,
    modifier: Modifier = Modifier,
    onBreadcrumbsFolderClick: (index: Int) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(pathSegments) {
        if (pathSegments.isNotEmpty()) {
            listState.animateScrollToItem(pathSegments.lastIndex)
        }
    }

    LazyRow(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(
            start = dimensions().spacing16x,
            end = dimensions().spacing16x
        ),
    ) {
        pathSegments.forEachIndexed { index, item ->
            item {
                if (index != pathSegments.lastIndex) {
                    Text(
                        modifier = Modifier
                            .clickable { onBreadcrumbsFolderClick(index) },
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
                        modifier = Modifier
                            .clickable { onBreadcrumbsFolderClick(index) },
                        text = item,
                        style = MaterialTheme.wireTypography.button02.copy(
                            color = colorsScheme().onBackground
                        )
                    )
                }
            }
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewBreadcrumbs() {
    Breadcrumbs(
        arrayOf(
            "Folder 1",
            "Folder 2",
            "Folder 3",
        ),
        onBreadcrumbsFolderClick = {}
    )
}
