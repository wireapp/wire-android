package com.wire.android.framework.livedata

import androidx.lifecycle.MutableLiveData
import com.wire.android.UnitTest
import org.amshove.kluent.shouldBe
import org.junit.Test

class LiveDataAssertionsTest : UnitTest() {

    @Test
    fun `given shouldBeUpdated called on a LiveData, when a value is received, then executes the assertion and doesn't disrupt the test`() {
        val liveData = MutableLiveData<Int>()
        liveData.value = 5

        var assertionCalled = false
        liveData shouldBeUpdated { assertionCalled = true }

        assertionCalled shouldBe true
    }

    @Test(expected = AssertionError::class)
    fun `given shouldBeUpdated called on a LiveData, when the value is not received, then doesn't execute assertion and fails the test`() {
        val liveData = MutableLiveData<Int>()

        liveData shouldBeUpdated { throw IllegalStateException("Assertion shouldn't be executed") }
    }

    @Test
    fun `given shouldNotBeUpdated called on a LiveData, when the value is not updated, then doesn't disrupt the test`() {
        val liveData = MutableLiveData<Int>()

        liveData.shouldNotBeUpdated()
    }

    @Test(expected = AssertionError::class)
    fun `given shouldNotBeUpdated called on a LiveData, when the value is updated, then fails the test`() {
        val liveData = MutableLiveData<Int>()

        liveData.value = 5

        liveData.shouldNotBeUpdated()
    }
}
