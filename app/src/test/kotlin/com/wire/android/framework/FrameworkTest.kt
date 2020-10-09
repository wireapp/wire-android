package com.wire.android.framework

import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.wire.android.AndroidTest
import com.wire.android.framework.android.contentEquals
import com.wire.android.framework.livedata.TestLifecycleOwner
import com.wire.android.framework.livedata.assertNotUpdated
import com.wire.android.framework.livedata.awaitValue
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.TimeoutException

class FrameworkTest : AndroidTest() {

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

    @Test
    fun `given contentEquals called on a bundle, when two bundles contain the same mappings, then returns true`() {
        val bundle1 = bundleOf("key1" to "value1", "key2" to 4, "key3" to arrayOf("a", "b"), "key4" to bundleOf("x" to 9))
        val bundle2 = bundleOf("key1" to "value1", "key2" to 4, "key3" to arrayOf("a", "b"), "key4" to bundleOf("x" to 9))

        assertThat(bundle1.contentEquals(bundle2)).isTrue()
        assertThat(bundle2.contentEquals(bundle1)).isTrue()
    }

    @Test
    fun `given contentEquals called on a bundle, when one of the bundles is a subset of the other, then returns false`() {
        val bundle1 = bundleOf("key1" to "value1", "key2" to 4, "key3" to arrayOf("a", "b"))
        val bundle2 = bundleOf("key1" to "value1", "key2" to 4)

        assertThat(bundle1.contentEquals(bundle2)).isFalse()
        assertThat(bundle2.contentEquals(bundle1)).isFalse()
    }

    @Test
    fun `given contentEquals called on a bundle, when two bundles are not equal, then returns false`() {
        val bundle1 = bundleOf("key3" to arrayOf("a", "b"))
        val bundle2 = bundleOf("key1" to "value1", "key2" to 4)

        assertThat(bundle1.contentEquals(bundle2)).isFalse()
        assertThat(bundle2.contentEquals(bundle1)).isFalse()
    }
}
