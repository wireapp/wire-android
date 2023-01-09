package com.wire.android.ui.home.conversations

import android.content.Context
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.wire.android.R
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberConversationScreenState(
    snackBarHostState: SnackbarHostState = remember { SnackbarHostState() },
    bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): ConversationScreenState {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    return remember {
        ConversationScreenState(
            context = context,
            clipboardManager = clipboardManager,
            snackBarHostState = snackBarHostState,
            modalBottomSheetState = bottomSheetState,
            coroutineScope = coroutineScope
        )
    }
}

//todo: pass directly the strings, to avoid passing the context
@OptIn(ExperimentalMaterialApi::class)
class ConversationScreenState(
    val context: Context,
    val clipboardManager: ClipboardManager,
    val snackBarHostState: SnackbarHostState,
    val modalBottomSheetState: ModalBottomSheetState,
    val coroutineScope: CoroutineScope
) {

    var selectedMessage by mutableStateOf<UIMessage?>(null)

    val isMyMessage
        get() = selectedMessage?.messageSource == MessageSource.Self

    val isTextMessage
        get() = selectedMessage?.messageContent is UIMessageContent.TextMessage

    fun showEditContextMenu(message: UIMessage) {
        selectedMessage = message

        coroutineScope.launch { modalBottomSheetState.animateTo(ModalBottomSheetValue.Expanded) }
    }

    fun hideEditContextMenu(onComplete: () -> Unit = {}) {
        coroutineScope.launch {
            modalBottomSheetState.animateTo(ModalBottomSheetValue.Hidden)
            onComplete()
        }
    }

    fun copyMessage() {
        selectedMessage?.messageContent.let { messageContent ->
            if (messageContent is UIMessageContent.TextMessage) {
                clipboardManager.setText(AnnotatedString(messageContent.messageBody.message.asString(context.resources)))
                coroutineScope.launch {
                    modalBottomSheetState.animateTo(ModalBottomSheetValue.Hidden)
                    snackBarHostState.showSnackbar(context.getString(R.string.info_message_copied))
                }
            }
        }
    }
}
