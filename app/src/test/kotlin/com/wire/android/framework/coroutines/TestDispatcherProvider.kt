package com.wire.android.framework.coroutines

import com.wire.android.core.async.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher

@ExperimentalCoroutinesApi
class TestDispatcherProvider(val dispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()) : DispatcherProvider {
    override fun main() = dispatcher
    override fun io() = dispatcher
    override fun default() = dispatcher
    override fun unconfined() = dispatcher
    override fun newSingleThreadedDispatcher(poolName: String): CoroutineDispatcher = dispatcher
}
