package com.wire.android.core.websocket.data

import android.util.Log
import com.wire.android.core.websocket.EventRepository
import kotlinx.coroutines.flow.Flow
import okhttp3.WebSocket
import java.io.IOException

class EventDataSource(
    private var wireWebSocketListener: WireWebSocketListener?,
    private var webSocket: WebSocket?
) : EventRepository {

    override fun startSocket(): Flow<Message> = with(wireWebSocketListener) {
        this@with?.socketFlow!!
    }

    override fun closeSocket() {
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
