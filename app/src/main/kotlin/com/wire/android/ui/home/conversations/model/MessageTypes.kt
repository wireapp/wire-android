package com.wire.android.ui.home.conversations.model

import android.text.util.Linkify
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset
import com.wire.android.ui.common.LinkifyText
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.model.messagetypes.asset.MessageAsset
import com.wire.android.ui.home.conversations.model.messagetypes.image.DisplayableImageMessage
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageFailed
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageInProgress
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
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
    asset: ImageAsset.PrivateAsset?,
    imgParams: ImageMessageParams,
    uploadStatus: Message.UploadStatus,
    downloadStatus: Message.DownloadStatus,
    onImageClick: Clickable,
) {
    Box(
        Modifier.clip(shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)).background(
            color = MaterialTheme.wireColorScheme.onPrimary, shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
        ).border(
            width = 1.dp,
            color = MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline,
            shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
        ).wrapContentSize().combinedClickable(
            enabled = onImageClick.enabled,
            onClick = onImageClick.onClick,
            onLongClick = onImageClick.onLongClick,
        )
    ) {
        when {
            asset != null -> DisplayableImageMessage(asset, imgParams)

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
internal fun MessageQuote(
    quotedMessageUIData: QuotedMessageUIData
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
        modifier = Modifier.border(
            width = dimensions().messageQuoteBorderRadius,
            color = colorsScheme().divider,
            shape = RoundedCornerShape(dimensions().corner12x)
        ).padding(dimensions().spacing8x).fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_event_badge_unread_reply),
                tint = colorsScheme().secondaryText,
                contentDescription = null
            )
            Text(text = quotedMessageUIData.senderName, style = typography().label02, color = colorsScheme().secondaryText)
        }
        quotedMessageUIData.text?.let { text ->
            Text(text = text, style = typography().subline01)
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
    assetDownloadStatus: Message.DownloadStatus
) {
    MessageAsset(assetName, assetExtension, assetSizeInBytes, onAssetClick, assetUploadStatus, assetDownloadStatus)
}
