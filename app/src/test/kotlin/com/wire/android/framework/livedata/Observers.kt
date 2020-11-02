package com.wire.android.framework.livedata

import androidx.lifecycle.LiveData
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
//TODO: re-write w/ mockk
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

//TODO: re-write w/ mockk
suspend fun <T> LiveData<T>.assertNotUpdated(timeout: Long = 2L) {
    val value: T?

    try {
        value = awaitValue(timeout)
    } catch (ex: TimeoutException) {
        return
    }

    value?.let { fail("Didn't expect a value update but got $it") }
}

private fun <T> LiveData<T>.observeOnce(onChanged: (T) -> Unit) {
    val lifecycleOwner = TestLifecycleOwner()

    observe(lifecycleOwner, {
        onChanged(it)
        lifecycleOwner.destroy()
    })
}
