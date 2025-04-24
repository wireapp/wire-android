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

sealed class Attachment {
    abstract val name: String

    data class File(
        override val name: String,
        val extension: String
    ) : Attachment() {
        val type: FileType
            get() = FileType.fromExtension(extension)
    }

    data class Folder(
        override val name: String,
        val contents: List<Attachment>
    ) : Attachment()
}

enum class FileType(val extensions: List<String>) {
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
        fun fromExtension(ext: String): FileType =
            entries.firstOrNull { it.extensions.contains(ext.lowercase()) } ?: OTHER

        fun fromMimeType(mimeType: String): FileType {
            return FileType.fromExtension(mimeType.substringAfterLast("/"))
        }
    }
}

fun FileType.icon(): Int =
    when (this) {
        FileType.IMAGE -> R.drawable.ic_file_type_image
        FileType.VIDEO -> R.drawable.ic_file_type_video
        FileType.AUDIO -> R.drawable.ic_file_type_audio
        FileType.PDF -> R.drawable.ic_file_type_pdf
        FileType.DOC -> R.drawable.ic_file_type_doc
        FileType.SPREADSHEET -> R.drawable.ic_file_type_spreadsheet
        FileType.PRESENTATION -> R.drawable.ic_file_type_presentation
        FileType.ARCHIVE -> R.drawable.ic_file_type_archive
        FileType.CODE -> R.drawable.ic_file_type_code
        FileType.OTHER -> R.drawable.ic_file_type_other
    }

fun FileType.previewSupported(): Boolean =
    this in listOf(FileType.IMAGE, FileType.VIDEO)
