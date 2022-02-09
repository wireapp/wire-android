package com.wire.android.ui.home.messagecomposer


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography


@Composable
fun rememberMessageComposerState(
    defaultMessageText: TextFieldValue = TextFieldValue(""),
    defaultMessageComposeInputState: MessageComposeInputState = MessageComposeInputState.Enabled
) = remember {
    MessageComposerState(
        defaultMessageText,
        defaultMessageComposeInputState
    )


    Modifier.padding(hori)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessageComposer(
    content: @Composable () -> Unit
) {
    val state = rememberMessageComposerState()

    MessageComposer(
        content = content,
        state = state
    )
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
private fun MessageComposer(
    content: @Composable () -> Unit,
    state: MessageComposerState
) {
    val focusManager = LocalFocusManager.current

    if (state.messageComposeInputState == MessageComposeInputState.Enabled) {
        focusManager.clearFocus()
    }

    BoxWithConstraints {
        Surface(
            Modifier
                .fillMaxWidth()
        ) {
            Column {
                Box(
                    Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            state.toEnabled()
                        }) {
                    content()
                }
                MessageComposerContent(state)
            }
        }
    }
}

@OptIn(
    ExperimentalAnimationApi::class, androidx.compose.animation.core.ExperimentalTransitionApi::class,
    ExperimentalAnimationApi::class
)
@Composable
private fun MessageComposerContent(messageComposerState: MessageComposerState) {
    val transition = updateTransition(messageComposerState.messageComposeInputState, label = "MessageComposeInputState transition")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column {
            Divider()
            transition.AnimatedVisibility(visible = { state -> (state != MessageComposeInputState.Enabled) }) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    val collapseButtonRotationDegree by transition.animateFloat(
                        label = "Collapse button rotation degree transition"
                    ) { state ->
                        when (state) {
                            MessageComposeInputState.Active -> 180f
                            MessageComposeInputState.Enabled -> 180f
                            MessageComposeInputState.FullScreen -> 0f
                        }
                    }

                    CollapseIconButton(
                        onCollapseClick = { messageComposerState.toggleFullScreen() },
                        modifier = Modifier.rotate(degrees = collapseButtonRotationDegree)
                    )
                }
            }
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .then(
                            if (messageComposerState.messageComposeInputState == MessageComposeInputState.FullScreen)
                                Modifier.weight(1f) else Modifier
                        )

                ) {
                    transition.AnimatedVisibility(visible = { messageComposerState.messageComposeInputState == MessageComposeInputState.Enabled }) {
                        AdditionalOptionButton()
                    }
                    Spacer(Modifier.width(8.dp))
                    MessageComposerInput(
                        messageText = messageComposerState.messageText,
                        onMessageTextChanged = { messageComposerState.messageText = it },
                        messageComposerInputState = messageComposerState.messageComposeInputState
                    ) { messageComposerState.toActive() }
                    transition.AnimatedVisibility(visible = { messageComposerState.messageComposeInputState != MessageComposeInputState.Enabled }) {
                        SendButton(messageComposerState.sendButtonEnabled)
                    }
                }
                Divider()
                MessageComposeActions()
            }
        }
    }
}

@Composable
private fun CollapseIconButton(onCollapseClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onCollapseClick
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_collapse),
            contentDescription = stringResource(R.string.content_description_drop_down_icon),
            modifier = modifier
        )
    }
}

@Composable
private fun MessageComposerInput(
    messageText: TextFieldValue,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    messageComposerInputState: MessageComposeInputState,
    onFocusChanged: () -> Unit,
) {
    BasicTextField(
        value = messageText,
        onValueChange = onMessageTextChanged,
        singleLine = messageComposerInputState == MessageComposeInputState.Enabled,
        textStyle = MaterialTheme.wireTypography.body01,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when (messageComposerInputState) {
                    MessageComposeInputState.Enabled -> Modifier.wrapContentHeight()
                    MessageComposeInputState.Active -> Modifier.heightIn(max = 168.dp)
                    MessageComposeInputState.FullScreen -> Modifier.fillMaxHeight()
                }
            )
            .animateContentSize()
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocusChanged()
                }
            }
    )
}

@Composable
private fun SendButton(isEnabled: Boolean) {
    IconButton(
        onClick = { }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    animateColorAsState(
                        when {
                            isEnabled -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.wireColorScheme.onSecondaryButtonDisabled
                        }
                    ).value
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_send),
                contentDescription = stringResource(R.string.content_description_back_button),
                tint = MaterialTheme.wireColorScheme.surface
            )
        }
    }
}

@Composable
private fun MessageComposeActions() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        AdditionalOptionButton()
        RichTextEditingAction()
        AddEmojiAction()
        AddGifAction()
        AddMentionAction()
    }
}

@Composable
private fun AdditionalOptionButton() {
    WireSecondaryButton(
        onClick = { },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = stringResource(R.string.content_description_conversation_search_icon),
            )
        },
        fillMaxWidth = false,
        minHeight = 32.dp,
        minWidth = 40.dp,
    )
}

@Composable
private fun RichTextEditingAction() {
    WireSecondaryButton(
        onClick = { },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_rich_text),
                contentDescription = stringResource(R.string.content_description_conversation_search_icon),
            )
        },
        fillMaxWidth = false,
        minHeight = 32.dp,
        minWidth = 40.dp
    )
}

@Composable
private fun AddEmojiAction() {
    WireSecondaryButton(
        onClick = { },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_emoticon),
                contentDescription = stringResource(R.string.content_description_conversation_search_icon),
            )
        },
        fillMaxWidth = false,
        minHeight = 32.dp,
        minWidth = 40.dp
    )
}

@Composable
private fun AddGifAction() {
    WireSecondaryButton(
        onClick = { },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_gif),
                contentDescription = stringResource(R.string.content_description_conversation_search_icon),
            )
        },
        fillMaxWidth = false,
        minHeight = 32.dp,
        minWidth = 40.dp,
    )
}

@Composable
private fun AddMentionAction() {
    WireSecondaryButton(
        onClick = { },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_mention),
                contentDescription = stringResource(R.string.content_description_conversation_search_icon),
            )
        },
        fillMaxWidth = false,
        minHeight = 32.dp,
        minWidth = 40.dp,
    )
}
