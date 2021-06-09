package com.wire.android.core.websocket.di

import com.wire.android.core.network.di.NetworkDependencyProvider
import com.wire.android.core.websocket.WebSocketConfig
import com.wire.android.core.websocket.data.EventDataSource
import com.wire.android.core.websocket.data.WireWebSocketListener
import com.wire.android.core.websocket.usecase.CloseWebSocketUseCase
import com.wire.android.core.websocket.usecase.ListenToWebSocketUseCase
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.koin.core.qualifier.named
import org.koin.dsl.module

private fun buildSocket(
    socketOkHttpClient: OkHttpClient,
    config: WebSocketConfig,
    webSocketListener: WireWebSocketListener,
    clientId: String
) : WebSocket {
    val webSocket = socketOkHttpClient.newWebSocket(
        Request.Builder().url("${config.socketUrl}$clientId").build(),
        webSocketListener
    )
    socketOkHttpClient.dispatcher.executorService.shutdown()
    return webSocket
}

val webSocketModule = module {
    factory { EventDataSource(get(), get()) }
    factory { CloseWebSocketUseCase(get()) }
    factory { ListenToWebSocketUseCase(get()) }
    single { WebSocketConfig() }
    val webSocketClient = "WEB_SOCKET_HTTP_CLIENT"
    single(named(webSocketClient)) {
        NetworkDependencyProvider.createHttpClientWithAuth(get(), get(), get(), get())
    }
    //TODO hardcoded client should be replaced with real client value
    single { buildSocket(get(named(webSocketClient)), get(), get(), "5c1016deb756de3e") }
    single { WireWebSocketListener() }
}
