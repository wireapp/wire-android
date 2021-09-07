package com.wire.android.core.events.datasource.remote

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.messageadapter.gson.GsonMessageAdapter
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.wire.android.core.events.adapter.FlowStreamAdapter
import okhttp3.OkHttpClient

interface WebSocketServiceProvider {
    fun provideWebSocketService(client: OkHttpClient, lifecycle: Lifecycle, webSocketUrl: String): WebSocketService
}

class DefaultWebSocketServiceProvider: WebSocketServiceProvider{
    override fun provideWebSocketService(
        client: OkHttpClient,
        lifecycle: Lifecycle,
        webSocketUrl: String
    ): WebSocketService {
        val scarlet = Scarlet.Builder()
            .webSocketFactory(client.newWebSocketFactory(webSocketUrl))
            .addMessageAdapterFactory(GsonMessageAdapter.Factory())
            .addStreamAdapterFactory(FlowStreamAdapter.Factory)
            .lifecycle(lifecycle)
            .build()
        return scarlet.create()
    }
}