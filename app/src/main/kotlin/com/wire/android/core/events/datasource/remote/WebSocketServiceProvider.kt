package com.wire.android.core.events.datasource.remote

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.messageadapter.gson.GsonMessageAdapter
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.wire.android.core.events.adapter.FlowStreamAdapter
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient

interface WebSocketServiceProvider {
    fun provideWebSocketService(
        webSocketUrl: String
    ): WebSocketService
}

class DefaultWebSocketServiceProvider(
    private val client: OkHttpClient,
    private val lifecycle: Lifecycle,
    private val externalScope: CoroutineScope,
) : WebSocketServiceProvider {
    override fun provideWebSocketService(
        webSocketUrl: String
    ): WebSocketService {
        val scarlet = Scarlet.Builder()
            .webSocketFactory(client.newWebSocketFactory(webSocketUrl))
            .addMessageAdapterFactory(GsonMessageAdapter.Factory())
            .addStreamAdapterFactory(FlowStreamAdapter.Factory(externalScope))
            .lifecycle(lifecycle)
            .build()
        return scarlet.create()
    }
}
