package com.wire.android.core.network.either

import com.wire.android.UnitTest
import io.mockk.impl.annotations.MockK
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit

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
}
