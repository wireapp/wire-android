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

package com.wire.android.ui.authentication.devices.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.feature.e2ei.MLSClientIdentity
import com.wire.kalium.logic.util.inWholeWeeks
import com.wire.kalium.util.DateTimeUtil.toIsoDateTimeString
import kotlin.time.Clock

data class Device(
    val name: UIText = UIText.DynamicString(""),
    val clientId: ClientId = ClientId(""),
    val registrationTime: String? = null,
    val lastActiveInWholeWeeks: Int? = null,
    val isValid: Boolean = true,
    val isVerifiedProteus: Boolean = false,
    val mlsClientIdentity: MLSClientIdentity? = null
) {
    constructor(client: Client, mlsClientIdentity: MLSClientIdentity? = null) : this(
        name = client.displayName(),
        clientId = client.id,
        registrationTime = client.registrationTime?.toIsoDateTimeString(),
        lastActiveInWholeWeeks = client.lastActiveInWholeWeeks(),
        isValid = client.isValid,
        isVerifiedProteus = client.isVerified,
        mlsClientIdentity = mlsClientIdentity
    )

    fun updateFromClient(client: Client): Device = copy(
        name = client.displayName(),
        clientId = client.id,
        registrationTime = client.registrationTime?.toIsoDateTimeString(),
        lastActiveInWholeWeeks = client.lastActiveInWholeWeeks(),
        isValid = client.isValid,
        isVerifiedProteus = client.isVerified,
        mlsClientIdentity = null,
    )

    fun updateE2EICertificate(mlsClientIdentity: MLSClientIdentity): Device = copy(
        mlsClientIdentity = mlsClientIdentity
    )
}

/**
 * Returns the device name if it is not null, otherwise returns the device type.
 */
@Stable
fun Client.displayName(): UIText = (model ?: deviceType?.name)?.let {
    UIText.DynamicString(it)
} ?: UIText.StringResource(R.string.device_name_unknown)

fun Client.lastActiveInWholeWeeks(): Int? {
    return lastActive?.let { (Clock.System.now() - it).inWholeWeeks.toInt() }
}

@Stable
@Composable
fun Device.lastActiveDescription(): String? =
    lastActiveInWholeWeeks?.let {
        if (it == 0) {
            stringResource(R.string.label_client_last_active_time_zero_weeks)
        } else {
            stringResource(
                R.string.label_client_last_active_time,
                pluralStringResource(R.plurals.weeks_long_label, it, it)
            )
        }
    }
