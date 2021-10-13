package com.wire.android.core.network.either

import com.wire.android.UnitTest
import com.wire.android.framework.network.mockNetworkResponse
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.ResponseBody
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Converter

class EitherResponseCallTest : UnitTest() {

    class ResponseError
    class ResponseSuccess

    @MockK
    private lateinit var delegate: Call<ResponseSuccess>

    @MockK
    private lateinit var errorConverter: Converter<ResponseBody, ResponseError>

    private lateinit var subject: EitherResponseCall<ResponseError, ResponseSuccess>


    @Before
    fun setUp() {
        subject = EitherResponseCall(delegate, errorConverter)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `given the call delegate, when executing the request, then UnsupportedOperationException should be thrown`() {
        subject.execute()
    }

    @Test
    fun `given the call delegate, when checking if the request isExecuted, then the delegate should handle it`() {
        subject.isExecuted

        verify(exactly = 1) { delegate.isExecuted }
    }

    @Test
    fun `given the call delegate, when checking if the request isCanceled, then the delegate should handle it`() {
        subject.isCanceled

        verify(exactly = 1) { delegate.isCanceled }
    }

    @Test
    fun `given the call delegate, when canceling the request, then the delegate should handle it`() {
        subject.cancel()

        verify(exactly = 1) { delegate.cancel() }
    }

    @Test
    fun `given the call delegate, when calling request, then the delegate should handle it`() {
        subject.request()

        verify(exactly = 1) { delegate.request() }
    }

    @Test
    fun `given the call delegate, when calling timeout, then the delegate should handle it`() {
        subject.timeout()

        verify(exactly = 1) { delegate.timeout() }
    }

    @Test
    fun `given the call delegate, when calling clone, then a new EitherResponseCall should be returned with a clone of the delegate`() {
        val delegateClone: Call<ResponseSuccess> = mockk()
        every { delegate.clone() } returns delegateClone

        val result = subject.clone()

        result shouldNotBeEqualTo subject
    }

    @Test
    fun `given the call delegate, when calling clone, then a clone of the delegate should be created`() {
        val delegateClone: Call<ResponseSuccess> = mockk()
        every { delegate.clone() } returns delegateClone

        subject.clone()

        verify(exactly = 1) { delegate.clone() }
    }

    @Test
    fun `given a callback, when calling enqueue, the delegate should enqueue the request`() {
        val delegatedCallback = slot<Callback<ResponseSuccess>>()
        every { delegate.enqueue(capture(delegatedCallback)) } returns Unit

        subject.enqueue(mockk())

        verify(exactly = 1) { delegate.enqueue(delegatedCallback.captured) }
    }

    @Test
    fun `given the delegated callback returns successfully, when calling enqueue, the provided callback should receive ResponseSuccess`() {
        val delegatedCallback = slot<Callback<ResponseSuccess>>()
        every { delegate.enqueue(capture(delegatedCallback)) } returns Unit
        val callback = mockk<Callback<EitherResponse<ResponseError, ResponseSuccess>>>()
        every { callback.onResponse(any(), any()) } returns Unit
        subject.enqueue(callback)

        val mockNetworkResponse = mockNetworkResponse(ResponseSuccess())
        delegatedCallback.captured.onResponse(delegate, mockNetworkResponse)

        verify(exactly = 1) {
            callback.onResponse(subject, match {
                it.isSuccessful && it.body() is EitherResponse.Success
            })
        }
    }

    @Test
    fun `given the delegated callback returns empty body, when calling enqueue, the provided callback should receive FailureEmptyBody`() {
        val delegatedCallback = slot<Callback<ResponseSuccess>>()
        every { delegate.enqueue(capture(delegatedCallback)) } returns Unit
        val callback = mockk<Callback<EitherResponse<ResponseError, ResponseSuccess>>>()
        every { callback.onResponse(any(), any()) } returns Unit
        subject.enqueue(callback)

        val mockNetworkResponse = mockNetworkResponse<ResponseSuccess?>(null)
        delegatedCallback.captured.onResponse(delegate, mockNetworkResponse)

        verify(exactly = 1) {
            callback.onResponse(subject, match {
                it.isSuccessful && it.body() is EitherResponse.Failure.EmptyBody<ResponseError>
            })
        }
    }
}
