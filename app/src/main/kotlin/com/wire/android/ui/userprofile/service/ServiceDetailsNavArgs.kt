/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.userprofile.service

import com.ramcosta.composedestinations.navargs.DestinationsNavTypeSerializer
import com.ramcosta.composedestinations.navargs.NavTypeSerializer
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.VALUE_DOMAIN_SEPARATOR
import com.wire.kalium.logic.data.user.BotService

data class ServiceDetailsNavArgs(
    val botService: BotService,
    val conversationId: ConversationId
)

@NavTypeSerializer
class BotServiceNavTypeSerializer : DestinationsNavTypeSerializer<BotService> {
    override fun toRouteString(value: BotService): String = value.toString()
    override fun fromRouteString(routeString: String): BotService = routeString.split(VALUE_DOMAIN_SEPARATOR).takeIf {
        it.size > 1
    }?.let {
        BotService(it.first(), it.last())
    } ?: run {
        BotService(routeString, String.EMPTY)
    }
}
