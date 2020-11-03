@file:Suppress("MatchingDeclarationName")

package com.wire.android.core.network.di

import com.wire.android.BuildConfig
import com.wire.android.core.network.BackendConfig
import com.wire.android.core.network.HttpRequestParams
import com.wire.android.core.network.NetworkClient
import com.wire.android.core.network.NetworkHandler
import com.wire.android.core.network.RetrofitClient
import com.wire.android.core.network.UserAgentInterceptor
import com.wire.android.core.network.di.NetworkDependencyProvider.defaultHttpClient
import com.wire.android.core.network.di.NetworkDependencyProvider.retrofit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkDependencyProvider {

    fun retrofit(okHttpClient: OkHttpClient, baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    fun defaultHttpClient(
        userAgentInterceptor: UserAgentInterceptor,
        requestParams: HttpRequestParams
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectionSpecs(requestParams.getConnectionSpecs())
            .addInterceptor(userAgentInterceptor)
            .addLoggingInterceptor()
            .build()

    private fun OkHttpClient.Builder.addLoggingInterceptor() = this.apply {
        if (BuildConfig.DEBUG) {
            addInterceptor(HttpLoggingInterceptor().setLevel(Level.BODY))
        }
    }
}

val networkModule: Module = module {
    single { NetworkHandler(androidContext()) }
    single<NetworkClient> { RetrofitClient(get()) }
    single { defaultHttpClient(get(), get()) }
    factory { HttpRequestParams() }
    factory { UserAgentInterceptor(get()) }
    single { retrofit(get(), get<BackendConfig>().baseUrl) }
    single { BackendConfig() }
}
