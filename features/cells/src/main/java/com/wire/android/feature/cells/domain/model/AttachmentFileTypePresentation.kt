/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.cells.domain.model

import com.wire.android.feature.cells.R

fun AttachmentFileType.icon(): Int =
    when (this) {
        AttachmentFileType.IMAGE -> R.drawable.ic_file_type_image
        AttachmentFileType.VIDEO -> R.drawable.ic_file_type_video
        AttachmentFileType.AUDIO -> R.drawable.ic_file_type_audio
        AttachmentFileType.PDF -> R.drawable.ic_file_type_pdf
        AttachmentFileType.DOC -> R.drawable.ic_file_type_doc
        AttachmentFileType.SPREADSHEET -> R.drawable.ic_file_type_spreadsheet
        AttachmentFileType.PRESENTATION -> R.drawable.ic_file_type_presentation
        AttachmentFileType.ARCHIVE -> R.drawable.ic_file_type_archive
        AttachmentFileType.CODE -> R.drawable.ic_file_type_code
        AttachmentFileType.TEXT -> R.drawable.ic_file_type_text
        AttachmentFileType.OTHER -> R.drawable.ic_file_type_other
    }

fun AttachmentFileType.previewSupported(): Boolean =
    this in listOf(AttachmentFileType.IMAGE, AttachmentFileType.VIDEO)
