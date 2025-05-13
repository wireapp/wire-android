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
package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
internal fun CollapsableMessageContainer(
    messageContent: UIMessageContent.Regular,
    isCollapsed: Boolean?,
    content: @Composable () -> Unit,
) {
    isCollapsed?.let {
        messageContent.toCollapsedMessageType()?.let { type ->
            AnimatedContent(isCollapsed) { collapsed ->
                if (collapsed) {
                    CollapsedMessageView(type)
                } else {
                    content()
                }
            }
        } ?: content()
    } ?: content()
}

@Composable
private fun CollapsedMessageView(
    type: CollapsedMessageType,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensions().spacing16x),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(type.messageResId),
            color = colorsScheme().onBackground,
            style = typography().body05,
            fontStyle = FontStyle.Italic,
        )
        Spacer(modifier = Modifier.fillMaxWidth().weight(1f))
        Icon(
            painter = painterResource(type.iconResId),
            tint = colorsScheme().onSurface,
            contentDescription = null,
        )
        HorizontalSpace.x12()
        Image(
            painter = painterResource(R.drawable.ic_collapse_expand),
            contentDescription = null,
        )
    }
}

private fun UIMessageContent.Regular.toCollapsedMessageType() =
    when (this) {
        is UIMessageContent.ImageMessage -> CollapsedMessageType.IMAGE
        is UIMessageContent.VideoMessage -> CollapsedMessageType.VIDEO
        is UIMessageContent.AudioAssetMessage -> CollapsedMessageType.AUDIO
        is UIMessageContent.AssetMessage -> CollapsedMessageType.FILE
        is UIMessageContent.Location -> CollapsedMessageType.LOCATION
        else -> null
    }

private enum class CollapsedMessageType(
    val iconResId: Int,
    val messageResId: Int,
) {
    IMAGE(
        iconResId = R.drawable.ic_collapsed_image,
        messageResId = R.string.collapse_picture_text,
    ),
    VIDEO(
        iconResId = R.drawable.ic_collapsed_video,
        messageResId = R.string.collapse_video_text,
    ),
    AUDIO(
        iconResId = R.drawable.ic_collapsed_audio,
        messageResId = R.string.collapse_audio_text,
    ),
    FILE(
        iconResId = R.drawable.ic_collapsed_file,
        messageResId = R.string.collapse_file_text,
    ),
    LOCATION(
        iconResId = R.drawable.ic_collapsed_location,
        messageResId = R.string.collapse_location_text,
    ),
}

@PreviewMultipleThemes
@Composable
private fun PreviewCollapsedView() {
    WireTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing12x)
        ) {
            CollapsedMessageView(
                type = CollapsedMessageType.FILE,
            )
            CollapsedMessageView(
                type = CollapsedMessageType.IMAGE,
            )
            CollapsedMessageView(
                type = CollapsedMessageType.AUDIO,
            )
            CollapsedMessageView(
                type = CollapsedMessageType.VIDEO,
            )
            CollapsedMessageView(
                type = CollapsedMessageType.LOCATION,
            )
        }
    }
}
