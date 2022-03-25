package com.wire.android.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * This extension provides a way to set (before and after each test) the coroutine dispatcher for testing
 * In our case [UnconfinedTestDispatcher] so it's guaranteed that coroutines are executed immediately
 *
 * Add this JUnit 5 extension to your test class using
 * @JvmField
 * @RegisterExtension
 * val coroutinesTestExtension = CoroutinesTestExtension()
 *
 * or:
 *
 * Annotating the class with
 * @ExtendWith(CoroutineTestExtension::class)
 */
@ExperimentalCoroutinesApi
class CoroutineTestExtension(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(dispatcher)
    }

    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }
}
