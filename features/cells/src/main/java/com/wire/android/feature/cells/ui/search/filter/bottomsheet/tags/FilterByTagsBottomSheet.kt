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
package com.wire.android.feature.cells.ui.search.filter.bottomsheet.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.search.filter.data.FilterTagUi
import com.wire.android.ui.common.SearchBarInput
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.chip.WireFilterChip
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import kotlinx.coroutines.launch
import com.wire.android.ui.common.R as CommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterByTagsBottomSheet(
    sheetState: SheetState,
    items: List<FilterTagUi>,
    onDismiss: () -> Unit,
    onSave: (List<FilterTagUi>) -> Unit,
    onRemoveAll: () -> Unit,
    modifier: Modifier = Modifier
) {

    val scope = rememberCoroutineScope()

    val state = rememberTagsFilterSheetState(items)

    val searchState = remember { TextFieldState() }
    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }
            .collect(state::onQueryChange)
    }

    fun dismiss() {
        scope.launch { sheetState.hide() }
            .invokeOnCompletion { onDismiss() }
    }
    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = ::dismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(horizontal = dimensions().spacing16x)
        ) {
            Text(
                text = stringResource(R.string.bottom_sheet_title_filter_by_tags),
                style = typography().title02,
                modifier = Modifier.padding(
                    horizontal = dimensions().spacing4x,
                    vertical = dimensions().spacing12x
                )
            )

            SearchBarInput(
                placeholderText = stringResource(R.string.bottom_sheet_title_search_tags),
                textState = searchState,
                leadingIcon = {
                    Icon(
                        modifier = Modifier.padding(
                            start = dimensions().spacing12x,
                            end = dimensions().spacing12x
                        ),
                        painter = painterResource(CommonR.drawable.ic_search),
                        contentDescription = null,
                        tint = MaterialTheme.wireColorScheme.onBackground,
                    )
                },
            )

            Spacer(Modifier.height(dimensions().spacing12x))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState())
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
                ) {
                    state.filteredTags.forEach { tag ->
                        WireFilterChip(
                            label = tag.name,
                            isSelected = tag.selected,
                            onClick = { state.toggle(tag.id) }
                        )
                    }
                }

                Spacer(Modifier.height(dimensions().spacing12x))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensions().spacing12x),
                horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x)
            ) {
                WireSecondaryButton(
                    text = stringResource(R.string.button_remove_all_label),
                    onClick = {
                        state.removeAll()
                        onRemoveAll()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = dimensions().spacing14x)
                )

                WirePrimaryButton(
                    text = stringResource(R.string.save_label),
                    onClick = { onSave(state.selectedTags()) },
                    modifier = Modifier.weight(1f),
                    state = if (state.hasChanges) WireButtonState.Default else WireButtonState.Disabled,
                    contentPadding = PaddingValues(vertical = dimensions().spacing14x)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@MultipleThemePreviews
@Composable
fun PreviewFilterByTagsBottomSheet() {
    WireTheme {
        FilterByTagsBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            items = listOf(
                FilterTagUi("1", "Work", true),
                FilterTagUi("2", "Personal", true),
                FilterTagUi("3", "Important", true),
                FilterTagUi("4", "Later"),
                FilterTagUi("5", "Travel"),
                FilterTagUi("6", "Shopping"),
                FilterTagUi("7", "Fitness"),
                FilterTagUi("8", "Health"),
                FilterTagUi("11", "Work"),
                FilterTagUi("21", "Personal"),
                FilterTagUi("31", "Important"),
                FilterTagUi("41", "Later"),
                FilterTagUi("51", "Travel"),
                FilterTagUi("61", "Shopping"),
                FilterTagUi("71", "Fitness"),
                FilterTagUi("81", "Health"),
            ),
            onDismiss = {},
            onSave = {},
            onRemoveAll = {}
        )
    }
}
