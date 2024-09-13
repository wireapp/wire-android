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

package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.util.extension.folderWithElements
import com.wire.android.util.ui.KeepOnTopWhenNotScrolled
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.ImmutableMap

@Suppress("LongParameterList")
@Composable
fun ConversationList(
    conversationListItems: ImmutableMap<ConversationFolder, List<ConversationItem>>,
    searchQuery: String,
    onOpenConversation: (ConversationId) -> Unit,
    onEditConversation: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    onJoinCall: (ConversationId) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    isSelectableList: Boolean = false,
    conversationsAddedToGroup: List<ConversationItem> = emptyList(),
    onConversationSelectedOnRadioGroup: (ConversationId) -> Unit = {},
    onAudioPermissionPermanentlyDenied: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize()
    ) {
        conversationListItems.forEach { (conversationFolder, conversationList) ->
            folderWithElements(
                header = when (conversationFolder) {
                    is ConversationFolder.Predefined -> context.getString(conversationFolder.folderNameResId)
                    is ConversationFolder.Custom -> conversationFolder.folderName
                    is ConversationFolder.WithoutHeader -> null
                },
                items = conversationList.associateBy {
                    it.conversationId.toString()
                }
            ) { generalConversation ->
                ConversationItemFactory(
                    searchQuery = searchQuery,
                    conversation = generalConversation,
                    isSelectableItem = isSelectableList,
                    isChecked = conversationsAddedToGroup.contains(generalConversation),
                    onConversationSelectedOnRadioGroup = { onConversationSelectedOnRadioGroup(generalConversation.conversationId) },
                    openConversation = onOpenConversation,
                    openMenu = onEditConversation,
                    openUserProfile = onOpenUserProfile,
                    joinCall = onJoinCall,
                    onAudioPermissionPermanentlyDenied = onAudioPermissionPermanentlyDenied,
                )
            }
        }
    }

    KeepOnTopWhenNotScrolled(lazyListState)
}
