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

package com.wire.android.feature.cells.util

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.wire.android.util.FILE_PROVIDER_SHARED_FILES_ROOT
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FileHelperSharingTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val fileHelper = FileHelper(context)

    @Test
    fun givenCellFileOutsideProviderRoot_whenOpeningExternally_thenSharedCacheUriIsGranted() {
        val sourceFile = createCellFile("open-cell.txt")
        var errorCalled = false

        fileHelper.openAssetFileWithExternalApp(
            localPath = sourceFile.toOkioPath(),
            assetName = sourceFile.name,
            mimeType = MIME_TYPE,
            onError = { errorCalled = true }
        )

        val viewIntent = shadowOf(context).nextStartedActivity
        val sharedUri = viewIntent.data
        assertFalse(errorCalled)
        assertEquals(Intent.ACTION_VIEW, viewIntent.action)
        assertNotNull(sharedUri)
        assertEquals(FILE_PROVIDER_SHARED_FILES_ROOT, sharedUri?.pathSegments?.first())
        assertEquals(FILE_CONTENT, sharedUri?.readText())
    }

    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    @Test
    fun givenCellFileOutsideProviderRoot_whenSharingExternally_thenChooserContainsSharedCacheUri() {
        val sourceFile = createCellFile("share-cell.txt")
        var errorCalled = false

        fileHelper.shareFileChooser(
            assetDataPath = sourceFile.toOkioPath(),
            assetName = sourceFile.name,
            mimeType = MIME_TYPE,
            onError = { errorCalled = true }
        )

        val chooserIntent = shadowOf(context).nextStartedActivity
        val sendIntent = chooserIntent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        val sharedUri = sendIntent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        assertFalse(errorCalled)
        assertEquals(Intent.ACTION_CHOOSER, chooserIntent.action)
        assertEquals(Intent.ACTION_SEND, sendIntent?.action)
        assertNotNull(sharedUri)
        assertEquals(FILE_PROVIDER_SHARED_FILES_ROOT, sharedUri?.pathSegments?.first())
        assertEquals(FILE_CONTENT, sharedUri?.readText())
    }

    private fun createCellFile(name: String): File =
        File(requireNotNull(context.getExternalFilesDir(null)), name).apply {
            writeText(FILE_CONTENT)
        }

    private fun Uri.readText(): String {
        val inputStream = requireNotNull(context.contentResolver.openInputStream(this)) {
            "Expected FileProvider URI to be readable"
        }
        return inputStream.bufferedReader().use { it.readText() }
    }

    private companion object {
        const val MIME_TYPE = "text/plain"
        const val FILE_CONTENT = "cell content"
    }
}
