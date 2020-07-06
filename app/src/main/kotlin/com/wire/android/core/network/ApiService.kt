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
import com.wire.android.core.exception.Unauthorized
import com.wire.android.core.functional.Either
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

abstract class ApiService {
    abstract val networkHandler: NetworkHandler

    suspend fun <T> request(default: T? = null, call: suspend () -> Response<T>): Either<Failure, T> =
        withContext(Dispatchers.IO) {
            return@withContext when (networkHandler.isConnected) {
                true -> performRequest(call, default)
                false, null -> Either.Left(NetworkConnection)
            }
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> performRequest(call: suspend () -> Response<T>, default: T? = null): Either<Failure, T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                response.body()?.let { Either.Right(it) }
                    ?: (default?.let { Either.Right(it) } ?: Either.Left(EmptyResponseBody))
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

    private fun <T> handleRequestError(response: Response<T>): Either<Failure, T> {
        return when (response.code()) {
            CODE_BAD_REQUEST -> Either.Left(BadRequest)
            CODE_UNAUTHORIZED -> Either.Left(Unauthorized)
            CODE_FORBIDDEN -> Either.Left(Forbidden)
            CODE_NOT_FOUND -> Either.Left(NotFound)
            CODE_CONFLICT -> Either.Left(Conflict)
            CODE_INTERNAL_SERVER_ERROR -> Either.Left(InternalServerError)
            else -> Either.Left(ServerError)
        }
    }

    companion object {
        private const val CODE_BAD_REQUEST = 400
        private const val CODE_UNAUTHORIZED = 401
        private const val CODE_FORBIDDEN = 403
        private const val CODE_NOT_FOUND = 404
        private const val CODE_CONFLICT = 409
        private const val CODE_INTERNAL_SERVER_ERROR = 500
    }
}
