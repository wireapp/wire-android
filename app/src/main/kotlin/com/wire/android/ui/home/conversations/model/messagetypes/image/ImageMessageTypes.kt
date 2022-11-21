package com.wire.android.ui.home.conversations.model.messagetypes.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.rememberAsyncImagePainter
import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.ui.common.WireCircularProgressIndicator
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.getUriFromDrawable

@Composable
fun DisplayableImageMessage(imageData: ImageAsset.PrivateAsset, imgParams: ImageMessageParams, modifier: Modifier = Modifier) {
    Image(
        painter = imageData.paint(),
        contentDescription = stringResource(R.string.content_description_image_message),
        modifier = modifier
            .width(imgParams.normalizedWidth)
            .height(imgParams.normalizedHeight),
        alignment = Alignment.Center,
        contentScale = ContentScale.Crop
    )
}

@Composable
fun ImageMessageInProgress(imgParams: ImageMessageParams, isDownloading: Boolean) {
    Box {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .width(imgParams.normalizedWidth)
                .height(imgParams.normalizedHeight)
        ) {
            WireCircularProgressIndicator(
                progressColor = MaterialTheme.wireColorScheme.primary,
                size = MaterialTheme.wireDimensions.spacing24x
            )
            Text(
                text = stringResource(
                    id = if (isDownloading) R.string.asset_message_download_in_progress_text
                    else R.string.asset_message_upload_in_progress_text
                ),
                style = MaterialTheme.wireTypography.body01.copy(color = MaterialTheme.wireColorScheme.secondaryText),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ImageMessageFailed(isDownloadFailure: Boolean) {
    Box {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .size(dimensions().spacing200x)
        ) {
            Image(
                painter = rememberAsyncImagePainter(getUriFromDrawable(LocalContext.current, R.drawable.ic_gallery)),
                contentDescription = null,
                modifier = Modifier
                    .width(dimensions().spacing24x)
                    .height(dimensions().spacing24x),
                alignment = Alignment.CenterStart,
                colorFilter = ColorFilter.tint(Color.Red),
                contentScale = ContentScale.Crop
            )
            Text(
                text = stringResource(
                    id = if (isDownloadFailure) R.string.error_downloading_image_message
                    else R.string.error_uploading_image_message
                ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.wireTypography.body01.copy(color = MaterialTheme.wireColorScheme.error)
            )
        }
    }
}
