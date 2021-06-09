package com.wire.android.core.websocket.data

import com.wire.android.core.exception.SocketAbortedFailure
import com.wire.android.core.exception.SocketFailure
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString


class WireWebSocketListener : WebSocketListener() {

    val socketEventChannel: Channel<Message> = Channel(Channel.UNLIMITED)
    val socketFlow = socketEventChannel.consumeAsFlow()

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        GlobalScope.launch {
            socketEventChannel.send(Message(failure = SocketAbortedFailure))
        }
        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        socketEventChannel.close()
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        GlobalScope.launch {
            socketEventChannel.send(Message(byteString = bytes))
        }
        super.onMessage(webSocket, bytes)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        GlobalScope.launch {
            socketEventChannel.send(Message(failure = SocketFailure(t)))
        }
    }
    companion object {
        const val NORMAL_CLOSURE_STATUS = 1000
    }

}
