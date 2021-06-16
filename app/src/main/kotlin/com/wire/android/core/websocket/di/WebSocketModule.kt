package com.wire.android.core.websocket.di

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.protobuf.ProtobufMessageAdapter
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.wire.android.core.websocket.WebSocketConfig
import com.wire.android.core.websocket.WebSocketService
import com.wire.android.core.websocket.adapter.FlowStreamAdapter
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val webSocketModule = module {
    fun provideWebSocketService(client: OkHttpClient, lifecycle: Lifecycle, webSocketUrl: String): WebSocketService {
        val scarlet = Scarlet.Builder()
            .webSocketFactory(client.newWebSocketFactory(webSocketUrl))
            .addMessageAdapterFactory(ProtobufMessageAdapter.Factory())
            .addStreamAdapterFactory(FlowStreamAdapter.Factory)
            .lifecycle(lifecycle)
            .build()
        return scarlet.create()
    }
    //TODO hardcoded client to be replaced with current clientId
    single { WebSocketConfig("1b307f60d99f1f29") }
    single {
        provideWebSocketService(
            get(),
            AndroidLifecycle.ofApplicationForeground(androidApplication(), WebSocketConfig.THROTTLE_TIMEOUT),
            get<WebSocketConfig>().socketUrl
        )
    }
}
