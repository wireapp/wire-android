/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.conversations

import android.net.Uri
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.paging.PagingData
import com.wire.android.media.audiomessage.PlayingAudioMessage
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.emoji.EmojiPickerBottomSheet
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messagelist.ConversationMessageList
import com.wire.android.ui.home.conversations.messages.ThreadSummaryUi
import com.wire.android.ui.home.conversations.messages.item.MessageClickActions
import com.wire.android.ui.home.conversations.model.MessageSenderId
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.messagecomposer.MessageComposer
import com.wire.android.ui.home.messagecomposer.model.MessageBundle
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.util.ui.collectAsLazyPagingItemsWithLifecycle
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.MessageAssetStatus
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant

@Suppress("LongParameterList")
@Composable
internal fun ConversationMessageComposer(
    conversationId: ConversationId,
    bottomSheetVisible: Boolean,
    lastUnreadMessageInstant: Instant?,
    unreadEventCount: Int,
    playingAudioMessage: PlayingAudioMessage,
    assetStatuses: PersistentMap<String, MessageAssetStatus>,
    selectedMessageId: String?,
    messageComposerStateHolder: MessageComposerStateHolder,
    attachments: List<AttachmentDraftUi>,
    messages: Flow<PagingData<UIMessage>>,
    threadSummaryByRootMessageId: PersistentMap<String, ThreadSummaryUi>,
    isThreadMode: Boolean,
    onSendMessage: (MessageBundle) -> Unit,
    onPingOptionClicked: () -> Unit,
    onImagesPicked: (List<Uri>, Boolean) -> Unit,
    onAttachmentPicked: (UriAsset) -> Unit,
    onAudioRecorded: (UriAsset) -> Unit,
    onAssetItemClicked: (String) -> Unit,
    onImageFullScreenMode: (UIMessage.Regular, Boolean, String?) -> Unit,
    onVideoClick: (localPath: String?, contentUrl: String?, fileName: String?) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    onOpenProfile: (senderId: MessageSenderId) -> Unit,
    onUpdateConversationReadDate: (Instant) -> Unit,
    onShowEditingOptions: (UIMessage.Regular) -> Unit,
    onSwipedToReply: (UIMessage.Regular) -> Unit,
    onSelfDeletingMessageRead: (UIMessage) -> Unit,
    conversationDetailsData: ConversationDetailsData,
    onFailedMessageRetryClicked: (String, ConversationId) -> Unit,
    onFailedMessageCancelClicked: (String) -> Unit,
    onChangeSelfDeletionClicked: (SelfDeletionTimer) -> Unit,
    onClearMentionSearchResult: () -> Unit,
    onLocationClicked: () -> Unit,
    onPermissionPermanentlyDenied: (type: ConversationActionPermissionType) -> Unit,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?,
    onLinkClick: (String) -> Unit,
    onNavigateToReplyOriginalMessage: (UIMessage) -> Unit,
    onOpenThreadClick: (threadId: String, rootMessageId: String, rootMessageSelfDeletionDurationMillis: Long?) -> Unit,
    openDrawingCanvas: () -> Unit,
    onAttachmentClick: (AttachmentDraftUi) -> Unit,
    onAttachmentMenuClick: (AttachmentDraftUi) -> Unit,
    onVisibleRootMessagesChanged: (List<String>) -> Unit,
    currentTimeInMillisFlow: Flow<Long> = flow {},
    onReachedOldestMessage: () -> Unit = {},
    showHistoryLoadingIndicator: Boolean = false,
    isFetchingOlderMessages: Boolean = false,
    hasMoreRemoteMessages: Boolean = false,
    isBubbleUiEnabled: Boolean = false,
    isWireCellsEnabled: Boolean = false,
) {
    val lazyPagingMessages = messages.collectAsLazyPagingItemsWithLifecycle()

    val lazyListState = rememberSaveable(unreadEventCount, lazyPagingMessages, saver = LazyListState.Saver) {
        LazyListState(unreadEventCount)
    }

    val emojiPickerState = rememberWireModalSheetState<String>(skipPartiallyExpanded = false)

    MessageComposer(
        conversationId = conversationId,
        bottomSheetVisible = bottomSheetVisible,
        isThreadMode = isThreadMode,
        messageComposerStateHolder = messageComposerStateHolder,
        attachments = attachments,
        messageListContent = {
            ConversationMessageList(
                lazyPagingMessages = lazyPagingMessages,
                lazyListState = lazyListState,
                lastUnreadMessageInstant = lastUnreadMessageInstant,
                playingAudioMessage = playingAudioMessage,
                assetStatuses = assetStatuses,
                onUpdateConversationReadDate = onUpdateConversationReadDate,
                clickActions = MessageClickActions.Content(
                    onFullMessageLongClicked = onShowEditingOptions,
                    onProfileClicked = onOpenProfile,
                    onReactionClicked = onReactionClicked,
                    onAssetClicked = onAssetItemClicked,
                    onImageClicked = onImageFullScreenMode,
                    onVideoClicked = onVideoClick,
                    onLinkClicked = onLinkClick,
                    onReplyClicked = onNavigateToReplyOriginalMessage,
                    onThreadClicked = { rootMessageId, threadId, rootMessageSelfDeletionDurationMillis ->
                        if (!isThreadMode) onOpenThreadClick(threadId, rootMessageId, rootMessageSelfDeletionDurationMillis)
                    },
                    onResetSessionClicked = onResetSessionClicked,
                    onFailedMessageRetryClicked = onFailedMessageRetryClicked,
                    onFailedMessageCancelClicked = onFailedMessageCancelClicked,
                ),
                onSelfDeletingMessageRead = onSelfDeletingMessageRead,
                onSwipedToReply = onSwipedToReply,
                onSwipedToReact = { message ->
                    emojiPickerState.show(message.header.messageId)
                },
                conversationDetailsData = conversationDetailsData,
                selectedMessageId = selectedMessageId,
                interactionAvailability = messageComposerStateHolder.messageComposerViewState.value.interactionAvailability,
                threadSummaryByRootMessageId = threadSummaryByRootMessageId,
                isThreadMode = isThreadMode,
                onVisibleRootMessageIdsChanged = onVisibleRootMessagesChanged,
                currentTimeInMillisFlow = currentTimeInMillisFlow,
                onReachedOldestMessage = onReachedOldestMessage,
                showHistoryLoadingIndicator = showHistoryLoadingIndicator,
                isFetchingOlderMessages = isFetchingOlderMessages,
                hasMoreRemoteMessages = hasMoreRemoteMessages,
                isBubbleUiEnabled = isBubbleUiEnabled,
                isWireCellsEnabled = isWireCellsEnabled,
            )
        },
        onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
        onLocationClicked = onLocationClicked,
        onClearMentionSearchResult = onClearMentionSearchResult,
        onSendMessageBundle = onSendMessage,
        onPingOptionClicked = onPingOptionClicked,
        onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
        tempWritableVideoUri = tempWritableVideoUri,
        tempWritableImageUri = tempWritableImageUri,
        onImagesPicked = onImagesPicked,
        openDrawingCanvas = openDrawingCanvas,
        onAttachmentClick = onAttachmentClick,
        onAttachmentMenuClick = onAttachmentMenuClick,
        onAttachmentPicked = onAttachmentPicked,
        onAudioRecorded = onAudioRecorded,
    )

    EmojiPickerBottomSheet(
        sheetState = emojiPickerState,
        onEmojiSelected = { emoji, messageId ->
            emojiPickerState.hide()
            onReactionClicked(messageId, emoji)
        },
    )
}
