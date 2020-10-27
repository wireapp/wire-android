package com.wire.android.core.extension

import android.net.Uri
import com.wire.android.AndroidTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.junit.Test

class UriExtensionsTest : AndroidTest() {

    @Test
    fun uriBuilder_domainAddress_setsSchemeAndAuthority() {
        val uriBuilder = Uri.Builder()
        val uri = uriBuilder.domainAddress(TEST_URI).build()

        uri.scheme shouldEqual TEST_SCHEME
        uri.authority shouldEqual TEST_DOMAIN
        uri.toString() shouldEqual TEST_URI
    }

    companion object {
        private const val TEST_SCHEME = "https"
        private const val TEST_DOMAIN = "www.google.com"
        private const val TEST_URI = "$TEST_SCHEME://$TEST_DOMAIN"
    }
}
