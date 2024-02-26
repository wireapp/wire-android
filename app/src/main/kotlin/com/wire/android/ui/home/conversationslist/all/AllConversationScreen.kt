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

package com.wire.android.ui.home.conversationslist.all

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.HomeNavGraph
import com.wire.android.ui.common.dialogs.calling.JoinAnywayDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.HomeStateHolder
import com.wire.android.ui.home.archive.ArchivedConversationsEmptyStateScreen
import com.wire.android.ui.home.conversationslist.ConversationItemType
import com.wire.android.ui.home.conversationslist.ConversationListViewModel
import com.wire.android.ui.home.conversationslist.ConversationRouterHomeBridge
import com.wire.android.ui.home.conversationslist.common.ConversationList
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.permission.PermissionDenialType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@HomeNavGraph(start = true)
@Destination
@Composable
fun AllConversationScreen(homeStateHolder: HomeStateHolder) {
    with(homeStateHolder) {
        ConversationRouterHomeBridge(
            navigator = navigator,
            conversationItemType = ConversationItemType.ALL_CONVERSATIONS,
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
fun AllConversationScreenContent(
    conversations: ImmutableMap<ConversationFolder, List<ConversationItem>>,
    hasNoConversations: Boolean,
    isFromArchive: Boolean = false,
    viewModel: ConversationListViewModel = hiltViewModel(),
    onEditConversation: (ConversationItem) -> Unit,
    onOpenConversation: (ConversationId) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    onJoinedCall: (ConversationId) -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val callConversationIdToJoin = remember { mutableStateOf(ConversationId("", "")) }

    if (viewModel.conversationListCallState.shouldShowJoinAnywayDialog) {
        appLogger.i("$TAG showing showJoinAnywayDialog..")
        JoinAnywayDialog(
            onDismiss = viewModel::dismissJoinCallAnywayDialog,
            onConfirm = { viewModel.joinAnyway(callConversationIdToJoin.value, onJoinedCall) }
        )
    }
    if (hasNoConversations) {
        if (isFromArchive) {
            ArchivedConversationsEmptyStateScreen()
        } else {
            ConversationListEmptyStateScreen()
        }
    } else {
        ConversationList(
            lazyListState = lazyListState,
            conversationListItems = conversations,
            searchQuery = "",
            onOpenConversation = onOpenConversation,
            onEditConversation = onEditConversation,
            onOpenUserProfile = onOpenUserProfile,
            onJoinCall = {
                callConversationIdToJoin.value = it
                viewModel.joinOngoingCall(it, onJoinedCall)
            },
            onPermissionPermanentlyDenied = onPermissionPermanentlyDenied
        )
    }
}

@Composable
fun ConversationListEmptyStateScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                dimensions().spacing40x
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(
                bottom = dimensions().spacing24x,
                top = dimensions().spacing100x
            ),
            text = stringResource(R.string.conversation_empty_list_title),
            style = MaterialTheme.wireTypography.title01,
            color = MaterialTheme.wireColorScheme.onSurface,
        )
        Text(
            modifier = Modifier.padding(bottom = dimensions().spacing8x),
            text = stringResource(R.string.conversation_empty_list_description),
            style = MaterialTheme.wireTypography.body01,
            textAlign = TextAlign.Center,
            color = MaterialTheme.wireColorScheme.onSurface,
        )
        Image(
            modifier = Modifier.padding(start = dimensions().spacing100x),
            painter = painterResource(
                id = R.drawable.ic_empty_conversation_arrow
            ),
            contentDescription = ""
        )
    }
}

@Preview
@Composable
fun PreviewAllConversationScreen() {
    AllConversationScreenContent(
        conversations = persistentMapOf(),
        hasNoConversations = false,
        onEditConversation = {},
        onOpenConversation = {},
        onOpenUserProfile = {},
        onJoinedCall = {},
        onPermissionPermanentlyDenied = {}
    )
}

@Preview
@Composable
fun ConversationListEmptyStateScreenPreview() {
    AllConversationScreenContent(
        conversations = persistentMapOf(),
        hasNoConversations = true,
        onEditConversation = {},
        onOpenConversation = {},
        onOpenUserProfile = {},
        onJoinedCall = {},
        onPermissionPermanentlyDenied = {}
    )
}

private const val TAG = "AllConversationScreen"
