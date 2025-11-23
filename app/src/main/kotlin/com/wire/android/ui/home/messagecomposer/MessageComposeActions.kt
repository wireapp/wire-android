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
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionArgs
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionViewModel
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionViewModelImpl
import com.wire.android.ui.home.messagecomposer.attachments.AdditionalOptionButton
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSelectItem
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.util.isPositiveNotNull

@Composable
fun MessageComposeActions(
    conversationId: ConversationId,
    isEditing: Boolean,
    attachmentsVisible: Boolean,
    selectedOption: AdditionalOptionSelectItem,
    onMentionButtonClicked: () -> Unit,
    onAdditionalOptionButtonClicked: () -> Unit,
    onPingButtonClicked: () -> Unit,
    onSelfDeletionOptionButtonClicked: (SelfDeletionTimer) -> Unit,
    onGifButtonClicked: () -> Unit,
    onRichEditingButtonClicked: () -> Unit,
    isFileSharingEnabled: Boolean,
    isMentionActive: Boolean = true,
    onDrawingModeClicked: () -> Unit
) {
    if (isEditing) {
        EditingActions(
            selectedOption = selectedOption,
            isMentionActive = isMentionActive,
            onRichEditingButtonClicked = onRichEditingButtonClicked,
            onMentionButtonClicked = onMentionButtonClicked
        )
    } else {
        ComposingActions(
            conversationId = conversationId,
            selectedOption = selectedOption,
            isMentionActive = isMentionActive,
            attachmentsVisible = attachmentsVisible,
            onAdditionalOptionButtonClicked = onAdditionalOptionButtonClicked,
            onRichEditingButtonClicked = onRichEditingButtonClicked,
            onGifButtonClicked = onGifButtonClicked,
            onSelfDeletionOptionButtonClicked = onSelfDeletionOptionButtonClicked,
            onPingButtonClicked = onPingButtonClicked,
            onMentionButtonClicked = onMentionButtonClicked,
            onDrawingModeClicked = onDrawingModeClicked,
            isFileSharingEnabled = isFileSharingEnabled
        )
    }
}

@Composable
private fun ComposingActions(
    conversationId: ConversationId,
    selectedOption: AdditionalOptionSelectItem,
    isFileSharingEnabled: Boolean,
    attachmentsVisible: Boolean,
    isMentionActive: Boolean,
    onAdditionalOptionButtonClicked: () -> Unit,
    onRichEditingButtonClicked: () -> Unit,
    onGifButtonClicked: () -> Unit,
    onSelfDeletionOptionButtonClicked: (SelfDeletionTimer) -> Unit,
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
                isSelected = attachmentsVisible,
                onClick = onAdditionalOptionButtonClicked
            )
            RichTextEditingAction(
                isSelected = selectedOption == AdditionalOptionSelectItem.RichTextEditing,
                onRichEditingButtonClicked
            )
            if (DrawingIcon && isFileSharingEnabled) {
                DrawingModeAction(onDrawingModeClicked)
            }
            if (EmojiIcon) AddEmojiAction({})
            if (GifIcon) AddGifAction(onGifButtonClicked)
            SelfDeletingMessageAction(
                conversationId = conversationId,
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
    onMentionButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier
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
private fun DrawingModeAction(onButtonClicked: () -> Unit) {
    WireSecondaryIconButton(
        onButtonClicked = onButtonClicked,
        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
        iconResource = R.drawable.ic_drawing,
        state = WireButtonState.Default,
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
fun SelfDeletingMessageAction(
    conversationId: ConversationId,
    onButtonClicked: (SelfDeletionTimer) -> Unit,
    viewModel: SelfDeletingMessageActionViewModel =
        hiltViewModelScoped<SelfDeletingMessageActionViewModelImpl, SelfDeletingMessageActionViewModel, SelfDeletingMessageActionViewModelImpl.Factory, SelfDeletingMessageActionArgs>(
            SelfDeletingMessageActionArgs(conversationId = conversationId)
        ),
) {
    when (val state = viewModel.state()) {
        SelfDeletionTimer.Disabled -> {}
        is SelfDeletionTimer.Enabled -> WireSecondaryIconButton(
            onButtonClicked = {
                onButtonClicked(state)
            },
            clickBlockParams = ClickBlockParams(blockWhenSyncing = false, blockWhenConnecting = false),
            iconResource = R.drawable.ic_timer,
            contentDescription = R.string.content_description_self_deleting_message_timer,
            state = if (state.duration.isPositiveNotNull()) WireButtonState.Selected else WireButtonState.Default
        )

        is SelfDeletionTimer.Enforced.ByGroup -> WireSecondaryIconButton(
            onButtonClicked = {
                onButtonClicked(state)
            },
            clickBlockParams = ClickBlockParams(blockWhenSyncing = false, blockWhenConnecting = false),
            iconResource = R.drawable.ic_timer,
            contentDescription = R.string.content_description_self_deleting_message_timer,
            state = WireButtonState.Disabled
        )

        is SelfDeletionTimer.Enforced.ByTeam -> WireSecondaryIconButton(
            onButtonClicked = {
                onButtonClicked(state)
            },
            clickBlockParams = ClickBlockParams(blockWhenSyncing = false, blockWhenConnecting = false),
            iconResource = R.drawable.ic_timer,
            contentDescription = R.string.content_description_self_deleting_message_timer,
            state = WireButtonState.Disabled
        )
    }
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
        DrawingModeAction {}
        RichTextEditingAction(true) { }
        AddEmojiAction {}
        AddGifAction {}
        AddMentionAction(false) {}
        PingAction {}
    }
}
