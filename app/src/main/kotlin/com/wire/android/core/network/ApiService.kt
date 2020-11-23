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
import com.wire.android.core.functional.flatMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

abstract class ApiService {
    abstract val networkHandler: NetworkHandler

    suspend fun <T> rawRequest(call: suspend () -> Response<T>): Either<Failure, Response<T>> =
        withContext(Dispatchers.IO) {
            return@withContext when (networkHandler.isConnected()) {
                true -> performRequest(call)
                false -> Either.Left(NetworkConnection)
            }
        }

    suspend fun <T> request(default: T? = null, call: suspend () -> Response<T>): Either<Failure, T> =
        rawRequest(call).flatMap { response ->
            response.body()?.let { Either.Right(it) }
                ?: default?.let { Either.Right(it) }
                ?: Either.Left(EmptyResponseBody)
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> performRequest(call: suspend () -> Response<T>): Either<Failure, Response<T>> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                Either.Right(response)
            } else {
                handleRequestError(response)
            }
        } catch (exception: Throwable) {
            when (exception) {
                is CancellationException -> Either.Left(Cancelled)
                else -> Either.Left(ServerError)
            }
        }
    }

    private fun <T> handleRequestError(response: Response<T>): Either<Failure, Response<T>> =
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
