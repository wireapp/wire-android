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
package com.wire.android

import android.text.style.URLSpan
import android.text.util.Linkify
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wire.android.ui.common.LinkSpannableString
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkSpannableStringTest {

    lateinit var linkSpannableString: LinkSpannableString

    @Before
    fun setUp() {
        linkSpannableString = LinkSpannableString("Hello, world!")
    }

    @Test
    fun givenValidIndices_whenSetSpanIsCalled_thenSpanIsSet() {
        // Given
        val start = 0
        val end = 5

        // When
        linkSpannableString.setSpan(URLSpan("http://example.com"), start, end, 0)

        // Then
        assert(linkSpannableString.getSpans(start, end, URLSpan::class.java).isNotEmpty())
    }

    @Test
    fun givenInvalidStartIndex_whenSetSpanIsCalled_thenSpanIsNotSet() {
        // Given
        val start = -1
        val end = 5

        // When
        linkSpannableString.setSpan(URLSpan("http://example.com"), start, end, 0)

        // Then
        assert(linkSpannableString.getSpans(start, end, URLSpan::class.java).isEmpty())
    }

    @Test
    fun givenInvalidEndIndex_whenSetSpanIsCalled_thenSpanIsNotSet() {
        // Given
        val start = 0
        val end = 20

        // When
        linkSpannableString.setSpan(URLSpan("http://example.com"), start, end, 0)

        // Then
        assert(linkSpannableString.getSpans(start, end, URLSpan::class.java).isEmpty())
    }

    @Test
    fun givenASetSpan_whenRemoveSpanIsCalled_thenSpanIsRemoved() {
        // Given
        val urlSpan = URLSpan("http://example.com")
        linkSpannableString.setSpan(urlSpan, 0, 5, 0)

        // When
        linkSpannableString.removeSpan(urlSpan)

        // Then
        assert(linkSpannableString.getSpans(0, 5, URLSpan::class.java).isEmpty())
    }

    @Test
    fun givenATextWithLink_whenGetLinkInfosIsCalled_thenLinkInfoIsReturned() {
        // Given
        val text = "Visit http://example.com for more info."
        val mask = Linkify.WEB_URLS

        // When
        val linkInfos = LinkSpannableString.getLinkInfos(text, mask)

        // Then
        assert(linkInfos.size == 1)
        assert(linkInfos[0].url == "http://example.com")
    }
}
