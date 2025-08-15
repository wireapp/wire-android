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
package com.wire.android.feature.cells.ui.filter

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.chip.WireFilterChip
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun FilterBottomSheet(
    selectableTags: Set<String>,
    selectedTags: Set<String>,
    onApply: (Set<String>) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: WireModalSheetState<Unit> = rememberWireModalSheetState<Unit>(WireSheetValue.Expanded(Unit))
) {
    WireModalSheetLayout(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        SheetContent(
            selectableTags = selectableTags,
            selectedTags = selectedTags,
            onApply = {
                if (sheetState.currentValue is WireSheetValue.Expanded) {
                    sheetState.hide { onApply(it) }
                }
            },
            onClearAll = {
                if (sheetState.currentValue is WireSheetValue.Expanded) {
                    sheetState.hide { onClearAll() }
                }
            }
        )
    }
}

@Composable
private fun SheetContent(
    selectableTags: Set<String>,
    selectedTags: Set<String>,
    onApply: (Set<String>) -> Unit = {},
    onClearAll: () -> Unit = {},
) {
    var isExpanded by remember { mutableStateOf(true) }

    val interactionSource = remember { MutableInteractionSource() }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 180f,
    )

    var selectedChips by remember { mutableStateOf(selectedTags) }

    Column(modifier = Modifier.padding(horizontal = dimensions().spacing16x)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensions().spacing16x, bottom = dimensions().spacing16x),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.filter_label),
                style = MaterialTheme.wireTypography.title01,
                textAlign = TextAlign.Center,
            )
        }
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .padding(top = dimensions().spacing16x, bottom = dimensions().spacing16x)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        isExpanded = !isExpanded
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.tags_label),
                    style = MaterialTheme.wireTypography.body01,
                    color = colorsScheme().onBackground,
                )
                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    modifier = Modifier.rotate(rotationAngle),
                    painter = painterResource(id = R.drawable.ic_chevron_up),
                    contentDescription = null
                )
            }

            if (isExpanded) {
                Text(
                    text = stringResource(R.string.select_tags_label).uppercase(),
                    style = MaterialTheme.wireTypography.label01,
                    color = colorsScheme().secondaryText,
                )
                LazyRow(
                    modifier = Modifier.padding(
                        top = dimensions().spacing16x,
                        bottom = dimensions().spacing16x
                    )
                ) {
                    selectableTags.forEach { tag ->
                        val isSelected = selectedChips.contains(tag)

                        item {
                            WireFilterChip(
                                label = tag,
                                isSelected = isSelected,
                                modifier = Modifier.padding(end = dimensions().spacing16x),
                                onSelectChip = { label ->
                                    selectedChips = if (isSelected) {
                                        selectedChips - label
                                    } else {
                                        selectedChips + label
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .padding(
                    top = dimensions().spacing16x,
                    bottom = dimensions().spacing16x
                )
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            WireSecondaryButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.clear_all_label),
                onClick = {
                    selectedChips = emptySet()
                    onClearAll()
                },
            )

            Spacer(modifier = Modifier.width(dimensions().spacing16x))

            WirePrimaryButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.apply_label),
                onClick = { onApply(selectedChips) },
                state = WireButtonState.Default,
            )
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewFilterBottomSheet() {
    WireTheme {
        FilterBottomSheet(
            setOf("Android", "iOS", "Web", "QA"),
            emptySet(),
            onApply = {},
            onClearAll = {},
            onDismiss = {}
        )
    }
}
