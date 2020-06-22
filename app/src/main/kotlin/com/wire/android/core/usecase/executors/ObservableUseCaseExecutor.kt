@file:Suppress("UNCHECKED_CAST")

package com.wire.android.core.usecase.executors

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.ObservableFailure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.ObservableUseCase
import com.wire.android.core.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ObservableUseCaseExecutor : UseCaseExecutor {

    override suspend fun <P, T> execute(
            owner: UseCase<P, T>,
            params: P,
            scope: CoroutineScope,
            dispatcher: CoroutineDispatcher,
            onResult: (Either<Failure, T>) -> Unit
    ) {
        if (owner is ObservableUseCase<*, *>) {
            val usecase: ObservableUseCase<P, T> = owner as ObservableUseCase<P, T>
            val backgroundJob = scope.async(dispatcher) { usecase.run(params) }
            scope.launch {
                backgroundJob.await()
                    .catch { exception ->
                        Either.Left(ObservableFailure(exception))
                    }
                    .collectLatest {
                        onResult(Either.Right(it))
                    }
            }
        } else throw RuntimeException(
            "ObservableUseCaseExecutor needs an ObservableUseCase, please check you're using the correct executor or executing the correct use-case"
        )
    }
}