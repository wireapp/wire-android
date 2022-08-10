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
import com.wire.android.ui.common.button.WireIconButton
import com.wire.android.ui.common.dimensions

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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions().spacing56x)
    ) {
        AdditionalOptionButton(messageComposerState.attachmentOptionsDisplayed) {
            focusManager.clearFocus()
            messageComposerState.toggleAttachmentOptionsVisibility()
        }
        RichTextEditingAction()
        AddEmojiAction()
        AddGifAction()
        AddMentionAction()
        TakePictureAction()
    }
}

@Composable
private fun RichTextEditingAction() {
    WireIconButton(
        onButtonClicked = {},
        iconResource = R.drawable.ic_rich_text,
        contentDescription = R.string.content_description_conversation_search_icon
    )
}

@Composable
private fun AddEmojiAction() {
    WireIconButton(
        onButtonClicked = {},
        iconResource = R.drawable.ic_emoticon,
        contentDescription = R.string.content_description_conversation_search_icon
    )
}

@Composable
private fun AddGifAction() {
    WireIconButton(
        onButtonClicked = {},
        iconResource = R.drawable.ic_gif,
        contentDescription = R.string.content_description_conversation_search_icon
    )
}

@Composable
private fun AddMentionAction() {
    WireIconButton(
        onButtonClicked = {},
        iconResource = R.drawable.ic_mention,
        contentDescription = R.string.content_description_conversation_search_icon
    )
}

@Composable
private fun TakePictureAction() {
    WireIconButton(
        onButtonClicked = {},
        iconResource = R.drawable.ic_ping,
        contentDescription = R.string.content_description_ping_everyone
    )
}
