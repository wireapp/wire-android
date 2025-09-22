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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.common.attachmentdraft.ui.AttachmentDraftView
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun MessageAttachments(
    attachments: List<AttachmentDraftUi>,
    onClick: (AttachmentDraftUi) -> Unit,
    onMenuClick: (AttachmentDraftUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var previousSize by remember { mutableStateOf(attachments.size) }

    // Scroll to the last item when a new attachment is added
    LaunchedEffect(attachments.size) {
        if (attachments.size > previousSize) {
            listState.animateScrollToItem(attachments.lastIndex)
        }
        previousSize = attachments.size
    }

    LazyRow(
        modifier = modifier
            .background(color = colorsScheme().surface)
            .fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(end = dimensions().spacing16x)
    ) {
        items(
            items = attachments,
            key = { it.uuid },
        ) { attachment ->
            AttachmentDraftView(
                modifier = Modifier.animateItem(),
                attachment = attachment,
                onClick = { onClick(attachment) },
                onMenuButtonClick = { onMenuClick(attachment) },
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
                localFilePath = "",
                fileSize = 123124,
            ),
            AttachmentDraftUi(
                uuid = "",
                fileName = "New Data Reviewed Final Plus.zip",
                localFilePath = "",
                fileSize = 12312784,
            ),
        ),
        onClick = {},
        onMenuClick = {},
    )
}
