package com.wire.android.core.events.handler

interface EventsHandler<in T> {
    suspend fun subscribe(event : T)
}
