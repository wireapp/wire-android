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

package com.wire.android.ui.calling.common

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.waz.avs.CameraPreviewBuilder

@Composable
fun CallVideoPreview(
    isCameraOn: Boolean,
    onVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit
) {
    if (isCameraOn) {
        Box {
            val context = LocalContext.current
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    val videoPreview = CameraPreviewBuilder(context)
                        .shouldFill(true)
                        .build()
                    onVideoPreviewCreated(videoPreview)
                    videoPreview
                }
            ) {
                it.alpha = 0.5F
            }
        }
    } else {
        onSelfClearVideoPreview()
    }
}

@Preview
@Composable
fun PreviewCallVideoPreview() {
    CallVideoPreview(false, {}, {})
}
