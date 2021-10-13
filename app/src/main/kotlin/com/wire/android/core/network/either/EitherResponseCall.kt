package com.wire.android.core.network.either

import okhttp3.Request
import okhttp3.ResponseBody
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Converter
import retrofit2.Response

class EitherResponseCall<E : Any, S : Any>(
    private val delegate: Call<S>,
    private val errorConverter: Converter<ResponseBody, E>
) : Call<EitherResponse<E, S>> {

    override fun enqueue(callback: Callback<EitherResponse<E, S>>) = delegate.enqueue(object : Callback<S> {
        override fun onResponse(call: Call<S>, response: Response<S>) {
            if (!response.isSuccessful) {
                handleFailureResponse(response, callback)
                return
            }
            val code = response.code()
            response.body()?.let {
                callback.onResponse(this@EitherResponseCall, Response.success(EitherResponse.Success(it)))
            } ?: run {
                callback.onResponse(this@EitherResponseCall, Response.success(EitherResponse.Failure.EmptyBody(code)))
            }
        }

        override fun onFailure(call: Call<S>, throwable: Throwable) {
            val networkResponse = EitherResponse.Failure.Exception<E>(throwable)
            callback.onResponse(this@EitherResponseCall, Response.success(networkResponse))
        }
    })


    private fun handleFailureResponse(response: Response<S>, callback: Callback<EitherResponse<E, S>>) {
        val code = response.code()
        response.errorBody()
            ?.takeIf { it.contentLength() != 0L }
            ?.let { errorConverter.convert(it) }
            ?.let {
                // There's an ErrorBody and it has been successfully parsed
                callback.onResponse(this@EitherResponseCall, Response.success(EitherResponse.Failure.ErrorBody(it, code)))
            } ?: run {
            // Unable to read ErrorBody, or empty error body
            callback.onResponse(this@EitherResponseCall, Response.success(EitherResponse.Failure.EmptyBody(code)))
        }
    }

    override fun isExecuted() = delegate.isExecuted

    override fun clone() = EitherResponseCall(delegate.clone(), errorConverter)

    override fun isCanceled() = delegate.isCanceled

    override fun cancel() = delegate.cancel()

    override fun execute(): Response<EitherResponse<E, S>> {
        throw UnsupportedOperationException("EitherResponseCall doesn't support execute")
    }

    override fun request(): Request = delegate.request()

    override fun timeout(): Timeout = delegate.timeout()
}
