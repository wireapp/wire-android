/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.home.threads

import androidx.compose.animation.splineBasedDecay
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.generated.app.destinations.ThreadConversationScreenDestination
import com.wire.android.R
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.annotation.app.WireHomeDestination
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.LoadingListContent
import com.wire.android.ui.home.HomeStateHolder
import com.wire.android.ui.home.globalThreadsViewModel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.uiMessageDateTime
import kotlin.math.roundToInt
import com.wire.android.ui.common.R as commonR

@WireHomeDestination
@Composable
fun GlobalThreadsScreen(
    homeStateHolder: HomeStateHolder,
    viewModel: GlobalThreadsViewModel = globalThreadsViewModel(),
) {
    val query = homeStateHolder.searchBarState.searchQueryTextState.text.toString().trim()
    val allThreads = viewModel.state.threads
    val filteredThreads = remember(allThreads, query) {
        if (query.isBlank()) {
            allThreads
        } else {
            allThreads.filter { it.searchText.contains(query, ignoreCase = true) }
        }
    }

    homeStateHolder.searchBarState.searchVisibleChanged(
        isSearchVisible = allThreads.isNotEmpty() || homeStateHolder.searchBarState.isSearchActive
    )

    when {
        viewModel.state.isLoading -> LoadingListContent(lazyListState = homeStateHolder.lazyListStateFor(HomeDestination.Threads))
        filteredThreads.isEmpty() -> ThreadsEmptyContent(
            isSearching = query.isNotBlank(),
            modifier = Modifier.fillMaxSize()
        )

        else -> ThreadsOverviewList(
            threads = filteredThreads,
            lazyListState = homeStateHolder.lazyListStateFor(HomeDestination.Threads),
            onOpenThread = { thread ->
                homeStateHolder.navigator.navigate(
                    NavigationCommand(
                        ThreadConversationScreenDestination(
                            navArgs = com.wire.android.ui.home.conversations.ThreadConversationNavArgs(
                                conversationId = thread.conversationId,
                                threadId = thread.threadId,
                                threadRootMessageId = thread.rootMessageId,
                                threadRootSelfDeletionDurationMillis = thread.rootMessageSelfDeletionDurationMillis,
                            )
                        )
                    )
                )
            },
            onUnfollowThread = viewModel::unfollowThread,
            showConversationLabel = true,
        )
    }
}

@Composable
internal fun ThreadsOverviewList(
    threads: List<UiGlobalThread>,
    lazyListState: LazyListState,
    onOpenThread: (UiGlobalThread) -> Unit,
    onUnfollowThread: ((UiGlobalThread) -> Unit)?,
    showConversationLabel: Boolean,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = dimensions().spacing12x,
        vertical = dimensions().spacing8x
    ),
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
    ) {
        items(
            items = threads,
            key = { it.key }
        ) { thread ->
            if (onUnfollowThread == null) {
                ThreadCard(
                    thread = thread,
                    onOpenThread = { onOpenThread(thread) },
                    showConversationLabel = showConversationLabel,
                )
            } else {
                SwipeableThreadCard(
                    thread = thread,
                    onOpenThread = { onOpenThread(thread) },
                    onUnfollowThread = { onUnfollowThread(thread) },
                    showConversationLabel = showConversationLabel,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableThreadCard(
    thread: UiGlobalThread,
    onOpenThread: () -> Unit,
    onUnfollowThread: () -> Unit,
    showConversationLabel: Boolean,
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val dragState = remember(thread.key, screenWidth) {
        AnchoredDraggableState(
            initialValue = ThreadSwipeAnchor.CENTERED,
            // require a deliberate drag past half the screen before committing to the unfollow
            positionalThreshold = { totalDistance -> totalDistance * SWIPE_COMMIT_FRACTION },
            velocityThreshold = { screenWidth },
            // glide the card all the way off-screen instead of an instant snap
            snapAnimationSpec = tween(durationMillis = SWIPE_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing),
            decayAnimationSpec = splineBasedDecay(density),
            anchors = DraggableAnchors {
                ThreadSwipeAnchor.CENTERED at 0f
                ThreadSwipeAnchor.DISMISSED at screenWidth
            }
        )
    }

    LaunchedEffect(dragState.settledValue) {
        // once the card has finished animating off-screen, trigger the unfollow; the list flow
        // then removes the item reactively (keyed LazyColumn), so no manual snap-back is needed
        if (dragState.settledValue == ThreadSwipeAnchor.DISMISSED) {
            onUnfollowThread()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(colorsScheme().primary),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                painter = painterResource(commonR.drawable.ic_notifications_filled),
                contentDescription = stringResource(R.string.label_unfollow),
                tint = colorsScheme().onPrimary,
                modifier = Modifier
                    .padding(start = dimensions().spacing24x)
                    .size(dimensions().fabIconSize)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .anchoredDraggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    startDragImmediately = false
                )
                .offset { IntOffset(dragState.requireOffset().roundToInt(), 0) }
        ) {
            ThreadCard(
                thread = thread,
                onOpenThread = onOpenThread,
                showConversationLabel = showConversationLabel,
            )
        }
    }
}

@Composable
private fun ThreadCard(
    thread: UiGlobalThread,
    onOpenThread: () -> Unit,
    showConversationLabel: Boolean,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenThread() }
                .padding(horizontal = dimensions().spacing12x, vertical = dimensions().spacing12x),
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x),
            verticalAlignment = Alignment.Top
        ) {
            ThreadLeadingAvatar(thread = thread)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensions().spacing6x)
            ) {
                if (showConversationLabel) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThreadConversationLabel(thread = thread, modifier = Modifier.weight(1f))
                        Text(
                            text = thread.lastActivityAt.uiMessageDateTime(),
                            style = MaterialTheme.wireTypography.label02,
                            color = colorsScheme().secondaryText,
                        )
                    }
                } else {
                    Text(
                        text = thread.lastActivityAt.uiMessageDateTime(),
                        style = MaterialTheme.wireTypography.label02,
                        color = colorsScheme().secondaryText,
                    )
                }

                Text(
                    text = thread.previewText.ifBlank { stringResource(R.string.thread_root_fallback_label) },
                    style = MaterialTheme.wireTypography.body01,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                ThreadReplyPill(replyCount = thread.replyCount)
            }
        }
    }
}

