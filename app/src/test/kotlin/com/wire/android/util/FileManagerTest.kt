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
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FileManagerTest {

    private val fileManager = FileManager(ApplicationProvider.getApplicationContext())

    @Test
    fun givenContentUri_whenCheckingScheme_thenItIsAccepted() {
        fileManager.checkValidSchema("content://example.provider/assets/1".toUri())
    }

    @Test
    fun givenUnsupportedUriScheme_whenCheckingScheme_thenItIsRejected() {
        val unsupportedUris = listOf(
            "file:///data/asset.txt",
            "https://example.com/asset.txt",
            "data:text/plain,asset",
            "android.resource://com.example/raw/asset",
            "custom://example/asset",
            "asset-without-scheme.txt",
        )

        unsupportedUris.forEach { uri ->
            assertThrows("Expected $uri to be rejected", IllegalArgumentException::class.java) {
                fileManager.checkValidSchema(uri.toUri())
            }
        }
    }
}
