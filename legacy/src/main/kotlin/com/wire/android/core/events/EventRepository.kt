package com.wire.android.core.events

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    suspend fun events(): Flow<Either<Failure, Event>>
}
