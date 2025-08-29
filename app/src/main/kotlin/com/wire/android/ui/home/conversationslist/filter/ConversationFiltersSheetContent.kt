/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversationslist.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.RichMenuItemState
import com.wire.android.ui.common.bottomsheet.SelectableMenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.dimensions
import com.wire.kalium.logic.data.conversation.Filter

@Composable
fun ConversationFiltersSheetContent(
    sheetData: ConversationFilterSheetData,
    onChangeFilter: (Filter.Conversation) -> Unit,
    showFoldersBottomSheet: (selectedFolderId: String?) -> Unit
) {
    WireMenuModalSheetContent(
        header = MenuModalSheetHeader.Visible(
            title = stringResource(R.string.label_filter_conversations),
            customVerticalPadding = dimensions().spacing8x
        ),
        menuItems = buildList<@Composable () -> Unit> {
            add {
                val state = if (Filter.Conversation.All == sheetData.currentFilter) {
                    RichMenuItemState.SELECTED
                } else {
                    RichMenuItemState.DEFAULT
                }
                SelectableMenuBottomSheetItem(
                    title = Filter.Conversation.All.toSheetItemLabel().asString(),
                    onItemClick = Clickable(
                        enabled = state == RichMenuItemState.DEFAULT,
                        onClickDescription = stringResource(id = R.string.content_description_select_label),
                        onClick = { onChangeFilter(Filter.Conversation.All) },
                    ),
                    state = state
                )
            }
            add {
                val state = if (Filter.Conversation.Favorites == sheetData.currentFilter) {
                    RichMenuItemState.SELECTED
                } else {
                    RichMenuItemState.DEFAULT
                }
                SelectableMenuBottomSheetItem(
                    title = Filter.Conversation.Favorites.toSheetItemLabel().asString(),
                    onItemClick = Clickable(
                        enabled = state == RichMenuItemState.DEFAULT,
                        onClickDescription = stringResource(id = R.string.content_description_select_label),
                        onClick = { onChangeFilter(Filter.Conversation.Favorites) },
                    ),
                    state = state
                )
            }
            add {
                val state = if (Filter.Conversation.Channels == sheetData.currentFilter) {
                    RichMenuItemState.SELECTED
                } else {
                    RichMenuItemState.DEFAULT
                }
                SelectableMenuBottomSheetItem(
                    title = Filter.Conversation.Channels.toSheetItemLabel().asString(),
                    onItemClick = Clickable(
                        enabled = state == RichMenuItemState.DEFAULT,
                        onClickDescription = stringResource(id = R.string.content_description_select_label),
                        onClick = { onChangeFilter(Filter.Conversation.Channels) },
                    ),
                    state = state
                )
            }
            add {
                val state = if (Filter.Conversation.Groups == sheetData.currentFilter) {
                    RichMenuItemState.SELECTED
                } else {
                    RichMenuItemState.DEFAULT
                }
                SelectableMenuBottomSheetItem(
                    title = Filter.Conversation.Groups.toSheetItemLabel().asString(),
                    onItemClick = Clickable(
                        enabled = state == RichMenuItemState.DEFAULT,
                        onClickDescription = stringResource(id = R.string.content_description_select_label),
                        onClick = { onChangeFilter(Filter.Conversation.Favorites) },
                    ),
                    state = state
                )
            }
            add {
                val state = if (Filter.Conversation.OneOnOne == sheetData.currentFilter) {
                    RichMenuItemState.SELECTED
                } else {
                    RichMenuItemState.DEFAULT
                }
                SelectableMenuBottomSheetItem(
                    title = Filter.Conversation.OneOnOne.toSheetItemLabel().asString(),
                    onItemClick = Clickable(
                        enabled = state == RichMenuItemState.DEFAULT,
                        onClickDescription = stringResource(id = R.string.content_description_select_label),
                        onClick = { onChangeFilter(Filter.Conversation.Favorites) },
                    ),
                    state = state
                )
            }
            add {
                val state = if (sheetData.currentFilter is Filter.Conversation.Folder) {
                    RichMenuItemState.SELECTED
                } else {
                    RichMenuItemState.DEFAULT
                }
                SelectableMenuBottomSheetItem(
                    title = stringResource(R.string.label_filter_folders),
                    description = (sheetData.currentFilter as? Filter.Conversation.Folder)?.folderName,
                    onItemClick = Clickable(
                        enabled = true,
                        onClickDescription = stringResource(id = R.string.content_description_select_label),
                        onClick = { showFoldersBottomSheet((sheetData.currentFilter as? Filter.Conversation.Folder)?.folderId) },
                    ),
                    state = state
                )
            }
        }
    )
}
