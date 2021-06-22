package com.wire.android.core.usecase

import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

interface UseCaseExecutor {
    val dispatcherProvider: DispatcherProvider

    operator fun <T, P> UseCase<T, P>.invoke(
        scope: CoroutineScope,
        params: P,
        dispatcher: CoroutineDispatcher = dispatcherProvider.io(),
        onResult: (Either<Failure, T>) -> Unit = {}
    )

    operator fun <T, P> ObservableUseCase<T, P>.invoke(
        scope: CoroutineScope,
        params: P,
        dispatcher: CoroutineDispatcher = dispatcherProvider.io(),
        onResult: (T) -> Unit = {}
    )
}

class DefaultUseCaseExecutor(override val dispatcherProvider: DispatcherProvider) : UseCaseExecutor {

    override operator fun <T, P> UseCase<T, P>.invoke(
        scope: CoroutineScope,
        params: P,
        dispatcher: CoroutineDispatcher,
        onResult: (Either<Failure, T>) -> Unit
    ) {
        val backgroundJob = scope.async(dispatcher) { run(params) }
        scope.launch {
            onResult(backgroundJob.await())
        }
    }

    override fun <T, P> ObservableUseCase<T, P>.invoke(
        scope: CoroutineScope,
        params: P,
        dispatcher: CoroutineDispatcher,
        onResult: (T) -> Unit
    ) {
        val backgroundJob = scope.async(dispatcher) { run(params) }
        scope.launch {
            backgroundJob.await().collect {
                onResult(it)
            }
        }
    }
}
