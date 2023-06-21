/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionMenuState
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSubMenuState
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun AdditionalOptionButton(isEnabled: Boolean, onClick: () -> Unit) {
    var isSelected by remember { mutableStateOf(false) }

    WireSecondaryIconButton(
        onButtonClicked = onClick,
        iconResource = R.drawable.ic_add,
        contentDescription = R.string.content_description_attachment_item,
        state = if (!isEnabled) WireButtonState.Disabled
        else if (isSelected) WireButtonState.Selected else WireButtonState.Default,
    )
}


@Composable
fun AdditionalOptionsMenu(
    onOnSelfDeletingOptionClicked: (() -> Unit)? = null,
    onAttachmentOptionClicked: (() -> Unit)? = null,
    onMentionButtonClicked: (() -> Unit)? = null,
    onGifOptionClicked: () -> Unit,
    onPingOptionClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var additionalOptionState: AdditionalOptionMenuState by remember { mutableStateOf(AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu) }

    Box(modifier) {
        when (additionalOptionState) {
            is AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu -> {
                AttachmentAndAdditionalOptionsMenuItems(
                    isMentionActive = onMentionButtonClicked != null,
                    isFileSharingEnabled = onAttachmentOptionClicked != null,
                    onMentionButtonClicked = onMentionButtonClicked ?: {},
                    onAttachmentOptionClicked = onAttachmentOptionClicked ?: {},
                    onGifButtonClicked = onGifOptionClicked,
                    onSelfDeletionOptionButtonClicked = onOnSelfDeletingOptionClicked ?: {},
                    onRichEditingButtonClicked = { additionalOptionState = AdditionalOptionMenuState.RichTextEditing },
                    onPingClicked = onPingOptionClicked,
                    showSelfDeletingOption = true,
                    modifier = Modifier.background(Color.Black)
                )
            }

            is AdditionalOptionMenuState.RichTextEditing -> {
                RichTextOptions(
                    onRichTextHeaderButtonClicked = {},
                    onRichTextBoldButtonClicked = {},
                    onRichTextItalicButtonClicked = {},
                    onCloseRichTextEditingButtonClicked = {
                        additionalOptionState = AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu
                    }
                )
            }
        }
    }
}

@Composable
fun AdditionalOptionSubMenu(
    additionalOptionsState: AdditionalOptionSubMenuState,
    modifier: Modifier
) {
    when (additionalOptionsState) {
        AdditionalOptionSubMenuState.AttachFile -> {
            AttachmentOptionsComponent(
                onAttachmentPicked = {},
                tempWritableImageUri = null,
                tempWritableVideoUri = null,
                isFileSharingEnabled = true,
                modifier = modifier
            )
        }

        AdditionalOptionSubMenuState.Emoji -> {}
        AdditionalOptionSubMenuState.Gif -> {}
        AdditionalOptionSubMenuState.RecordAudio -> {}
        AdditionalOptionSubMenuState.AttachImage -> {}
        AdditionalOptionSubMenuState.Hidden -> {}
    }
}

@Composable
fun AttachmentAndAdditionalOptionsMenuItems(
    isMentionActive: Boolean,
    isFileSharingEnabled: Boolean,
    onMentionButtonClicked: () -> Unit,
    onAttachmentOptionClicked: () -> Unit = {},
    onPingClicked: () -> Unit = {},
    onSelfDeletionOptionButtonClicked: () -> Unit,
    showSelfDeletingOption: Boolean,
    onGifButtonClicked: () -> Unit = {},
    onRichEditingButtonClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier.wrapContentSize()) {
        Divider(color = MaterialTheme.wireColorScheme.outline)
        MessageComposeActions(
            false,
            isMentionActive,
            false,
            isEditMessage = false,
            isFileSharingEnabled,
            onMentionButtonClicked = onMentionButtonClicked,
            onAdditionalOptionButtonClicked = onAttachmentOptionClicked,
            onPingButtonClicked = onPingClicked,
            onSelfDeletionOptionButtonClicked = onSelfDeletionOptionButtonClicked,
            showSelfDeletingOption = showSelfDeletingOption,
            onGifButtonClicked = onGifButtonClicked,
            onRichEditingButtonClicked = onRichEditingButtonClicked
        )
    }
}

@Preview
@Composable
fun PreviewAdditionalOptionButton() {
    AdditionalOptionButton(isSelected = false, isEnabled = false, onClick = {})
}
