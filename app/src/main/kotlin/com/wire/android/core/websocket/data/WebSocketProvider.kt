package com.wire.android.core.websocket.data

import android.util.Log
import kotlinx.coroutines.channels.Channel
import okhttp3.WebSocket
import java.io.IOException

class WebSocketProvider(private var wireWebSocketListener: WireWebSocketListener?, private var webSocket: WebSocket?) {

    fun startSocket(): Channel<Message> =
        with(wireWebSocketListener) {
            this@with?.socketEventChannel!!
        }

    fun stopSocket() {
        try {
            webSocket?.close(NORMAL_CLOSURE_STATUS, null)
            webSocket = null
            wireWebSocketListener?.socketEventChannel?.close()
            wireWebSocketListener = null
        } catch (ex: IOException) {
            Log.d("TAG", "stopSocket: $ex")
        }
    }


    companion object {
        const val NORMAL_CLOSURE_STATUS = 1000
    }

}
