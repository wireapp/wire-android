package com.wire.android.core.storage.cache

import com.wire.android.core.exception.EmptyCacheFailure
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface CacheService {
    suspend fun <T> requestCache(call: suspend () -> T?): Either<Failure, T> =
        call()?.let { Either.Right(it) }
            ?: Either.Left(EmptyCacheFailure)
}
