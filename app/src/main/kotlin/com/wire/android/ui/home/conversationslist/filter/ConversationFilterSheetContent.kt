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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.wire.android.R
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ConversationFilter
import com.wire.kalium.logic.data.conversation.ConversationFolder

@Composable
fun ConversationFilterSheetContent(
    currentFilter: ConversationFilter,
    folders: List<ConversationFolder>,
    onChangeFilter: (ConversationFilter) -> Unit,
    isBottomSheetVisible: () -> Boolean = { true }
) {
    var currentTab: FilterTab by rememberSaveable { mutableStateOf(FilterTab.FILTERS) }
    val sheetData = ConversationFilterSheetData(currentTab, currentFilter, folders)
    when (currentTab) {
        FilterTab.FILTERS -> {
            ConversationFiltersSheetContent(
                sheetData = sheetData,
                onChangeFilter = onChangeFilter,
                showFoldersBottomSheet = {
                    currentTab = FilterTab.FOLDERS
                }
            )
        }

        FilterTab.FOLDERS -> {
            ConversationFoldersSheetContent(
                sheetData = sheetData,
                onChangeFolder = onChangeFilter,
                onBackClick = {
                    currentTab = FilterTab.FILTERS
                }
            )
        }
    }

    BackHandler(currentTab == FilterTab.FOLDERS && isBottomSheetVisible()) {
        currentTab = FilterTab.FILTERS
    }
}

fun ConversationFilter.toSheetItemLabel(): UIText = when (this) {
    ConversationFilter.All -> UIText.StringResource(R.string.label_filter_all)
    ConversationFilter.Favorites -> UIText.StringResource(R.string.label_filter_favorites)
    ConversationFilter.Groups -> UIText.StringResource(R.string.label_filter_group)
    ConversationFilter.OneOnOne -> UIText.StringResource(R.string.label_filter_one_on_one)
    ConversationFilter.Channels -> UIText.StringResource(R.string.label_filter_channels)
    is ConversationFilter.Folder -> UIText.DynamicString(this.folderName)
}

fun ConversationFilter.toTopBarTitle(): UIText = when (this) {
    ConversationFilter.All -> UIText.StringResource(R.string.conversations_screen_title)
    ConversationFilter.Favorites -> UIText.StringResource(R.string.label_filter_favorites)
    ConversationFilter.Groups -> UIText.StringResource(R.string.label_filter_group)
    ConversationFilter.OneOnOne -> UIText.StringResource(R.string.label_filter_one_on_one)
    ConversationFilter.Channels -> UIText.StringResource(R.string.label_filter_channels)
    is ConversationFilter.Folder -> UIText.DynamicString(this.folderName)
}
