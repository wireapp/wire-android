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
package com.wire.android.ui.home.conversations.messages.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.messages.QuotedMessageStyle
import com.wire.android.ui.home.conversations.messages.QuotedMultipartMessageContent
import com.wire.android.ui.home.conversations.messages.QuotedStyle
import com.wire.android.ui.home.conversations.messages.UIQuotedMultipartMessage
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.model.UIMultipartQuotedContent
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText

@Composable
private fun PreviewMultipartMessage(text: String?, message: UIQuotedMultipartMessage) {
    QuotedMultipartMessageContent(
        senderName = UIText.DynamicString("Compose UI Tester"),
        originalDateTimeText = UIText.DynamicString(""),
        text = text,
        style = QuotedMessageStyle(
            messageStyle = MessageStyle.NORMAL,
            quotedStyle = QuotedStyle.PREVIEW,
            selfAccent = Accent.Blue,
            senderAccent = Accent.Amber
        ),
        accent = Accent.Blue,
        quotedMultipartMessage = message,
        clickable = null,
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewReplies() {
    WireTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
        ) {
            PreviewMultipartMessage(
                text = null,
                message = UIQuotedMultipartMessage(
                    mediaAttachment = UIMultipartQuotedContent(
                        name = "Test File.mp4",
                        localPath = null,
                        previewUrl = "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d",
                        mimeType = "video/mp4",
                        assetAvailable = true,
                    )
                )
            )
            PreviewMultipartMessage(
                text = "Testing multipart quoted message with text and image attachment.",
                message = UIQuotedMultipartMessage(
                    fileAttachment = UIMultipartQuotedContent(
                        name = "Test File.pdf",
                        localPath = null,
                        previewUrl = "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d",
                        mimeType = "application/pdf",
                        assetAvailable = true,
                    )
                ),
            )
            PreviewMultipartMessage(
                text = "Testing multipart quoted message with text and image attachment.",
                message = UIQuotedMultipartMessage(
                    attachmentsCount = 2
                )
            )
        }
    }
}
