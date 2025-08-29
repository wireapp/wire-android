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

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.wire.android.R
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Filter

@Composable
fun ConversationFilterSheetContent(
    filterSheetState: ConversationFilterSheetState,
    onChangeFilter: (Filter.Conversation) -> Unit,
    isBottomSheetVisible: () -> Boolean = { true }
) {
    when (filterSheetState.currentData.tab) {
        FilterTab.FILTERS -> {
            ConversationFiltersSheetContent(
                sheetData = filterSheetState.currentData,
                onChangeFilter = onChangeFilter,
                showFoldersBottomSheet = {
                    filterSheetState.toFolders()
                }
            )
        }

        FilterTab.FOLDERS -> {
            ConversationFoldersSheetContent(
                sheetData = filterSheetState.currentData,
                onChangeFolder = onChangeFilter,
                onBackClick = {
                    filterSheetState.toFilters()
                }
            )
        }
    }

    BackHandler(
        filterSheetState.currentData.tab == FilterTab.FOLDERS
                && isBottomSheetVisible()
    ) {
        filterSheetState.toFilters()
    }
}

@Composable
fun rememberFilterSheetState(
    filterSheetData: ConversationFilterSheetData,
): ConversationFilterSheetState {
    return remember(filterSheetData) {
        ConversationFilterSheetState(
            filterSheetData = filterSheetData
        )
    }
}

fun Filter.Conversation.toSheetItemLabel(): UIText = when (this) {
    Filter.Conversation.All -> UIText.StringResource(R.string.label_filter_all)
    Filter.Conversation.Favorites -> UIText.StringResource(R.string.label_filter_favorites)
    Filter.Conversation.Groups -> UIText.StringResource(R.string.label_filter_group)
    Filter.Conversation.OneOnOne -> UIText.StringResource(R.string.label_filter_one_on_one)
    Filter.Conversation.Channels -> UIText.StringResource(R.string.label_filter_channels)
    is Filter.Conversation.Folder -> UIText.DynamicString(this.folderName)
}

fun Filter.Conversation.toTopBarTitle(): UIText = when (this) {
    Filter.Conversation.All -> UIText.StringResource(R.string.conversations_screen_title)
    Filter.Conversation.Favorites -> UIText.StringResource(R.string.label_filter_favorites)
    Filter.Conversation.Groups -> UIText.StringResource(R.string.label_filter_group)
    Filter.Conversation.OneOnOne -> UIText.StringResource(R.string.label_filter_one_on_one)
    Filter.Conversation.Channels -> UIText.StringResource(R.string.label_filter_channels)
    is Filter.Conversation.Folder -> UIText.DynamicString(this.folderName)
}
