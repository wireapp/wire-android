package com.wire.android.core.usecase.executors

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

interface UseCaseExecutor {

    suspend fun <P, T> execute(
            owner: UseCase<P, T>,
            params: P,
            scope: CoroutineScope,
            dispatcher: CoroutineDispatcher = Dispatchers.IO,
            onResult: (Either<Failure, T>) -> Unit
    )
}