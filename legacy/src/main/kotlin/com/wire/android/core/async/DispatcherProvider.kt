package com.wire.android.core.async

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext

interface DispatcherProvider {
    fun main(): CoroutineDispatcher
    fun io(): CoroutineDispatcher
    fun default(): CoroutineDispatcher
    fun unconfined(): CoroutineDispatcher
    fun newSingleThreadedDispatcher(poolName: String): CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
    override fun main() = Dispatchers.Main
    override fun io() = Dispatchers.IO
    override fun default() = Dispatchers.Default
    override fun unconfined() = Dispatchers.Unconfined
    override fun newSingleThreadedDispatcher(poolName: String): CoroutineDispatcher {
        //TODO: Replace with dispatcher.Default.limitedParallelism(1, WORK_POOL_NAME) when migrating to Coroutines 1.6
        return newSingleThreadContext(poolName)
    }
}
