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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.kalium.logic.data.conversation.ConversationFilter

@Composable
fun ConversationFiltersSheetContent(
    sheetData: ConversationFilterSheetData,
    onChangeFilter: (ConversationFilter) -> Unit,
    showFoldersBottomSheet: (selectedFolderId: String?) -> Unit
) {
    WireMenuModalSheetContent(
        header = MenuModalSheetHeader.Visible(
            title = stringResource(R.string.label_filter_conversations),
            customVerticalPadding = dimensions().spacing8x
        ),
        menuItems = buildList<@Composable () -> Unit> {
            add {
                MenuBottomSheetItem(
                    title = ConversationFilter.All.uiText().asString(),
                    trailing = {
                        if (ConversationFilter.All == sheetData.currentFilter) {
                            MenuItemIcon(
                                id = R.drawable.ic_check_circle,
                                contentDescription = stringResource(R.string.label_selected),
                                tint = MaterialTheme.wireColorScheme.switchEnabledChecked,
                            )
                        }
                    },
                    onItemClick = { onChangeFilter(ConversationFilter.All) },
                    onItemClickDescription = stringResource(R.string.content_description_select_label)
                )
            }
            add {
                MenuBottomSheetItem(
                    title = ConversationFilter.Favorites.uiText().asString(),
                    trailing = {
                        if (ConversationFilter.Favorites == sheetData.currentFilter) {
                            MenuItemIcon(
                                id = R.drawable.ic_check_circle,
                                contentDescription = stringResource(R.string.label_selected),
                                tint = MaterialTheme.wireColorScheme.switchEnabledChecked,
                            )
                        }
                    },
                    onItemClick = { onChangeFilter(ConversationFilter.Favorites) },
                    onItemClickDescription = stringResource(R.string.content_description_select_label)
                )
            }
            add {
                MenuBottomSheetItem(
                    title = ConversationFilter.Groups.uiText().asString(),
                    trailing = {
                        if (ConversationFilter.Groups == sheetData.currentFilter) {
                            MenuItemIcon(
                                id = R.drawable.ic_check_circle,
                                contentDescription = stringResource(R.string.label_selected),
                                tint = MaterialTheme.wireColorScheme.switchEnabledChecked,
                            )
                        }
                    },
                    onItemClick = { onChangeFilter(ConversationFilter.Groups) },
                    onItemClickDescription = stringResource(R.string.content_description_select_label)
                )
            }
            add {
                MenuBottomSheetItem(
                    title = ConversationFilter.OneOnOne.uiText().asString(),
                    trailing = {
                        if (ConversationFilter.OneOnOne == sheetData.currentFilter) {
                            MenuItemIcon(
                                id = R.drawable.ic_check_circle,
                                contentDescription = stringResource(R.string.label_selected),
                                tint = MaterialTheme.wireColorScheme.switchEnabledChecked,
                            )
                        }
                    },
                    onItemClick = { onChangeFilter(ConversationFilter.OneOnOne) },
                    onItemClickDescription = stringResource(R.string.content_description_select_label)
                )
            }
            add {
                MenuBottomSheetItem(
                    title = if (sheetData.currentFilter is ConversationFilter.Folder) {
                        stringResource(R.string.label_filter_folders, sheetData.currentFilter.folderName) // TODO color selected one
                    } else {
                        stringResource(R.string.label_filter_folders, "")
                    },
                    trailing = {
                        if (sheetData.currentFilter is ConversationFilter.Folder) {
                            MenuItemIcon(
                                id = R.drawable.ic_check_circle,
                                contentDescription = stringResource(R.string.label_selected),
                                tint = MaterialTheme.wireColorScheme.switchEnabledChecked,
                            )
                        }
                    },
                    onItemClick = { showFoldersBottomSheet((sheetData.currentFilter as? ConversationFilter.Folder)?.folderId) },
                    onItemClickDescription = stringResource(R.string.content_description_select_label)
                )
            }
        }
    )
}
