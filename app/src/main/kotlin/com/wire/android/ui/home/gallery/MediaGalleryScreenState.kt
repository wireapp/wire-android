package com.wire.android.ui.home.gallery

import android.content.Context
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
class MediaGalleryScreenState(
    val context: Context,
    val modalBottomSheetState: ModalBottomSheetState,
    val coroutineScope: CoroutineScope
) {
    fun showEditContextMenu() {
        coroutineScope.launch { modalBottomSheetState.animateTo(ModalBottomSheetValue.Expanded) }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberMediaGalleryScreenState(
    bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): MediaGalleryScreenState {
    val context = LocalContext.current

    return remember {
        MediaGalleryScreenState(
            context = context,
            modalBottomSheetState = bottomSheetState,
            coroutineScope = coroutineScope
        )
    }
}
