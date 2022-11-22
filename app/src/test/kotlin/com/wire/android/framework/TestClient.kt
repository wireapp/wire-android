package com.wire.android.framework

import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.client.ClientType
import com.wire.kalium.logic.data.conversation.ClientId

object TestClient {
    val CLIENT_ID = ClientId("test")

    val CLIENT = Client(
        CLIENT_ID, ClientType.Permanent, "time", null,
        null, "label", "cookie", null, "model", emptyMap()
    )
}
