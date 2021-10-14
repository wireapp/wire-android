package com.wire.android.core.network.either

import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class EitherResponseAdapterFactory : CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<out Annotation>, retrofit: Retrofit): CallAdapter<*, *>? = when {
        // Only handle Calls, we only deal with suspendable functions
        getRawType(returnType) != Call::class.java -> null

        // Don't handle Calls that are not parameterized i.e. Call<Foo>
        returnType !is ParameterizedType -> null

        // Don't handle anything that is not an EitherResponse i.e. Call<EitherResponse>
        (getRawType(getParameterUpperBound(0, returnType)) != EitherResponse::class.java) -> null

        else -> {
            val responseType = getParameterUpperBound(0, returnType)

            require(responseType is ParameterizedType) { "EitherResponse must be parameterized with <FailureBodyType,SuccessBodyType>" }

            val errorBodyType = getParameterUpperBound(0, responseType)
            val successBodyType = getParameterUpperBound(1, responseType)

            // Find a suitable body converter inside Retrofit for the error response
            val errorBodyConverter = retrofit.nextResponseBodyConverter<Any>(null, errorBodyType, annotations)

            EitherResultAdapter<Any, Any>(successBodyType, errorBodyConverter)
        }
    }

}
