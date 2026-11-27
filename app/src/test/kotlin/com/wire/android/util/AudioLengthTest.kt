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
import okio.Path.Companion.toOkioPath
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaMetadataRetriever
import org.robolectric.shadows.util.DataSource

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AudioLengthTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // A recording cut short by a dead audioserver leaves only the 44-byte WAV header.
    private fun headerOnlyWav() = tempFolder.newFile("empty.wav").apply { writeBytes(ByteArray(WAV_HEADER_SIZE)) }

    @Test
    fun givenEmptyWavThatTheRetrieverRejects_whenGettingLength_thenReturnsZeroInsteadOfCrashing() {
        val file = headerOnlyWav()
        // The JVM has no media framework, so reproduce what the device throws for this file.
        ShadowMediaMetadataRetriever.addException(DataSource.toDataSource(file.absolutePath), RuntimeException("setDataSource failed"))

        val result = getAudioLengthInMs(file.toOkioPath(), SUPPORTED_AUDIO_MIME_TYPE)

        assertEquals(0L, result)
    }

    @Test
    fun givenNonAudioMimeType_whenGettingLength_thenReturnsZero() {
        assertEquals(0L, getAudioLengthInMs(headerOnlyWav().toOkioPath(), "image/png"))
    }

    private companion object {
        const val WAV_HEADER_SIZE = 44
    }
}
