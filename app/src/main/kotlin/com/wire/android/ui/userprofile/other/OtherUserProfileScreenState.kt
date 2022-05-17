package com.wire.android.ui.userprofile.other

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberOtherUserProfileScreenState(
    snackBarHostState: SnackbarHostState = remember { SnackbarHostState() }
): OtherUserProfileScreenState {
    val coroutineScope = rememberCoroutineScope()
    val clipBoardManager = LocalClipboardManager.current

    return remember {
        OtherUserProfileScreenState(
            clipBoardManager = clipBoardManager,
            snackbarHostState = snackBarHostState,
            coroutineScope = coroutineScope
        )
    }
}

class OtherUserProfileScreenState(
    private val clipBoardManager: ClipboardManager,
    val snackbarHostState: SnackbarHostState,
    private val coroutineScope: CoroutineScope
) {

    fun copy(text: String) {
        clipBoardManager.setText(AnnotatedString(text))
        coroutineScope.launch { snackbarHostState.showSnackbar(text) }
    }
}
