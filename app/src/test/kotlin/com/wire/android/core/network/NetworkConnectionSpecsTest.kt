package com.wire.android.core.network

import com.wire.android.UnitTest
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import okhttp3.TlsVersion
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainAll
import org.junit.Before
import org.junit.Test

class NetworkConnectionSpecsTest : UnitTest() {

    private lateinit var connectionSpecs: List<ConnectionSpec>

    @Before
    fun setup() {
        val requestParams = HttpRequestParams()
        connectionSpecs = requestParams.connectionSpecs()
    }

    @Test
    fun `Given connectionSpecs are created, then ensure list contains two specs`() {
        connectionSpecs.size shouldBeEqualTo 2
    }

    @Test
    fun `Given connectionSpecs are created, then ensure list contains modern specification`() {
        with(connectionSpecs.first()) {
            tlsVersions.orEmpty() shouldContain TlsVersion.TLS_1_2
            cipherSuites.orEmpty() shouldContainAll listOf(
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
            )
        }
    }

    @Test
    fun `Given connectionSpecs are created, then list should container CLEARTEXT`() {
        connectionSpecs shouldContain ConnectionSpec.CLEARTEXT
    }
}
