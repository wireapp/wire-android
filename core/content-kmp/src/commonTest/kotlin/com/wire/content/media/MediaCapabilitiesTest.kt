/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.content.media

import com.wire.content.external.ExternalContentReference
import com.wire.content.external.PlatformResult
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaCapabilitiesTest {
    @Test
    fun `unsupported media capabilities report unsupported instead of fake success`() = runTest {
        val path = "/tmp/image.jpg".toPath()
        val reference = ExternalContentReference("opaque-image")

        assertEquals(PlatformResult.Unsupported, UnsupportedMediaMetadataReader.read(path, "image/jpeg"))
        assertEquals(
            PlatformResult.Unsupported,
            UnsupportedImageProcessor.process(
                ImageProcessingRequest(reference, path, ImageResizeProfile.AVATAR, removeMetadata = true)
            )
        )
        assertEquals(PlatformResult.Unsupported, UnsupportedImageTargetProvider.createTarget(path))
        assertEquals(
            PlatformResult.Unsupported,
            UnsupportedEncodedImageExporter.export(
                EncodedImageExportRequest(
                    image = EncodedImage(byteArrayOf(1), "image/jpeg"),
                    displayName = "image.jpg",
                    fallbackPath = path,
                )
            )
        )
    }
}
