package com.wire.android.ui.authentication.devices.model

import com.wire.kalium.logic.data.conversation.ClientId

data class Device(
    val name: String = "",
    val clientId: ClientId = ClientId(""),
    val registrationTime: String = ""
)
