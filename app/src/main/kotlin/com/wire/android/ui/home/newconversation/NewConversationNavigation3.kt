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

package com.wire.android.ui.home.newconversation

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

/**
 * Navigation 3 contract shared by the core steps which own one [NewConversationViewModel].
 *
 * [flowId] is deliberately required instead of being derived from a route or back-stack position.
 * The Navigation 3 flow decorator can therefore retain one Metro/ViewModel scope while individual
 * entries are pushed and popped, including after saved-state restoration.
 */
sealed interface NewConversationRoute : SessionRoute {
    override val flowId: String
}

@Serializable
data class NewConversationSearchPeopleRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : NewConversationRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        validateNewConversationFlowId(flowId)
    }

    companion object {
        const val ROUTE_ID = "app/new_conversation_search_people_screen"

        fun start(sessionId: WireSessionId): NewConversationSearchPeopleRoute {
            val entryId = WireNavEntryId.random()
            return NewConversationSearchPeopleRoute(
                sessionId = sessionId,
                flowId = entryId.value,
                entryId = entryId,
            )
        }
    }
}

@Serializable
data class NewGroupConversationSearchPeopleRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : NewConversationRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        validateNewConversationFlowId(flowId)
    }

    companion object {
        const val ROUTE_ID = "app/new_group_conversation_search_people_screen"
    }
}

@Serializable
data class NewGroupNameRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : NewConversationRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        validateNewConversationFlowId(flowId)
    }

    companion object {
        const val ROUTE_ID = "app/new_group_name_screen"
    }
}

@Serializable
data class GroupOptionRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : NewConversationRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        validateNewConversationFlowId(flowId)
    }

    companion object {
        const val ROUTE_ID = "app/group_option_screen"
    }
}

@Serializable
data class ChannelAccessOnCreateRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : NewConversationRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        validateNewConversationFlowId(flowId)
    }

    companion object {
        const val ROUTE_ID = "app/channel_access_on_create_screen"
    }
}

private fun validateNewConversationFlowId(flowId: String) {
    require(flowId.isNotBlank()) { "A new-conversation flow id cannot be blank" }
}
