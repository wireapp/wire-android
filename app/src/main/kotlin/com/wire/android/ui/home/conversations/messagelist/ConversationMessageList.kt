/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.conversations.messagelist

import android.annotation.SuppressLint
import android.text.format.DateUtils
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.sebaslogen.resaca.rememberKeysInScope
import com.wire.android.R
import com.wire.android.mapper.MessageDateTimeGroup
import com.wire.android.media.audiomessage.AudioMessageArgs
import com.wire.android.media.audiomessage.PlayingAudioMessage
import com.wire.android.ui.common.PageLoadingIndicator
import com.wire.android.ui.common.applyIf
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.home.conversations.AuthorHeaderHelper.rememberShouldHaveSmallBottomPadding
import com.wire.android.ui.home.conversations.AuthorHeaderHelper.rememberShouldShowHeader
import com.wire.android.ui.home.conversations.LocalAssetLocalPathKeyInScopeResolver
import com.wire.android.ui.home.conversations.LocalAudioMessageKeyInScopeResolver
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messages.item.AssetLocalPathArgs
import com.wire.android.ui.home.conversations.messages.item.MessageClickActions
import com.wire.android.ui.home.conversations.messages.item.MessageContainerItem
import com.wire.android.ui.home.conversations.messages.item.SwipeableMessageConfiguration
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.DateAndTimeParsers
import com.wire.kalium.logic.data.conversation.InteractionAvailability
import com.wire.kalium.logic.data.message.MessageAssetStatus
import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import com.wire.android.ui.common.R as commonR

private const val MAXIMUM_SCROLLED_MESSAGES_UNTIL_AUTOSCROLL_STOPS = 5
private const val SCOPED_VIEW_MODEL_PREFETCH_WINDOW = 3

