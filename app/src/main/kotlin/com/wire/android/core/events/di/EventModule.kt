package com.wire.android.core.events.di

import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.wire.android.core.config.GlobalConfig
import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.WebSocketConfig
import com.wire.android.core.events.datasource.EventDataSource
import com.wire.android.core.events.datasource.local.NotificationLocalDataSource
import com.wire.android.core.events.datasource.remote.DefaultWebSocketServiceProvider
import com.wire.android.core.events.datasource.remote.NotificationApi
import com.wire.android.core.events.datasource.remote.NotificationRemoteDataSource
import com.wire.android.core.events.datasource.remote.WebSocketServiceProvider
import com.wire.android.core.events.handler.EventsHandler
import com.wire.android.core.events.handler.MessageEventsHandler
import com.wire.android.core.events.mapper.EventMapper
import com.wire.android.core.events.usecase.ListenToEventsUseCase
import com.wire.android.core.network.NetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val eventModule = module {
    single { WebSocketConfig(GlobalConfig.WEB_SOCKET_URL) }
    factory { CoroutineScope(Dispatchers.IO) }
    factory { AndroidLifecycle.ofApplicationForeground(androidApplication(), get<WebSocketConfig>().throttleTimeout) }
    factory<WebSocketServiceProvider> { DefaultWebSocketServiceProvider(get(), get(), get()) }
    single { NotificationRemoteDataSource(get(), get(), get(), get(), get()) }
    single { get<NetworkClient>().create(NotificationApi::class.java) }
    single<EventsHandler<Event.Conversation.MessageEvent>> { MessageEventsHandler(get(), get(), get(), get(), get()) }
    factory { NotificationLocalDataSource(get()) }
    single<EventRepository> { EventDataSource(get(), get(), get(), get()) }
    single { ListenToEventsUseCase(get(), get()) }
    factory { EventMapper() }
}
