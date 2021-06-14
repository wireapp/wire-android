package com.wire.android.core.websocket.di

import com.wire.android.core.network.di.NetworkDependencyProvider
import com.wire.android.core.websocket.WebSocketConfig
import com.wire.android.core.websocket.data.EventDataSource
import com.wire.android.core.websocket.data.WebSocketConnection
import com.wire.android.core.websocket.data.WebSocketWorkHandler
import com.wire.android.core.websocket.usecase.ListenToWebSocketUseCase
import org.koin.android.ext.koin.androidApplication
import org.koin.core.qualifier.named
import org.koin.dsl.module

val webSocketModule = module {
    factory { EventDataSource(get()) }
    factory { ListenToWebSocketUseCase(get()) }
    single { WebSocketConfig() }
    val webSocketClient = "WEB_SOCKET_HTTP_CLIENT"
    single(named(webSocketClient)) {
        NetworkDependencyProvider.createHttpClientWithAuth(get(), get(), get(), get())
    }
    //TODO hardcoded client should be replaced with real client value
    single { WebSocketConnection(get(), get(), get(), "7af05ab5a33b2492") }
    single { WebSocketWorkHandler(androidApplication()) }
}
