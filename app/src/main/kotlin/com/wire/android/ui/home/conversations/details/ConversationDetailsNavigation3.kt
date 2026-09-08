/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.conversations.details

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResultContract
import com.wire.navigation.WireNavResultContractId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

@Serializable
data class ConversationDetailsId(val value: String, val domain: String) {
    init {
        require(value.isNotBlank())
        require(domain.isNotBlank())
    }
}

sealed interface ConversationDetailsRoute : SessionRoute {
    val conversationId: ConversationDetailsId
}

@Serializable
data class GroupConversationDetailsRoute(
    override val sessionId: WireSessionId,
    override val conversationId: ConversationDetailsId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : ConversationDetailsRoute {
    override val routeId = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/group_conversation_details_screen"
    }
}

@Serializable
data class EditConversationNameRoute(
    override val sessionId: WireSessionId,
    override val conversationId: ConversationDetailsId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : ConversationDetailsRoute {
    override val routeId = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/edit_conversation_name_screen"
    }
}

@Serializable
data class EditSelfDeletingMessagesRoute(
    override val sessionId: WireSessionId,
    override val conversationId: ConversationDetailsId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : ConversationDetailsRoute {
    override val routeId = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/edit_self_deleting_messages_screen"
    }
}

@Serializable
data class GroupConversationAllParticipantsRoute(
    override val sessionId: WireSessionId,
    override val conversationId: ConversationDetailsId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : ConversationDetailsRoute {
    override val routeId = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/group_conversation_all_participants_screen"
    }
}

@Serializable
data class UpdateAppsAccessRoute(
    override val sessionId: WireSessionId,
    override val conversationId: ConversationDetailsId,
    val isGuestAllowed: Boolean,
    val isAppsAllowed: Boolean,
    val shouldUseNewAppsUi: Boolean,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : ConversationDetailsRoute {
    override val routeId = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/update_apps_access_screen"
    }
}

@Serializable
enum class ChannelAccessSelection {
    PUBLIC,
    PRIVATE,
}

@Serializable
enum class ChannelPermissionSelection {
    ADMINS,
    EVERYONE,
}

@Serializable
data class ChannelAccessOnUpdateRoute(
    override val sessionId: WireSessionId,
    override val conversationId: ConversationDetailsId,
    val accessType: ChannelAccessSelection = ChannelAccessSelection.PRIVATE,
    val permissionType: ChannelPermissionSelection = ChannelPermissionSelection.ADMINS,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : ConversationDetailsRoute {
    override val routeId = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/channel_access_on_update_screen"
    }
}

@Serializable
data class EditGuestAccessRoute(
    override val sessionId: WireSessionId,
    override val conversationId: ConversationDetailsId,
    val isGuestAccessAllowed: Boolean,
    val isServicesAllowed: Boolean,
    val isUpdatingGuestAccessAllowed: Boolean,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : ConversationDetailsRoute {
    override val routeId = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/edit_guest_access_screen"
    }
}

@Serializable
data class CreatePasswordProtectedGuestLinkRoute(
    override val sessionId: WireSessionId,
    override val conversationId: ConversationDetailsId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : ConversationDetailsRoute {
    override val routeId = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/create_password_protected_guest_link_screen"
    }
}

@Serializable
data class EditConversationNameResult(val succeeded: Boolean)

internal val EditConversationNameResultContract =
    WireNavResultContract<EditConversationNameResult>(
        WireNavResultContractId("conversation-details.edit-name")
    )

@Serializable
data class ChannelAccessUpdateResult(
    val accessType: ChannelAccessSelection,
    val permissionType: ChannelPermissionSelection,
)

internal val ChannelAccessUpdateResultContract =
    WireNavResultContract<ChannelAccessUpdateResult>(
        WireNavResultContractId("conversation-details.channel-access")
    )
