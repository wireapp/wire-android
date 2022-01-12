package com.wire.android.core.network.either

import com.wire.android.core.functional.Either

sealed class EitherResponse<out ErrorBodyType : Any, out SuccessfulBodyType : Any> {

    data class Success<out S : Any>(val body: S) : EitherResponse<Nothing, S>()

    sealed class Failure<out E : Any> : EitherResponse<E, Nothing>() {

        data class ErrorBody<out E : Any>(val errorBody: E, val responseCode: Int) : Failure<E>()

        data class EmptyBody<out E: Any>(val responseCode: Int) : Failure<E>()

        data class Exception<out E : Any>(val throwable: Throwable?) : Failure<E>()
    }

    fun asEither(): Either<Failure<ErrorBodyType>, SuccessfulBodyType> = when (this) {
        is Failure -> Either.Left(this)
        is Success -> Either.Right(this.body)
    }
}
