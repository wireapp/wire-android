package com.wire.android.core.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface UseCase<out Type, in Params> {
    suspend fun run(params: Params): Either<Failure, Type>
}