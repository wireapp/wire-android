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

package com.wire.android.ui.home.conversations.model

import android.text.util.Linkify
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset
import com.wire.android.ui.common.LinkifyText
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.messagetypes.asset.MessageAsset
import com.wire.android.ui.home.conversations.model.messagetypes.image.DisplayableImageMessage
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageFailed
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageInProgress
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImportedImageMessage
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.Message.DownloadStatus.DOWNLOAD_IN_PROGRESS
import com.wire.kalium.logic.data.message.Message.DownloadStatus.FAILED_DOWNLOAD
import com.wire.kalium.logic.data.message.Message.UploadStatus.FAILED_UPLOAD
import com.wire.kalium.logic.data.message.Message.UploadStatus.UPLOAD_IN_PROGRESS

// TODO: Here we actually need to implement some logic that will distinguish MentionLabel with Body of the message,
//       waiting for the backend to implement mapping logic for the MessageBody
@Composable
internal fun MessageBody(
    messageBody: MessageBody,
    onLongClick: (() -> Unit)? = null,
    onOpenProfile: (String) -> Unit,
) {
    LinkifyText(
        text = messageBody.message,
        mask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES,
        color = MaterialTheme.colorScheme.onBackground,
        onLongClick = onLongClick,
        modifier = Modifier.defaultMinSize(minHeight = dimensions().spacing20x),
        style = MaterialTheme.wireTypography.body01,
        onOpenProfile = onOpenProfile
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageImage(
    asset: ImageAsset?,
    imgParams: ImageMessageParams,
    uploadStatus: Message.UploadStatus,
    downloadStatus: Message.DownloadStatus,
    onImageClick: Clickable,
    shouldFillMaxWidth: Boolean = false,
    isImportedMediaAsset: Boolean = false
) {
    Box(
        Modifier
            .clip(shape = RoundedCornerShape(dimensions().messageAssetBorderRadius))
            .background(
                color = MaterialTheme.wireColorScheme.onPrimary, shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline,
                shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
            )
            .wrapContentSize()
            .combinedClickable(
                enabled = onImageClick.enabled,
                onClick = onImageClick.onClick,
                onLongClick = onImageClick.onLongClick,
            )
    ) {
        when {
            asset != null -> {
                if (isImportedMediaAsset) ImportedImageMessage(asset, shouldFillMaxWidth)
                else DisplayableImageMessage(asset, imgParams)
            }

            // Trying to upload the asset
            uploadStatus == UPLOAD_IN_PROGRESS || downloadStatus == DOWNLOAD_IN_PROGRESS -> {
                ImageMessageInProgress(imgParams, downloadStatus == DOWNLOAD_IN_PROGRESS)
            }

            // Show error placeholder
            uploadStatus == FAILED_UPLOAD || downloadStatus == FAILED_DOWNLOAD -> {
                ImageMessageFailed(downloadStatus == FAILED_DOWNLOAD)
            }
        }
    }
}

@Composable
internal fun MessageGenericAsset(
    assetName: String,
    assetExtension: String,
    assetSizeInBytes: Long,
    onAssetClick: Clickable,
    assetUploadStatus: Message.UploadStatus,
    assetDownloadStatus: Message.DownloadStatus,
    shouldFillMaxWidth: Boolean = true,
    isImportedMediaAsset: Boolean = false
) {
    MessageAsset(
        assetName,
        assetExtension,
        assetSizeInBytes,
        onAssetClick,
        assetUploadStatus,
        assetDownloadStatus,
        shouldFillMaxWidth,
        isImportedMediaAsset
    )
}
