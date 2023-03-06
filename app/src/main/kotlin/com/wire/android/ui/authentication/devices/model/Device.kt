/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.authentication.devices.model

import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.util.DateTimeUtil.toIsoDateTimeString

data class Device(
    val name: String = "",
    val clientId: ClientId = ClientId(""),
    val registrationTime: String? = null,
    val isValid: Boolean = true
) {
    constructor(client: Client) : this(client.name, client.id, client.registrationTime?.toIsoDateTimeString(), true)
}