private enum class ThreadSwipeAnchor {
    CENTERED,
    DISMISSED,
}

// fraction of the row width the user must drag past before the unfollow is committed
private const val SWIPE_COMMIT_FRACTION = 0.5f

// duration of the off-screen glide once the swipe is committed (or snaps back)
private const val SWIPE_ANIMATION_DURATION_MS = 400

@Composable
private fun ThreadLeadingAvatar(thread: UiGlobalThread) {
    when (thread.conversationType) {
        UiGlobalThread.ConversationType.ONE_ON_ONE -> {
            thread.avatarData?.let {
                UserProfileAvatar(
                    avatarData = it,
                    clickable = null
                )
            } ?: GenericThreadAvatar(iconRes = R.drawable.ic_reply)
        }

        UiGlobalThread.ConversationType.GROUP -> GenericThreadAvatar(iconRes = R.drawable.ic_conversation)
        UiGlobalThread.ConversationType.CHANNEL -> GenericThreadAvatar(iconRes = commonR.drawable.ic_channel)
    }
}

@Composable
private fun GenericThreadAvatar(iconRes: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(dimensions().avatarDefaultSize)
            .clip(MaterialTheme.shapes.medium)
            .background(colorsScheme().surfaceVariant)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colorsScheme().secondaryText,
            modifier = Modifier.size(dimensions().spacing20x)
        )
    }
}

@Composable
private fun ThreadConversationLabel(
    thread: UiGlobalThread,
    modifier: Modifier = Modifier,
) {
    val iconRes = when (thread.conversationType) {
        UiGlobalThread.ConversationType.ONE_ON_ONE -> R.drawable.ic_conversation
        UiGlobalThread.ConversationType.GROUP -> R.drawable.ic_conversation
        UiGlobalThread.ConversationType.CHANNEL -> commonR.drawable.ic_channel
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing6x),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colorsScheme().secondaryText,
            modifier = Modifier.size(dimensions().spacing14x)
        )
        Text(
            text = thread.conversationName ?: stringResource(R.string.member_name_deleted_label),
            style = MaterialTheme.wireTypography.label01,
            color = colorsScheme().secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ThreadReplyPill(replyCount: Long) {
    Surface(
        color = colorsScheme().surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dimensions().spacing8x, vertical = dimensions().spacing4x),
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_reply),
                contentDescription = null,
                tint = colorsScheme().secondaryText,
                modifier = Modifier.size(dimensions().spacing14x)
            )
            Text(
                text = pluralStringResource(R.plurals.unread_event_reply, replyCount.toInt(), replyCount.toInt()),
                style = MaterialTheme.wireTypography.label02,
                color = colorsScheme().secondaryText
            )
        }
    }
}

@Composable
internal fun ThreadsEmptyContent(
    isSearching: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(horizontal = dimensions().spacing24x),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing12x)
        ) {
            GenericThreadAvatar(iconRes = R.drawable.ic_reply)
            Text(
                text = if (isSearching) {
                    stringResource(R.string.threads_search_empty_title)
                } else {
                    stringResource(R.string.threads_empty_title)
                },
                style = MaterialTheme.wireTypography.title03
            )
            Text(
                text = if (isSearching) {
                    stringResource(R.string.threads_empty_search_description)
                } else {
                    stringResource(R.string.threads_empty_description)
                },
                style = MaterialTheme.wireTypography.body02,
                color = colorsScheme().secondaryText
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewThreadsEmptyContent() = WireTheme {
    ThreadsEmptyContent(isSearching = false)
}
