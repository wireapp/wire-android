package com.wire.android.config

import com.wire.android.util.dispatchers.DispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherProvider : DispatcherProvider {
    override fun main() = UnconfinedTestDispatcher()
    override fun io() = UnconfinedTestDispatcher()
    override fun default() = UnconfinedTestDispatcher()
    override fun unconfined() = UnconfinedTestDispatcher()
}
