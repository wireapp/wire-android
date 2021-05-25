package com.wire.android.core.websocket.di

import com.wire.android.core.network.auth.accesstoken.AccessTokenInterceptor
import com.wire.android.core.websocket.WebSocketConfig
import com.wire.android.core.websocket.data.WebSocketDataSource
import com.wire.android.core.websocket.data.WebSocketProvider
import com.wire.android.core.websocket.data.WireWebSocketListener
import com.wire.android.core.websocket.usecase.CloseWebSocketUseCase
import com.wire.android.core.websocket.usecase.ListenToWebSocketUseCase
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

const val READ_TIMEOUT: Long = 30
const val CONNECT_TIMEOUT: Long = 30
const val PING_INTERVAL: Long = 30

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


private fun socketHttpClient(accessTokenInterceptor: AccessTokenInterceptor): OkHttpClient =
    OkHttpClient.Builder()
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .pingInterval(PING_INTERVAL, TimeUnit.SECONDS)
        .addInterceptor(accessTokenInterceptor)
        .hostnameVerifier { _, _ -> true }
        .build()


val webSocketModule = module {
    factory { WebSocketProvider(get(), get()) }
    factory { WebSocketDataSource(get()) }
    factory { CloseWebSocketUseCase(get()) }
    factory { ListenToWebSocketUseCase(get()) }
    single { WebSocketConfig() }
    val webSocketClient = "WEB_SOCKET_HTTP_CLIENT"
    single(named(webSocketClient)) {
        socketHttpClient(get())
    }
    //TODO hardcoded client should be replaced with real client value
    single { buildSocket(get(named(webSocketClient)), get(), get(), "9ea731e3f0de0735") }
    single { WireWebSocketListener() }
}
