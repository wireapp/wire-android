package com.wire.android.core.websocket

import com.wire.android.core.events.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun events() : Flow<Event>
}
