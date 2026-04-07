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
package com.wire.android.feature.cells.ui.search.filter.data

import com.wire.android.feature.cells.R
import com.wire.kalium.cells.data.MIMEType

object TypeFilter {
    val typeItems: List<FilterTypeUi> by lazy {
        MIMEType.entries.map { it.toFilterTypeUi() }
    }
}

fun MIMEType.toFilterTypeUi(): FilterTypeUi =
    when (this) {
        MIMEType.PDF -> FilterTypeUi(
            id = name,
            label = R.string.filter_pdf_type,
            iconRes = R.drawable.ic_file_type_pdf,
            mimeType = this
        )

        MIMEType.DOCUMENT -> FilterTypeUi(
            id = name,
            label = R.string.filter_documents_type,
            iconRes = R.drawable.ic_file_type_doc,
            mimeType = this
        )

        MIMEType.IMAGE -> FilterTypeUi(
            id = name,
            label = R.string.filter_images_type,
            iconRes = R.drawable.ic_file_type_image,
            mimeType = this
        )

        MIMEType.EXCEL -> FilterTypeUi(
            id = name,
            label = R.string.filter_spreadsheets_type,
            iconRes = R.drawable.ic_file_type_spreadsheet,
            mimeType = this
        )

        MIMEType.PRESENTATION -> FilterTypeUi(
            id = name,
            label = R.string.filter_presentations_type,
            iconRes = R.drawable.ic_file_type_presentation,
            mimeType = this
        )

        MIMEType.VIDEO -> FilterTypeUi(
            id = name,
            label = R.string.filter_videos_type,
            iconRes = R.drawable.ic_file_type_video,
            mimeType = this
        )

        MIMEType.AUDIO -> FilterTypeUi(
            id = name,
            label = R.string.filter_audio_type,
            iconRes = R.drawable.ic_file_type_audio,
            mimeType = this
        )

        MIMEType.ARCHIVE -> FilterTypeUi(
            id = name,
            label = R.string.filter_archives_type,
            iconRes = R.drawable.ic_file_type_archive,
            mimeType = this
        )

        MIMEType.TEXT -> FilterTypeUi(
            id = name,
            label = R.string.filter_text_files_type,
            iconRes = R.drawable.ic_file_type_text,
            mimeType = this
        )
    }
