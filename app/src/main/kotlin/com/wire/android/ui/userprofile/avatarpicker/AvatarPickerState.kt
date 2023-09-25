/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.userprofile.avatarpicker

import android.content.Context
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.imagepreview.AvatarPickerFlow
import com.wire.android.ui.common.imagepreview.rememberPickPictureState
import com.wire.android.ui.snackbar.LocalSnackbarHostState
import com.wire.android.util.ui.UIText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberAvatarPickerState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    modalBottomSheetState: WireModalSheetState = rememberWireModalSheetState(),
    onImageSelected: (Uri) -> Unit,
    onPictureTaken: () -> Unit,
    targetPictureFileUri: Uri
): AvatarPickerState {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    val avatarPickerFlow: AvatarPickerFlow = rememberPickPictureState(onImageSelected, onPictureTaken, targetPictureFileUri)

    return remember(avatarPickerFlow) {
        AvatarPickerState(
            context,
            coroutineScope,
            snackbarHostState,
            modalBottomSheetState,
            avatarPickerFlow,
        )
    }
}

class AvatarPickerState(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    val snackbarHostState: SnackbarHostState,
    val modalBottomSheetState: WireModalSheetState,
    private val avatarPickerFlow: AvatarPickerFlow,
) {

    fun showModalBottomSheet() {
        coroutineScope.launch { modalBottomSheetState.show() }
    }

    fun showSnackbar(uiText: UIText) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(uiText.asString(context.resources))
        }
    }

    private fun openImageSource(imageSource: ImageSource) {
        avatarPickerFlow.launch(imageSource)
        coroutineScope.launch { modalBottomSheetState.hide() }
    }

    fun openCamera() {
        openImageSource(ImageSource.Camera)
    }

    fun openGallery() {
        openImageSource(ImageSource.Gallery)
    }
}

sealed class ImageSource {
    object Camera : ImageSource()
    object Gallery : ImageSource()
}
