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
fun ConversationFilterSheetContent(
    currentFilter: ConversationFilter,
    onChangeFilter: (ConversationFilter) -> Unit
) {
    WireMenuModalSheetContent(
        header = MenuModalSheetHeader.Visible(
            title = stringResource(R.string.label_filter_conversations),
            customVerticalPadding = dimensions().spacing8x
        ),
        menuItems = buildList<@Composable () -> Unit> {
            ConversationFilter.entries.forEach { filter ->
                add {
                    MenuBottomSheetItem(
                        title = stringResource(filter.getResource()),
                        trailing = {
                            if (filter == currentFilter) {
                                MenuItemIcon(
                                    id = R.drawable.ic_check_circle,
                                    contentDescription = stringResource(R.string.label_selected),
                                    tint = MaterialTheme.wireColorScheme.switchEnabledChecked,
                                )
                            }
                        },
                        onItemClick = { onChangeFilter(filter) },
                        onItemClickDescription = stringResource(R.string.content_description_select_label)
                    )
                }
            }
        }
    )
}

private fun ConversationFilter.getResource(): Int = when (this) {
    ConversationFilter.NONE -> R.string.label_filter_all
    ConversationFilter.FAVORITES -> R.string.label_filter_favorites
    ConversationFilter.GROUPS -> R.string.label_filter_group
    ConversationFilter.ONE_ON_ONE -> R.string.label_filter_one_on_one
}
