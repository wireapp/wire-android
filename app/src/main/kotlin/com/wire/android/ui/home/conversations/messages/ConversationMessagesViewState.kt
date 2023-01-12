package com.wire.android.ui.home.conversations.messages

import androidx.paging.PagingData
import com.wire.android.media.AudioState
import com.wire.android.ui.home.conversations.DownloadedAssetDialogVisibilityState
import com.wire.android.ui.home.conversations.model.UIMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.Instant

data class ConversationMessagesViewState(
    val messages: Flow<PagingData<UIMessage>> = emptyFlow(),
    val firstUnreadInstant: Instant? = null,
    val downloadedAssetDialogState: DownloadedAssetDialogVisibilityState = DownloadedAssetDialogVisibilityState.Hidden,
    val audioMessagesState: Map<String, AudioState> = emptyMap()
)
