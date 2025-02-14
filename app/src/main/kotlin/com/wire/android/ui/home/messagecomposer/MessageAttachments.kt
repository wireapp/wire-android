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
package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.attachmentdraft.ui.AttachmentCard
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun MessageAttachments(
    attachments: List<AttachmentDraftUi>,
    onClick: (AttachmentDraftUi) -> Unit,
    onClickDelete: (AttachmentDraftUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(color = colorsScheme().surface)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        attachments.forEach { node ->
            AttachmentCard(
                modifier = Modifier.width(300.dp),
                file = node,
                onClick = { onClick(node) },
                onClickDelete = { onClickDelete(node) },
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewMessageAttachments() {
    MessageAttachments(
        attachments = listOf(
            AttachmentDraftUi(
                uuid = "",
                fileName = "CDR_20220120 Accessibility Report Reviewed Final Plus.doc",
                fileSize = 123124,
            ),
            AttachmentDraftUi(
                uuid = "",
                fileName = "New Data Reviewed Final Plus.zip",
                fileSize = 12312784,
            ),
        ),
        onClick = {},
        onClickDelete = {},
    )
}