@Suppress("ComplexMethod", "ComplexCondition")
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ConversationMessageList(
    lazyPagingMessages: LazyPagingItems<UIMessage>,
    lazyListState: LazyListState,
    lastUnreadMessageInstant: Instant?,
    playingAudioMessage: PlayingAudioMessage,
    assetStatuses: PersistentMap<String, MessageAssetStatus>,
    onUpdateConversationReadDate: (Instant) -> Unit,
    onSwipedToReply: (UIMessage.Regular) -> Unit,
    onSwipedToReact: (UIMessage.Regular) -> Unit,
    onSelfDeletingMessageRead: (UIMessage) -> Unit,
    conversationDetailsData: ConversationDetailsData,
    selectedMessageId: String?,
    interactionAvailability: InteractionAvailability,
    clickActions: MessageClickActions.Content,
    modifier: Modifier = Modifier,
    currentTimeInMillisFlow: Flow<Long> = flow { },
    showHistoryLoadingIndicator: Boolean = false,
    isFetchingOlderMessages: Boolean = false,
    hasMoreRemoteMessages: Boolean = false,
    isBubbleUiEnabled: Boolean = false,
    isWireCellsEnabled: Boolean = false,
    onReachedOldestMessage: () -> Unit = {},
) {
    val prevItemCount = remember { mutableStateOf(lazyPagingMessages.itemCount) }
    val readLastMessageAtStartTriggered = remember { mutableStateOf(false) }
    val shouldTriggerOldestMessageFetch = remember { mutableStateOf(true) }
    val currentTime by currentTimeInMillisFlow.collectAsState(initial = System.currentTimeMillis())
    val isPrependLoading = lazyPagingMessages.loadState.prepend is LoadState.Loading
    val isPrependCompleted = lazyPagingMessages.loadState.prepend.endOfPaginationReached

    LaunchedEffect(lazyPagingMessages.itemCount) {
        if (lazyPagingMessages.itemCount > prevItemCount.value && selectedMessageId == null) {
            val canScrollToLastMessage = prevItemCount.value > 0 &&
                lazyListState.firstVisibleItemIndex > 0 &&
                lazyListState.firstVisibleItemIndex <= MAXIMUM_SCROLLED_MESSAGES_UNTIL_AUTOSCROLL_STOPS
            if (canScrollToLastMessage) {
                lazyListState.stopScroll()
                lazyListState.animateScrollToItem(0)
            }
            if (
                shouldAutoTriggerOldestFetch(
                    selectedMessageId = selectedMessageId,
                    isScrollInProgress = lazyListState.isScrollInProgress,
                    canScrollForward = lazyListState.canScrollForward,
                    hasMoreRemoteMessages = hasMoreRemoteMessages,
                    isFetchingOlderMessages = isFetchingOlderMessages,
                )
            ) {
                onReachedOldestMessage()
            } else {
                shouldTriggerOldestMessageFetch.value = true
            }
            prevItemCount.value = lazyPagingMessages.itemCount
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress && lazyPagingMessages.itemCount > 0) {
            val lastVisibleMessage = lazyPagingMessages[lazyListState.firstVisibleItemIndex] ?: return@LaunchedEffect
            updateLastReadMessage(lastVisibleMessage, lastUnreadMessageInstant, onUpdateConversationReadDate)
        }
    }

    LaunchedEffect(lazyPagingMessages.itemCount) {
        if (
            (!readLastMessageAtStartTriggered.value || (!lazyListState.canScrollBackward && !lazyListState.canScrollForward)) &&
            lazyPagingMessages.itemSnapshotList.items.isNotEmpty()
        ) {
            val lastVisibleMessage = lazyPagingMessages[lazyListState.firstVisibleItemIndex] ?: return@LaunchedEffect
            if (!readLastMessageAtStartTriggered.value) {
                readLastMessageAtStartTriggered.value = true
            }
            updateLastReadMessage(lastVisibleMessage, lastUnreadMessageInstant, onUpdateConversationReadDate)
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress, lazyPagingMessages.itemCount) {
        if (!lazyListState.isScrollInProgress && lazyPagingMessages.itemCount > 0) {
            val reachedOldest = !lazyListState.canScrollForward
            if (reachedOldest && shouldTriggerOldestMessageFetch.value) {
                onReachedOldestMessage()
                shouldTriggerOldestMessageFetch.value = false
            } else if (!reachedOldest) {
                shouldTriggerOldestMessageFetch.value = true
            }
        }
    }

    val scopedMessages by remember(lazyListState, lazyPagingMessages) {
        derivedStateOf {
            lazyPagingMessages.peekVisibleWindowItems(lazyListState, SCOPED_VIEW_MODEL_PREFETCH_WINDOW)
        }
    }
    val playingAudioMessageKey = (playingAudioMessage as? PlayingAudioMessage.Some)?.let {
        AudioMessageArgs(it.conversationId, it.messageId).key
    }
    val audioMessageKeysInScope = remember(scopedMessages, playingAudioMessageKey) {
        buildList {
            scopedMessages.mapNotNullTo(this) { it.audioMessageScopedKeyOrNull() }
            playingAudioMessageKey?.let(::add)
        }.distinct()
    }
    val assetLocalPathKeysInScope = remember(scopedMessages) {
        scopedMessages.flatMap { it.assetLocalPathScopedKeys() }.distinct()
    }
    val audioMessageKeyInScopeResolver = rememberKeysInScope(audioMessageKeysInScope)
    val assetLocalPathKeyInScopeResolver = rememberKeysInScope(assetLocalPathKeysInScope)
    val messageListContentDescription = stringResource(R.string.content_description_conversation_message_list)
    val focusManager = LocalFocusManager.current
    val messageListFocusRequester = remember { FocusRequester() }
    val firstMessageFocusRequester = remember { FocusRequester() }
    var isMessageNavigationActive by remember { mutableStateOf(false) }
    var isMessageListFocused by remember { mutableStateOf(false) }
    var messageNavigationTargetIndex by remember { mutableStateOf(0) }

    LaunchedEffect(isMessageNavigationActive) {
        if (isMessageNavigationActive && lazyPagingMessages.itemCount > 0) {
            withFrameNanos { }
            firstMessageFocusRequester.requestFocus()
        }
    }

    CompositionLocalProvider(
        LocalAudioMessageKeyInScopeResolver provides audioMessageKeyInScopeResolver,
        LocalAssetLocalPathKeyInScopeResolver provides assetLocalPathKeyInScopeResolver,
    ) {
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = modifier
                .fillMaxSize()
                .background(
                    color = if (isBubbleUiEnabled) {
                        colorsScheme().bubblesBackground
                    } else {
                        colorsScheme().surfaceContainerLow
                    }
                )
                .applyIf(isMessageListFocused) {
                    background(MaterialTheme.wireColorScheme.primaryVariant)
                }
                .focusRequester(messageListFocusRequester)
                .onFocusChanged { focusState ->
                    isMessageListFocused = focusState.isFocused
                    if (!focusState.hasFocus) {
                        isMessageNavigationActive = false
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        isMessageListFocused && (event.key == Key.Enter || event.key == Key.NumPadEnter) -> {
                            messageNavigationTargetIndex = lazyListState.firstVisibleItemIndex
                            isMessageNavigationActive = true
                            true
                        }
                        isMessageNavigationActive && event.key == Key.DirectionUp ->
                            focusManager.moveFocus(FocusDirection.Up)
                        isMessageNavigationActive && event.key == Key.DirectionDown ->
                            focusManager.moveFocus(FocusDirection.Down)
                        isMessageNavigationActive && (event.key == Key.Escape || event.key == Key.Back) -> {
                            isMessageNavigationActive = false
                            messageListFocusRequester.requestFocus()
                            true
                        }
                        else -> false
                    }
                }
                .semantics {
                    isTraversalGroup = true
                    contentDescription = messageListContentDescription
                }
                .focusable(),
        ) {
            LazyColumn(
                state = lazyListState,
                reverseLayout = true,
                contentPadding = PaddingValues(
                    bottom = dimensions().typingIndicatorHeight - dimensions().messageItemBottomPadding
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    count = lazyPagingMessages.itemCount,
                    key = lazyPagingMessages.itemKey { it.header.messageId },
                    contentType = lazyPagingMessages.itemContentType { message ->
                        when (message) {
                            is UIMessage.Regular -> "regular_message"
                            is UIMessage.System -> "system_message"
                        }
                    }
                ) { index ->
                    val message: UIMessage = lazyPagingMessages[index]
                        ?: return@items Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimensions().spacing56x)
                        )

                    val showAuthor = if (!isBubbleUiEnabled || conversationDetailsData is ConversationDetailsData.Group) {
                        rememberShouldShowHeader(index, message, lazyPagingMessages)
                    } else {
                        false
                    }
                    val useSmallBottomPadding = rememberShouldHaveSmallBottomPadding(index, message, lazyPagingMessages)
                    var isMessageKeyboardFocused by remember(message.header.messageId) { mutableStateOf(false) }

                    if (index > 0) {
                        val previousMessage = lazyPagingMessages[index - 1] ?: message
                        val shouldDisplayDateTimeDivider = message.header.messageTime.shouldDisplayDatesDifferenceDivider(
                            previousDate = previousMessage.header.messageTime.instant
                        )
                        if (shouldDisplayDateTimeDivider) {
                            val previousGroup = previousMessage.header.messageTime.getFormattedDateGroup(now = currentTime)
                            MessageGroupDateTime(
                                messageDateTime = previousMessage.header.messageTime.instant,
                                messageDateTimeGroup = previousGroup,
                                now = currentTime,
                                isBubbleUiEnabled = isBubbleUiEnabled,
                            )
                        }
                    }

                    val swipeableConfiguration = remember(message, lazyListState.isScrollInProgress) {
                        if (!lazyListState.isScrollInProgress && message is UIMessage.Regular && message.isSwipeable) {
                            SwipeableMessageConfiguration.Swipeable(
                                onSwipedRight = { onSwipedToReply(message) }.takeIf { message.isReplyable },
                                onSwipedLeft = { onSwipedToReact(message) }.takeIf { message.isReactionAllowed },
                            )
                        } else {
                            SwipeableMessageConfiguration.NotSwipeable
                        }
                    }

                    MessageContainerItem(
                        modifier = Modifier
                            .applyIf(isMessageKeyboardFocused) {
                                background(MaterialTheme.wireColorScheme.primaryVariant)
                            }
                            .applyIf(index == messageNavigationTargetIndex) {
                                focusRequester(firstMessageFocusRequester)
                            }
                            .onFocusChanged { isMessageKeyboardFocused = it.isFocused }
                            .focusProperties { canFocus = isMessageNavigationActive }
                            .focusable(isMessageNavigationActive),
                        message = message,
                        conversationDetailsData = conversationDetailsData,
                        showAuthor = showAuthor,
                        useSmallBottomPadding = useSmallBottomPadding,
                        assetStatus = assetStatuses[message.header.messageId]?.transferStatus,
                        clickActions = clickActions,
                        swipeableMessageConfiguration = swipeableConfiguration,
                        onSelfDeletingMessageRead = onSelfDeletingMessageRead,
                        isSelectedMessage = message.header.messageId == selectedMessageId,
                        failureInteractionAvailable = interactionAvailability == InteractionAvailability.ENABLED,
                        isBubbleUiEnabled = isBubbleUiEnabled,
                        isWireCellsEnabled = isWireCellsEnabled,
                    )

                    val isTheOnlyItem = index == 0 && lazyPagingMessages.itemCount == 1
                    val isTheLastItem = index + 1 == lazyPagingMessages.itemCount
                    if (isTheOnlyItem || isTheLastItem) {
                        val currentGroup = message.header.messageTime.getFormattedDateGroup(now = currentTime)
                        MessageGroupDateTime(
                            messageDateTime = message.header.messageTime.instant,
                            messageDateTimeGroup = currentGroup,
                            now = currentTime,
                            isBubbleUiEnabled = isBubbleUiEnabled,
                        )
                    }
                }

                if (
                    lazyPagingMessages.itemCount > 0 &&
                    (isPrependLoading || (showHistoryLoadingIndicator && isPrependCompleted))
                ) {
                    item(
                        key = "prepend_loading_indicator",
                        contentType = "prepend_loading_indicator",
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(dimensions().spacing16x),
                        ) {
                            val (text, prefixIconResId) = when {
                                showHistoryLoadingIndicator && isPrependCompleted ->
                                    stringResource(R.string.conversation_history_loaded) to null
                                showHistoryLoadingIndicator ->
                                    stringResource(R.string.conversation_history_loading) to R.drawable.ic_undo
                                else -> "" to null
                            }

                            if (showHistoryLoadingIndicator) {
                                PageLoadingIndicator(text = text, prefixIconResId = prefixIconResId)
                            } else {
                                WireCircularProgressIndicator(
                                    progressColor = MaterialTheme.wireColorScheme.secondaryText,
                                    size = dimensions().spacing24x,
                                )
                            }
                        }
                    }
                }
                if (isFetchingOlderMessages && hasMoreRemoteMessages && lazyPagingMessages.itemCount > 0) {
                    item(
                        key = "nomad_prepend_loading_indicator",
                        contentType = "nomad_prepend_loading_indicator",
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(dimensions().spacing16x),
                        ) {
                            PageLoadingIndicator(text = stringResource(R.string.conversation_history_loading))
                        }
                    }
                }
            }
            ScrollDateOverlay(lazyListState = lazyListState, lazyPagingMessages = lazyPagingMessages)
            JumpToPlayingAudioButton(
                lazyListState = lazyListState,
                lazyPagingMessages = lazyPagingMessages,
                playingAudioMessage = playingAudioMessage,
            )
            JumpToLastMessageButton(lazyListState = lazyListState)
        }
    }
}

