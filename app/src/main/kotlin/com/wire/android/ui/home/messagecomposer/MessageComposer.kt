package com.wire.android.ui.home.messagecomposer


import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Surface
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.SwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.OnDropDownIconButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
class MessageComposerState(
    defaultMessageComposerTextState: MessageComposerValue,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    confirmStateChange: (MessageComposerValue) -> Boolean = { true }
) : SwipeableState<MessageComposerValue>(
    initialValue = MessageComposerValue.Normal,
    animationSpec = animationSpec,
    confirmStateChange = confirmStateChange
) {

    companion object {
        /**
         * The default [Saver] implementation for [ModalBottomSheetState].
         */
        fun Saver(
            animationSpec: AnimationSpec<Float>,
            confirmStateChange: (MessageComposerValue) -> Boolean
        ): Saver<MessageComposerState, *> = Saver(
            save = { it.currentValue },
            restore = {
                MessageComposerState(
                    defaultMessageComposerTextState = it,
                    animationSpec = animationSpec,
                    confirmStateChange = confirmStateChange
                )
            }
        )
    }

//    var text by mutableStateOf(defaultText)
//        private set

    var messageComposerTextState by mutableStateOf(defaultMessageComposerTextState)

//    var sendButtonEnabled by mutableStateOf(defaultSendButtonEnabledState)

//    var isExpanded by mutableStateOf(defaultIsExpandedState)
//
//    fun onTextChanged(newText: TextFieldValue) {
//        when (messageComposerTextState) {
//            MessageComposerTextState.Enabled -> {
//                text = newText
//                messageComposerTextState = MessageComposerTextState.Active
//            }
//            MessageComposerTextState.Active -> {
//                text = newText
//                sendButtonEnabled = newText.text.filter { !it.isWhitespace() }.isNotBlank()
//            }
//            MessageComposerTextState.Expanded -> {
//            }
//        }
//    }

    suspend fun expand() {
        animateTo(MessageComposerValue.FullScreen)
    }

//    val isActive: Boolean
//        get() = messageComposerTextState is MessageComposerTextState.Active
//}

}

enum class MessageComposerValue {
    Normal,
    FullScreen
}


//
//    @Composable
//    fun rememberMessageComposerState(
//        defaultText: TextFieldValue = TextFieldValue(""),
//        defaultMessageComposerTextState: MessageComposerTextState = MessageComposerTextState.Enabled,
//        defaultSendButtonEnabledState: Boolean = false,
//        defaultIsExpandedState: Boolean = false,
//        coroutineScope: CoroutineScope = rememberCoroutineScope()
//    ) = remember {
//        MessageComposerState(
//            defaultText,
//            defaultMessageComposerTextState,
//            defaultSendButtonEnabledState,
//            defaultIsExpandedState,
//            coroutineScope
//        )
//    }

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessageComposercontent(
    content: @Composable () -> Unit,
    state: MessageComposerState = rememberSaveableState(MessageComposerValue.Normal)
) {
    MessageComposComposerState(content, state)
}

@Composable
@ExperimentalMaterialApi
fun rememberSaveableState(
    initialValue: MessageComposerValue,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    confirmStateChange: (MessageComposerValue) -> Boolean = { true }
): MessageComposerState {
    return rememberSaveable(
        saver = MessageComposerState.Saver(
            animationSpec = animationSpec,
            confirmStateChange = confirmStateChange
        )
    ) {
        MessageComposerState(
            defaultMessageComposerTextState = initialValue,
            animationSpec = animationSpec,
            confirmStateChange = confirmStateChange
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MessageComposComposerState(
    content: @Composable () -> Unit,
    state: MessageComposerState,
) {
    val scope = rememberCoroutineScope()
    BoxWithConstraints {
        val fullHeight = constraints.maxHeight.toFloat()
        val sheetHeightState = remember { mutableStateOf<Float?>(null) }
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
        Surface(
            Modifier
                .fillMaxWidth()
                .offset {
                    val y = state.offset.value.roundToInt()

                    IntOffset(0, y)
                }.height(400.dp)
                .bottomSheetSwipeable(state, fullHeight, sheetHeightState)
                .onGloballyPositioned {
                    sheetHeightState.value = it.size.height.toFloat()
                }.semantics {

                    dismiss {
                        scope.launch { state.expand() }
                        true
                    }
                        expand {
                            scope.launch { state.expand()  }
                            true
                        }
                        collapse {
                            scope.launch { state.expand() }
                            true
                        }
            }
        ) {
            Column {
                Divider()
//                if (state.isActive) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    OnDropDownIconButton { scope.launch { state.expand() } }
                }
//                }
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    if (state.messageComposerTextState is MessageComposerTextState.Enabled) {
//                        AddButton()
//                    }
                MessageTextInput(
                    text = TextFieldValue("this is test"),
                    onValueChange = { },
                )
//                    if (state.isActive) {
//                        SendButton(state.sendButtonEnabled)
//                    }
//                }
            }
        }
    }
}

@Suppress("ModifierInspectorInfo")
@OptIn(ExperimentalMaterialApi::class)
private fun Modifier.bottomSheetSwipeable(
    sheetState: MessageComposerState,
    fullHeight: Float,
    sheetHeightState: State<Float?>
): Modifier {
    val sheetHeight = sheetHeightState.value
    val modifier = if (sheetHeight != null) {
        val anchors = if (sheetHeight < fullHeight / 2) {
            mapOf(
                100f to MessageComposerValue.Normal,
                800f to MessageComposerValue.FullScreen
            )
        } else {
            mapOf(
                100f to MessageComposerValue.Normal,
                800f to MessageComposerValue.FullScreen,
            )
        }
        Modifier.swipeable(
            state = sheetState,
            anchors = anchors,
            orientation = Orientation.Vertical,
            enabled = true,
            resistance = null
        )
    } else {
        Modifier
    }

    return this.then(modifier)
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
