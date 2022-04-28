package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.unit.dp
import com.wire.android.ui.home.messagecomposer.button.AddEmojiAction
import com.wire.android.ui.home.messagecomposer.button.AddGifAction
import com.wire.android.ui.home.messagecomposer.button.AddMentionAction
import com.wire.android.ui.home.messagecomposer.button.AdditionalOptionButton
import com.wire.android.ui.home.messagecomposer.button.RichTextEditingAction
import com.wire.android.ui.home.messagecomposer.button.TakePictureAction


@Composable
fun MessageComposeActions(
    messageComposerState: MessageComposerState,
    focusManager: FocusManager
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
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
