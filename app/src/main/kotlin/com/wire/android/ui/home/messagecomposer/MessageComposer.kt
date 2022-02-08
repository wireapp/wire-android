package com.wire.android.ui.home.messagecomposer


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.OnDropDownIconButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography


private class TransistionData(
    dropDownButtonRotation: State<Float>,
    val dropDownButtonVisiblity: Boolean,
//    sendButtonVisiblity: State<Boolean>,
    messageComposerMaxHeight: State<Dp>,
    messageComposerMinHeight: State<Dp>,
) {
    val dropDownButtonRotation by dropDownButtonRotation

    //    val addButtonVisiblity by addButtonVisiblity
//    val sendButtonVisiblity by sendButtonVisiblity
    val messageComposerMaxHeight by messageComposerMaxHeight
    val messageComposerMinHeight by messageComposerMinHeight
}

@OptIn(ExperimentalAnimationApi::class, androidx.compose.animation.core.ExperimentalTransitionApi::class)
@Composable
private fun updateTransistionData(messageComposerInputState: MessageComposeInputState): TransistionData {
    val transistion = updateTransition(messageComposerInputState, label = "")

    val dropDownButtonRotation = transistion.animateFloat(label = "") { state ->
        when (state) {
            MessageComposeInputState.Active -> 0f
            MessageComposeInputState.Enabled -> 180f
            MessageComposeInputState.FullScreen -> 180f
        }
    }
    val messageComposerMaxHeight = transistion.animateDp(label = "") { state ->
        when (state) {
            MessageComposeInputState.Active -> 56.dp
            MessageComposeInputState.Enabled -> 56.dp
            MessageComposeInputState.FullScreen -> 250.dp
        }
    }

    val messageComposerMinHeight = transistion.animateDp(label = "") { state ->
        when (state) {
            MessageComposeInputState.Enabled -> 64.dp
            MessageComposeInputState.Active -> 64.dp
            MessageComposeInputState.FullScreen -> 250.dp
        }
    }

    val dropDownButtonVisibility = messageComposerInputState == MessageComposeInputState.Active

    return remember(transistion) {
        TransistionData(dropDownButtonRotation, dropDownButtonVisibility, messageComposerMaxHeight, messageComposerMinHeight)
    }
}


class MessageComposerState(
    defaultMessageText: TextFieldValue,
    defaultMessageComposeInputState: MessageComposeInputState,
) {

    var messageText by mutableStateOf(defaultMessageText)

    var messageComposeInputState by mutableStateOf(defaultMessageComposeInputState)
        private set

    val sendButtonEnabled: Boolean
        @Composable get() = if (messageComposeInputState == MessageComposeInputState.Enabled) {
            false
        } else {
            messageText.text.filter { !it.isWhitespace() }
                .isNotBlank()
        }

    fun toEnabled() {
        messageComposeInputState = MessageComposeInputState.Enabled
    }

    fun toActive() {
        messageComposeInputState = MessageComposeInputState.Active
    }

    fun toggleFullScreen() {
        messageComposeInputState = if (messageComposeInputState == MessageComposeInputState.Active)
            MessageComposeInputState.FullScreen else MessageComposeInputState.Active
    }

    private fun isFullScreenOrActive() =
        (messageComposeInputState == MessageComposeInputState.Active) or
                (messageComposeInputState == MessageComposeInputState.FullScreen)

}

enum class MessageComposeInputState {
    Active, Enabled, FullScreen
}

@Composable
fun rememberMessageComposerState(
    defaultMessageText: TextFieldValue = TextFieldValue(""),
    defaultMessageComposeInputState: MessageComposeInputState = MessageComposeInputState.Enabled
) = remember {
    MessageComposerState(
        defaultMessageText,
        defaultMessageComposeInputState
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessageComposer(
    content: @Composable () -> Unit
) {
    val state = rememberMessageComposerState()

    MessageComposer(
        content,
        state
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
        val fullHeight = constraints.maxHeight.toFloat()
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

@OptIn(ExperimentalAnimationApi::class, androidx.compose.animation.core.ExperimentalTransitionApi::class)
@Composable
fun MessageComposerContent(messageComposerState: MessageComposerState) {
    val transition = updateTransition(messageComposerState.messageComposeInputState, label = "")

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
                    val onDropDownButtonRotationDegree by transition.animateFloat(label = "", transitionSpec = {
                        spring(stiffness = StiffnessLow)
                    }) { state ->
                        when (state) {
                            MessageComposeInputState.Active -> 180f
                            MessageComposeInputState.Enabled -> 180f
                            MessageComposeInputState.FullScreen -> 0f
                        }
                    }

                    OnDropDownIconButton(
                        onDropDownClick = { messageComposerState.toggleFullScreen() },
                        modifier = Modifier.rotate(degrees = onDropDownButtonRotationDegree)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                transition.AnimatedVisibility(visible = { messageComposerState.messageComposeInputState == MessageComposeInputState.Enabled }) {
                    AddButton()
                }
                Spacer(Modifier.width(8.dp))
                MessageComposerInput(
                    messageText = messageComposerState.messageText,
                    onMessageTextChanged = { messageComposerState.messageText = it },
                    messageComposerInputState = messageComposerState.messageComposeInputState,
                    onFocusChanged = { messageComposerState.toActive() })
                transition.AnimatedVisibility(visible = { messageComposerState.messageComposeInputState != MessageComposeInputState.Enabled }) {
                    SendButton(isEnabledTransition = transition.createChildTransition {
                        messageComposerState.sendButtonEnabled
                    })
                }
            }
            Divider()
        }
    }
}

@Composable
fun RowScope.MessageComposerInput(
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
            .weight(1f)
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
fun AddButton() {
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
        shape = RoundedCornerShape(size = 12.dp),
        contentPadding = PaddingValues(0.dp)
    )
}

@Composable
fun SendButton(isEnabledTransition: Transition<Boolean>) {
    val backgroundColor by isEnabledTransition.animateColor(label = "") { isEnabled ->
        if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.wireColorScheme.onSecondaryButtonDisabled
    }

    IconButton(
        onClick = { }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(backgroundColor)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_send),
                contentDescription = stringResource(R.string.content_description_back_button),
                tint = MaterialTheme.wireColorScheme.surface
            )
        }
    }
}
