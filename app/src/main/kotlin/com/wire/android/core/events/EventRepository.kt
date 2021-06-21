package com.wire.android.core.events

import com.wire.android.core.events.datasource.remote.EventResponse
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun events(): Flow<EventResponse>
}
