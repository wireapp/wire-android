/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.home.conversations.ConversationActionPermissionType
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.messagecomposer.location.GeoLocatedAddress
import com.wire.android.ui.home.messagecomposer.location.LocationPickerComponent
import com.wire.android.ui.home.messagecomposer.recordaudio.RecordAudioComponent
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionMenuState
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSelectItem
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSubMenuState
import com.wire.android.ui.home.messagecomposer.state.RichTextMarkdown
import com.wire.android.ui.theme.wireColorScheme
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer

@Composable
fun AdditionalOptionsMenu(
    conversationId: ConversationId,
    additionalOptionsState: AdditionalOptionMenuState,
    selectedOption: AdditionalOptionSelectItem,
    isEditing: Boolean,
    isMentionActive: Boolean,
    onAdditionalOptionsMenuClicked: () -> Unit,
    onMentionButtonClicked: (() -> Unit),
    onPingOptionClicked: () -> Unit,
    onRichEditingButtonClicked: () -> Unit,
    onCloseRichEditingButtonClicked: () -> Unit,
    onRichOptionButtonClicked: (RichTextMarkdown) -> Unit,
    onDrawingModeClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onOnSelfDeletingOptionClicked: ((SelfDeletionTimer) -> Unit)? = null,
    onGifOptionClicked: (() -> Unit)? = null
) {
    Box(modifier.background(colorsScheme().messageComposerBackgroundColor)) {
        when (additionalOptionsState) {
            AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu -> {
                AttachmentAndAdditionalOptionsMenuItems(
                    conversationId = conversationId,
                    selectedOption = selectedOption,
                    isEditing = isEditing,
                    isMentionActive = isMentionActive,
                    onMentionButtonClicked = onMentionButtonClicked,
                    onAdditionalOptionsMenuClicked = onAdditionalOptionsMenuClicked,
                    onGifButtonClicked = onGifOptionClicked ?: {},
                    onSelfDeletionOptionButtonClicked = onOnSelfDeletingOptionClicked ?: {},
                    onRichEditingButtonClicked = onRichEditingButtonClicked,
                    onPingClicked = onPingOptionClicked,
                    onDrawingModeClicked = onDrawingModeClicked
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
    onPermissionPermanentlyDenied: (type: ConversationActionPermissionType) -> Unit,
    onLocationPickerClicked: () -> Unit,
    onCloseAdditionalAttachment: () -> Unit,
    onRecordAudioMessageClicked: () -> Unit,
    additionalOptionsState: AdditionalOptionSubMenuState,
    onImagesPicked: (List<Uri>) -> Unit,
    onAttachmentPicked: (UriAsset) -> Unit,
    onAudioRecorded: (UriAsset) -> Unit,
    onLocationPicked: (GeoLocatedAddress) -> Unit,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AttachmentOptionsComponent(
            onImagesPicked = onImagesPicked,
            onAttachmentPicked = onAttachmentPicked,
            tempWritableImageUri = tempWritableImageUri,
            tempWritableVideoUri = tempWritableVideoUri,
            isFileSharingEnabled = isFileSharingEnabled,
            onRecordAudioMessageClicked = onRecordAudioMessageClicked,
            onLocationPickerClicked = onLocationPickerClicked,
            onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
        )
        when (additionalOptionsState) {
            AdditionalOptionSubMenuState.AttachFile -> {
                /* DO NOTHING, ALREADY DISPLAYED AS PARENT */
            }

            AdditionalOptionSubMenuState.RecordAudio -> {
                RecordAudioComponent(
                    onAudioRecorded = onAudioRecorded,
                    onCloseRecordAudio = onCloseAdditionalAttachment
                )
            }

            AdditionalOptionSubMenuState.Location -> {
                LocationPickerComponent(
                    onLocationPicked = onLocationPicked,
                    onLocationClosed = onCloseAdditionalAttachment
                )
            }
            // non functional for now
            AdditionalOptionSubMenuState.AttachImage -> {}
            AdditionalOptionSubMenuState.Emoji -> {}
            AdditionalOptionSubMenuState.Gif -> {}
        }
    }
}

@Composable
fun AttachmentAndAdditionalOptionsMenuItems(
    conversationId: ConversationId,
    isEditing: Boolean,
    selectedOption: AdditionalOptionSelectItem,
    isMentionActive: Boolean,
    onMentionButtonClicked: () -> Unit,
    onSelfDeletionOptionButtonClicked: (SelfDeletionTimer) -> Unit,
    modifier: Modifier = Modifier,
    onAdditionalOptionsMenuClicked: () -> Unit = {},
    onPingClicked: () -> Unit = {},
    onGifButtonClicked: () -> Unit = {},
    onRichEditingButtonClicked: () -> Unit = {},
    onDrawingModeClicked: () -> Unit = {}
) {
    Column(modifier.wrapContentSize()) {
        HorizontalDivider(color = MaterialTheme.wireColorScheme.outline)
        MessageComposeActions(
            conversationId = conversationId,
            isEditing = isEditing,
            selectedOption = selectedOption,
            isMentionActive = isMentionActive,
            onMentionButtonClicked = onMentionButtonClicked,
            onAdditionalOptionButtonClicked = onAdditionalOptionsMenuClicked,
            onPingButtonClicked = onPingClicked,
            onSelfDeletionOptionButtonClicked = onSelfDeletionOptionButtonClicked,
            onGifButtonClicked = onGifButtonClicked,
            onRichEditingButtonClicked = onRichEditingButtonClicked,
            onDrawingModeClicked = onDrawingModeClicked
        )
    }
}
