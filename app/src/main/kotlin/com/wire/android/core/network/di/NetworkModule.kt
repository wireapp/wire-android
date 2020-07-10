@file:Suppress("MatchingDeclarationName")

package com.wire.android.core.network.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.wire.android.BuildConfig
import com.wire.android.core.network.NetworkClient
import com.wire.android.core.network.NetworkHandler
import com.wire.android.core.network.RetrofitClient
import com.wire.android.core.network.di.NetworkDependencyProvider.defaultHttpClient
import com.wire.android.core.network.di.NetworkDependencyProvider.retrofit
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit

object NetworkDependencyProvider {
    //TODO: dynamic base url
    private const val BASE_URL = "https://staging-nginz-https.zinfra.io"
    private const val APPLICATION_JSON = "application/json"

    @UnstableDefault
    fun retrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(Json.asConverterFactory(APPLICATION_JSON.toMediaType()))
            .build()

    fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder().addLoggingInterceptor().build()

    private fun OkHttpClient.Builder.addLoggingInterceptor() = this.apply {
        if (BuildConfig.DEBUG) {
            addInterceptor(HttpLoggingInterceptor().setLevel(Level.BODY))
        }
    }
}

@UnstableDefault
val networkModule: Module = module {
    single { NetworkHandler(androidContext()) }
    single<NetworkClient> { RetrofitClient(get()) }
    single { defaultHttpClient() }
    single { retrofit(get()) }
}
