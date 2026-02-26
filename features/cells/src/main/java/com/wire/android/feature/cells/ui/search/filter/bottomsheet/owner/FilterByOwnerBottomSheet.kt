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
package com.wire.android.feature.cells.ui.search.filter.bottomsheet.owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.search.filter.data.FilterOwnerUi
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.SearchBarInput
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import kotlinx.coroutines.launch
import com.wire.android.ui.common.R as CommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterByOwnerBottomSheet(
    sheetState: SheetState,
    items: List<FilterOwnerUi>,
    onDismiss: () -> Unit,
    onSave: (List<FilterOwnerUi>) -> Unit,
    onRemoveAll: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val scope = rememberCoroutineScope()
    val state = rememberOwnersFilterSheetState(items)

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
        onDismissRequest = ::dismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
        ) {
            Text(
                text = stringResource(R.string.bottom_sheet_title_filter_by_owner),
                style = typography().title02,
                modifier = Modifier.padding(
                    horizontal = dimensions().spacing16x,
                    vertical = dimensions().spacing12x
                )
            )

            SearchBarInput(
                modifier = Modifier.padding(start = dimensions().spacing16x, end = dimensions().spacing16x),
                placeholderText = stringResource(R.string.search_owners_text_input_hint),
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentPadding = PaddingValues(top = dimensions().spacing8x, bottom = dimensions().spacing8x)
            ) {
                items(
                    items = state.filteredOwners,
                    key = { it.id }
                ) { owner ->
                    OwnerRow(
                        owner = owner,
                        onToggle = { state.toggleOwner(owner.id) },
                    )
                    HorizontalDivider()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensions().spacing16x,
                        end = dimensions().spacing16x,
                        bottom = dimensions().spacing12x
                    ),
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
                    onClick = { onSave(state.selectedOwners()) },
                    modifier = Modifier.weight(1f),
                    state = if (state.hasChanges) WireButtonState.Default else WireButtonState.Disabled,
                    contentPadding = PaddingValues(vertical = dimensions().spacing14x)
                )
            }
        }
    }
}

@Composable
private fun OwnerRow(
    owner: FilterOwnerUi,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(
                start = dimensions().spacing12x,
                end = dimensions().spacing12x,
                top = dimensions().spacing8x,
                bottom = dimensions().spacing8x
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserProfileAvatar(avatarData = UserAvatarData(owner.userAvatarAsset))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = owner.displayName,
                style = typography().body01,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.wireColorScheme.onSurface
            )
            Text(
                text = owner.handle,
                style = typography().label04,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.wireColorScheme.secondaryText
            )
        }
        Checkbox(
            checked = owner.selected,
            onCheckedChange = { onToggle() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@MultipleThemePreviews
@Composable
fun PreviewFilterByOwnerBottomSheet() {
    WireTheme {
        FilterByOwnerBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            items = listOf(
                FilterOwnerUi(
                    id = "1",
                    displayName = "John Doe",
                    handle = "@johndoe",
                    selected = true
                ),
                FilterOwnerUi(
                    id = "2",
                    displayName = "Jane Smith with very long name that should be truncated",
                    handle = "@janesmith",
                    selected = false
                ),
                FilterOwnerUi(
                    id = "3",
                    displayName = "Alice Johnson",
                    handle = "@alicejohnson",
                    selected = false
                )
            ),
            onDismiss = {},
            onSave = {},
            onRemoveAll = {}
        )
    }
}
