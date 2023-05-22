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

package com.wire.android.ui.home.conversations.model.messagetypes.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun DisplayableImageMessage(
    imageData: ImageAsset,
    imgParams: ImageMessageParams,
    modifier: Modifier = Modifier
) {
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
fun ImportedImageMessage(
    imageData: ImageAsset,
    shouldFillMaxWidth: Boolean,
    modifier: Modifier = Modifier
) {
    Image(
        painter = imageData.paint(),
        contentDescription = stringResource(R.string.content_description_image_message),
        modifier = if (!shouldFillMaxWidth) modifier
            .width(dimensions().importedMediaAssetSize)
            .height(dimensions().importedMediaAssetSize)
        else modifier
            .fillMaxWidth()
            .height(dimensions().importedMediaAssetSize),
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
                .padding(MaterialTheme.wireDimensions.spacing8x)
        ) {
            WireCircularProgressIndicator(
                progressColor = MaterialTheme.wireColorScheme.primary,
                size = MaterialTheme.wireDimensions.spacing24x
            )
            Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.spacing8x))
            Text(
                text = stringResource(
                    id = if (isDownloading) R.string.asset_message_download_in_progress_text
                    else R.string.asset_message_upload_in_progress_text
                ),
                style = MaterialTheme.wireTypography.subline01.copy(color = MaterialTheme.wireColorScheme.secondaryText),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ImageMessageFailed(imgParams: ImageMessageParams, isDownloadFailure: Boolean) {
    Box {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .width(imgParams.normalizedWidth)
                .height(imgParams.normalizedHeight)
                .padding(MaterialTheme.wireDimensions.spacing8x)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_gallery),
                contentDescription = null,
                modifier = Modifier
                    .width(dimensions().spacing24x)
                    .height(dimensions().spacing24x),
                alignment = Alignment.CenterStart,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.spacing8x))
            Text(
                text = stringResource(
                    id = if (isDownloadFailure) R.string.error_downloading_image_message
                    else R.string.error_uploading_image_message
                ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.wireTypography.subline01.copy(color = MaterialTheme.wireColorScheme.error)
            )
        }
    }
}
