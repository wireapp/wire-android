package com.wire.android.core.ui.event

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.wire.android.UnitTest
import com.wire.android.framework.livedata.TestLifecycleOwner
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule

import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class EventTest : UnitTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mutableEvent: MutableEvent<Int>

    @Before
    fun setUp() {
        mutableEvent = MutableEvent()
    }

    @Test
    fun `given a value, when event() is called, sets value by wrapping inside OneTimeEvent`() {
        mutableEvent.event(EVENT)

        assertThat(mutableEvent.value).isInstanceOf(OneTimeEvent::class.java)
        assertThat(mutableEvent.value?.peekContent()).isEqualTo(EVENT)
    }

    @Test
    fun `given a value, when postEvent() is called, posts value by wrapping inside OneTimeEvent`() {
        mutableEvent.postEvent(EVENT)

        assertThat(mutableEvent.value).isInstanceOf(OneTimeEvent::class.java)
        assertThat(mutableEvent.value?.peekContent()).isEqualTo(EVENT)
    }

    @Test
    fun `given an event with unhandled content, when onEvent is called, then passes the content to onChanged function`() {
        mutableEvent.event(EVENT)

        runBlocking {
            assertThat(awaitEvent(mutableEvent)).isEqualTo(EVENT)
        }
    }

    @Test(expected = TimeoutException::class)
    fun `given an event handled content, when onEvent is called, then does not trigger onChanged function`() {
        mutableEvent.event(EVENT)

        runBlocking {
            awaitEvent(mutableEvent) // observe content once
            awaitEvent(mutableEvent) // should not receive the event for the 2nd time
        }
    }

    @Test
    fun `given an event with handled content, when a new event is sent, then notifies observers with new event`() {
        mutableEvent.event(EVENT)

        runBlocking {
            assertThat(awaitEvent(mutableEvent)).isEqualTo(EVENT)

            val newEvent = EVENT + 2
            mutableEvent.event(newEvent)

            assertThat(awaitEvent(mutableEvent)).isEqualTo(newEvent)
        }
    }

    private suspend fun <T> awaitEvent(event: Event<T>): T = suspendCoroutine { cont ->
        val latch = CountDownLatch(1)

        val lifecycleOwner = TestLifecycleOwner().also { it.start() }

        event.onEvent(lifecycleOwner) {
            lifecycleOwner.destroy()
            latch.countDown()
            cont.resume(it)
        }

        if (!latch.await(2L, TimeUnit.SECONDS)) {
            cont.resumeWithException(TimeoutException("Didn't receive Event after 2 seconds"))
        }
    }

    companion object {
        private const val EVENT = 3
    }
}