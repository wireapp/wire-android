package com.wire.android.core.network.either

import com.wire.android.UnitTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class EitherResponseAdapterFactoryTest : UnitTest() {

    @MockK
    private lateinit var retrofit: Retrofit

    private lateinit var subject: EitherResponseAdapterFactory

    @Before
    fun setup() {
        subject = EitherResponseAdapterFactory()
    }

    @Test
    fun `given a returnType that is not a Call, when getting the adapter, then it should return null`() {
        val result = subject.get(Int::class.java, arrayOf(), retrofit)

        result shouldBeEqualTo null
    }

    @Test
    fun `given a Call returnType that is not parameterized, when getting the adapter, then it should return null`() {
        val result = subject.get(Call::class.java, arrayOf(), retrofit)

        result shouldBeEqualTo null
    }

    @Test
    fun `given a Call returnType that does not have EitherResponse as parameter, when getting the adapter, then it should return null`() {
        val myType = object : ParameterizedType {
            override fun getActualTypeArguments(): Array<Type> = arrayOf(String::class.java)

            override fun getRawType(): Type = Call::class.java

            override fun getOwnerType() = null
        }

        val result = subject.get(myType, arrayOf(), retrofit)

        result shouldBeEqualTo null
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given a Call EitherResponse without type parameters, when getting the adapter, then it should throw IllegalArgumentException`() {
        val myType = object : ParameterizedType {
            override fun getActualTypeArguments(): Array<Type> = arrayOf(EitherResponse::class.java)

            override fun getRawType(): Type = Call::class.java

            override fun getOwnerType() = null
        }

        subject.get(myType, arrayOf(), retrofit)
    }

    @Test
    fun `given a correct Call EitherResponse, when getting the adapter, then it should get a body converter for the error type`() {
        class ErrorType
        class SuccessType

        val eitherResponseType = object : ParameterizedType {
            override fun getActualTypeArguments(): Array<Type> = arrayOf(ErrorType::class.java, SuccessType::class.java)

            override fun getRawType(): Type = EitherResponse::class.java

            override fun getOwnerType() = null
        }
        val callClass = object : ParameterizedType {
            override fun getActualTypeArguments(): Array<Type> = arrayOf(eitherResponseType)

            override fun getRawType(): Type = Call::class.java

            override fun getOwnerType() = null
        }
        val annotations = arrayOf<Annotation>()
        every { retrofit.nextResponseBodyConverter<ErrorType>(any(), any(), any()) } returns mockk()

        subject.get(callClass, arrayOf(), retrofit)

        verify(exactly = 1){
            retrofit.nextResponseBodyConverter<ErrorType>(null, ErrorType::class.java, annotations)
        }
    }
}
