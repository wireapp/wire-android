package com.wire.android.framework.livedata

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.wire.android.UnitTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.TimeoutException

class LiveDataAssertionsTest : UnitTest() {

    @Test
    fun `given awaitValue called on a LiveData, when a value is received, then doesn't disrupt the test`() {
        runBlocking {
            val liveData = MutableLiveData<Int>()
            liveData.value = 5

            liveData.awaitValue()
        }
    }

    @Test(expected = TimeoutException::class)
    fun `given awaitValue called on a LiveData, when the value is not received, then throws a TimeoutException`() {
        runBlocking {
            val liveData = MutableLiveData<Int>()

            liveData.awaitValue()
        }
    }

    @Test
    fun `given assertNotUpdated called on a LiveData, when the value is not updated, then doesn't disrupt the test`() {
        runBlocking {
            val liveData = MutableLiveData<Int>()

            liveData.assertNotUpdated()
        }
    }

    @Test(expected = AssertionError::class)
    fun `given assertNotUpdated called on a LiveData, when the value is updated, then fails the test`() {
        runBlocking {
            val liveData = MutableLiveData<Int>()
            val lifecycleOwner = TestLifecycleOwner()
            liveData.value = 5

            liveData.assertNotUpdated()

            liveData.observe(lifecycleOwner, Observer { })
            lifecycleOwner.destroy()
        }
    }
}
