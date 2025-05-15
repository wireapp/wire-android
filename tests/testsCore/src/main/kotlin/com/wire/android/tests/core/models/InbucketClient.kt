
package com.wire.android.tests.core.models

import com.wire.android.tests.core.utils.ZetaLogger
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.IOException
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.util.logging.Logger

class InbucketClient(baseUrl: String?, user: String, password: String, useProxy: Boolean) {

    companion object {
        private val log: Logger = ZetaLogger.getLog(InbucketClient::class.simpleName)
    }

    protected val service: InbucketService
    protected val simpleService: InbucketService

    init {
        val clientBuilder = OkHttpClient().newBuilder()

        if (useProxy) {
            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    val credentials = com.wire.android.tests.core.config.Credentials()
                    return PasswordAuthentication(
                        "qa",
                        credentials.getCredentials("SOCKS_PROXY_PASSWORD").toCharArray()
                    )
                }
            })
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("socks.wire.link", 1080))
            clientBuilder.proxy(proxy)
        }

        class LoggingInterceptor : Interceptor {
            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request()

                val t1 = System.nanoTime()
                log.info("Sending request ${request.url} on ${chain.connection()}\n${request.headers}")

                val response = chain.proceed(request)

                val t2 = System.nanoTime()
                log.info("Received response for ${response.request.url} in ${(t2 - t1) / 1e6}ms\n${response.headers}")

                return response
            }
        }

        val okHttpClient = clientBuilder
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", Credentials.basic(user, password))
                    .build()
                chain.proceed(newRequest)
            }
            .addInterceptor(LoggingInterceptor())
            .build()

        service = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(InbucketService::class.java)

        simpleService = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .build()
            .create(InbucketService::class.java)
    }

    @Throws(IOException::class)
    fun getMailbox(mailboxName: String): List<MessageInfo> =
        service.getMailbox(mailboxName).execute().body()!!

    @Throws(IOException::class)
    fun getMessage(mailboxName: String, messageId: String): Message =
        service.getMessage(mailboxName, messageId).execute().body()!!

    @Throws(IOException::class)
    fun getMessageSource(mailboxName: String, messageId: String): String =
        simpleService.getMessageSource(mailboxName, messageId).execute().body()!!.string()

    @Throws(IOException::class)
    fun deleteMailbox(mailboxName: String): String =
        service.deleteMailbox(mailboxName).execute().body()!!

    @Throws(IOException::class)
    fun deleteMessage(mailboxName: String, messageId: String): String? =
        service.deleteMessage(mailboxName, messageId).execute().body()
}
