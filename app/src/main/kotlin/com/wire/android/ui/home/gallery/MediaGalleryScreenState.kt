package com.wire.android.ui.home.gallery

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
data class MediaGalleryScreenState(
    val snackbarHostState: SnackbarHostState,
    val modalBottomSheetState: ModalBottomSheetState,
    val coroutineScope: CoroutineScope
) {
    fun showContextualMenu(show: Boolean) {
        coroutineScope.launch { modalBottomSheetState.animateTo(if (show) ModalBottomSheetValue.Expanded else ModalBottomSheetValue.Hidden) }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberMediaGalleryScreenState(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): MediaGalleryScreenState {

    return remember {
        MediaGalleryScreenState(
            snackbarHostState = snackbarHostState,
            modalBottomSheetState = bottomSheetState,
            coroutineScope
        )
    }
}
