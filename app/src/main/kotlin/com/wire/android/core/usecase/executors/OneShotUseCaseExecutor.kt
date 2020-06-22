@file:Suppress("UNCHECKED_CAST")

package com.wire.android.core.usecase.executors

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.OneShotUseCase
import com.wire.android.core.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class OneShotUseCaseExecutor : UseCaseExecutor {

    override suspend fun <P, T> execute(
            owner: UseCase<P, T>,
            params: P,
            scope: CoroutineScope,
            dispatcher: CoroutineDispatcher,
            onResult: (Either<Failure, T>) -> Unit
    ) {
        if (owner is OneShotUseCase<*, *>) {
            val usecase: OneShotUseCase<P, T> = owner as OneShotUseCase<P, T>
            val backgroundJob = scope.async { usecase.run(params) }
            scope.launch {
                onResult(backgroundJob.await())
            }
        } else throw RuntimeException(
            "OneShotUseCaseExecutor needs a OneShotUseCase to execute, please check you're using the correct executor or executing the correct use-case"
        )
    }
}