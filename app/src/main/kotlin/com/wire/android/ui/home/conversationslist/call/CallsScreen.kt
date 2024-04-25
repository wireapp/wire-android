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

package com.wire.android.ui.home.conversationslist.call

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.R
import com.wire.android.navigation.HomeNavGraph
import com.wire.android.ui.home.HomeStateHolder
import com.wire.android.ui.home.conversationslist.ConversationItemType
import com.wire.android.ui.home.conversationslist.ConversationRouterHomeBridge
import com.wire.android.ui.home.conversationslist.common.ConversationItemFactory
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.util.extension.folderWithElements
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@HomeNavGraph
@Destination
@Composable
fun CallsScreen(homeStateHolder: HomeStateHolder) {
    with(homeStateHolder) {
        ConversationRouterHomeBridge(
            navigator = navigator,
            conversationItemType = ConversationItemType.CALLS,
            onHomeBottomSheetContentChanged = ::changeBottomSheetContent,
            onOpenBottomSheet = ::openBottomSheet,
            onCloseBottomSheet = ::closeBottomSheet,
            onSnackBarStateChanged = ::setSnackBarState,
            searchBarState = searchBarState,
            isBottomSheetVisible = ::isBottomSheetVisible
        )
    }
}

@Composable
fun CallsScreenContent(
    missedCalls: List<ConversationItem> = emptyList(),
    callHistory: List<ConversationItem> = emptyList(),
    onCallItemClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit
) {
    val lazyListState = rememberLazyListState()

    CallContent(
        lazyListState = lazyListState,
        missedCalls = missedCalls,
        callHistory = callHistory,
        onCallItemClick = onCallItemClick,
        onEditConversationItem = onEditConversationItem,
        onOpenUserProfile = onOpenUserProfile
    )
}

@Composable
fun CallContent(
    lazyListState: LazyListState,
    missedCalls: List<ConversationItem>,
    callHistory: List<ConversationItem>,
    onCallItemClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        folderWithElements(
            header = context.getString(R.string.calls_label_missed_calls),
            items = missedCalls.associateBy { it.conversationId.toString() }
        ) { missedCall ->
            ConversationItemFactory(
                conversation = missedCall,
                openConversation = onCallItemClick,
                openMenu = onEditConversationItem,
                openUserProfile = onOpenUserProfile,
                joinCall = { },
                onPermissionPermanentlyDenied = {},
                searchQuery = ""
            )
        }

        folderWithElements(
            header = context.getString(R.string.calls_label_calls_history),
            items = callHistory.associateBy { it.conversationId.toString() }
        ) { callHistory ->
            ConversationItemFactory(
                conversation = callHistory,
                openConversation = onCallItemClick,
                openMenu = onEditConversationItem,
                openUserProfile = onOpenUserProfile,
                joinCall = { },
                onPermissionPermanentlyDenied = {},
                searchQuery = " "
            )
        }
    }
}
