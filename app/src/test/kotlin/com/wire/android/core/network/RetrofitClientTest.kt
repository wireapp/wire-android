package com.wire.android.core.network

import com.wire.android.UnitTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import retrofit2.Retrofit

class RetrofitClientTest : UnitTest() {

    private lateinit var retrofitClient: RetrofitClient

    @Mock
    private lateinit var retrofit: Retrofit

    private interface ServiceClass

    @Before
    fun setUp() {
        retrofitClient = RetrofitClient(retrofit)
    }

    @Test
    fun `given a retrofit instance, when create is called, then calls retrofit to create the service`() {
        retrofitClient.create(ServiceClass::class.java)

        verify(retrofit).create(ServiceClass::class.java)
    }
}
