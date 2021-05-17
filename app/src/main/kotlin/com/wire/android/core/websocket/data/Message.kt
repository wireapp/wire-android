package com.wire.android.core.websocket.data

import com.wire.android.core.exception.Failure
import okio.ByteString

data class Message(
    val text: String? = null,
    val byteString: ByteString? = null,
    val failure: Failure? = null
)
