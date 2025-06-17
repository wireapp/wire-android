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

package com.wire.android.framework

import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.client.ClientType
import com.wire.kalium.logic.data.client.DeviceType
import com.wire.kalium.logic.data.conversation.ClientId
import kotlinx.datetime.Instant

object TestClient {
    val CLIENT_ID = ClientId("test")

    val CLIENT = Client(
        id = CLIENT_ID,
        type = ClientType.Permanent,
        registrationTime = Instant.DISTANT_FUTURE,
        lastActive = Instant.DISTANT_PAST,
        isVerified = false,
        isValid = true,
        deviceType = DeviceType.Desktop,
        label = "label",
        model = null,
        isMLSCapable = false,
        mlsPublicKeys = null,
        isAsyncNotificationsCapable = false
    )
}
