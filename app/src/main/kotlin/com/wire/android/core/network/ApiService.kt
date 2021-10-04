package com.wire.android.core.network

import com.wire.android.core.exception.BadRequest
import com.wire.android.core.exception.Cancelled
import com.wire.android.core.exception.Conflict
import com.wire.android.core.exception.EmptyResponseBody
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.InternalServerError
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.exception.NotFound
import com.wire.android.core.exception.ServerError
import com.wire.android.core.exception.TooManyRequests
import com.wire.android.core.exception.Unauthorized
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

abstract class ApiService {
    abstract val networkHandler: NetworkHandler

    suspend fun <T> rawRequest(
        onResponseError: (suspend (response: Response<T>) -> Either<Failure, Response<T>>) = { handleRequestError(it) },
        call: suspend () -> Response<T>
    ): Either<Failure, Response<T>> = suspending {
        performRequest(call).flatMap { response ->
            if (response.isSuccessful) Either.Right(response)
            else onResponseError.invoke(response)
        }
    }

    suspend fun <T> request(
        handleEmptyBody: (suspend (response: Response<T>) -> Either<Failure, T>)? = null,
        call: suspend () -> Response<T>
    ): Either<Failure, T> = suspending {
        performRequest(call).flatMap { response ->
            if (response.isSuccessful) {
                response.body()?.let { Either.Right(it) }
                    ?: handleEmptyBody?.invoke(response)
                    ?: Either.Left(EmptyResponseBody)
            } else {
                handleRequestError(response)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> performRequest(call: suspend () -> Response<T>): Either<Failure, Response<T>> = withContext(Dispatchers.IO) {
        try {
            if (!networkHandler.isConnected()) {
                Either.Left(NetworkConnection)
            } else {
                Either.Right(call())
            }
        } catch (exception: Throwable) {
            when (exception) {
                is CancellationException -> Either.Left(Cancelled)
                else -> Either.Left(ServerError)
            }
        }
    }

    private fun <T> handleRequestError(response: Response<T>): Either.Left<Failure> =
        Either.Left(buildFailure(response.code()))

    private fun buildFailure(errorCode: Int): Failure =
        when (errorCode) {
            CODE_BAD_REQUEST -> BadRequest
            CODE_UNAUTHORIZED -> Unauthorized
            CODE_FORBIDDEN -> Forbidden
            CODE_NOT_FOUND -> NotFound
            CODE_CONFLICT -> Conflict
            CODE_TOO_MANY_REQUESTS -> TooManyRequests
            CODE_INTERNAL_SERVER_ERROR -> InternalServerError
            else -> ServerError
        }

    companion object {
        private const val CODE_BAD_REQUEST = 400
        private const val CODE_UNAUTHORIZED = 401
        private const val CODE_FORBIDDEN = 403
        private const val CODE_NOT_FOUND = 404
        private const val CODE_CONFLICT = 409
        private const val CODE_TOO_MANY_REQUESTS = 429
        private const val CODE_INTERNAL_SERVER_ERROR = 500
    }
}
