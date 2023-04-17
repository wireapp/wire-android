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

import androidx.compose.runtime.Stable
import com.wire.android.R
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.util.DateTimeUtil.toIsoDateTimeString

data class Device(
    val name: UIText = UIText.DynamicString(""),
    val clientId: ClientId = ClientId(""),
    val registrationTime: String? = null,
    val isValid: Boolean = true,
    val isVerified: Boolean = false
) {
    constructor(client: Client) : this(
        client.displayName(),
        client.id,
        client.registrationTime?.toIsoDateTimeString(),
        true,
        client.isVerified
    )
}

/**
 * Returns the device name if it is not null, otherwise returns the device type.
 */
@Stable
fun Client.displayName(): UIText = (model ?: deviceType?.name)?.let {
    UIText.DynamicString(it)
} ?: UIText.StringResource(R.string.device_name_unknown)
