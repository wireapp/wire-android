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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Transition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
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
import com.wire.android.ui.home.messagecomposer.state.MessageComposeInputState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.debug.LocalFeatureVisibilityFlags

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MessageComposeActionsBox(
    transition: Transition<MessageComposeInputState>,
    isMentionActive: Boolean,
    isFileSharingEnabled: Boolean,
    startMention: () -> Unit,
    onAdditionalOptionButtonClicked: () -> Unit,
    onPingClicked: () -> Unit,
    onSelfDeletionOptionButtonClicked: () -> Unit,
    showSelfDeletingOption: Boolean,
    onRichTextEditingButtonClicked: () -> Unit,
    onCloseRichTextEditingButtonClicked: () -> Unit,
    onRichTextEditingHeaderButtonClicked: () -> Unit,
    onRichTextEditingBoldButtonClicked: () -> Unit,
    onRichTextEditingItalicButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.wrapContentSize()) {
        Divider(color = MaterialTheme.wireColorScheme.outline)
        Box(Modifier.wrapContentSize()) {
            transition.AnimatedContent(
                contentKey = { state -> state is MessageComposeInputState.Active },
                transitionSpec = {
                    slideInVertically { fullHeight -> fullHeight / 2 } + fadeIn() with
                            slideOutVertically { fullHeight -> fullHeight / 2 } + fadeOut()
                }
            ) { state ->
                if (state is MessageComposeInputState.Active) {
                    MessageComposeActions(
                        state.isEphemeral,
                        state.attachmentOptionsDisplayed,
                        isMentionActive,
                        state.isEditMessage,
                        isFileSharingEnabled,
                        startMention,
                        onAdditionalOptionButtonClicked,
                        onPingClicked,
                        onSelfDeletionOptionButtonClicked,
                        showSelfDeletingOption,
                        state.isRichTextFormattingOptionsDisplayed,
                        onRichTextEditingButtonClicked,
                        onCloseRichTextEditingButtonClicked,
                        onRichTextEditingHeaderButtonClicked,
                        onRichTextEditingBoldButtonClicked,
                        onRichTextEditingItalicButtonClicked
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageComposeActions(
    selfDeletingOptionSelected: Boolean,
    attachmentOptionsDisplayed: Boolean,
    isMentionsSelected: Boolean,
    isEditMessage: Boolean,
    isFileSharingEnabled: Boolean = true,
    startMention: () -> Unit,
    onAdditionalOptionButtonClicked: () -> Unit,
    onPingClicked: () -> Unit,
    onSelfDeletionOptionButtonClicked: () -> Unit,
    showSelfDeletingOption: Boolean,
    richTextEditingDisplayed: Boolean,
    onRichTextEditingButtonClicked: () -> Unit,
    onCloseRichTextEditingButtonClicked: () -> Unit,
    onRichTextEditingHeaderButtonClicked: () -> Unit,
    onRichTextEditingBoldButtonClicked: () -> Unit,
    onRichTextEditingItalicButtonClicked: () -> Unit
) {
    val localFeatureVisibilityFlags = LocalFeatureVisibilityFlags.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (richTextEditingDisplayed) Arrangement.SpaceBetween else Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions().spacing56x)
    ) {
        if (richTextEditingDisplayed) {
            RichTextOptions(
                onRichTextHeaderButtonClicked = onRichTextEditingHeaderButtonClicked,
                onRichTextBoldButtonClicked = onRichTextEditingBoldButtonClicked,
                onRichTextItalicButtonClicked = onRichTextEditingItalicButtonClicked,
                onCloseRichTextEditingButtonClicked = onCloseRichTextEditingButtonClicked
            )
        } else {
            with(localFeatureVisibilityFlags) {
                if (!isEditMessage) AdditionalOptionButton(
                    isSelected = attachmentOptionsDisplayed,
                    isEnabled = isFileSharingEnabled,
                    onClick = onAdditionalOptionButtonClicked
                )
                if (RichTextIcon) RichTextEditingAction(
                    onButtonClicked = onRichTextEditingButtonClicked
                )
                if (!isEditMessage && EmojiIcon) AddEmojiAction()
                if (!isEditMessage && GifIcon) AddGifAction()
                if (!isEditMessage && showSelfDeletingOption) SelfDeletingMessageAction(
                    isSelected = selfDeletingOptionSelected,
                    onButtonClicked = onSelfDeletionOptionButtonClicked
                )
                if (!isEditMessage && PingIcon) PingAction(onPingClicked = onPingClicked)
                AddMentionAction(isMentionsSelected, startMention)
            }
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
private fun AddGifAction() {
    WireSecondaryIconButton(
        onButtonClicked = {},
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
        AdditionalOptionButton(isSelected = false, isEnabled = true, onClick = {})
        RichTextEditingAction(onButtonClicked = {})
        AddEmojiAction()
        AddGifAction()
        AddMentionAction(isSelected = false, addMentionAction = {})
        PingAction {}
    }
}

@Preview
@Composable
fun PreviewRichTextOptions() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions().spacing56x)
    ) {
        RichTextOptions(
            onRichTextHeaderButtonClicked = {},
            onRichTextBoldButtonClicked = {},
            onRichTextItalicButtonClicked = {},
            onCloseRichTextEditingButtonClicked = {}
        )
    }
}
