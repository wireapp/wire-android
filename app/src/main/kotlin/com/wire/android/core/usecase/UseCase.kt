package com.wire.android.core.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import kotlinx.coroutines.flow.Flow

interface UseCase<Params, Type> {
    suspend fun run(params: Params): Type
}

interface OneShotUseCase<Params, Type> : UseCase<Params, Either<Failure, Type>>
interface ObservableUseCase<Params, Type> : UseCase<Params, Flow<Type>>
