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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.wireTypography

@Composable
fun Breadcrumbs(
    pathSegments: Array<String>,
    modifier: Modifier = Modifier,
    isRecycleBin: Boolean = false,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (index != 0) {
                        Icon(
                            modifier = Modifier.padding(
                                start = dimensions().spacing8x,
                                end = dimensions().spacing8x
                            ),
                            painter = painterResource(id = R.drawable.ic_chevron_right),
                            contentDescription = null
                        )
                    }

                    Text(
                        modifier = Modifier.clickable { onBreadcrumbsFolderClick(index) },
                        text = item,
                        style = MaterialTheme.wireTypography.button02.run {
                            if (index == pathSegments.lastIndex && index != 0) {
                                copy(color = colorsScheme().primary)
                            } else this
                        }
                    )

                    if (isRecycleBin && index == 0) {
                        val isRecycleBinLast = pathSegments.size == 1
                        RecycleBinItem(
                            color = if (isRecycleBinLast) {
                                colorsScheme().primary
                            } else {
                                colorsScheme().onSurfaceVariant
                            },
                            onBreadcrumbsFolderClick = { onBreadcrumbsFolderClick(0) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecycleBinItem(
    color: Color? = null,
    onBreadcrumbsFolderClick: () -> Unit = { }
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.padding(
                start = dimensions().spacing8x,
                end = dimensions().spacing8x
            ),
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null
        )
        Icon(
            modifier = Modifier.padding(end = dimensions().spacing8x),
            painter = painterResource(id = R.drawable.ic_trash),
            contentDescription = null,
            tint = color ?: colorsScheme().onSurfaceVariant
        )
        Text(
            modifier = Modifier.clickable { onBreadcrumbsFolderClick() },
            text = "Recycle Bin",
            style = MaterialTheme.wireTypography.button02.run {
                if (color != null) {
                    copy(color = color)
                } else this
            }
        )
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
