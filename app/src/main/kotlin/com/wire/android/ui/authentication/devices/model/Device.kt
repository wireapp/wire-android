package com.wire.android.ui.authentication.devices.model

import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.conversation.ClientId

data class Device(
    val name: String = "",
    val clientId: ClientId = ClientId(""),
    val registrationTime: String = "",
    val isValid: Boolean = true
) {
    constructor(client: Client) : this(client.name, client.id, client.registrationTime, true)
}
