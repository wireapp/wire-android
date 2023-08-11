/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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

import com.wire.android.string
import org.junit.jupiter.api.Test
import kotlin.random.Random

class UriUtilTest {
    @Test
    fun givenLink_whenTheLinkStartsWithHttps_thenReturnsTheSameLink() {
        val input = "https://google.com"
        val expected = "https://google.com"
        val actual = normalizeLink(input)
        assert(expected == actual)
    }

    @Test
    fun givenLink_whenTheLinkStartsWithHttp_thenReturnsTheSameLink() {
        val input = "http://google.com"
        val expected = "http://google.com"
        val actual = normalizeLink(input)
        assert(expected == actual)
    }

    @Test
    fun givenLink_whenTheLinkStartsWithMailTo_thenReturnsTheSameLink() {
        val input = "mailto:alice@wire.com"
        val expected = "mailto:alice@wire.com"
        val actual = normalizeLink(input)
        assert(expected == actual)
    }

    @Test
    fun givenLink_whenTheLinkIsWireDeepLink_thenReturnsTheSameLink() {
        val input = "wire://access/?config=https://nginz-https.elna.wire.link/deeplink.json"
        val expected = "wire://access/?config=https://nginz-https.elna.wire.link/deeplink.json"
        val actual = normalizeLink(input)
        assert(expected == actual)
    }

    @Test
    fun givenLink_whenTheLinkStartsWithRandomSchema_thenReturnsTheSameLink() {
        val randomString = Random.string(Random.nextInt(5))
        val input = "$randomString://google.com"
        val expected = "$randomString://google.com"
        val actual = normalizeLink(input)
        assert(expected == actual)
    }

    @Test
    fun givenLink_whenTheLinkWithoutSchema_thenReturnsTheLinkWithHttps() {
        val input = Random.string(Random.nextInt(20))
        val expected = "https://$input"
        val actual = normalizeLink(input)
        assert(expected == actual)
    }

    @Test
    fun givenLink_whenTheLinkIsValidWithoutSchema_thenReturnsTheLinkWithHttps() {
        val input = "google.com"
        val expected = "https://$input"
        val actual = normalizeLink(input)
        assert(expected == actual)
    }
}
