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
import com.wire.kalium.logic.data.service.ServiceId
import com.wire.kalium.logic.data.user.BotService
import com.wire.kalium.logic.data.user.UserId
import kotlinx.serialization.Serializable

@Serializable
data class ServiceDetailsNavArgs(
    val conversationId: ConversationId?,
    val id: Id
) {
    sealed interface Id {
        val serviceId: ServiceId
        val userId: UserId

        data class BotServiceId(val botService: BotService) : Id {
            override val serviceId: ServiceId
                get() = ServiceId(botService.id, botService.provider)

            override val userId: UserId
                get() = UserId(botService.id, botService.provider)
        }

        data class AppId(val appId: UserId) : Id {
            override val serviceId: ServiceId
                get() = ServiceId(appId.value, appId.domain)

            override val userId: UserId
                get() = appId
        }
    }
}

@NavTypeSerializer
class ServiceDetailsIdNavTypeSerializer : DestinationsNavTypeSerializer<ServiceDetailsNavArgs.Id> {
    override fun toRouteString(value: ServiceDetailsNavArgs.Id): String = when (value) {
        is ServiceDetailsNavArgs.Id.AppId -> APP_PREFIX + value.appId.toString()
        is ServiceDetailsNavArgs.Id.BotServiceId -> BOT_PREFIX + value.botService.toString()
    }

    override fun fromRouteString(routeStr: String): ServiceDetailsNavArgs.Id = when {
        routeStr.startsWith(APP_PREFIX) -> {
            routeStr.removePrefix(APP_PREFIX).split(VALUE_DOMAIN_SEPARATOR).takeIf {
                it.size > 1
            }?.let {
                ServiceDetailsNavArgs.Id.AppId(UserId(it.first(), it.last()))
            } ?: run {
                ServiceDetailsNavArgs.Id.AppId(UserId(routeStr.removePrefix(APP_PREFIX), String.EMPTY))
            }
        }
        routeStr.startsWith(BOT_PREFIX) -> {
            routeStr.removePrefix(BOT_PREFIX).split(VALUE_DOMAIN_SEPARATOR).takeIf {
                it.size > 1
            }?.let {
                ServiceDetailsNavArgs.Id.BotServiceId(BotService(it.first(), it.last()))
            } ?: run {
                ServiceDetailsNavArgs.Id.BotServiceId(BotService(routeStr.removePrefix(BOT_PREFIX), String.EMPTY))
            }
        }
        else -> throw IllegalArgumentException("Invalid route string for ServiceDetailsNavArgs.Id: $routeStr")
    }

    private companion object {
        const val BOT_PREFIX = "bot:"
        const val APP_PREFIX = "app:"
    }
}
