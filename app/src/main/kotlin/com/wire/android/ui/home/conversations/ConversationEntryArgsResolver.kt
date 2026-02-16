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
package com.wire.android.ui.home.conversations

import androidx.lifecycle.SavedStateHandle
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.kalium.logic.data.id.QualifiedID

internal data class ThreadEntryContext(
    val threadId: String,
    val threadRootMessageId: String,
    val threadRootSelfDeletionDurationMillis: Long?,
)

internal data class ConversationEntryArgs(
    val conversationId: QualifiedID,
    val searchedMessageId: String? = null,
    val pendingBundles: ArrayList<AssetBundle>? = null,
    val pendingTextBundle: String? = null,
    val threadContext: ThreadEntryContext? = null,
)

internal fun SavedStateHandle.resolveConversationEntryArgs(): ConversationEntryArgs {
    val threadArgs = runCatching { navArgs<ThreadConversationNavArgs>() }.getOrNull()
    if (threadArgs != null) {
        return ConversationEntryArgs(
            conversationId = threadArgs.conversationId,
            threadContext = ThreadEntryContext(
                threadId = threadArgs.threadId,
                threadRootMessageId = threadArgs.threadRootMessageId,
                threadRootSelfDeletionDurationMillis = threadArgs.threadRootSelfDeletionDurationMillis
            )
        )
    }

    val conversationArgs = navArgs<ConversationNavArgs>()
    return ConversationEntryArgs(
        conversationId = conversationArgs.conversationId,
        searchedMessageId = conversationArgs.searchedMessageId,
        pendingBundles = conversationArgs.pendingBundles,
        pendingTextBundle = conversationArgs.pendingTextBundle
    )
}
