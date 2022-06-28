package com.wire.android.ui.calling.common

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.waz.avs.VideoPreview

@Composable
fun CallVideoPreview(
    isCameraOn: Boolean,
    onVideoPreviewCreated: (view: View) -> Unit
) {
    Box {
        if (isCameraOn) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    val videoPreview = VideoPreview(it)
                    onVideoPreviewCreated(videoPreview)
                    videoPreview
                }
            ) {
                it.alpha = 0.5F
            }
        }
    }
}
