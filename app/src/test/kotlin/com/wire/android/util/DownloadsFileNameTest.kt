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

import android.app.Application
import android.webkit.MimeTypeMap
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DownloadsFileNameTest {

    @Before
    fun setUp() {
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypeMapping("jpg", "image/jpeg")
    }

    @Test
    fun givenNameWithoutExtension_whenAddingExtensionFromMimeType_thenMatchingExtensionIsAppended() {
        assertEquals("image.jpg", "image".withExtensionFromMimeType("image/jpeg"))
    }

    @Test
    fun givenNameWithExtension_whenAddingExtensionFromMimeType_thenNameIsUnchanged() {
        assertEquals("photo.png", "photo.png".withExtensionFromMimeType("image/jpeg"))
    }

    @Test
    fun givenNullMimeType_whenAddingExtensionFromMimeType_thenNameIsUnchanged() {
        assertEquals("image", "image".withExtensionFromMimeType(null))
    }

    @Test
    fun givenUnknownMimeType_whenAddingExtensionFromMimeType_thenNameIsUnchanged() {
        assertEquals("image", "image".withExtensionFromMimeType("application/x-unknown-type"))
    }

    @Test
    fun givenNameWithExtension_whenAddingTimestampSuffix_thenSuffixIsAddedBeforeExtension() {
        assertEquals("image_123.jpg", "image.jpg".withTimestampSuffix(123L))
    }

    @Test
    fun givenNameWithoutExtension_whenAddingTimestampSuffix_thenSuffixIsAppended() {
        assertEquals("image_123", "image".withTimestampSuffix(123L))
    }
}
