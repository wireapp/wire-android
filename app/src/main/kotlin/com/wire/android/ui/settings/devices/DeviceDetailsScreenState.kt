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
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberConversationScreenState(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): DeviceDetailsScreenState {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = LocalSnackbarHostState.current

    return remember {
        DeviceDetailsScreenState(
            clipboardManager = clipboardManager,
            coroutineScope = coroutineScope,
            copiedText = context.getString(R.string.label_text_copied),
            snackBarHostState = snackbarHostState
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
