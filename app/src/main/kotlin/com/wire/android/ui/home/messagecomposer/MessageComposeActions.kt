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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.messagecomposer.attachments.AdditionalOptionButton
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSelectItem
import com.wire.android.util.debug.LocalFeatureVisibilityFlags

@Composable
fun MessageComposeActions(
    isEditing: Boolean,
    selectedOption: AdditionalOptionSelectItem,
    isMentionActive: Boolean = true,
    isSelfDeletingSettingEnabled: Boolean = true,
    isSelfDeletingActive: Boolean = false,
    onMentionButtonClicked: () -> Unit,
    onAdditionalOptionButtonClicked: () -> Unit,
    onPingButtonClicked: () -> Unit,
    onSelfDeletionOptionButtonClicked: () -> Unit,
    onGifButtonClicked: () -> Unit,
    onRichEditingButtonClicked: () -> Unit,
    onDrawingModeClicked: () -> Unit
) {
    if (isEditing) {
        EditingActions(
            selectedOption,
            isMentionActive,
            onRichEditingButtonClicked,
            onMentionButtonClicked
        )
    } else {
        ComposingActions(
            selectedOption,
            isMentionActive,
            onAdditionalOptionButtonClicked,
            onRichEditingButtonClicked,
            onGifButtonClicked,
            isSelfDeletingSettingEnabled,
            isSelfDeletingActive,
            onSelfDeletionOptionButtonClicked,
            onPingButtonClicked,
            onMentionButtonClicked,
            onDrawingModeClicked
        )
    }
}

@Composable
private fun ComposingActions(
    selectedOption: AdditionalOptionSelectItem,
    isMentionActive: Boolean,
    onAdditionalOptionButtonClicked: () -> Unit,
    onRichEditingButtonClicked: () -> Unit,
    onGifButtonClicked: () -> Unit,
    isSelfDeletingSettingEnabled: Boolean,
    isSelfDeletingActive: Boolean,
    onSelfDeletionOptionButtonClicked: () -> Unit,
    onPingButtonClicked: () -> Unit,
    onMentionButtonClicked: () -> Unit,
    onDrawingModeClicked: () -> Unit
) {
    val localFeatureVisibilityFlags = LocalFeatureVisibilityFlags.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions().spacing56x)
    ) {
        with(localFeatureVisibilityFlags) {
            AdditionalOptionButton(
                isSelected = selectedOption == AdditionalOptionSelectItem.AttachFile,
                onClick = onAdditionalOptionButtonClicked
            )
            RichTextEditingAction(
                isSelected = selectedOption == AdditionalOptionSelectItem.RichTextEditing,
                onRichEditingButtonClicked
            )
            if (DrawingIcon) {
                DrawingModeAction(
                    isSelected = selectedOption == AdditionalOptionSelectItem.DrawingMode,
                    onDrawingModeClicked
                )
            }
            if (EmojiIcon) AddEmojiAction({})
            if (GifIcon) AddGifAction(onGifButtonClicked)
            if (isSelfDeletingSettingEnabled) SelfDeletingMessageAction(
                isSelected = isSelfDeletingActive,
                onButtonClicked = onSelfDeletionOptionButtonClicked
            )
            if (PingIcon) PingAction(onPingButtonClicked)
            AddMentionAction(
                isActive = isMentionActive,
                onButtonClicked = onMentionButtonClicked
            )
        }
    }
}

@Composable
fun EditingActions(
    selectedOption: AdditionalOptionSelectItem,
    isMentionActive: Boolean,
    onRichEditingButtonClicked: () -> Unit,
    onMentionButtonClicked: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions().spacing56x)
    ) {
        RichTextEditingAction(
            isSelected = selectedOption == AdditionalOptionSelectItem.RichTextEditing,
            onButtonClicked = onRichEditingButtonClicked
        )
        AddMentionAction(
            isActive = isMentionActive,
            onButtonClicked = onMentionButtonClicked
        )
    }
}

@Composable
private fun RichTextEditingAction(isSelected: Boolean, onButtonClicked: () -> Unit) {
    WireSecondaryIconButton(
        onButtonClicked = onButtonClicked,
        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
        iconResource = R.drawable.ic_rich_text,
        state = if (isSelected) WireButtonState.Selected else WireButtonState.Default,
        contentDescription = R.string.content_description_conversation_enable_rich_text_mode
    )
}

@Composable
private fun DrawingModeAction(isSelected: Boolean, onButtonClicked: () -> Unit) {
    WireSecondaryIconButton(
        onButtonClicked = onButtonClicked,
        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
        iconResource = R.drawable.ic_drawing,
        state = if (isSelected) WireButtonState.Selected else WireButtonState.Default,
        contentDescription = R.string.content_description_conversation_enable_drawing_mode
    )
}

@Composable
private fun AddEmojiAction(onButtonClicked: () -> Unit) {
    var isSelected by remember { mutableStateOf(false) }

    WireSecondaryIconButton(
        onButtonClicked = {
            isSelected = !isSelected
            onButtonClicked()
        },
        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
        iconResource = R.drawable.ic_emoticon,
        contentDescription = R.string.content_description_conversation_send_emoticon
    )
}

@Composable
private fun AddGifAction(onButtonClicked: () -> Unit) {
    var isSelected by remember { mutableStateOf(false) }

    WireSecondaryIconButton(
        onButtonClicked = {
            isSelected = !isSelected
            onButtonClicked()
        },
        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
        iconResource = R.drawable.ic_gif,
        contentDescription = R.string.content_description_conversation_send_gif
    )
}

@Composable
private fun AddMentionAction(isActive: Boolean, onButtonClicked: () -> Unit) {
    WireSecondaryIconButton(
        onButtonClicked = {
            onButtonClicked()
        },
        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
        iconResource = R.drawable.ic_mention,
        contentDescription = R.string.content_description_conversation_mention_someone,
        state = if (isActive) WireButtonState.Selected else WireButtonState.Default
    )
}

@Composable
private fun PingAction(onButtonClicked: () -> Unit) {
    WireSecondaryIconButton(
        onButtonClicked = onButtonClicked,
        clickBlockParams = ClickBlockParams(blockWhenSyncing = false, blockWhenConnecting = false),
        iconResource = R.drawable.ic_ping,
        contentDescription = R.string.content_description_ping_everyone,
        state = WireButtonState.Default
    )
}

@Composable
fun SelfDeletingMessageAction(isSelected: Boolean, onButtonClicked: () -> Unit) {
    WireSecondaryIconButton(
        onButtonClicked = {
            onButtonClicked()
        },
        clickBlockParams = ClickBlockParams(blockWhenSyncing = false, blockWhenConnecting = false),
        iconResource = R.drawable.ic_timer,
        contentDescription = R.string.content_description_self_deleting_message_timer,
        state = if (isSelected) WireButtonState.Selected else WireButtonState.Default
    )
}

@Preview
@Composable
fun PreviewMessageActionsBox() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions().spacing56x)
    ) {
        AdditionalOptionButton(isSelected = false, onClick = {})
        RichTextEditingAction(true, { })
        AddEmojiAction({})
        AddGifAction({})
        AddMentionAction(false, {})
        PingAction {}
    }
}
