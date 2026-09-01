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
@file:Suppress("MatchingDeclarationName")

package com.wire.android.pdfviewer

import androidx.compose.runtime.Composable
import com.wire.android.di.metro.WireAssistedViewModelFactoryGroup
import com.wire.android.di.metro.wireAssistedMetroViewModel

@WireAssistedViewModelFactoryGroup
object PdfViewerManualViewModelFactoryGroup

@Composable
fun pdfViewerViewModel(
    localPath: String?,
    assetId: String?,
    remotePath: String?,
    conversationId: String?,
    assetSize: Long,
    fileName: String?,
): PdfViewerViewModel =
    wireAssistedMetroViewModel<PdfViewerViewModel, PdfViewerManualViewModelFactory>(
        instanceKey = "pdf_viewer_${localPath ?: assetId}"
    ) {
        pdfViewerViewModel(localPath, assetId, remotePath, conversationId, assetSize, fileName)
    }
