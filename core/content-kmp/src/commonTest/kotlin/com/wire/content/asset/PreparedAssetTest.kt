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

package com.wire.content.asset

import com.wire.content.external.ExternalContentImportResult
import com.wire.content.external.ExternalContentReference
import com.wire.kalium.logic.data.asset.AttachmentType
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PreparedAssetTest {
    @Test
    fun extensionAndNameAreDerivedWithoutPlatformFileApis() {
        val asset = asset(fileName = "meeting.notes.pdf", dataSize = 2L * 1024L * 1024L)

        assertEquals("meeting", asset.assetName)
        assertEquals("PDF (2.0 MB)", asset.extensionWithSize)
    }

    @Test
    fun externalReferenceRejectsBlankTokens() {
        assertFailsWith<IllegalArgumentException> { ExternalContentReference(" ") }
    }

    @Test
    fun tooLargeResultKeepsBytePrecision() {
        val asset = asset()

        assertEquals(
            ExternalContentImportResult.TooLarge(asset, 25L * 1024L * 1024L),
            ExternalContentImportResult.TooLarge(asset, 25L * 1024L * 1024L),
        )
    }

    @Test
    fun fileNamePolicyPreservesCurrentCompatibilityRules() {
        assertEquals(false, AssetFileNamePolicy.isCompatible(".hidden"))
        assertEquals(false, AssetFileNamePolicy.isCompatible("bad/name"))
        assertEquals("bad_name_", AssetFileNamePolicy.sanitize(".bad/name\""))
        assertEquals("file", AssetFileNamePolicy.sanitize("."))
    }

    private fun asset(
        fileName: String = "photo.jpg",
        dataSize: Long = 42L,
    ) = PreparedAsset(
        key = "key",
        mimeType = "image/jpeg",
        dataPath = "/tmp/asset".toPath(),
        dataSize = dataSize,
        fileName = fileName,
        assetType = AttachmentType.IMAGE,
    )
}
