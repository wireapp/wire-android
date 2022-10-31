package com.wire.android.ui.home.messagecomposer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Transition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.dimensions
import com.wire.android.util.debug.LocalFeatureVisibilityFlags

@ExperimentalAnimationApi
@Composable
fun MessageComposeActionsBox(
    modifier: Modifier,
    transition: Transition<MessageComposeInputState>,
    messageComposerState: MessageComposerInnerState,
    focusManager: FocusManager
) {
    Column(
        modifier
            .wrapContentSize()
    ) {
        Divider()
        Box(Modifier.wrapContentSize()) {
            transition.AnimatedVisibility(
                visible = { messageComposerState.messageComposeInputState != MessageComposeInputState.Enabled },
                // we are animating the exit, so that the MessageComposeActions go down
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight / 2 }
                ) + fadeOut()
            ) {
                MessageComposeActions(messageComposerState, focusManager)
            }
        }
    }
}

@Composable
private fun MessageComposeActions(
    messageComposerState: MessageComposerInnerState,
    focusManager: FocusManager
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
            AdditionalOptionButton(messageComposerState.attachmentOptionsDisplayed) {
                focusManager.clearFocus()
                messageComposerState.toggleAttachmentOptionsVisibility()
            }
            if (RichTextIcon)
                RichTextEditingAction()
            if (EmojiIcon)
                AddEmojiAction()
            if (GifIcon)
                AddGifAction()
            AddMentionAction(messageComposerState::startMention)
            if (PingIcon)
                PingAction()
        }
    }
}

@Composable
private fun RichTextEditingAction() {
    WireSecondaryIconButton(
        onButtonClicked = {},
        blockUntilSynced = true,
        iconResource = R.drawable.ic_rich_text,
        contentDescription = R.string.content_description_conversation_enable_rich_text_mode
    )
}

@Composable
private fun AddEmojiAction() {
    WireSecondaryIconButton(
        onButtonClicked = {},
        blockUntilSynced = true,
        iconResource = R.drawable.ic_emoticon,
        contentDescription = R.string.content_description_conversation_send_emoticon
    )
}

@Composable
private fun AddGifAction() {
    WireSecondaryIconButton(
        onButtonClicked = {},
        blockUntilSynced = true,
        iconResource = R.drawable.ic_gif,
        contentDescription = R.string.content_description_conversation_send_gif
    )
}

@Composable
private fun AddMentionAction(addMentionAction: () -> Unit) {
    WireSecondaryIconButton(
        onButtonClicked = addMentionAction,
        blockUntilSynced = true,
        iconResource = R.drawable.ic_mention,
        contentDescription = R.string.content_description_conversation_mention_someone
    )
}

@Composable
private fun PingAction() {
    WireSecondaryIconButton(
        onButtonClicked = {},
        blockUntilSynced = true,
        iconResource = R.drawable.ic_ping,
        contentDescription = R.string.content_description_ping_everyone
    )
}
