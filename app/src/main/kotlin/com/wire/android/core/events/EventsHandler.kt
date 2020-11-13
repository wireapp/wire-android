package com.wire.android.core.events

class EventsHandler {
    fun <T> subscribe(onEventReceived: (T) -> Unit) { }
}
