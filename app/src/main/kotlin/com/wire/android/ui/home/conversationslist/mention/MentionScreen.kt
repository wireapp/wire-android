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

package com.wire.android.ui.home.conversationslist.mention

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
fun MentionScreen(homeStateHolder: HomeStateHolder) {
    with(homeStateHolder) {
        ConversationRouterHomeBridge(
            navigator = navigator,
            conversationItemType = ConversationItemType.MENTIONS,
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
fun MentionScreenContent(
    unreadMentions: List<ConversationItem> = emptyList(),
    allMentions: List<ConversationItem> = emptyList(),
    onMentionItemClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    openConversationNotificationsSettings: (ConversationItem) -> Unit
) {
    val lazyListState = rememberLazyListState()

    MentionContent(
        lazyListState = lazyListState,
        unreadMentions = unreadMentions,
        allMentions = allMentions,
        onMentionItemClick = onMentionItemClick,
        onEditConversationItem = onEditConversationItem,
        onOpenUserProfile = onOpenUserProfile,
        openConversationNotificationsSettings = openConversationNotificationsSettings,
    )
}

@Composable
private fun MentionContent(
    lazyListState: LazyListState,
    unreadMentions: List<ConversationItem>,
    allMentions: List<ConversationItem>,
    onMentionItemClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    openConversationNotificationsSettings: (ConversationItem) -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        folderWithElements(
            header = context.getString(R.string.mention_label_unread_mentions),
            items = unreadMentions.associateBy { it.conversationId.toString() }
        ) { unreadMention ->
            ConversationItemFactory(
                conversation = unreadMention,
                openConversation = onMentionItemClick,
                openMenu = onEditConversationItem,
                openUserProfile = onOpenUserProfile,
                joinCall = {},
                onPermissionPermanentlyDenied = {},
                searchQuery = ""
            )
        }

        folderWithElements(
            header = context.getString(R.string.mention_label_all_mentions),
            items = allMentions.associateBy { it.conversationId.toString() }
        ) { mention ->
            ConversationItemFactory(
                conversation = mention,
                openConversation = onMentionItemClick,
                openMenu = onEditConversationItem,
                openUserProfile = onOpenUserProfile,
                joinCall = {},
                onPermissionPermanentlyDenied = {},
                searchQuery = ""
            )
        }
    }
}
