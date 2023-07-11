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

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.messagecomposer.recordaudio.RecordAudioComponent
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionMenuState
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSelectItem
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSubMenuState
import com.wire.android.ui.home.messagecomposer.state.RichTextMarkdown
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun AdditionalOptionButton(
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
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
    additionalOptionsState: AdditionalOptionMenuState,
    selectedOption: AdditionalOptionSelectItem,
    isFileSharingEnabled: Boolean,
    isSelfDeletingSettingEnabled: Boolean,
    isEditing: Boolean,
    isMentionActive: Boolean,
    onOnSelfDeletingOptionClicked: (() -> Unit)? = null,
    onAdditionalOptionsMenuClicked: () -> Unit,
    onMentionButtonClicked: (() -> Unit),
    onGifOptionClicked: (() -> Unit)? = null,
    onPingOptionClicked: () -> Unit,
    onRichEditingButtonClicked: () -> Unit,
    onCloseRichEditingButtonClicked: () -> Unit,
    onRichOptionButtonClicked: (RichTextMarkdown) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        when (additionalOptionsState) {
            AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu -> {
                AttachmentAndAdditionalOptionsMenuItems(
                    selectedOption = selectedOption,
                    isFileSharingEnabled = isFileSharingEnabled,
                    isEditing = isEditing,
                    isSelfDeletingSettingEnabled = isSelfDeletingSettingEnabled,
                    isMentionActive = isMentionActive,
                    onMentionButtonClicked = onMentionButtonClicked ?: {},
                    onAdditionalOptionsMenuClicked = onAdditionalOptionsMenuClicked,
                    onGifButtonClicked = onGifOptionClicked ?: {},
                    onSelfDeletionOptionButtonClicked = onOnSelfDeletingOptionClicked ?: {},
                    onRichEditingButtonClicked = onRichEditingButtonClicked,
                    onPingClicked = onPingOptionClicked
                )
            }

            AdditionalOptionMenuState.RichTextEditing -> {
                RichTextOptions(
                    onRichTextHeaderButtonClicked = { onRichOptionButtonClicked(RichTextMarkdown.Header) },
                    onRichTextBoldButtonClicked = { onRichOptionButtonClicked(RichTextMarkdown.Bold) },
                    onRichTextItalicButtonClicked = { onRichOptionButtonClicked(RichTextMarkdown.Italic) },
                    onCloseRichTextEditingButtonClicked = onCloseRichEditingButtonClicked
                )
            }

            AdditionalOptionMenuState.Hidden -> {}
        }
    }
}

@Composable
fun AdditionalOptionSubMenu(
    isFileSharingEnabled: Boolean,
    onRecordAudioMessageClicked: () -> Unit,
    onCloseRecordAudio: () -> Unit,
    additionalOptionsState: AdditionalOptionSubMenuState,
    onAttachmentPicked: (UriAsset) -> Unit,
    onAudioRecorded: (UriAsset) -> Unit,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?,
    modifier: Modifier
) {
    when (additionalOptionsState) {
        AdditionalOptionSubMenuState.AttachFile -> {
            AttachmentOptionsComponent(
                onAttachmentPicked = onAttachmentPicked,
                tempWritableImageUri = tempWritableImageUri,
                tempWritableVideoUri = tempWritableVideoUri,
                isFileSharingEnabled = isFileSharingEnabled,
                onRecordAudioMessageClicked = onRecordAudioMessageClicked,
                modifier = modifier
            )
        }

        AdditionalOptionSubMenuState.RecordAudio -> {
            RecordAudioComponent(
                onAudioRecorded = onAudioRecorded,
                onCloseRecordAudio = onCloseRecordAudio
            )
        }
        // non functional for now
        AdditionalOptionSubMenuState.AttachImage -> {}
        AdditionalOptionSubMenuState.Emoji -> {}
        AdditionalOptionSubMenuState.Gif -> {}
        AdditionalOptionSubMenuState.Hidden -> {}
    }
}

@Composable
fun AttachmentAndAdditionalOptionsMenuItems(
    isEditing: Boolean,
    selectedOption: AdditionalOptionSelectItem,
    isMentionActive: Boolean,
    isFileSharingEnabled: Boolean,
    onMentionButtonClicked: () -> Unit,
    onAdditionalOptionsMenuClicked: () -> Unit = {},
    onPingClicked: () -> Unit = {},
    onSelfDeletionOptionButtonClicked: () -> Unit,
    isSelfDeletingSettingEnabled: Boolean,
    onGifButtonClicked: () -> Unit = {},
    onRichEditingButtonClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier.wrapContentSize()) {
        Divider(color = MaterialTheme.wireColorScheme.outline)
        MessageComposeActions(
            isEditing = isEditing,
            selectedOption = selectedOption,
            isFileSharingEnabled = isFileSharingEnabled,
            isMentionActive = isMentionActive,
            onMentionButtonClicked = onMentionButtonClicked,
            onAdditionalOptionButtonClicked = onAdditionalOptionsMenuClicked,
            onPingButtonClicked = onPingClicked,
            onSelfDeletionOptionButtonClicked = onSelfDeletionOptionButtonClicked,
            isSelfDeletingSettingEnabled = isSelfDeletingSettingEnabled,
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
