package com.wire.android.framework.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@ExperimentalCoroutinesApi
class CoroutinesTestRule : TestWatcher() {

    private val mainDispatcher by lazy { TestCoroutineDispatcher() }

    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(mainDispatcher)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        Dispatchers.resetMain()
        mainDispatcher.cleanupTestCoroutines()
    }
}