@VisibleForTesting
internal fun shouldAutoTriggerOldestFetch(
    selectedMessageId: String?,
    isScrollInProgress: Boolean,
    canScrollForward: Boolean,
    hasMoreRemoteMessages: Boolean,
    isFetchingOlderMessages: Boolean,
): Boolean =
    selectedMessageId == null &&
        !isScrollInProgress &&
        !canScrollForward &&
        hasMoreRemoteMessages &&
        !isFetchingOlderMessages

private fun UIMessage.audioMessageScopedKeyOrNull(): String? =
    (this as? UIMessage.Regular)
        ?.takeIf { it.messageContent is UIMessageContent.AudioAssetMessage }
        ?.let { AudioMessageArgs(it.conversationId, it.header.messageId).key }

private fun UIMessage.assetLocalPathScopedKeys(): List<String> {
    val regularMessage = this as? UIMessage.Regular ?: return emptyList()
    val keys = mutableListOf<String>()

    when (regularMessage.messageContent) {
        is UIMessageContent.ImageMessage,
        is UIMessageContent.VideoMessage,
        is UIMessageContent.AssetMessage -> {
            keys.add(AssetLocalPathArgs(regularMessage.conversationId, regularMessage.header.messageId).key)
        }

        else -> Unit
    }

    val quotedImageAsset = when (val content = regularMessage.messageContent) {
        is UIMessageContent.TextMessage -> {
            (content.messageBody.quotedMessage as? UIQuotedMessage.UIQuotedData)
                ?.quotedContent as? UIQuotedMessage.UIQuotedData.DisplayableImage
        }

        is UIMessageContent.Composite -> {
            (content.messageBody?.quotedMessage as? UIQuotedMessage.UIQuotedData)
                ?.quotedContent as? UIQuotedMessage.UIQuotedData.DisplayableImage
        }

        else -> null
    }?.displayable

    if (quotedImageAsset != null) {
        keys.add(AssetLocalPathArgs(quotedImageAsset.conversationId, quotedImageAsset.messageId).key)
    }
    return keys
}

