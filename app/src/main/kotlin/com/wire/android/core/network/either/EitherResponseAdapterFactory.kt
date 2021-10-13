package com.wire.android.core.network.either

import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class EitherResponseAdapterFactory : CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<out Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        // Only handle Calls, we only deal with suspendable functions
        if (getRawType(returnType) != Call::class.java) {
            return null
        }

        // Don't handle Calls that are not parameterized i.e. Call<Foo>
        if (returnType !is ParameterizedType) {
            return null
        }

        // Don't handle anything that is not an EitherResponse i.e. Call<EitherResponse>
        val responseType = getParameterUpperBound(0, returnType)
        if (getRawType(responseType) != EitherResponse::class.java) {
            return null
        }

        require(responseType is ParameterizedType) { "EitherResponse must be parameterized with <FailureBodyType,SuccessBodyType>" }

        val errorBodyType = getParameterUpperBound(0, responseType)
        val successBodyType = getParameterUpperBound(1, responseType)

        // Find a suitable body converter inside Retrofit for the error response
        val errorBodyConverter = retrofit.nextResponseBodyConverter<Any>(null, errorBodyType, annotations)

        return EitherResultAdapter<Any, Any>(successBodyType, errorBodyConverter)
    }
}
