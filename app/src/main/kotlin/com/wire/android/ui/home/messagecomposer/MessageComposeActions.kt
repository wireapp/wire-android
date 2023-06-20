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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.dimensions
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
@Composable
fun MessageComposeActions(
    selfDeletingOptionSelected: Boolean,
    attachmentOptionsDisplayed: Boolean,
    isMentionsSelected: Boolean,
    isEditMessage: Boolean,
    isFileSharingEnabled: Boolean = true,
    onMentionButtonClicked: () -> Unit,
    onAdditionalOptionButtonClicked: () -> Unit,
    onPingButtonClicked: () -> Unit,
    onSelfDeletionOptionButtonClicked: () -> Unit,
    showSelfDeletingOption: Boolean,
    onGifButtonClicked: () -> Unit,
    onRichEditingButtonClicked: () -> Unit
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
            if (!isEditMessage) AdditionalOptionButton(
                isSelected = attachmentOptionsDisplayed,
                isEnabled = isFileSharingEnabled,
                onClick = onAdditionalOptionButtonClicked
            )
            RichTextEditingAction(onRichEditingButtonClicked)
            if (!isEditMessage && EmojiIcon) AddEmojiAction()
            if (!isEditMessage && GifIcon) AddGifAction(true, onGifButtonClicked)
            if (!isEditMessage && showSelfDeletingOption) SelfDeletingMessageAction(
                isSelected = selfDeletingOptionSelected,
                onButtonClicked = onSelfDeletionOptionButtonClicked
            )
            if (!isEditMessage && PingIcon) PingAction(onPingClicked = onPingButtonClicked)
            AddMentionAction(isMentionsSelected, onMentionButtonClicked)
        }
    }
}

@Composable
private fun RichTextEditingAction(onButtonClicked: () -> Unit) {
    WireSecondaryIconButton(
        onButtonClicked = onButtonClicked,
        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
        iconResource = R.drawable.ic_rich_text,
        contentDescription = R.string.content_description_conversation_enable_rich_text_mode
    )
}

@Composable
private fun AddEmojiAction() {
    WireSecondaryIconButton(
        onButtonClicked = {},
        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
        iconResource = R.drawable.ic_emoticon,
        contentDescription = R.string.content_description_conversation_send_emoticon
    )
}

@Composable
private fun AddGifAction(isSelected: Boolean, onPingClicked: () -> Unit) {
    val onButtonClicked = remember(isSelected) {
        { if (!isSelected) onPingClicked() }
    }

    WireSecondaryIconButton(
        onButtonClicked = onButtonClicked,
        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
        iconResource = R.drawable.ic_gif,
        contentDescription = R.string.content_description_conversation_send_gif
    )
}

@Composable
private fun AddMentionAction(isSelected: Boolean, addMentionAction: () -> Unit) {
    val onButtonClicked = remember(isSelected) {
        { if (!isSelected) addMentionAction() }
    }
    WireSecondaryIconButton(
        onButtonClicked = onButtonClicked,
        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
        iconResource = R.drawable.ic_mention,
        contentDescription = R.string.content_description_conversation_mention_someone,
        state = if (isSelected) WireButtonState.Selected else WireButtonState.Default
    )
}

@Composable
private fun PingAction(onPingClicked: () -> Unit) {
    WireSecondaryIconButton(
        onButtonClicked = onPingClicked,
        clickBlockParams = ClickBlockParams(blockWhenSyncing = false, blockWhenConnecting = false),
        iconResource = R.drawable.ic_ping,
        contentDescription = R.string.content_description_ping_everyone
    )
}

@Composable
fun SelfDeletingMessageAction(isSelected: Boolean, onButtonClicked: () -> Unit) {
    WireSecondaryIconButton(
        onButtonClicked = onButtonClicked,
        clickBlockParams = ClickBlockParams(blockWhenSyncing = false, blockWhenConnecting = false),
        iconResource = R.drawable.ic_timer,
        contentDescription = R.string.content_description_ping_everyone,
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
        AdditionalOptionButton(isSelected = false, isEnabled = true, onClick = {})
        RichTextEditingAction({})
        AddEmojiAction()
        AddGifAction(false, {})
        AddMentionAction(isSelected = false, addMentionAction = {})
        PingAction {}
    }
}
