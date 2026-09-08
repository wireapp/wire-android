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

package com.wire.android.ui.home.newconversation.channelhistory

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResultContract
import com.wire.navigation.WireNavResultContractId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

/**
 * Navigation 3 contract for the channel-history step of the new-conversation flow.
 *
 * It intentionally lives next to the feature while the legacy generated destination remains
 * active. [flowId] gives every step in one new-conversation flow the same ViewModel owner.
 */
@Serializable
data class ChannelHistoryRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A channel-history flow id cannot be blank" }
    }

    companion object {
        // Kept equal to the legacy baseRoute so screen analytics remain stable during migration.
        const val ROUTE_ID = "app/channel_history_screen"
    }
}

@Serializable
data class ChannelHistoryCustomRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    val currentType: ChannelHistorySelection,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A channel-history flow id cannot be blank" }
    }

    companion object {
        // Kept equal to the legacy baseRoute so screen analytics remain stable during migration.
        const val ROUTE_ID = "app/channel_history_custom_screen"
    }
}

@Serializable
sealed interface ChannelHistorySelection {
    @Serializable
    data object Off : ChannelHistorySelection

    @Serializable
    data object Unlimited : ChannelHistorySelection

    @Serializable
    data class Specific(
        val amount: Int,
        val unit: AmountUnit,
    ) : ChannelHistorySelection {
        init {
            require(amount > 0) { "A channel-history amount must be positive" }
        }
    }

    @Serializable
    enum class AmountUnit {
        DAYS,
        WEEKS,
        MONTHS,
    }
}

@Serializable
data class ChannelHistoryCustomResult(
    val customType: ChannelHistorySelection.Specific,
)

internal val ChannelHistoryCustomResultContract = WireNavResultContract<ChannelHistoryCustomResult>(
    WireNavResultContractId("new-conversation.channel-history.custom")
)
