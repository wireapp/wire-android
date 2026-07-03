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
package com.wire.android.util

import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SupportUrlResolverTest {

    @Test
    fun `given empty url and backend website, when resolving url, then return backend support url`() {
        SupportUrlResolver.setBackendWebsiteUrl("https://example.com")

        val result = SupportUrlResolver.resolveUrl("")

        assertEquals("https://example.com/support", result)
    }

    @Test
    fun `given empty url and backend website with trailing slash, when resolving url, then return backend support url`() {
        SupportUrlResolver.setBackendWebsiteUrl("https://example.com/")

        val result = SupportUrlResolver.resolveUrl("")

        assertEquals("https://example.com/support", result)
    }

    @Test
    fun `given valid url, when resolving url, then keep original url`() {
        SupportUrlResolver.setBackendWebsiteUrl("https://example.com")

        val result = SupportUrlResolver.resolveUrl("https://wire.com/help")

        assertEquals("https://wire.com/help", result)
    }

    @Test
    fun `given invalid url, when resolving url, then return null`() {
        SupportUrlResolver.setBackendWebsiteUrl("https://example.com")

        val result = SupportUrlResolver.resolveUrl("not a valid url")

        assertNull(result)
    }

    @Test
    fun `given empty url and no backend website, when resolving url, then return null`() {
        SupportUrlResolver.setBackendWebsiteUrl(null)

        val result = SupportUrlResolver.resolveUrl("")

        assertNull(result)
    }
}
