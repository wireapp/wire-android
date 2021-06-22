package com.wire.android.shared.asset.ui.imageloader

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class CoroutineDataFetcher<T> : DataFetcher<T> {

    private var job: Job? = null

    abstract suspend fun fetch(priority: Priority): Either<Failure, T>

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in T>) {
        job = GlobalScope.launch {
            fetch(priority)
                .onSuccess { callback.onDataReady(it) }
                .onFailure { failure ->
                    callback.onLoadFailed(
                        Exception("Error while loading Asset via ${this@CoroutineDataFetcher::class.java.simpleName}. Cause: $failure")
                    )
                }
        }
    }

    override fun cancel() {
        job?.let { if (it.isActive) it.cancel() }
    }

    @Suppress("EmptyFunctionBlock")
    override fun cleanup() {}

    override fun getDataSource(): DataSource = DataSource.REMOTE
}
