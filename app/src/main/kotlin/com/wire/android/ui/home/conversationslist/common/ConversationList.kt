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
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.extension.folderWithElements
import com.wire.android.util.ui.KeepOnTopWhenNotScrolled
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentMap

@Suppress("LongParameterList")
@Composable
fun ConversationList(
    conversationListItems: ImmutableMap<ConversationFolder, List<ConversationItem>>,
    searchQuery: String,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    isSelectableList: Boolean = false,
    selectedConversations: List<ConversationItem> = emptyList(),
    onOpenConversation: (ConversationId) -> Unit = {},
    onEditConversation: (ConversationItem) -> Unit = {},
    onOpenUserProfile: (UserId) -> Unit = {},
    onJoinCall: (ConversationId) -> Unit = {},
    onConversationSelectedOnRadioGroup: (ConversationId) -> Unit = {},
    onAudioPermissionPermanentlyDenied: () -> Unit = {}
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
                    isChecked = selectedConversations.contains(generalConversation),
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

fun previewConversationList(count: Int, startIndex: Int = 0, unread: Boolean = false) = buildList {
    repeat(count) { index ->
        val currentIndex = startIndex + index
        when (index % 2) {
            0 -> add(
                ConversationItem.GroupConversation(
                    groupName = "Conversation $currentIndex",
                    conversationId = QualifiedID(currentIndex.toString(), "domain"),
                    mutedStatus = MutedConversationStatus.AllAllowed,
                    lastMessageContent = UILastMessageContent.TextMessage(MessageBody(UIText.DynamicString("Message"))),
                    badgeEventType = if (unread) BadgeEventType.UnreadMessage(1) else BadgeEventType.None,
                    selfMemberRole = null,
                    teamId = null,
                    hasOnGoingCall = false,
                    isArchived = false,
                    mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
                    proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED
                )
            )

            1 -> add(
                ConversationItem.PrivateConversation(
                    userAvatarData = UserAvatarData(),
                    conversationId = QualifiedID(currentIndex.toString(), "domain"),
                    mutedStatus = MutedConversationStatus.AllAllowed,
                    lastMessageContent = UILastMessageContent.TextMessage(MessageBody(UIText.DynamicString("Message"))),
                    badgeEventType = if (unread) BadgeEventType.UnreadMessage(1) else BadgeEventType.None,
                    conversationInfo = ConversationInfo("User $currentIndex"),
                    blockingState = BlockingState.BLOCKED,
                    teamId = null,
                    userId = UserId("userId_$currentIndex", "domain"),
                    isArchived = false,
                    mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
                    proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED
                )
            )
        }
    }
}.toImmutableList()

@Suppress("MagicNumber")
fun previewConversationFolders() = buildMap<ConversationFolder, List<ConversationItem>> {
    put(ConversationFolder.Predefined.NewActivities, previewConversationList(3, 0, true))
    put(ConversationFolder.Predefined.Conversations, previewConversationList(6, 3, false))
}.toImmutableMap()

@PreviewMultipleThemes
@Composable
fun PreviewConversationList() = WireTheme {
    ConversationList(
        conversationListItems = previewConversationFolders().toPersistentMap(),
        searchQuery = "",
        isSelectableList = false,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewConversationListSearch() = WireTheme {
    ConversationList(
        conversationListItems = previewConversationFolders(),
        searchQuery = "er",
        isSelectableList = false,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewConversationListSelect() = WireTheme {
    val conversationFolders = previewConversationFolders()
    ConversationList(
        conversationListItems = conversationFolders,
        searchQuery = "",
        isSelectableList = true,
        selectedConversations = conversationFolders.values.flatten().filterIndexed { index, _ -> index % 3 == 0 },
    )
}
