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
package com.wire.android.ui.common.attachmentdraft.ui

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.common.dimensions
import com.wire.kalium.logic.util.fileExtension

@Composable
fun AttachmentDraftView(
    attachment: AttachmentDraftUi,
    onClick: () -> Unit,
    onMenuButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val extension = remember(attachment.fileName) { attachment.fileName.fileExtension() ?: "" }
    val attachmentFileType = remember(extension) { AttachmentFileType.fromExtension(extension) }

    AttachmentScaffold(
        onClick = onClick,
        onMenuButtonClick = onMenuButtonClick,
        showMenuButton = attachment.uploadError,
        modifier = modifier,
    ) {
        when (attachmentFileType) {
            AttachmentFileType.IMAGE -> AttachmentImageView(
                attachment = attachment,
            )

            AttachmentFileType.VIDEO -> AttachmentVideoView(
                attachment = attachment,
            )

            else -> AttachmentFileView(
                attachment = attachment,
                modifier = Modifier.width(dimensions().spacing300x)
            )
        }
    }
}
