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

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import okio.Path.Companion.toOkioPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.ExperimentalPathApi

@OptIn(ExperimentalPathApi::class)
class FileHelperTest {

    @Test
    fun given_local_file_when_opening_with_external_app_then_stage_copy_under_provider_export_directory() {
        val sourceDirectory = createTempDirectory().toFile()
        val cacheDirectory = createTempDirectory().toFile()
        val sourceFile = File(sourceDirectory, "cells.pdf").apply {
            writeText("cells payload")
        }
        val stagedFile = slot<File>()
        val displayName = slot<String>()
        val arrangement = Arrangement(cacheDirectory)
            .withStagedFileCapture(stagedFile, displayName)
            .arrange()

        arrangement.fileHelper.openAssetFileWithExternalApp(
            sourceFile.toOkioPath(),
            "visible.pdf",
            "application/pdf"
        ) {
            throw AssertionError("External open should not fail")
        }

        assertEquals("visible.pdf", displayName.captured)
        assertEquals("exported", stagedFile.captured.parentFile?.name)
        assertEquals("file-provider", stagedFile.captured.parentFile?.parentFile?.name)
        assertEquals("cells payload", stagedFile.captured.readText())
        assertNotEquals(sourceFile.absolutePath, stagedFile.captured.absolutePath)

        sourceDirectory.toPath().deleteRecursively()
        cacheDirectory.toPath().deleteRecursively()
    }

    private class Arrangement(cacheDirectory: File) {
        @MockK
        lateinit var context: Context

        @MockK
        lateinit var uri: Uri

        val fileHelper by lazy { FileHelper(context) }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockkStatic(FileProvider::class)
            every { context.packageName } returns "com.wire.android.internal.debug"
            every { context.cacheDir } returns cacheDirectory
            every { context.startActivity(any()) } just runs
            every { FileProvider.getUriForFile(any(), any(), any(), any()) } returns uri
        }

        fun withStagedFileCapture(fileSlot: io.mockk.CapturingSlot<File>, displayNameSlot: io.mockk.CapturingSlot<String>) = apply {
            every { FileProvider.getUriForFile(any(), any(), capture(fileSlot), capture(displayNameSlot)) } returns uri
        }

        fun arrange() = this
    }
}
