package com.wire.android.framework

import com.wire.kalium.logic.data.user.Connection
import com.wire.kalium.logic.data.user.ConnectionState

object TestConnection {
    val CONNECTION = Connection(
        TestConversation.ID.value,
        "FROM",
        "2022-03-30T15:36:00.000Z",
        TestConversation.ID,
        TestUser.USER_ID,
        ConnectionState.SENT,
        TestUser.OTHER_USER.id.value,
        TestUser.OTHER_USER
    )
}
