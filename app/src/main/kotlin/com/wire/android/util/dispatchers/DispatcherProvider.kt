package com.wire.android.util.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Wrapper injectable provided interface that enables correctness for testing
 */
interface DispatcherProvider {
    fun main(): CoroutineDispatcher
    fun io(): CoroutineDispatcher
    fun default(): CoroutineDispatcher
    fun unconfined(): CoroutineDispatcher
}

/**
 * Default production dispatchers provider wrapper
 */
class DefaultDispatcherProvider : DispatcherProvider {
    override fun main() = Dispatchers.Main
    override fun io() = Dispatchers.IO
    override fun default() = Dispatchers.Default
    override fun unconfined() = Dispatchers.Unconfined
}
