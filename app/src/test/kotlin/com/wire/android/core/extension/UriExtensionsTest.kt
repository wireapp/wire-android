package com.wire.android.core.extension

import android.net.Uri
import com.wire.android.AndroidTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UriExtensionsTest : AndroidTest() {

    @Test
    fun uriBuilder_domainAddress_setsSchemeAndAuthority() {
        val uriBuilder = Uri.Builder()

        val uri = uriBuilder.domainAddress(TEST_URI).build()

        assertThat(uri.scheme).isEqualTo(TEST_SCHEME)
        assertThat(uri.authority).isEqualTo(TEST_DOMAIN)

        assertThat(uri.toString()).isEqualTo(TEST_URI)
    }

    companion object {
        private const val TEST_SCHEME = "https"
        private const val TEST_DOMAIN = "www.google.com"
        private const val TEST_URI = "$TEST_SCHEME://$TEST_DOMAIN"
    }
}
