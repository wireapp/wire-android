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
package com.wire.android.feature.cells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.feature.cells.ui.model.CellNodeUi
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CellFilesScreen(
    cellNodes: LazyPagingItems<CellNodeUi>,
    isRefreshing: State<Boolean>,
    onRefresh: () -> Unit,
    onItemClick: (CellNodeUi) -> Unit,
    onItemMenuClick: (CellNodeUi) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing.value,
        onRefresh = onRefresh,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = dimensions().spacing80x)
        ) {
            items(
                count = cellNodes.itemCount,
                key = cellNodes.itemKey { it.uuid },
                contentType = cellNodes.itemContentType { it }
            ) { index ->

                cellNodes[index]?.let { item ->
                    CellListItem(
                        modifier = Modifier
                            .animateItem()
                            .background(color = colorsScheme().surface)
                            .clickable { onItemClick(item) },
                        cell = item,
                        onMenuClick = { onItemMenuClick(item) }
                    )
                    WireDivider(modifier = Modifier.fillMaxWidth())
                }
            }

            when (cellNodes.loadState.append) {
                is LoadState.Error -> item(contentType = "error") {
                    ErrorFooter(
                        onRetry = { cellNodes.retry() }
                    )
                }

                is LoadState.Loading -> item(contentType = "progress") {
                    ProgressFooter()
                }

                is LoadState.NotLoading -> {}
            }
        }
    }
}

@MultipleThemePreviews
@Composable
private fun ProgressFooter() {
    Box(
        modifier = Modifier
            .height(dimensions().spacing56x)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(dimensions().spacing24x)
                .background(
                    color = colorsScheme().surface,
                    shape = RoundedCornerShape(dimensions().corner10x)
                )
                .padding(
                    horizontal = dimensions().spacing16x,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = dimensions().spacing6x,
                alignment = Alignment.CenterHorizontally,
            ),
        ) {
            WireCircularProgressIndicator(
                progressColor = colorsScheme().secondaryText,
                size = dimensions().spacing14x,
            )
            Text(
                text = stringResource(R.string.loading_files),
                style = typography().subline01
            )
        }
    }
}

@Composable
private fun ErrorFooter(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colorsScheme().errorVariant)
            .padding(dimensions().spacing12x),
        verticalArrangement = Arrangement.spacedBy(
            space = dimensions().spacing8x,
            alignment = Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = stringResource(R.string.file_list_load_error),
            style = typography().label03,
            color = colorsScheme().error,
            textAlign = TextAlign.Center,
        )

        WireSecondaryButton(
            modifier = Modifier.height(dimensions().spacing32x),
            text = stringResource(R.string.retry),
            onClick = onRetry,
            fillMaxWidth = false,
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewProgressFooter() {
    WireTheme {
        ProgressFooter()
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewErrorFooter() {
    WireTheme {
        ErrorFooter(
            onRetry = {}
        )
    }
}
