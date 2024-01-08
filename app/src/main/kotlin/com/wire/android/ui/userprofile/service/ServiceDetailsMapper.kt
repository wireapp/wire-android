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

import com.wire.kalium.logic.data.id.VALUE_DOMAIN_SEPARATOR
import com.wire.kalium.logic.data.service.ServiceId
import com.wire.kalium.logic.data.user.BotService
import javax.inject.Inject

class ServiceDetailsMapper @Inject constructor() {
    fun fromStringToServiceId(id: String): ServiceId? {
        val components = id.split(VALUE_DOMAIN_SEPARATOR).filter { it.isNotBlank() }
        val count = id.count { it == VALUE_DOMAIN_SEPARATOR }

        return when {
            count == 1 && components.size == 2 -> {
                ServiceId(
                    id = components.first(),
                    provider = components.last()
                )
            }
            else -> null
        }
    }

    fun fromBotServiceToServiceId(botService: BotService): ServiceId = ServiceId(botService.id, botService.provider)
}