@Composable
private fun BoxScope.ScrollDateOverlay(
    lazyListState: LazyListState,
    lazyPagingMessages: LazyPagingItems<UIMessage>,
) {
    val context = LocalContext.current
    val dateLabel by remember(lazyListState, lazyPagingMessages) {
        derivedStateOf {
            if (!lazyListState.isScrollInProgress) return@derivedStateOf null

            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            var message: UIMessage? = null
            for (index in visibleItems.lastIndex downTo 0) {
                message = lazyPagingMessages.peekOrNull(visibleItems[index].index)
                if (message != null) break
            }
            message ?: return@derivedStateOf null
            val messageDate = message.header.messageTime.instant

            DateUtils.formatDateTime(
                context,
                messageDate.toEpochMilliseconds(),
                DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE,
            )
        }
    }

    AnimatedVisibility(
        visible = dateLabel != null,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.wireColorScheme.surface.copy(alpha = 0.8f))
                .padding(vertical = dimensions().spacing2x, horizontal = dimensions().spacing4x),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = dateLabel.orEmpty(),
                style = MaterialTheme.wireTypography.title03,
                color = MaterialTheme.wireColorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun LazyPagingItems<UIMessage>.peekOrNull(index: Int): UIMessage? =
    if (index in 0 until itemCount) peek(index) else null

private fun LazyPagingItems<UIMessage>.peekVisibleWindowItems(
    lazyListState: LazyListState,
    prefetchWindow: Int,
): List<UIMessage> {
    val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
    return if (itemCount == 0 || visibleItems.isEmpty()) {
        emptyList()
    } else {
        val firstVisibleIndex = visibleItems.minOf { it.index }
        val lastVisibleIndex = visibleItems.maxOf { it.index }
        val firstIndex = (firstVisibleIndex - prefetchWindow).coerceAtLeast(0)
        val lastIndex = (lastVisibleIndex + prefetchWindow).coerceAtMost(itemCount - 1)

        if (firstIndex > lastIndex) {
            emptyList()
        } else {
            (firstIndex..lastIndex).mapNotNull { index -> peekOrNull(index) }
        }
    }
}

@Composable
private fun MessageGroupDateTime(
    now: Long,
    messageDateTime: Instant,
    messageDateTimeGroup: MessageDateTimeGroup,
    isBubbleUiEnabled: Boolean,
) {
    val context = LocalContext.current
    val messageDateTimeInMillis = messageDateTime.toEpochMilliseconds()
    val timeString = when (messageDateTimeGroup) {
        is MessageDateTimeGroup.Now -> context.resources.getString(R.string.message_datetime_now)
        is MessageDateTimeGroup.Within30Minutes -> DateUtils.getRelativeTimeSpanString(
            messageDateTimeInMillis,
            now,
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()

        is MessageDateTimeGroup.Daily -> when (messageDateTimeGroup.type) {
            MessageDateTimeGroup.Daily.Type.Today,
            MessageDateTimeGroup.Daily.Type.Yesterday -> DateUtils.getRelativeTimeSpanString(
                messageDateTimeInMillis,
                now,
                DateUtils.DAY_IN_MILLIS,
                0,
            ).toString()

            MessageDateTimeGroup.Daily.Type.WithinWeek,
            MessageDateTimeGroup.Daily.Type.NotWithinWeekButSameYear -> DateUtils.formatDateTime(
                context,
                messageDateTimeInMillis,
                DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE,
            )

            MessageDateTimeGroup.Daily.Type.Other -> DateUtils.formatDateTime(
                context,
                messageDateTimeInMillis,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR,
            )
        }
    }

    if (isBubbleUiEnabled) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(dimensions().spacing16x),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(modifier = Modifier.weight(1F), color = colorsScheme().outline)
            HorizontalSpace.x4()
            Text(
                text = timeString.uppercase(LocalLocale.current.platformLocale),
                maxLines = 1,
                color = colorsScheme().onBackground,
                style = MaterialTheme.wireTypography.label02,
            )
            HorizontalSpace.x4()
            HorizontalDivider(modifier = Modifier.weight(1F), color = colorsScheme().outline)
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = dimensions().spacing4x, bottom = dimensions().spacing8x)
                .background(color = colorsScheme().divider)
                .padding(
                    top = dimensions().spacing6x,
                    bottom = dimensions().spacing6x,
                    start = dimensions().spacing56x,
                )
        ) {
            Text(
                text = timeString.uppercase(LocalLocale.current.platformLocale),
                color = colorsScheme().secondaryText,
                style = MaterialTheme.wireTypography.title03,
            )
        }
    }
}

