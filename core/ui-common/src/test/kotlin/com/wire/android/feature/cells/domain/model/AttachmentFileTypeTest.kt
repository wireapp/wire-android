/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.feature.cells.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class AttachmentFileTypeTest {
    @ParameterizedTest
    @MethodSource("extensions")
    fun givenExtension_whenClassified_thenExpectedTypeIsReturned(extension: String, expected: AttachmentFileType) {
        assertEquals(expected, AttachmentFileType.fromExtension(extension))
    }

    @ParameterizedTest
    @MethodSource("mimeTypes")
    fun givenMimeType_whenClassified_thenExpectedTypeIsReturned(mimeType: String, expected: AttachmentFileType) {
        assertEquals(expected, AttachmentFileType.fromMimeType(mimeType))
    }

    private companion object {
        @JvmStatic
        fun extensions(): Stream<Array<Any>> = Stream.of(
            arrayOf("JPEG", AttachmentFileType.IMAGE),
            arrayOf("m4v", AttachmentFileType.VIDEO),
            arrayOf("flac", AttachmentFileType.AUDIO),
            arrayOf("pdf", AttachmentFileType.PDF),
            arrayOf("odt", AttachmentFileType.DOC),
            arrayOf("csv", AttachmentFileType.SPREADSHEET),
            arrayOf("odp", AttachmentFileType.PRESENTATION),
            arrayOf("7z", AttachmentFileType.ARCHIVE),
            arrayOf("kt", AttachmentFileType.CODE),
            arrayOf("markdown", AttachmentFileType.TEXT),
            arrayOf("unknown", AttachmentFileType.OTHER),
        )

        @JvmStatic
        fun mimeTypes(): Stream<Array<Any>> = Stream.of(
            arrayOf("IMAGE/PNG", AttachmentFileType.IMAGE),
            arrayOf("video/webm", AttachmentFileType.VIDEO),
            arrayOf("audio/aac", AttachmentFileType.AUDIO),
            arrayOf("application/pdf", AttachmentFileType.PDF),
            arrayOf("application/vnd.oasis.opendocument.text", AttachmentFileType.DOC),
            arrayOf("text/csv", AttachmentFileType.SPREADSHEET),
            arrayOf("application/vnd.oasis.opendocument.presentation", AttachmentFileType.PRESENTATION),
            arrayOf("application/x-7z-compressed", AttachmentFileType.ARCHIVE),
            arrayOf("text/x-python", AttachmentFileType.CODE),
            arrayOf("text/markdown", AttachmentFileType.TEXT),
            arrayOf("application/octet-stream", AttachmentFileType.OTHER),
        )
    }
}
