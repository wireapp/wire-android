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
package com.wire.android.ui.sharing

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportMediaAuthenticatedIntentParsingInstrumentedTest {

    @Test
    fun givenMultipleShareIntentWithOnlyUriEntries_whenExtractingStreams_thenAllUrisAreReturned() {
        val firstUri = Uri.parse("content://wire.test/first")
        val secondUri = Uri.parse("content://wire.test/second")
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(firstUri, secondUri))
        }

        val result = intent.sharedStreamUris()

        assertEquals(listOf(firstUri, secondUri), result)
    }

    @Test
    fun givenMultipleShareIntentWithMalformedParcelableEntry_whenExtractingStreams_thenOnlyUrisAreReturned() {
        val validUri = Uri.parse("content://wire.test/first")
        val malformedEntry = Bundle()
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf<Parcelable>(validUri, malformedEntry))
        }

        val result = intent.sharedStreamUris()

        assertEquals(listOf(validUri), result)
    }
}
