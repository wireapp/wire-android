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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationFolderItem
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.extension.folderWithElements
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.keepOnTopWhenNotScrolled
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOf

@Suppress("LongParameterList", "CyclomaticComplexMethod")
@Composable
fun ConversationList(
    lazyPagingConversations: LazyPagingItems<ConversationFolderItem>,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    isSelectableList: Boolean = false,
    selectedConversations: List<ConversationId> = emptyList(),
    onOpenConversation: (ConversationItem) -> Unit = {},
    onEditConversation: (ConversationItem) -> Unit = {},
    onOpenUserProfile: (UserId) -> Unit = {},
    onJoinCall: (ConversationId) -> Unit = {},
    onConversationSelectedOnRadioGroup: (ConversationItem) -> Unit = {},
    onAudioPermissionPermanentlyDenied: () -> Unit = {},
    onPlayPauseCurrentAudio: () -> Unit = { },
    onStopCurrentAudio: () -> Unit = {},
    onBrowsePublicChannels: () -> Unit = {}
) {
    val context = LocalContext.current

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize()
    ) {
        items(
            count = lazyPagingConversations.itemCount,
            key = lazyPagingConversations.itemKey {
                when (it) {
                    is ConversationFolder.Predefined -> "folder_predefined_${context.getString(it.folderNameResId)}"
                    is ConversationFolder.Custom -> "folder_custom_${it.folderName}"
                    is ConversationFolder.WithoutHeader -> "folder_without_header"
                    is ConversationItem -> it.conversationId.toString()
                }
            },
            contentType = lazyPagingConversations.itemContentType {
                when (it) {
                    is ConversationFolder -> ConversationFolderItem::class.simpleName
                    is ConversationItem -> ConversationItem::class.simpleName
                }
            }
        ) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { // for some reasons animateItem doesn't work with LazyPagingItems and shows empty composables on previews
                        if (LocalInspectionMode.current) it else it.animateItem()
                    }
            ) {
                val item = lazyPagingConversations[index]
                if (BuildConfig.PUBLIC_CHANNELS_ENABLED &&
                    item is ConversationFolder.Predefined.BrowseChannels
                ) { // add a flag to public channels based on compile time flag
                    BrowsePublicChannelsItem(onBrowsePublicChannels)
                }
                when (item) {
                    is ConversationFolder -> when (item) {
                        is ConversationFolder.Predefined -> FolderHeader(context.getString(item.folderNameResId))
                        is ConversationFolder.Custom -> FolderHeader(item.folderName)
                        is ConversationFolder.WithoutHeader -> {}
                    }

                    is ConversationItem ->
                        ConversationItemFactory(
                            conversation = item,
                            isSelectableItem = isSelectableList,
                            isChecked = selectedConversations.contains(item.conversationId),
                            onConversationSelectedOnRadioGroup = { onConversationSelectedOnRadioGroup(item) },
                            openConversation = onOpenConversation,
                            openMenu = onEditConversation,
                            openUserProfile = onOpenUserProfile,
                            joinCall = onJoinCall,
                            onAudioPermissionPermanentlyDenied = onAudioPermissionPermanentlyDenied,
                            onPlayPauseCurrentAudio = onPlayPauseCurrentAudio,
                            onStopCurrentAudio = onStopCurrentAudio
                        )

                    else -> {}
                }
            }
        }
        Snapshot.withoutReadObservation {
            keepOnTopWhenNotScrolled(lazyListState)
        }
    }
}

@Composable
private fun BrowsePublicChannelsItem(onBrowsePublicChannels: () -> Unit = {}) {
    RowItemTemplate(
        modifier = Modifier.padding(horizontal = dimensions().spacing8x),
        leadingIcon = {
            Icon(
                modifier = Modifier.size(dimensions().systemMessageIconSize),
                painter = painterResource(id = R.drawable.ic_channel),
                contentDescription = null,
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.label_browse_public_channels),
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier
                    .padding(
                        start = dimensions().spacing4x,
                        end = dimensions().spacing4x
                    )
            )
        },
        clickable = Clickable { onBrowsePublicChannels.invoke() },
        actions = {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
            ) {
                ArrowRightIcon(
                    modifier = Modifier.align(Alignment.TopEnd),
                    contentDescription = R.string.content_description_empty
                )
            }
        }
    )
}

@Deprecated("This is old version without pagination")
@Suppress("LongParameterList")
@Composable
fun ConversationList(
    conversationListItems: ImmutableMap<ConversationFolder, List<ConversationItem>>,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    isSelectableList: Boolean = false,
    selectedConversations: List<ConversationItem> = emptyList(),
    onOpenConversation: (ConversationItem) -> Unit = {},
    onEditConversation: (ConversationItem) -> Unit = {},
    onOpenUserProfile: (UserId) -> Unit = {},
    onJoinCall: (ConversationId) -> Unit = {},
    onConversationSelectedOnRadioGroup: (ConversationId) -> Unit = {},
    onAudioPermissionPermanentlyDenied: () -> Unit = {},
    onPlayPauseCurrentAudio: () -> Unit = { },
    onStopCurrentAudio: () -> Unit = {}
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
                    conversation = generalConversation,
                    isSelectableItem = isSelectableList,
                    isChecked = selectedConversations.contains(generalConversation),
                    onConversationSelectedOnRadioGroup = { onConversationSelectedOnRadioGroup(generalConversation.conversationId) },
                    openConversation = onOpenConversation,
                    openMenu = onEditConversation,
                    openUserProfile = onOpenUserProfile,
                    joinCall = onJoinCall,
                    onAudioPermissionPermanentlyDenied = onAudioPermissionPermanentlyDenied,
                    onPlayPauseCurrentAudio = onPlayPauseCurrentAudio,
                    onStopCurrentAudio = onStopCurrentAudio
                )
            }
        }
    }

    SideEffect {
        keepOnTopWhenNotScrolled(lazyListState)
    }
}

