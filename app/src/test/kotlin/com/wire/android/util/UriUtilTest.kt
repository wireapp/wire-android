/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.net.URI
import kotlin.random.Random

class UriUtilTest {

    @ParameterizedTest
    @EnumSource(TestParams::class)
    fun `should map other urls to normalized accordingly`(params: TestParams) {
        assertEquals(params.expected, normalizeLink(params.input), "Failed for input: <${params.input}>")
    }

    @Test
    fun givenLink_whenTheLinkStartsWithRandomSchema_thenReturnsTheSameLink() {
        val randomString = Random.string(Random.nextInt(1, 5))
        val input = "$randomString://google.com"
        val expected = "$randomString://google.com"
        val actual = normalizeLink(input)
        assertEquals(expected, actual)
    }

    @Test
    fun givenLink_whenTheLinkWithoutSchema_thenReturnsTheLinkWithHttps() {
        val input = Random.string(Random.nextInt(1, 20))
        val expected = "https://$input"
        val actual = normalizeLink(input)
        assertEquals(expected, actual)
    }

    @Test
    fun givenLinkWithoutRequestedParam_whenCallingFindParameterValue_thenReturnsParamValue() {
        val url = "https://example.com?play=value1"
        val actual = URI(url).findParameterValue("wire_client")
        assertEquals(null, actual)
    }

    @Test
    fun givenLinkWithoutParams_whenCallingFindParameterValue_thenReturnsParamValue() {
        val url = "https://example.com"
        val actual = URI(url).findParameterValue("wire_client")
        assertEquals(null, actual)
    }

    companion object {

        enum class TestParams(val input: String, val expected: String) {
            HTTPS_LINK("https://google.com", "https://google.com"),
            HTTP_LINK("http://google.com", "http://google.com"),
            MAIL_TO_LINK("mailto:alice@wire.com", "mailto:alice@wire.com"),
            DEEP_LINK(
                "wire://access/?config=https://nginz-https.elna.wire.link/deeplink.json",
                "wire://access/?config=https://nginz-https.elna.wire.link/deeplink.json"
            ),
            VALID_WITHOUT_SCHEMA("google.com", "https://google.com"),
            VALID_ENCODED_LINK("https://google.com/this+is+a+link+with+space", "https://google.com/this+is+a+link+with+space"),
        }
    }
}
