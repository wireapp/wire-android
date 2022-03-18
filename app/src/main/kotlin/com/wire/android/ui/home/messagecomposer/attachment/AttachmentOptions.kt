package com.wire.android.ui.home.messagecomposer.attachment

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import com.wire.android.R
import com.wire.android.ui.common.AttachmentButton
import com.wire.android.ui.common.dimensions

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun AttachmentOptionsComponent() {
    val attachmentOptions = buildAttachmentOptionItems()
    LazyVerticalGrid(
        cells = GridCells.Adaptive(dimensions().spacing64x),
        contentPadding = PaddingValues(start = dimensions().spacing32x, end = dimensions().spacing32x, top = dimensions().spacing32x),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        attachmentOptions.forEach { option ->
            item {
                AttachmentButton(
                    stringResource(option.text), option.icon,
                    modifier = Modifier.padding(bottom = dimensions().spacing24x)
                ) {
                    option.onClick()
                }
            }
        }
    }
}

private fun buildAttachmentOptionItems() = listOf(
    AttachmentOptionItem(R.string.attachment_share_file, R.drawable.ic_attach_file) {
        // TODO: implement attach file options
    },
    AttachmentOptionItem(R.string.attachment_share_image, R.drawable.ic_gallery) {
        // TODO: implement attach image options
    },
    AttachmentOptionItem(R.string.attachment_take_photo, R.drawable.ic_camera) {
        // TODO: implement take photo options
    },
    AttachmentOptionItem(R.string.attachment_record_video, R.drawable.ic_video_icon) {
        // TODO: implement record video options
    },
    AttachmentOptionItem(R.string.attachment_voice_message, R.drawable.ic_mic_on) {
        // TODO: implement voice message options
    },
    AttachmentOptionItem(R.string.attachment_share_location, R.drawable.ic_location) {
        // TODO: implement share location options
    }
)

private data class AttachmentOptionItem(
    @StringRes val text: Int,
    @DrawableRes val icon: Int,
    val onClick: () -> Unit
)

@Preview(showBackground = true)
@Composable
fun PreviewAttachmentComponents() {
    AttachmentOptionsComponent()
}
