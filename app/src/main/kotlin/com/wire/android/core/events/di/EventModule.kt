package com.wire.android.core.events.di

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.gson.GsonMessageAdapter
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.WebSocketConfig
import com.wire.android.core.events.adapter.FlowStreamAdapter
import com.wire.android.core.events.datasource.EventDataSource
import com.wire.android.core.events.datasource.remote.NotificationApi
import com.wire.android.core.events.datasource.remote.NotificationRemoteDataSource
import com.wire.android.core.events.datasource.remote.WebSocketService
import com.wire.android.core.events.handler.EventsHandler
import com.wire.android.core.events.handler.MessageEventsHandler
import com.wire.android.core.events.usecase.ListenToEventsUseCase
import com.wire.android.core.network.NetworkClient
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val eventModule = module {
    fun provideWebSocketService(client: OkHttpClient, lifecycle: Lifecycle, webSocketUrl: String): WebSocketService {
        val scarlet = Scarlet.Builder()
            .webSocketFactory(client.newWebSocketFactory(webSocketUrl))
            .addMessageAdapterFactory(GsonMessageAdapter.Factory())
            .addStreamAdapterFactory(FlowStreamAdapter.Factory)
            .lifecycle(lifecycle)
            .build()
        return scarlet.create()
    }
    //TODO hardcoded client to be replaced with current clientId
    single { WebSocketConfig("7896ffbb78c95f34") }
    single {
        provideWebSocketService(
            get(),
            AndroidLifecycle.ofApplicationForeground(androidApplication(), get<WebSocketConfig>().throttleTimeout),
            get<WebSocketConfig>().socketUrl
        )
    }
    single { get<NetworkClient>().create(NotificationApi::class.java) }
    single { NotificationRemoteDataSource(get(), get()) }
    single<EventsHandler<Event.Conversation.MessageEvent>> { MessageEventsHandler(get(), get(), get(), get(), get(), get()) }
    single<EventRepository> { EventDataSource(get()) }
    single { ListenToEventsUseCase(get(), get()) }
}
