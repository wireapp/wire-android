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
package com.wire.android.feature.cells.ui.search.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.chip.WireFilterChip
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme

@Composable
fun FilterChipsRow(
    isSharedByLinkSelected: Boolean,
    tagsCount: Int,
    typeCount: Int,
    ownerCount: Int,
    hasAnyFilter: Boolean,
    modifier: Modifier = Modifier,
    onFilterByTagsClicked: () -> Unit = { },
    onFilterByTypeClicked: () -> Unit = { },
    onFilterByOwnerClicked: () -> Unit = { },
    onRemoveAllFiltersClicked: () -> Unit = { },
    onFilterBySharedByLinkClicked: () -> Unit = { }
) {
    val scrollState = rememberScrollState()

    @Composable
    fun DropdownChip(labelRes: Int, count: Int, onClick: () -> Unit) {
        WireFilterChip(
            label = stringResource(labelRes),
            count = count.takeIf { it > 0 },
            isSelected = count > 0,
            trailingIconResource = R.drawable.ic_dropdown_chevron,
            onClick = { onClick() }
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .background(colorsScheme().background)
            .padding(horizontal = dimensions().spacing12x),
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
    ) {
        DropdownChip(R.string.filter_chip_tags, tagsCount, onFilterByTagsClicked)
        DropdownChip(R.string.filter_chip_type, typeCount, onFilterByTypeClicked)
        DropdownChip(R.string.filter_chip_owner, ownerCount, onFilterByOwnerClicked)

        WireFilterChip(
            label = stringResource(R.string.filter_chip_link_sharing),
            isSelected = isSharedByLinkSelected,
            onClick = {
                onFilterBySharedByLinkClicked()
            }
        )
        if (hasAnyFilter) {
            Text(
                modifier = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .clickable { onRemoveAllFiltersClicked() },
                text = stringResource(R.string.filter_chip_remove_all_filters),
                style = typography().button02,
                color = colorsScheme().primary,
            )
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewFilterChipsRow() {
    WireTheme {
        FilterChipsRow(
            isSharedByLinkSelected = true,
            tagsCount = 2,
            typeCount = 1,
            ownerCount = 0,
            hasAnyFilter = true,
        )
    }
}
