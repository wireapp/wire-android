package com.wire.android.core.network.either

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Converter
import java.lang.reflect.Type

class EitherResultAdapter<E : Any, S : Any>(
    private val successType: Type,
    private val errorBodyConverter: Converter<ResponseBody, E>
) : CallAdapter<S, Call<EitherResponse<E, S>>> {

    override fun responseType(): Type = successType

    override fun adapt(call: Call<S>): Call<EitherResponse<E, S>> {
        return EitherResponseCall(call, errorBodyConverter)
    }
}
