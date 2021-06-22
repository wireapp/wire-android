package com.wire.android.framework.network

import io.mockk.every
import io.mockk.mockk
import retrofit2.Response

inline fun <reified T> mockNetworkResponse(responseBody: T = mockk()): Response<T> =
    mockk<Response<T>>().also {
        every { it.isSuccessful } returns true
        every { it.body() } returns responseBody
    }

fun <T> mockNetworkError(errorCode: Int = 404): Response<T> =
    mockk<Response<T>>().also {
        every { it.isSuccessful } returns false
        every { it.code() } returns errorCode
    }
