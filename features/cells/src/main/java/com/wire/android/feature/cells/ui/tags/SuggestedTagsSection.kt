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
package com.wire.android.feature.cells.ui.tags

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.chip.WireFilterChip
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun SuggestedTagsSection(
    suggestedTags: Set<String>,
    modifier: Modifier = Modifier,
    onAddTag: (String) -> Unit
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(
                top = dimensions().spacing8x,
                bottom = dimensions().spacing8x,
                start = dimensions().spacing16x,
                end = dimensions().spacing16x
            ),
            text = stringResource(R.string.suggested_tags_label).uppercase(),
            style = MaterialTheme.wireTypography.label01.copy(
                color = colorsScheme().secondaryText,
            )
        )

        if (suggestedTags.isEmpty()) {
            Text(
                modifier = Modifier
                    .padding(
                        start = dimensions().spacing16x,
                        end = dimensions().spacing16x,
                        top = dimensions().spacing20x,
                        bottom = dimensions().spacing20x
                    ),
                text = stringResource(R.string.no_suggested_tags_label),
                style = MaterialTheme.wireTypography.body01,
            )
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = dimensions().spacing16x, end = dimensions().spacing16x)
            ) {
                suggestedTags.forEach { tag ->
                    item {
                        WireFilterChip(
                            modifier = Modifier
                                .padding(
                                    end = dimensions().spacing8x,
                                    bottom = dimensions().spacing8x
                                )
                                .animateItem(),
                            label = tag,
                            isSelected = false,
                            onSelectChip = onAddTag
                        )
                    }
                }
            }
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewSuggestedTagsSection() {
    WireTheme {
        SuggestedTagsSection(
            suggestedTags = setOf("Work", "Personal", "Important", "To-Do", "Urgent"),
            onAddTag = {}
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewEmptySuggestedTagsSection() {
    WireTheme {
        SuggestedTagsSection(
            suggestedTags = setOf(),
            onAddTag = {}
        )
    }
}
