package com.wire.android.ui.home.messagecomposer


import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
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

@OptIn(ExperimentalMaterialApi::class)
class MessageComposerState(
    defaultText: TextFieldValue,
    defaultMessageComposerTextState: MessageComposerTextState,
    defaultMessageComposerTextInputState: MessageComposerTextInputState,
    defaultSendButtonEnabledState: Boolean,
    val coroutineScope: CoroutineScope
) {

    var text by mutableStateOf(defaultText)
        private set

    var messageComposerTextState by mutableStateOf(defaultMessageComposerTextState)

    var messageComposerTextInputState by mutableStateOf(defaultMessageComposerTextInputState)

    var sendButtonEnabled by mutableStateOf(defaultSendButtonEnabledState)

    fun onTextChanged(newText: TextFieldValue) {
        when (messageComposerTextState) {
            MessageComposerTextState.Enabled -> {
                text = newText
                messageComposerTextState = MessageComposerTextState.Active
            }
            MessageComposerTextState.Active -> {
                text = newText
                sendButtonEnabled = newText.text.filter { !it.isWhitespace() }.isNotBlank()
            }
            MessageComposerTextState.Expanded -> {
            }
        }
    }

    val isActive: Boolean
        get() = messageComposerTextState is MessageComposerTextState.Active

}

enum class MessageComposerTextInputState {
    Normal,
    FullScreen
}

@Composable
fun rememberMessageComposerState(
    defaultText: TextFieldValue = TextFieldValue(""),
    defaultMessageComposerTextState: MessageComposerTextState = MessageComposerTextState.Enabled,
    defaultMessageComposerTextInputState: MessageComposerTextInputState = MessageComposerTextInputState.Normal,
    defaultSendButtonEnabledState: Boolean = false,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) = remember {
    MessageComposerState(
        defaultText,
        defaultMessageComposerTextState,
        defaultMessageComposerTextInputState,
        defaultSendButtonEnabledState,
        coroutineScope
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessageComposercontent(
    content: @Composable () -> Unit,
    state: MessageComposerState = rememberMessageComposerState()
) {
    MessageComposComposerState(content, state)
}

@OptIn(ExperimentalMaterialApi::class, androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
private fun MessageComposComposerState(
    content: @Composable () -> Unit,
    state: MessageComposerState,
) {
    val isFullScreen = remember { mutableStateOf(false) }

    BoxWithConstraints {
        val fullHeight = constraints.maxHeight.toFloat()
        val sheetHeightState = remember { mutableStateOf<Float?>(null) }
        Surface(
            Modifier
                .fillMaxWidth()
//                .offset {
//                    val y = state.offset.value.roundToInt()
//
//                    IntOffset(0, y)
//                }

                .onGloballyPositioned {
                    sheetHeightState.value = it.size.height.toFloat()
                }
        ) {
            Column {
                Box(Modifier.weight(1f)) {
                    content()
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
//                    var currentState by remember { mutableStateOf(BoxState.Collapsed) }
//                    val transition = updateTransition(currentState, label = "")
//
//                    Divider()
//                    if (state.isActive) {
//
////
////                        val size by transition.animateDp(label = "") { state ->
////                            when (state) {
////                                BoxState.Collapsed -> 200.dp
////                                BoxState.Expanded -> 1000.dp
////                            }
////                        }
//                        Box(Modifier.wrapContentSize()) {
//                            Box(
//                                contentAlignment = Alignment.Center,
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .height(size)
//                            ) {
//                                OnDropDownIconButton {
//                                    currentState = if (currentState == BoxState.Collapsed) {
//                                        BoxState.Expanded
//                                    } else {
//                                        BoxState.Collapsed
//                                    }
//                                }
//                            }
//
//                            MessageTextInput(
//                                text = TextFieldValue("this is test"),
//                                onValueChange = { },
//                            )
//                        }
//                    }
//                }
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    if (state.messageComposerTextState is MessageComposerTextState.Enabled) {
//                        AddButton()
//                    }

                }
//                    if (state.isActive) {
//                        SendButton(state.sendButtonEnabled)
//                    }
//                }
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

sealed class MessageComposerTextState {
    object Enabled : MessageComposerTextState()
    object Active : MessageComposerTextState()
    object Expanded : MessageComposerTextState()
}
