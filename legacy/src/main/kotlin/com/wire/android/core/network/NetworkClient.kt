package com.wire.android.core.network

import retrofit2.Retrofit

interface NetworkClient {
    fun <T> create(clazz: Class<T>): T
}

internal class RetrofitClient(private val retrofit: Retrofit) : NetworkClient {
    override fun <T> create(clazz: Class<T>): T = retrofit.create(clazz)
}
