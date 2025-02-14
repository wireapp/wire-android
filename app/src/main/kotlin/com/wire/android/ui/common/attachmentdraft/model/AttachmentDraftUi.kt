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
package com.wire.android.ui.common.attachmentdraft.model

import com.wire.kalium.cells.domain.model.AttachmentDraft
import com.wire.kalium.cells.domain.model.AttachmentUploadStatus

data class AttachmentDraftUi(
    val uuid: String,
    val fileName: String,
    val fileSize: Long = 0,
    val uploadProgress: Float? = null,
    val uploadError: Boolean = false,
    val showDraftLabel: Boolean = false,
)

fun AttachmentDraft.toUiModel() = AttachmentDraftUi(
    uuid = this.uuid,
    fileName = this.fileName,
    fileSize = this.fileSize,
    uploadError = this.uploadStatus == AttachmentUploadStatus.FAILED,
)
