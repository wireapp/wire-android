/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.tests.support.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import network.NetworkBackendClient.accessCredentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import user.utils.AccessCookie
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date

@RunWith(AndroidJUnit4::class)
class AccessCredentialsTest {
    private val existingCookie = AccessCookie("zuid", Date(Long.MAX_VALUE), "existing-session")
    private val response = """{"access_token":"refreshed-token","token_type":"Bearer","expires_in":3600}"""

    @Test
    fun givenRefreshWithoutSetCookie_whenParsingCredentials_thenExistingCookieIsPreserved() {
        val credentials = connection().accessCredentials(response, fallbackCookie = existingCookie)

        assertSame(existingCookie, credentials.accessCookie)
        assertEquals("refreshed-token", credentials.accessToken?.value)
    }

    @Test
    fun givenRefreshWithRotatedCookie_whenParsingCredentials_thenNewCookieReplacesExistingCookie() {
        val credentials = connection("zuid=rotated-session; Max-Age=3600")
            .accessCredentials(response, fallbackCookie = existingCookie)

        assertEquals("zuid", credentials.accessCookie?.name)
        assertEquals("rotated-session", credentials.accessCookie?.value)
    }

    @Test
    fun givenLoginWithoutSetCookie_whenParsingCredentials_thenNoCookieIsInvented() {
        val credentials = connection().accessCredentials(response)

        assertNull(credentials.accessCookie)
        assertEquals("refreshed-token", credentials.accessToken?.value)
    }

    private fun connection(cookieHeader: String? = null): HttpURLConnection = object : HttpURLConnection(URL("https://example.invalid")) {
        override fun getHeaderField(name: String?): String? = if (name.equals("Set-Cookie", ignoreCase = true)) cookieHeader else null
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
    }
}
