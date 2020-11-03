package com.wire.android.core.network

import com.wire.android.UnitTest
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import okhttp3.TlsVersion
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainAll
import org.junit.Test

class ConnectionSpecsFactoryTest : UnitTest() {

    @Test
    fun `Given connectionSpecs are created, then ensure list contains two specs`() {
        val connectionSpecs = ConnectionSpecsFactory.create()
        connectionSpecs.size shouldBeEqualTo 2
    }

    @Test
    fun `Given connectionSpecs are created, then ensure list contains modern specification`() {
        val connectionSpecs = ConnectionSpecsFactory.create()
        with(connectionSpecs.first()) {
            tlsVersions.orEmpty() shouldContain TlsVersion.TLS_1_2
            cipherSuites.orEmpty() shouldContainAll listOf(
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
            )
        }
    }

    @Test
    fun `Given connectionSpecs are created, then list should container CLEARTEXT`() {
        val connectionSpecs = ConnectionSpecsFactory.create()
        connectionSpecs shouldContain ConnectionSpec.CLEARTEXT
    }
}
