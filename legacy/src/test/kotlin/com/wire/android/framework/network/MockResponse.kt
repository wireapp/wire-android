package com.wire.android.framework.network

import com.wire.android.core.functional.Either
import com.wire.android.core.network.either.EitherResponse
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import retrofit2.Response

inline fun <reified T> mockNetworkResponse(responseBody: T = mockk(), code: Int = 200): Response<T> =
    mockk<Response<T>>().also {
        every { it.isSuccessful } returns true
        every { it.code() } returns code
        every { it.body() } returns responseBody
    }

fun <T> mockNetworkError(errorCode: Int = 404): Response<T> =
    mockk<Response<T>>().also {
        every { it.isSuccessful } returns false
        every { it.code() } returns errorCode
    }

inline fun <reified E : Any, reified T : Any> mockNetworkEitherResponse(responseBody: T = mockk()): EitherResponse<E, T> =
    mockk<EitherResponse.Success<T>>().also {
        every { it.asEither() } returns Either.Right(responseBody)
        every { it.body } returns responseBody
    }

inline fun <reified E : Any, reified T : Any> mockNetworkEitherErrorBodyFailure(
    errorBody: E = mockk(),
    errorCode: Int = 400
): EitherResponse<E, T> =
    mockk<EitherResponse.Failure.ErrorBody<E>>().also {
        every { it.asEither() } returns Either.Left(EitherResponse.Failure.ErrorBody(errorBody, errorCode))
        every { it.errorBody } returns errorBody
        every { it.responseCode } returns errorCode
    }

inline fun <reified E : Any, reified T : Any> mockNetworkEitherNoBodyFailure(
    errorCode: Int = 400,
): EitherResponse<E, T> =
    mockk<EitherResponse.Failure.EmptyBody<E>>().also {
        every { it.responseCode } returns errorCode
        every { it.asEither() } returns Either.Left(EitherResponse.Failure.EmptyBody(errorCode))
    }

inline fun <reified E : Any, reified T : Any> mockNetworkEitherThrowableFailure(
    throwable: Throwable = IOException()
): EitherResponse<E, T> = mockk<EitherResponse.Failure.Exception<E>>().also {
    every { it.asEither() } returns Either.Left(EitherResponse.Failure.Exception(throwable))
    every { it.throwable } returns throwable
}
