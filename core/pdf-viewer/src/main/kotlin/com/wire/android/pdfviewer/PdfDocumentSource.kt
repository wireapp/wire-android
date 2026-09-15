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
package com.wire.android.pdfviewer

/**
 * Everything needed to get hold of a PDF, from the tap that opens it through to the download.
 *
 * A document is reachable either because it is already on disk ([localPath]) or because it can be
 * downloaded ([assetId] + [remotePath]); the rest is metadata used to place and label it.
 *
 * @param localPath already downloaded copy, when there is one.
 * @param assetId cell asset UUID, used to download and to name the fallback file.
 * @param remotePath cells object key, which also decides where the file is stored locally.
 * @param conversationId conversation the asset belongs to, when it has one.
 * @param fileName display name, also used as the local file name.
 * @param assetSize expected size in bytes, 0 when unknown.
 */
data class PdfDocumentSource(
    val localPath: String? = null,
    val assetId: String? = null,
    val remotePath: String? = null,
    val conversationId: String? = null,
    val fileName: String? = null,
    val assetSize: Long = 0L,
)
