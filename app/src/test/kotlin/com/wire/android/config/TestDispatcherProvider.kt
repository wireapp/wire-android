package com.wire.android.config

import com.wire.android.util.dispatchers.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Default testing dispatchers provider wrapper
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherProvider(private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()) : DispatcherProvider {
    override fun main() = dispatcher
    override fun io() = dispatcher
    override fun default() = dispatcher
    override fun unconfined() = dispatcher
}


@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherProvider2(private val dispatcher: CoroutineDispatcher = StandardTestDispatcher()) : DispatcherProvider {
    override fun main() = dispatcher
    override fun io() = dispatcher
    override fun default() = dispatcher
    override fun unconfined() = dispatcher
}
