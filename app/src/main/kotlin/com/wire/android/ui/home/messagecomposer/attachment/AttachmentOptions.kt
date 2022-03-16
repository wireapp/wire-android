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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.AttachmentButton

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun AttachmentOptionsComponent() {
    val viewModel: AttachmentOptionsViewModel = hiltViewModel()
    LazyVerticalGrid(
        cells = GridCells.Fixed(4),
        contentPadding = PaddingValues(32.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        // First row options
        item {
            AttachmentButton(stringResource(R.string.attachment_share_file), R.drawable.ic_attach_file) {
                Log.d("AttachmentButton", "attach file clicked")
            }
        }
        item {
            AttachmentButton(stringResource(R.string.attachment_share_image), R.drawable.ic_gallery) {
                Log.d("AttachmentButton", "attach image clicked")
            }
        }
        item {
            AttachmentButton(stringResource(R.string.attachment_take_photo), R.drawable.ic_camera) {
                Log.d("AttachmentButton", "take photo clicked")
            }
        }
        item {
            AttachmentButton(stringResource(R.string.attachment_record_video), R.drawable.ic_video_icon) {
                Log.d("AttachmentButton", "take video clicked")
            }
        }
        // Second row options
        item {
            AttachmentButton(
                stringResource(R.string.attachment_voice_message), R.drawable.ic_mic_on, modifier = Modifier.padding(top = 32.dp)
            ) {
                Log.d("AttachmentButton", "voice message clicked")
            }
        }
        item {
            AttachmentButton(
                stringResource(R.string.attachment_share_location), R.drawable.ic_location, modifier = Modifier.padding(top = 32.dp)
            ) {
                Log.d("AttachmentButton", "share location clicked")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAttachmentComponents() {
    AttachmentOptionsComponent()
}
