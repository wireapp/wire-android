package com.wire.android.ui.home.conversations.messages

import com.wire.android.ui.home.conversations.ConversationSnackbarMessages
import com.wire.android.ui.home.conversations.DownloadedAssetDialogVisibilityState
import com.wire.android.ui.home.conversations.model.UIMessage

data class ConversationMessagesViewState(
    val messages: List<UIMessage> = emptyList(),
    val lastUnreadMessage: UIMessage? = null,
    val snackbarMessage: ConversationSnackbarMessages? = null,
    val downloadedAssetDialogState: DownloadedAssetDialogVisibilityState = DownloadedAssetDialogVisibilityState.Hidden
)
