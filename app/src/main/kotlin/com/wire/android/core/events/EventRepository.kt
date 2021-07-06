package com.wire.android.core.events

import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun events(): Flow<Event>
}
