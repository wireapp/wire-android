package com.wire.android.ui.home.messagecomposer


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.OnDropDownIconButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import kotlinx.coroutines.CoroutineScope


@Composable
fun rememberMessageComposerState(
    defaultText: TextFieldValue = TextFieldValue(""),
    defaultMessageComposerTextInputState: MessageComposerTextInputState = MessageComposerTextInputState.Enabled,
    defaultSendButtonEnabledState: Boolean = false,
    defaultSendButtonVisibleState: Boolean = false,
    defaultDropDownButtonVisibleState: Boolean = false,
    defaultAddButtonVisible: Boolean = true,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) = remember {
    MessageComposerState(
        defaultText,
        defaultMessageComposerTextInputState,
        defaultDropDownButtonVisibleState,
        defaultSendButtonEnabledState,
        defaultSendButtonVisibleState,
        defaultAddButtonVisible,
        coroutineScope
    )
}

@OptIn(ExperimentalMaterialApi::class)
class MessageComposerState(
    defaultText: TextFieldValue,
    defaultMessageComposerTextInputState: MessageComposerTextInputState,
    defaultSendButtonEnabledState: Boolean,
    defaultSendButtonVisibleState: Boolean,
    defaultDropDownButtonVisibleState: Boolean,
    defaultAddButtonVisible: Boolean,
    val coroutineScope: CoroutineScope
) {

    var text by mutableStateOf(defaultText)
        private set

    var messageComposerTextInputState by mutableStateOf(defaultMessageComposerTextInputState)

    var sendButtonEnabled by mutableStateOf(defaultSendButtonEnabledState)

    var sendButtonVisible by mutableStateOf(defaultSendButtonVisibleState)

    var addButtonVisible by mutableStateOf(defaultAddButtonVisible)

    var dropDownButtonVisible by mutableStateOf(defaultDropDownButtonVisibleState)

    fun onTextChanged(newText: TextFieldValue) {
        if (messageComposerTextInputState == MessageComposerTextInputState.Enabled) {
            changeMessageComposerState(MessageComposerTextInputState.Active)
        }

        sendButtonEnabled = newText.text.filter { !it.isWhitespace() }.isNotBlank()

        text = newText
    }

    fun changeMessageComposerState(state: MessageComposerTextInputState) {
        messageComposerTextInputState = state

        when (state) {
            MessageComposerTextInputState.Active -> {
                dropDownButtonVisible = true
                sendButtonVisible = true
                addButtonVisible = false
            }
            MessageComposerTextInputState.Enabled -> {
                dropDownButtonVisible = true
                sendButtonVisible = false
                addButtonVisible = true
            }
            MessageComposerTextInputState.FullScreen -> {
                dropDownButtonVisible = true
                sendButtonVisible = true
                addButtonVisible = false
            }
        }
    }

    fun toggleFullScreen() {
        messageComposerTextInputState = if (messageComposerTextInputState == MessageComposerTextInputState.FullScreen) {
            MessageComposerTextInputState.Active
        } else {
            MessageComposerTextInputState.FullScreen
        }
    }

}

enum class MessageComposerTextInputState {
    Active,
    Enabled,
    FullScreen
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessageComposer(
    content: @Composable () -> Unit
) {
    val state = rememberMessageComposerState()

    with(state) {
        MessageComposComposer(
            content = content,
            messageText = text,
            messageComposerTextInputState = messageComposerTextInputState,
            addButtonVisible = addButtonVisible,
            sendButtonVisible = sendButtonVisible,
            sendButtonEnabled = sendButtonEnabled,
            dropDownButtonVisible = dropDownButtonVisible,
            onFullScreenClick = ::toggleFullScreen,
            onTextChanged = ::onTextChanged
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MessageComposComposer(
    content: @Composable () -> Unit,
    messageText: TextFieldValue,
    messageComposerTextInputState: MessageComposerTextInputState,
    addButtonVisible: Boolean,
    sendButtonVisible: Boolean,
    sendButtonEnabled: Boolean,
    dropDownButtonVisible: Boolean,
    onFullScreenClick: () -> Unit,
    onTextChanged: (TextFieldValue) -> Unit
) {
    BoxWithConstraints {
        val fullHeight = constraints.maxHeight.toFloat()
        val sheetHeightState = remember { mutableStateOf<Float?>(null) }
        Surface(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    sheetHeightState.value = it.size.height.toFloat()
                }
        ) {
            Column {
                Box(Modifier.weight(1f)) {
                    content()
                }

                val transition = updateTransition(messageComposerTextInputState, label = "")

                val messageTextInputSize by transition.animateDp(label = "", transitionSpec = {
                    spring(stiffness = StiffnessLow)
                }) { state ->
                    when (state) {
                        MessageComposerTextInputState.FullScreen -> fullHeight.dp
                        MessageComposerTextInputState.Active -> 90.dp
                        MessageComposerTextInputState.Enabled -> 56.dp
                    }
                }

                val onDropDownButtonRotationDegree by transition.animateFloat(label = "", transitionSpec = {
                    spring(stiffness = StiffnessLow)
                }) { state ->
                    when (state) {
                        MessageComposerTextInputState.FullScreen -> 180f
                        MessageComposerTextInputState.Active, MessageComposerTextInputState.Enabled -> 0f
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(messageTextInputSize)
                ) {
                    Column {
                        Divider()
                        AnimatedVisibility(visible = dropDownButtonVisible) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                OnDropDownIconButton(
                                    onDropDownClick = onFullScreenClick,
                                    modifier = Modifier.rotate(degrees = onDropDownButtonRotationDegree)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedVisibility(visible = addButtonVisible) {
                                AddButton()
                            }
                            MessageTextInput(
                                text = messageText,
                                onValueChange = onTextChanged,
                                modifier = Modifier.weight(1f)
                            )
                            AnimatedVisibility(visible = sendButtonVisible) {
                                SendButton(isEnabled = sendButtonEnabled)
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
                painter = painterResource(id = R.drawable.ic_search_icon),
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
fun MessageTextInput(text: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = text,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.wireTypography.body01,
        modifier = modifier
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

