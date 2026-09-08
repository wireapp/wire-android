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

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResultContract
import com.wire.navigation.WireNavResultContractId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

@Serializable
data class ConversationRouteId(
    val value: String,
    val domain: String,
) {
    init {
        require(value.isNotBlank())
        require(domain.isNotBlank())
    }
}

@Serializable
data class ConversationPendingAsset(
    val key: String,
    val mimeType: String,
    val dataPath: String,
    val dataSize: Long,
    val fileName: String,
    val assetType: String,
    val audioWavesMask: List<Int>? = null,
)

@Serializable
data class ConversationRoute(
    override val sessionId: WireSessionId,
    val conversationId: ConversationRouteId,
    val searchedMessageId: String? = null,
    val pendingAssets: List<ConversationPendingAsset> = emptyList(),
    val pendingText: String? = null,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/conversation_screen"
    }
}

@Serializable
enum class ConversationCompletionAction {
    LEAVE_GROUP,
    DELETE_GROUP,
}

@Serializable
data class ConversationCompletionResult(
    val action: ConversationCompletionAction,
    val conversationName: String,
)

internal val ConversationCompletionResultContract =
    WireNavResultContract<ConversationCompletionResult>(
        WireNavResultContractId("conversation.completion")
    )
