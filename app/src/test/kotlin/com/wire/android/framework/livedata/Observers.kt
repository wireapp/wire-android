package com.wire.android.framework.livedata

import androidx.lifecycle.LiveData
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.fail
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * A function that suspends the current coroutine until the LiveData's value changes. Then it
 * resumes the coroutine with the new value.
 *
 * @throws TimeoutException if the value of LiveData is not updated within [timeout] seconds.
 */
@Deprecated("Use shouldBeUpdated instead")
suspend fun <T> LiveData<T>.awaitValue(timeout: Long = 2L): T = suspendCoroutine { cont ->
    val latch = CountDownLatch(1)

    this.observeOnce {
        latch.countDown()
        cont.resume(it)
    }

    if (!latch.await(timeout, TimeUnit.SECONDS)) {
        cont.resumeWithException(TimeoutException("Didn't receive LiveData value after $timeout seconds"))
    }
}

@Deprecated("Use shouldNotBeUpdated instead")
suspend fun <T> LiveData<T>.assertNotUpdated(timeout: Long = 2L) {
    val value: T?

    try {
        value = awaitValue(timeout)
    } catch (ex: TimeoutException) {
        return
    }

    value?.let { fail("Didn't expect a value update but got $it") }
}

infix fun <T> LiveData<T>.shouldBeUpdated(assertion: (T) -> Unit) = this.shouldBeUpdated(2000L, assertion)

fun <T> LiveData<T>.shouldBeUpdated(timeout: Long = 2000, assertion: (T) -> Unit) {
    val updateHelper = mockk<LiveDataUpdateHelper>(relaxUnitFun = true)

    this.observeOnce {
        assertion(it)
        updateHelper.onUpdated()
    }

    verify(exactly = 1, timeout = timeout) { updateHelper.onUpdated() }
}

fun <T> LiveData<T>.shouldNotBeUpdated(timeout: Long = 2000) {
    val updateHelper = mockk<LiveDataUpdateHelper>(relaxUnitFun = true)

    this.observeOnce {
        updateHelper.onUpdated()
    }

    verify(timeout = timeout) { updateHelper wasNot Called }
}

private class LiveDataUpdateHelper {
    fun onUpdated() {
        this.hashCode()
    }
}

private fun <T> LiveData<T>.observeOnce(onChanged: (T) -> Unit) {
    val lifecycleOwner = TestLifecycleOwner()

    observe(lifecycleOwner, {
        onChanged(it)
        lifecycleOwner.destroy()
    })
}
