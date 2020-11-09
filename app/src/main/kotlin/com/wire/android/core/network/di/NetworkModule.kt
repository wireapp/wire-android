@file:Suppress("MatchingDeclarationName")

package com.wire.android.core.network.di

import android.content.Context
import android.net.ConnectivityManager
import com.wire.android.BuildConfig
import com.wire.android.core.network.BackendConfig
import com.wire.android.core.network.HttpRequestParams
import com.wire.android.core.network.NetworkClient
import com.wire.android.core.network.NetworkHandler
import com.wire.android.core.network.RetrofitClient
import com.wire.android.core.network.UserAgentConfig
import com.wire.android.core.network.UserAgentInterceptor
import com.wire.android.core.network.auth.accesstoken.AccessTokenAuthenticator
import com.wire.android.core.network.auth.accesstoken.AccessTokenInterceptor
import com.wire.android.core.network.auth.accesstoken.RefreshTokenMapper
import com.wire.android.core.network.di.NetworkDependencyProvider.createHttpClientForToken
import com.wire.android.core.network.di.NetworkDependencyProvider.createHttpClientWithoutToken
import com.wire.android.core.network.di.NetworkDependencyProvider.retrofit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


enum class AuthenticationType {
    NONE, TOKEN
}

object NetworkDependencyProvider {

    fun retrofit(okHttpClient: OkHttpClient, baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    fun createHttpClientForToken(
        httpsRequestParams: HttpRequestParams,
        accessTokenInterceptor: AccessTokenInterceptor,
        accessTokenAuthenticator: AccessTokenAuthenticator,
        userAgentInterceptor: UserAgentInterceptor,
    ): OkHttpClient =
        defaultHttpClient(httpsRequestParams, userAgentInterceptor)
            .addInterceptor(accessTokenInterceptor)
            .authenticator(accessTokenAuthenticator)
            .build()

    fun createHttpClientWithoutToken(
        httpsRequestParams: HttpRequestParams,
        userAgentInterceptor: UserAgentInterceptor
    ): OkHttpClient =
        defaultHttpClient(httpsRequestParams, userAgentInterceptor)
            .build()

    private fun defaultHttpClient(
        httpParams: HttpRequestParams,
        userAgentInterceptor: UserAgentInterceptor
    ): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectionSpecs(httpParams.connectionSpecs())
            .addInterceptor(userAgentInterceptor)
            .addLoggingInterceptor()

    private fun OkHttpClient.Builder.addLoggingInterceptor() = this.apply {
        if (BuildConfig.DEBUG) {
            addInterceptor(HttpLoggingInterceptor().setLevel(Level.BODY))
        }
    }
}

val networkModule: Module = module {
    single { NetworkHandler(androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager) }
    factory<NetworkClient> { (authType: AuthenticationType) ->
        val baseUrl = get<BackendConfig>().baseUrl
        when (authType) {
            AuthenticationType.NONE -> RetrofitClient(retrofit(createHttpClientWithoutToken(get(), get()), baseUrl))
            AuthenticationType.TOKEN -> RetrofitClient(retrofit(createHttpClientForToken(get(), get(), get(), get()), baseUrl))
        }
    }
    factory { HttpRequestParams() }
    factory { AccessTokenAuthenticator(get(), get()) }
    factory { AccessTokenInterceptor(get()) }
    factory { UserAgentInterceptor(get()) }
    factory { UserAgentConfig(get()) }
    factory { RefreshTokenMapper() }
    single { BackendConfig() }
}
