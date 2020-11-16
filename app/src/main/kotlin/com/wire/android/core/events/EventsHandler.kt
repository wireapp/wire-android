package com.wire.android.core.events

import android.util.Log

class EventsHandler {
    fun <T> subscribe(onEventReceived: (T) -> Unit) { Log.d("EventsHandler", "subscribed!!!") }
}
