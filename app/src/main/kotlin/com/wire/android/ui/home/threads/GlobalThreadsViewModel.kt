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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.mapper.toUIPreview
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.UiTextResolver
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.android.R
import com.wire.kalium.logic.feature.message.ObserveGlobalThreadsResult
import com.wire.kalium.logic.feature.message.ObserveGlobalThreadsUseCase
import com.wire.kalium.logic.feature.message.GlobalThreadSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

@HiltViewModel
class GlobalThreadsViewModel @Inject constructor(
    private val observeGlobalThreads: ObserveGlobalThreadsUseCase,
    private val uiTextResolver: UiTextResolver,
) : ViewModel() {

    var state by mutableStateOf(GlobalThreadsState())
        private set

    init {
        viewModelScope.launch {
            observeGlobalThreads().collect { result ->
                state = when (result) {
                    ObserveGlobalThreadsResult.Failure -> GlobalThreadsState(isLoading = false, threads = emptyList())
                    is ObserveGlobalThreadsResult.Success -> GlobalThreadsState(
                        isLoading = false,
                        threads = result.threads.map(::toUiThread)
                    )
                }
            }
        }
    }

    private fun toUiThread(thread: GlobalThreadSummary): UiGlobalThread {
        val previewContent = thread.rootMessage.toUIPreview(emptyMap(), uiTextResolver)
        val previewText = previewContent.toPlainText(uiTextResolver).ifBlank {
            uiTextResolver.resolve(UIText.StringResource(R.string.thread_root_fallback_label))
        }

        return UiGlobalThread(
            conversationId = thread.conversationId,
            conversationName = thread.conversationName,
            conversationType = when (thread.conversationType) {
                Conversation.Type.Group.Channel -> UiGlobalThread.ConversationType.CHANNEL
                Conversation.Type.Group.Regular -> UiGlobalThread.ConversationType.GROUP
                else -> UiGlobalThread.ConversationType.ONE_ON_ONE
            },
            avatarData = if (thread.conversationType == Conversation.Type.OneOnOne || thread.conversationType == Conversation.Type.ConnectionPending) {
                UserAvatarData(
                    asset = thread.otherUserPreviewAssetId?.let(::UserAvatarAsset),
                    availabilityStatus = thread.otherUserAvailabilityStatus,
                    connectionState = thread.otherUserConnectionStatus,
                    nameBasedAvatar = NameBasedAvatar(
                        fullName = thread.conversationName,
                        accentColor = thread.otherUserAccentId ?: -1
                    )
                )
            } else {
                null
            },
            rootMessageId = thread.rootMessageId,
            threadId = thread.threadId,
            rootMessageSelfDeletionDurationMillis = thread.rootMessageSelfDeletionDurationMillis,
            previewText = previewText,
            replyCount = thread.visibleReplyCount,
            lastActivityAt = thread.lastReplyDate ?: thread.createdAt,
            searchText = buildString {
                append(thread.conversationName.orEmpty())
                append('\n')
                append(previewText)
            }
        )
    }
}

data class GlobalThreadsState(
    val isLoading: Boolean = true,
    val threads: List<UiGlobalThread> = emptyList(),
)

data class UiGlobalThread(
    val conversationId: ConversationId,
    val conversationName: String?,
    val conversationType: ConversationType,
    val avatarData: UserAvatarData?,
    val rootMessageId: String,
    val threadId: String,
    val rootMessageSelfDeletionDurationMillis: Long?,
    val previewText: String,
    val replyCount: Long,
    val lastActivityAt: Instant,
    val searchText: String,
) {
    enum class ConversationType {
        ONE_ON_ONE,
        GROUP,
        CHANNEL,
    }
}

private fun UILastMessageContent.toPlainText(uiTextResolver: UiTextResolver): String = when (this) {
    UILastMessageContent.None -> ""
    is UILastMessageContent.Connection -> ""
    is UILastMessageContent.MultipleMessage -> messages.joinToString(separator = " ") { uiTextResolver.resolve(it) }
    is UILastMessageContent.SenderWithMessage -> {
        listOf(sender, message)
            .joinToString(separator = " ") { uiTextResolver.resolve(it) }
            .trim()
    }

    is UILastMessageContent.TextMessage -> uiTextResolver.resolve(messageBody.message)
    is UILastMessageContent.VerificationChanged -> uiTextResolver.resolve(UIText.StringResource(textResId))
}
