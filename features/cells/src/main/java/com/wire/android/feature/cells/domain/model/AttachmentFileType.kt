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

enum class AttachmentFileType(val extensions: List<String>) {
    IMAGE(listOf("jpg", "jpeg", "png", "gif", "webp")),
    VIDEO(listOf("mp4", "mov", "m4v", "ogv", "webm")),
    AUDIO(listOf("mp3", "wav", "ogg", "m4a", "flac", "aac")),
    PDF(listOf("pdf")),
    DOC(listOf("doc", "docx", "dotx", "dot", "odt", "ott", "rtf")),
    SPREADSHEET(listOf("xls", "xlsx", "xltx", "xlt", "ods", "ots", "csv")),
    PRESENTATION(listOf("ppt", "pptx", "ppsx", "pps", "odp", "otp")),
    ARCHIVE(listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "z")),
    CODE(
        listOf(
            "xml", "html", "htm", "js", "json", "css", "php", "phtml", "sparql",
            "py", "cs", "java", "jsp", "sql", "cgi", "pl", "inc", "xsl", "c", "cpp", "kt"
        )
    ),
    OTHER(emptyList());

    companion object {
        fun fromExtension(ext: String): AttachmentFileType =
            entries.firstOrNull { it.extensions.contains(ext.lowercase()) } ?: OTHER

        fun fromMimeType(mimeType: String): AttachmentFileType {
            val lowerMime = mimeType.lowercase()
            return when {
                lowerMime.startsWith("image/") -> IMAGE
                lowerMime.startsWith("video/") -> VIDEO
                lowerMime.startsWith("audio/") -> AUDIO
                lowerMime == "application/pdf" -> PDF
                lowerMime in listOf(
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.oasis.opendocument.text",
                    "application/rtf"
                ) -> DOC

                lowerMime in listOf(
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "text/csv",
                    "application/vnd.oasis.opendocument.spreadsheet"
                ) -> SPREADSHEET

                lowerMime in listOf(
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/vnd.oasis.opendocument.presentation"
                ) -> PRESENTATION

                lowerMime in listOf(
                    "application/zip",
                    "application/x-rar-compressed",
                    "application/x-7z-compressed",
                    "application/x-tar",
                    "application/gzip",
                    "application/x-bzip2",
                    "application/x-xz"
                ) -> ARCHIVE

                lowerMime in listOf(
                    "application/json",
                    "application/javascript",
                    "text/html",
                    "application/xml",
                    "text/css",
                    "text/x-python",
                    "text/x-c",
                    "text/x-java-source"
                ) -> CODE
                else -> OTHER
            }
        }
    }
}

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
        AttachmentFileType.OTHER -> R.drawable.ic_file_type_other
    }

fun AttachmentFileType.previewSupported(): Boolean =
    this in listOf(AttachmentFileType.IMAGE, AttachmentFileType.VIDEO)
