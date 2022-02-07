package com.wire.android.ui.home.messagecomposer


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.OnDropDownIconButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

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

    val sendButtonVisible: Boolean
        @Composable get() = messageComposeInputState != MessageComposeInputState.Enabled

    val addButtonVisible: Boolean
        @Composable get() = messageComposeInputState == MessageComposeInputState.Enabled

    val dropDownButtonVisible: Boolean
        @Composable get() = isFullScreenOrActive()

    val dropDownButtonRotation: Float
        @Composable get() = when (messageComposeInputState) {
            MessageComposeInputState.FullScreen -> 0f
            MessageComposeInputState.Enabled, MessageComposeInputState.Active -> 180f
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


@OptIn(ExperimentalMaterialApi::class)
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

                val rotationTransition = updateTransition(state.dropDownButtonRotation, label = "")

                val onDropDownButtonRotationDegree by rotationTransition.animateFloat(label = "", transitionSpec = {
                    spring(stiffness = StiffnessLow)
                }) { rotationDegree ->
                    rotationDegree
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column {
                        Divider()
                        AnimatedVisibility(visible = state.dropDownButtonVisible) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                OnDropDownIconButton(
                                    onDropDownClick = { state.toggleFullScreen() },
                                    modifier = Modifier.rotate(degrees = onDropDownButtonRotationDegree)
                                )
                            }
                        }

                        val sizeTransition = updateTransition(state.messageComposeInputState, label = "")

                        val minMessageInputHeight by sizeTransition.animateDp(label = "", transitionSpec = {
                            spring(stiffness = StiffnessLow)
                        }) { state ->
                            when (state) {
                                MessageComposeInputState.Enabled -> 64.dp
                                MessageComposeInputState.Active -> 64.dp
                                MessageComposeInputState.FullScreen -> fullHeight.dp
                            }
                        }

                        val maxMessageInputHeight by sizeTransition.animateDp(label = "", transitionSpec = {
                            spring(stiffness = StiffnessLow)
                        }) { state ->
                            when (state) {
                                MessageComposeInputState.Enabled -> 64.dp
                                MessageComposeInputState.Active -> 168.dp
                                MessageComposeInputState.FullScreen -> fullHeight.dp
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
                            AnimatedVisibility(visible = state.addButtonVisible) {
                                AddButton()
                            }
                            Spacer(Modifier.width(8.dp))
                            MessageTextInput(
                                text = state.messageText,
                                onValueChange = {
                                    state.messageText = it
                                },
                                onIsFocused = {
                                    state.toActive()
                                }, modifier = Modifier
                                    .heightIn(
                                        min = minMessageInputHeight,
                                        max = maxMessageInputHeight
                                    ).wrapContentWidth()
                                    .weight(1f)
                            )
                            AnimatedVisibility(visible = state.sendButtonVisible) {
                                SendButton(isEnabled = state.sendButtonEnabled)
                            }
                        }
                    }
                }
            }
        }
    }
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
fun MessageTextInput(
    text: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onIsFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBarVisibility by remember { mutableStateOf(false) }



        BasicTextField(
            value = text,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.wireTypography.body01,
            modifier = modifier.then(
                Modifier
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onIsFocused()
                        }
                    }
                    .onGloballyPositioned { layoutCoordinates ->
                        val messageInputHeight = layoutCoordinates.size.height.dp

                        if (messageInputHeight == 168.dp) {

                        }
                    }
            )
        )


}

@Composable
fun SendButton(isEnabled: Boolean) {
    IconButton(
        onClick = { }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.wireColorScheme.onSecondaryButtonDisabled)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_send),
                contentDescription = stringResource(R.string.content_description_back_button),
                tint = MaterialTheme.wireColorScheme.surface
            )
        }
    }
}


