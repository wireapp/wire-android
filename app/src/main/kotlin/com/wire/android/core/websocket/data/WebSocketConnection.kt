package com.wire.android.core.websocket.data

import com.wire.android.core.exception.SocketAbortedFailure
import com.wire.android.core.websocket.WebSocketConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class WebSocketConnection(
    private val webSocketWorkHandler: WebSocketWorkHandler,
    private val socketOkHttpClient: OkHttpClient,
    private val config: WebSocketConfig,
    private val clientId: String
) : WebSocketListener() {

    private var webSocket: WebSocket? = null
    private val socketEventChannel: Channel<Message> = Channel(Channel.UNLIMITED)
    val socketFlow = socketEventChannel.receiveAsFlow()
    var isConnected = false

    fun connect() {
        if (!isConnected)
            webSocket = socketOkHttpClient.newWebSocket(Request.Builder().url("${config.socketUrl}$clientId").build(), this)
    }

    fun close() {
        webSocket?.close(NORMAL_CLOSURE_STATUS, "Closed")
        socketEventChannel.close()
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        isConnected = true
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        isConnected = false
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
        isConnected = false
        webSocketWorkHandler.run()
    }

    companion object {
        const val NORMAL_CLOSURE_STATUS = 1000
    }
}
