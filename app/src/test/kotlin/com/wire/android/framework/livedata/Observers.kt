package com.wire.android.framework.livedata

import androidx.lifecycle.LiveData
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify

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
