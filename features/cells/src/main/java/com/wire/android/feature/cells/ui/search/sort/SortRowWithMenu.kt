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
package com.wire.android.feature.cells.ui.search.sort

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme

@Composable
fun SortRowWithMenu(
    sortingCriteria: SortingCriteria,
    onSortByClicked: (SortBy) -> Unit,
    onOrderClicked: (SortingCriteria) -> Unit,
    modifier: Modifier = Modifier,
    isSearchResult: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.background(colorsScheme().surface)) {
        Row {
            SortAnchorRow(
                onClick = { expanded = true },
                sortingCriteria = sortingCriteria,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSearchResult) {
                Text(
                    modifier = Modifier
                        .padding(start = dimensions().spacing16x, end = dimensions().spacing16x)
                        .align(Alignment.CenterVertically),
                    text = stringResource(R.string.results_in_shared_drive_label),
                    style = typography().subline01,
                    color = colorsScheme().secondaryText
                )
            }
        }

        DropdownMenu(
            modifier = Modifier.padding(start = dimensions().spacing8x, end = dimensions().spacing8x),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            MenuSectionTitle(stringResource(R.string.sort_by))
            SortBy.entries.forEach { by ->
                MenuCheckItem(
                    text = stringResource(by.label),
                    checked = sortingCriteria.by == by,
                    onClick = {
                        onSortByClicked(by)
                        expanded = false
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = dimensions().spacing6x))

            val options = orderOptionsFor(sortingCriteria.by)
            options.forEach { order ->
                MenuCheckItem(
                    text = stringResource(order.label),
                    checked = sortingCriteria == order,
                    onClick = {
                        onOrderClicked(order)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SortAnchorRow(
    onClick: () -> Unit,
    sortingCriteria: SortingCriteria,
    modifier: Modifier = Modifier
) {
    val rotationAngle by animateFloatAsState(
        targetValue = sortingCriteria.rotationAngle
    )

    Row(
        modifier = modifier
            .heightIn(min = dimensions().spacing40x)
            .clickable(onClick = onClick)
            .padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.rotate(rotationAngle),
            painter = painterResource(R.drawable.ic_sorting),
            contentDescription = null
        )
        Spacer(Modifier.width(dimensions().spacing8x))
        Text(
            text = stringResource(sortingCriteria.label),
            style = typography().button02
        )
        Spacer(Modifier.width(dimensions().spacing6x))
        Icon(
            painter = painterResource(R.drawable.ic_dropdown_chevron),
            contentDescription = null
        )
    }
}

@Composable
private fun MenuSectionTitle(title: String) {
    Text(
        text = title,
        style = typography().button02,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = dimensions().spacing12x, vertical = dimensions().spacing6x)
    )
}

@Composable
private fun MenuCheckItem(
    text: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = typography().body01,
            )
        },
        onClick = onClick,
        leadingIcon = {
            Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                if (checked) {
                    Icon(
                        painter = painterResource(R.drawable.ic_checkmark),
                        contentDescription = null
                    )
                }
            }
        }
    )
}

fun orderOptionsFor(by: SortBy): List<SortingCriteria> = when (by) {
    SortBy.Modified -> listOf(SortingCriteria.Modified.NewestFirst, SortingCriteria.Modified.OldestFirst)
    SortBy.Name -> listOf(SortingCriteria.Name.AtoZ, SortingCriteria.Name.ZtoA)
    SortBy.Size -> listOf(SortingCriteria.Size.SmallestFirst, SortingCriteria.Size.LargestFirst)
}

@MultipleThemePreviews
@Composable
fun PreviewSortRowWithMenu() {
    WireTheme {
        SortRowWithMenu(
            sortingCriteria = SortingCriteria.Modified.NewestFirst,
            onSortByClicked = {},
            onOrderClicked = {},
        )
    }
}
