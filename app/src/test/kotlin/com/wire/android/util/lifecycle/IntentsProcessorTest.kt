/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.util.lifecycle

import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class IntentsProcessorTest {

    @Test
    fun `given null intent, returns null`() {
        val (_, intentsProcessor) = Arrangement().arrange()
        assertNull(intentsProcessor(null))
    }

    @Test
    fun `given intent without automated_login extra, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra(null)
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given valid JSON with both backendConfig and ssoCode, returns AutomatedLoginViaSSO`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"backendConfig":"$FAKE_BACKEND_CONFIG","ssoCode":"$FAKE_SSO_CODE"}""")
            .arrange()
        assertEquals(
            AutomatedLoginViaSSO(backendConfig = FAKE_BACKEND_CONFIG, ssoCode = FAKE_SSO_CODE),
            intentsProcessor(arrangement.intent)
        )
    }

    @Test
    fun `given valid JSON with only ssoCode, returns AutomatedLoginViaSSO`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"ssoCode":"$FAKE_SSO_CODE"}""")
            .arrange()
        assertEquals(
            AutomatedLoginViaSSO(ssoCode = FAKE_SSO_CODE),
            intentsProcessor(arrangement.intent)
        )
    }

    @Test
    fun `given valid JSON with only backendConfig using HTTPS, returns AutomatedLoginViaSSO`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"backendConfig":"$FAKE_BACKEND_CONFIG"}""")
            .arrange()
        assertEquals(
            AutomatedLoginViaSSO(backendConfig = FAKE_BACKEND_CONFIG),
            intentsProcessor(arrangement.intent)
        )
    }

    @Test
    fun `given JSON with both fields null, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{}""")
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given invalid JSON string, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("not-valid-json")
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given backendConfig with HTTP instead of HTTPS, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"backendConfig":"http://insecure.wire.com/deeplink.json"}""")
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given backendConfig with empty host, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"backendConfig":"https:///path"}""")
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    @Test
    fun `given backendConfig with malformed URI, returns null`() {
        val (arrangement, intentsProcessor) = Arrangement()
            .withAutomatedLoginExtra("""{"backendConfig":"not a url"}""")
            .arrange()
        assertNull(intentsProcessor(arrangement.intent))
    }

    class Arrangement {
        internal val intent: Intent = mockk()

        init {
            every { intent.getStringExtra(any()) } returns null
        }

        fun arrange() = this to IntentsProcessor()

        fun withAutomatedLoginExtra(json: String?) = apply {
            every { intent.getStringExtra("automated_login") } returns json
        }
    }

    private companion object {
        const val FAKE_BACKEND_CONFIG = "https://example.com/deeplink.json"
        const val FAKE_SSO_CODE = "wire-87080ee2-7855-47e2-a60a-4b3def45bbd4"
    }
}
