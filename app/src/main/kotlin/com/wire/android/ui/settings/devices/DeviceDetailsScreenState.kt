package com.wire.android.ui.settings.devices

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.wire.android.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberConversationScreenState(
    snackBarHostState: SnackbarHostState = remember { SnackbarHostState() },
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): DeviceDetailsScreenState {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    return remember {
        DeviceDetailsScreenState(
            clipboardManager = clipboardManager,
            snackBarHostState = snackBarHostState,
            coroutineScope = coroutineScope,
            copiedText = context.getString(R.string.label_text_copied)
        )
    }
}

class DeviceDetailsScreenState(
    private val clipboardManager: ClipboardManager,
    val snackBarHostState: SnackbarHostState,
    private val coroutineScope: CoroutineScope,
    private val copiedText: String
) {

    fun copyMessage(text: String) {
        clipboardManager.setText(AnnotatedString(text))
        coroutineScope.launch { snackBarHostState.showSnackbar(copiedText) }
    }
}