@Suppress("MagicNumber")
fun previewConversationList(count: Int, startIndex: Int = 0, unread: Boolean = false, searchQuery: String = "") = buildList {
    repeat(count) { index ->
        val currentIndex = startIndex + index
        when (index % 3) {
            0 -> add(fakeRegularGroup(currentIndex, unread, searchQuery))
            1 -> add(fakePrivateConversation(currentIndex, unread, searchQuery))
            2 -> add(fakeChannel(currentIndex, unread, searchQuery))
        }
    }
}.toImmutableList()

private fun fakeRegularGroup(
    currentIndex: Int,
    unread: Boolean,
    searchQuery: String
) = ConversationItem.Group.Regular(
    groupName = "Conversation $currentIndex",
    conversationId = QualifiedID(currentIndex.toString(), "domain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    lastMessageContent = UILastMessageContent.TextMessage(MessageBody(UIText.DynamicString("Message"))),
    badgeEventType = if (unread) BadgeEventType.UnreadMessage(1) else BadgeEventType.None,
    selfMemberRole = null,
    teamId = null,
    hasOnGoingCall = false,
    isArchived = false,
    isFromTheSameTeam = false,
    mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
    proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
    searchQuery = searchQuery,
    isFavorite = false,
    folder = null,
    playingAudio = null
)

private fun fakePrivateConversation(
    currentIndex: Int,
    unread: Boolean,
    searchQuery: String
) = ConversationItem.PrivateConversation(
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
    proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
    searchQuery = searchQuery,
    isFavorite = false,
    isUserDeleted = false,
    folder = null,
    playingAudio = null
)

private fun fakeChannel(
    currentIndex: Int,
    unread: Boolean,
    searchQuery: String
) = ConversationItem.Group.Channel(
    groupName = "Conversation $currentIndex",
    conversationId = QualifiedID(currentIndex.toString(), "domain"),
    mutedStatus = MutedConversationStatus.AllAllowed,
    lastMessageContent = UILastMessageContent.TextMessage(MessageBody(UIText.DynamicString("Message"))),
    badgeEventType = if (unread) BadgeEventType.UnreadMessage(1) else BadgeEventType.None,
    selfMemberRole = null,
    teamId = null,
    hasOnGoingCall = false,
    isArchived = false,
    isFromTheSameTeam = false,
    mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
    proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
    searchQuery = searchQuery,
    isFavorite = false,
    folder = null,
    playingAudio = null,
    isPrivate = currentIndex % 2 == 0
)

fun previewConversationFoldersFlow(
    searchQuery: String = "",
    list: List<ConversationFolderItem> = previewConversationFolders(searchQuery = searchQuery)
) = flowOf(
    PagingData.from(
        data = list,
        sourceLoadStates = LoadStates(
            prepend = LoadState.NotLoading(true),
            append = LoadState.NotLoading(true),
            refresh = LoadState.NotLoading(true),
        )
    )
)

fun previewConversationFolders(
    isChannels: Boolean = false,
    withFolders: Boolean = true,
    searchQuery: String = "",
    unreadCount: Int = 3,
    readCount: Int = 6
) =
    buildList {
        if (isChannels) add(ConversationFolder.Predefined.BrowseChannels)
        if (withFolders) add(ConversationFolder.Predefined.NewActivities)
        addAll(previewConversationList(unreadCount, 0, true, searchQuery))
        if (withFolders) add(ConversationFolder.Predefined.Conversations)
        addAll(previewConversationList(readCount, unreadCount, false, searchQuery))
    }

@PreviewMultipleThemes
@Composable
fun PreviewChannelsList() = WireTheme {
    ConversationList(
        lazyPagingConversations = previewConversationFoldersFlow(list = previewConversationFolders(isChannels = true))
            .collectAsLazyPagingItems(),
        isSelectableList = false,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewConversationList() = WireTheme {
    ConversationList(
        lazyPagingConversations = previewConversationFoldersFlow().collectAsLazyPagingItems(),
        isSelectableList = false,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewConversationListSearch() = WireTheme {
    ConversationList(
        lazyPagingConversations = previewConversationFoldersFlow().collectAsLazyPagingItems(),
        isSelectableList = false,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewConversationListSelect() = WireTheme {
    val conversationFolders = previewConversationFolders()
    ConversationList(
        lazyPagingConversations = previewConversationFoldersFlow(list = conversationFolders).collectAsLazyPagingItems(),
        isSelectableList = true,
        selectedConversations = conversationFolders.filterIsInstance<ConversationItem>().filterIndexed { index, _ -> index % 3 == 0 }
            .map { it.conversationId },
    )
}
