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
package com.wire.android.feature.cells.ui.search.filter.bottomsheet.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
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
import com.wire.android.feature.cells.ui.search.filter.data.FilterConversationUi
import com.wire.android.ui.common.SearchBarInput
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversationslist.common.ChannelConversationAvatar
import com.wire.android.ui.home.conversationslist.common.RegularGroupConversationAvatar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.launch
import com.wire.android.ui.common.R as CommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterByConversationBottomSheet(
    sheetState: WireModalSheetState<Unit>,
    conversations: List<FilterConversationUi>,
    onDismiss: () -> Unit,
    onRemoveAll: () -> Unit,
    onSave: (List<FilterConversationUi>) -> Unit,
    modifier: Modifier = Modifier,
) {

    val state = rememberConversationFilterSheetState(conversations)
    val scope = rememberCoroutineScope()

    val searchState = remember { TextFieldState() }
    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }
            .collect(state::onQueryChange)
    }

    fun dismiss() {
        scope.launch { sheetState.hide() }
            .invokeOnCompletion { onDismiss() }
    }

    WireModalSheetLayout(
        onDismissRequest = ::dismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Text(
                text = stringResource(R.string.bottom_sheet_title_filter_by_conversation),
                style = typography().title02,
                modifier = Modifier.padding(
                    horizontal = dimensions().spacing16x,
                    vertical = dimensions().spacing12x
                )
            )

            SearchBarInput(
                modifier = Modifier.padding(start = dimensions().spacing16x, end = dimensions().spacing16x),
                placeholderText = stringResource(R.string.search_conversations_text_input_hint),
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
                    items = state.filteredConversations,
                ) { item ->
                    ConversationRow(
                        item = item,
                        onClick = { state.selectConversation(item.id.toString()) }
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
                    onClick = { onSave(state.selectedConversation()) },
                    modifier = Modifier.weight(1f),
                    state = if (state.hasChanges) WireButtonState.Default else WireButtonState.Disabled,
                    contentPadding = PaddingValues(vertical = dimensions().spacing14x)
                )
            }
        }
    }
}

@Composable
private fun ConversationRow(
    item: FilterConversationUi,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() }
            .padding(
                start = dimensions().spacing12x,
                end = dimensions().spacing12x,
                top = dimensions().spacing8x,
                bottom = dimensions().spacing8x
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.isChannel) {
            ChannelConversationAvatar(
                conversationId = item.id,
                isPrivateChannel = item.isPrivateChannel,
            )
        } else {
            RegularGroupConversationAvatar(
                conversationId = item.id,
                cornerRadius = dimensions().groupAvatarConversationTopBarCornerRadius,
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name,
                style = typography().body01,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.wireColorScheme.onSurface
            )
        }

        RadioButton(
            selected = item.selected,
            onClick = onClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@MultipleThemePreviews
@Composable
fun PreviewFilterByConversationBottomSheet() {
    WireTheme {
        FilterByConversationBottomSheet(
            sheetState = rememberWireModalSheetState<Unit>(WireSheetValue.Expanded(Unit)),
            conversations = listOf(
                FilterConversationUi(id = ConversationId("1", "d"), name = "Conversation 1", selected = false),
                FilterConversationUi(id = ConversationId("2", "d"), name = "Conversation 2", selected = true),
                FilterConversationUi(id = ConversationId("3", "d"), name = "Conversation 3", selected = false),
            ),
            onDismiss = {},
            onRemoveAll = {},
            onSave = {}
        )
    }
}
