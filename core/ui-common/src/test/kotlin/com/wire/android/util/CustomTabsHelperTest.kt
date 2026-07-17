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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CustomTabsHelperTest {

    @Test
    fun `given empty url and backend website, when resolving url, then return backend support url`() {
        CustomTabsHelper.setBackendWebsiteUrl("https://example.com")

        val result = CustomTabsHelper.resolveUrl("")

        assertEquals("https://example.com/support", result)
    }

    @Test
    fun `given empty url and backend website with trailing slash, when resolving url, then return backend support url`() {
        CustomTabsHelper.setBackendWebsiteUrl("https://example.com/")

        val result = CustomTabsHelper.resolveUrl("")

        assertEquals("https://example.com/support", result)
    }

    @Test
    fun `given non empty url, when resolving url, then keep original url`() {
        CustomTabsHelper.setBackendWebsiteUrl("https://example.com")

        val result = CustomTabsHelper.resolveUrl("not a valid url")

        assertEquals("not a valid url", result)
    }

    @Test
    fun `given empty url and no backend website, when resolving url, then return null`() {
        CustomTabsHelper.setBackendWebsiteUrl(null)

        val result = CustomTabsHelper.resolveUrl("")

        assertNull(result)
    }
}
