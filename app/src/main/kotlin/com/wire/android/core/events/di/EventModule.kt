package com.wire.android.core.events.di

import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.EventsHandler
import com.wire.android.core.events.datasource.EventDataSource
import com.wire.android.core.events.usecase.ListenToEventsUseCase
import org.koin.dsl.module

val eventModule = module {
    single { EventsHandler() }
    single<EventRepository> { EventDataSource(get()) }
    single { ListenToEventsUseCase(get()) }
}
