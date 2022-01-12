package com.wire.android.core.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import kotlinx.coroutines.flow.Flow

interface UseCase<out Type, in Params> {
    suspend fun run(params: Params): Either<Failure, Type>
}

interface ObservableUseCase<out Type, in Params> {
    suspend fun run(params: Params): Flow<Type>
}