private fun updateLastReadMessage(
    lastVisibleMessage: UIMessage,
    lastUnreadMessageInstant: Instant?,
    onUpdateConversationReadDate: (Instant) -> Unit,
) {
    val lastVisibleMessageInstant = lastVisibleMessage.header.messageTime.instant
    if (lastVisibleMessageInstant > (lastUnreadMessageInstant ?: Instant.DISTANT_FUTURE)) {
        onUpdateConversationReadDate(lastVisibleMessageInstant)
    }
}

@Composable
fun JumpToLastMessageButton(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    val bottomPadding = dimensions().typingIndicatorHeight + dimensions().spacing8x
    val bottomPaddingPx = with(LocalDensity.current) { bottomPadding.toPx() }
    val showButton by remember(lazyListState, bottomPaddingPx) {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > bottomPaddingPx
        }
    }
    AnimatedVisibility(
        modifier = modifier.padding(bottom = bottomPadding, end = dimensions().spacing16x),
        visible = showButton,
        enter = scaleIn(),
        exit = scaleOut(),
    ) {
        SmallFloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    lazyListState.stopScroll()
                    lazyListState.animateScrollToItem(0)
                }
            },
            containerColor = MaterialTheme.wireColorScheme.secondaryText,
            contentColor = MaterialTheme.wireColorScheme.onPrimaryButtonEnabled,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(dimensions().spacing0x),
        ) {
            Icon(
                painter = painterResource(commonR.drawable.ic_keyboard_arrow_down),
                contentDescription = stringResource(id = R.string.content_description_jump_to_last_message),
                modifier = Modifier.size(dimensions().spacing32x),
            )
        }
    }
}

