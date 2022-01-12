package com.wire.android.core.network

import com.wire.android.UnitTest
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class RetrofitClientTest : UnitTest() {

    private lateinit var retrofitClient: RetrofitClient

    @MockK
    private lateinit var retrofit: Retrofit

    private interface ServiceClass

    @Before
    fun setUp() {
        retrofitClient = RetrofitClient(retrofit)
    }

    @Test
    fun `given a retrofit instance, when create is called, then calls retrofit to create the service`() {
        retrofitClient.create(ServiceClass::class.java)

        verify(exactly = 1) { retrofit.create(ServiceClass::class.java) }
    }
}
