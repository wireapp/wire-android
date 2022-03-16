package com.wire.android.ui.home.messagecomposer.attachment

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.AttachmentButton

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun AttachmentOptionsComponent() {
//    val keyboardController = LocalSoftwareKeyboardController.current
//    keyboardController?.hide()

    LazyVerticalGrid(
        cells = GridCells.Fixed(4),
        contentPadding = PaddingValues(32.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        // TODO: Change this to dynamic way of building using "items"
        item {
            AttachmentButton("Attach File", R.drawable.ic_attach_file) { Log.d("AttachmentButton", "attach file clicked") }
        }
        item {
            AttachmentButton("Attach Image", R.drawable.ic_gallery) { Log.d("AttachmentButton", "attach image clicked") }
        }
        item {
            AttachmentButton("Take Photo", R.drawable.ic_camera) { Log.d("AttachmentButton", "take photo clicked") }
        }
        item {
            AttachmentButton("Take Photo", R.drawable.ic_video_icon) { Log.d("AttachmentButton", "take video clicked") }
        }
        item {
            AttachmentButton(
                "Voice Message", R.drawable.ic_mic_on, modifier = Modifier.padding(top = 24.dp)
            ) {
                Log.d("AttachmentButton", "voice message clicked")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAttachmentComponents() {
    AttachmentOptionsComponent()
}
