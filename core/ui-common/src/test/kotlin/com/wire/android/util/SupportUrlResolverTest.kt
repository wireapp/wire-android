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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class SupportUrlResolverTest {

    @AfterEach
    fun tearDown() {
        SupportUrlResolver.setBaseUrl(null)
    }

    @Test
    fun givenHardcodedUrl_whenResolving_thenReturnHardcodedUrl() {
        val hardcodedUrl = "https://support.wire.com/hc/articles/123"
        SupportUrlResolver.setBaseUrl("https://custom.example")

        val result = SupportUrlResolver.resolve(
            hardcodedUrl = hardcodedUrl,
            page = SupportPage.FEDERATION_SUPPORT
        )

        assertEquals(hardcodedUrl, result)
    }

    @Test
    fun givenBlankHardcodedUrl_whenResolving_thenReturnDynamicUrl() {
        SupportUrlResolver.setBaseUrl("https://custom.example")

        val result = SupportUrlResolver.resolve(
            hardcodedUrl = "",
            page = SupportPage.FEDERATION_SUPPORT
        )

        assertEquals("https://custom.example/support/federation_support", result)
    }

    @Test
    fun givenWhitespaceHardcodedUrl_whenResolving_thenReturnDynamicUrl() {
        SupportUrlResolver.setBaseUrl("https://custom.example")

        val result = SupportUrlResolver.resolve(
            hardcodedUrl = "  ",
            page = SupportPage.SEARCH
        )

        assertEquals("https://custom.example/support/search", result)
    }

    @Test
    fun givenBaseUrlWithTrailingSlash_whenResolving_thenReturnDynamicUrlWithSingleSlash() {
        SupportUrlResolver.setBaseUrl(" https://custom.example/ ")

        val result = SupportUrlResolver.resolve(
            hardcodedUrl = "",
            page = SupportPage.CALL_QUALITY
        )

        assertEquals("https://custom.example/support/call_quality", result)
    }

    @Test
    fun givenSupportHomePage_whenResolving_thenReturnDynamicSupportPageUrl() {
        SupportUrlResolver.setBaseUrl("https://custom.example/")

        val result = SupportUrlResolver.resolve(hardcodedUrl = "", page = SupportPage.SUPPORT)

        assertEquals("https://custom.example/support/support", result)
    }

    @Test
    fun givenBaseUrlChanges_whenResolving_thenUseLatestBaseUrl() {
        SupportUrlResolver.setBaseUrl("https://first.example")
        SupportUrlResolver.setBaseUrl("https://second.example")

        val result = SupportUrlResolver.resolve(hardcodedUrl = "", page = SupportPage.SEARCH)

        assertEquals("https://second.example/support/search", result)
    }
}
