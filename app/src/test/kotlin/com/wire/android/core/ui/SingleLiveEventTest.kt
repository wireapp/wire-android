package com.wire.android.core.ui

import androidx.lifecycle.Observer
import com.wire.android.UnitTest
import com.wire.android.framework.livedata.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeoutException

class SingleLiveEventTest : UnitTest() {

    private lateinit var singleLiveEvent: SingleLiveEvent<Int>

    @Before
    fun setUp() {
        singleLiveEvent = SingleLiveEvent()
    }

    @Test
    fun `given that value is not consumed yet, when an observer is attached, notifies the observer`() {
        singleLiveEvent.value = TEST_VALUE

        runBlocking {
            assertThat(singleLiveEvent.awaitValue()).isEqualTo(TEST_VALUE)
        }
    }

    @Test(expected = TimeoutException::class)
    fun `given that value is already consumed, when an observer is attached, does not notify observer`() {
        singleLiveEvent.value = TEST_VALUE

        runBlocking {
            singleLiveEvent.awaitValue() //consume
            singleLiveEvent.awaitValue() //wait for 2nd time
        }
    }

    @Test
    fun `given that value's already consumed, when lifecycleOwner is stopped & started again, does not notify observer for the 2nd time`() {
        singleLiveEvent.value = TEST_VALUE

        var observedOnce = false
        val lifecycleOwner = TestLifecycleOwner()
        val observer = Observer<Int?> {
            if (observedOnce) throw AssertionError("Should not be invoked if observed once!")
            observedOnce = true
        }
        singleLiveEvent.observe(lifecycleOwner, observer)

        lifecycleOwner.destroy()
        lifecycleOwner.start()
        assertThat(observedOnce).isEqualTo(true)
    }

    @Test
    fun `given that value is already consumed, when a new value is set, notifies the observer about the new value`() {
        val newValue = TEST_VALUE + 5

        var count = 0
        val observer = Observer<Int> {
            assertThat(it).isEqualTo(if (count == 0) TEST_VALUE else newValue)
            count++
        }
        singleLiveEvent.observeForever(observer)
        singleLiveEvent.value = TEST_VALUE
        singleLiveEvent.value = newValue
        singleLiveEvent.removeObserver(observer)
        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `given that there are more than 1 active observers, only notifies one of them`() {
        val lifecycleOwner = TestLifecycleOwner()

        val observer1 = Observer<Int?> { assertThat(it).isEqualTo(TEST_VALUE) }
        val observer2 = Observer<Int?> { throw AssertionError("Didn't expect to be notified.") }

        singleLiveEvent.observe(lifecycleOwner, observer1)
        singleLiveEvent.observe(lifecycleOwner, observer2)
        singleLiveEvent.value = TEST_VALUE

        lifecycleOwner.destroy()
    }

    companion object {
        private const val TEST_VALUE = 2
    }
}
