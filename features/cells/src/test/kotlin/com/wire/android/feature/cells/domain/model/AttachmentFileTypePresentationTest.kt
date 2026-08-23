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

import com.wire.android.feature.cells.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AttachmentFileTypePresentationTest {
    @Test
    fun givenEveryFileType_whenItsIconIsRequested_thenCellsOwnsTheMappedDrawable() {
        val expectedIcons = mapOf(
            AttachmentFileType.IMAGE to R.drawable.ic_file_type_image,
            AttachmentFileType.VIDEO to R.drawable.ic_file_type_video,
            AttachmentFileType.AUDIO to R.drawable.ic_file_type_audio,
            AttachmentFileType.PDF to R.drawable.ic_file_type_pdf,
            AttachmentFileType.DOC to R.drawable.ic_file_type_doc,
            AttachmentFileType.SPREADSHEET to R.drawable.ic_file_type_spreadsheet,
            AttachmentFileType.PRESENTATION to R.drawable.ic_file_type_presentation,
            AttachmentFileType.ARCHIVE to R.drawable.ic_file_type_archive,
            AttachmentFileType.CODE to R.drawable.ic_file_type_code,
            AttachmentFileType.TEXT to R.drawable.ic_file_type_text,
            AttachmentFileType.OTHER to R.drawable.ic_file_type_other,
        )

        expectedIcons.forEach { (type, drawable) -> assertEquals(drawable, type.icon()) }
    }

    @Test
    fun givenFileType_whenCheckingPreviewSupport_thenOnlyMediaTypesAreSupported() {
        assertTrue(AttachmentFileType.IMAGE.previewSupported())
        assertTrue(AttachmentFileType.VIDEO.previewSupported())
        AttachmentFileType.entries
            .filterNot { it == AttachmentFileType.IMAGE || it == AttachmentFileType.VIDEO }
            .forEach { assertFalse(it.previewSupported()) }
    }
}