@Composable
fun BoxScope.JumpToPlayingAudioButton(
    lazyListState: LazyListState,
    playingAudioMessage: PlayingAudioMessage,
    lazyPagingMessages: LazyPagingItems<UIMessage>,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    if (playingAudioMessage is PlayingAudioMessage.Some && playingAudioMessage.state.isPlaying()) {
        val indexOfPlayedMessage = lazyPagingMessages.itemSnapshotList
            .indexOfFirst { playingAudioMessage.messageId == it?.header?.messageId }
        if (indexOfPlayedMessage < 0) return

        val firstVisibleIndex = lazyListState.firstVisibleItemIndex
        val lastVisibleIndex = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: firstVisibleIndex
        if (indexOfPlayedMessage in firstVisibleIndex..lastVisibleIndex) return

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .wrapContentWidth()
                .align(Alignment.TopCenter)
                .padding(all = dimensions().spacing8x)
                .clickable { coroutineScope.launch { lazyListState.animateScrollToItem(indexOfPlayedMessage) } }
                .background(
                    color = colorsScheme().secondaryText,
                    shape = RoundedCornerShape(dimensions().corner16x),
                )
                .padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x),
        ) {
            Icon(
                modifier = Modifier.size(dimensions().systemMessageIconSize),
                painter = painterResource(id = R.drawable.ic_play),
                contentDescription = null,
                tint = MaterialTheme.wireColorScheme.onPrimaryButtonEnabled,
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = dimensions().spacing8x)
                    .weight(1f, fill = false),
                text = playingAudioMessage.authorName.asString(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colorsScheme().onPrimaryButtonEnabled,
                style = MaterialTheme.wireTypography.body04,
            )
            Text(
                text = DateAndTimeParsers.audioMessageTime(playingAudioMessage.state.currentPositionInMs.toLong()),
                color = colorsScheme().onPrimaryButtonEnabled,
                style = MaterialTheme.wireTypography.label03,
            )
        }
    }
}
