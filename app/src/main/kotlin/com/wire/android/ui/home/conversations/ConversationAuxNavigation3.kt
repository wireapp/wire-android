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
data class ConversationAuxId(val value: String, val domain: String) {
    init {
        require(value.isNotBlank())
        require(domain.isNotBlank())
    }
}

@Serializable
data class BrowseChannelsRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/browse_channels_screen"
    }
}

@Serializable
data class ConversationFoldersRoute(
    override val sessionId: WireSessionId,
    val conversationId: ConversationAuxId,
    val conversationName: String,
    val currentFolderId: String?,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/conversation_folders_screen"
    }
}

@Serializable
data class NewConversationFolderRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId = ROUTE_ID

    init {
        require(flowId.isNotBlank())
    }

    companion object {
        const val ROUTE_ID = "app/new_conversation_folder_screen"
    }
}

@Serializable
data class SearchConversationMessagesRoute(
    override val sessionId: WireSessionId,
    val conversationId: ConversationAuxId,
    val groupName: String = "",
    val isCellsConversation: Boolean = false,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/search_conversation_messages_screen"
    }
}

@Serializable
data class PromoteAdminRoute(
    override val sessionId: WireSessionId,
    val conversationId: ConversationAuxId,
    val eligibleMembers: List<ConversationAuxId> = emptyList(),
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/promote_admin_screen"
    }
}

@Serializable
sealed interface ConversationProtocolSelection {
    @Serializable
    data object Proteus : ConversationProtocolSelection

    @Serializable
    sealed interface MlsCapable : ConversationProtocolSelection {
        val groupId: String
        val groupState: GroupState
        val epoch: ULong
        val keyingMaterialLastUpdate: String
        val cipherSuiteTag: Int
    }

    @Serializable
    data class Mls(
        override val groupId: String,
        override val groupState: GroupState,
        override val epoch: ULong,
        override val keyingMaterialLastUpdate: String,
        override val cipherSuiteTag: Int,
    ) : MlsCapable

    @Serializable
    data class Mixed(
        override val groupId: String,
        override val groupState: GroupState,
        override val epoch: ULong,
        override val keyingMaterialLastUpdate: String,
        override val cipherSuiteTag: Int,
    ) : MlsCapable

    @Serializable
    enum class GroupState {
        PENDING_CREATION,
        PENDING_JOIN,
        PENDING_WELCOME_MESSAGE,
        ESTABLISHED,
        PENDING_AFTER_RESET,
    }
}

@Serializable
data class AddMembersSearchRoute(
    override val sessionId: WireSessionId,
    val conversationId: ConversationAuxId,
    val isConversationAppsEnabled: Boolean,
    val isSelfPartOfATeam: Boolean,
    val protocol: ConversationProtocolSelection,
    val shouldUseNewAppsUi: Boolean = true,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/add_members_search_screen"
    }
}

@Serializable
data class DebugConversationRoute(
    override val sessionId: WireSessionId,
    val conversationId: ConversationAuxId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/debug_conversation_screen"
    }
}

@Serializable
data class ConversationFoldersResult(val message: String)

internal val ConversationFoldersResultContract = WireNavResultContract<ConversationFoldersResult>(
    WireNavResultContractId("conversation-folders.completed")
)

@Serializable
data class NewConversationFolderResult(
    val folderName: String,
    val folderId: String,
)

internal val NewConversationFolderResultContract = WireNavResultContract<NewConversationFolderResult>(
    WireNavResultContractId("conversation-folders.created")
)
