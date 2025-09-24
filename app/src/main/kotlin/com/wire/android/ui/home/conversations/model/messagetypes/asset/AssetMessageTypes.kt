/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.home.conversations.model.messagetypes.asset

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.model.Clickable
import com.wire.android.ui.common.applyIf
import com.wire.android.ui.common.attachmentdraft.ui.FileHeaderView
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.messages.item.isBubble
import com.wire.android.ui.home.conversations.messages.item.onBackground
import com.wire.android.ui.home.conversations.messages.item.textColor
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.DeviceUtil
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.asset.isFailed

@Composable
internal fun MessageAsset(
    assetName: String,
    assetExtension: String,
    assetSizeInBytes: Long,
    assetDataPath: String?,
    onAssetClick: Clickable,
    assetTransferStatus: AssetTransferStatus,
    messageStyle: MessageStyle
) {
    Box(
        modifier = Modifier
            .applyIf(!messageStyle.isBubble()) {
                padding(top = dimensions().spacing4x)
                    .background(
                        color = MaterialTheme.wireColorScheme.surfaceVariant,
                        shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline,
                        shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
                    )
            }
            .clickable(if (isNotClickable(assetTransferStatus)) null else onAssetClick)
    ) {
        if (assetTransferStatus == AssetTransferStatus.UPLOAD_IN_PROGRESS) {
            UploadInProgressAssetMessage(messageStyle)
        } else {
            val assetModifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
            Column(
                modifier = assetModifier.padding(
                    horizontal = if (messageStyle.isBubble()) {
                        dimensions().spacing0x
                    } else {
                        dimensions().spacing8x
                    },
                    vertical = dimensions().spacing8x
                ),
                verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
            ) {
                FileHeaderView(
                    extension = assetExtension,
                    size = assetSizeInBytes,
                    label = getDownloadStatusText(assetTransferStatus),
                    labelColor = if (assetTransferStatus.isFailed()) colorsScheme().error else null,
                    messageStyle = messageStyle
                )
                Text(
                    text = assetName,
                    style = MaterialTheme.wireTypography.body02,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = messageStyle.onBackground()
                )

                assetDataPath?.let { localPath ->
                    if (AttachmentFileType.fromExtension(assetExtension) == AttachmentFileType.PDF) {
                        Spacer(modifier = Modifier.height(dimensions().spacing12x))
                        PdfAssetPreview(localPath)
                    }
                }
            }
        }
    }
}

@Composable
fun UploadInProgressAssetMessage(messageStyle: MessageStyle, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MaterialTheme.wireDimensions.spacing72x),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WireCircularProgressIndicator(
            progressColor = messageStyle.textColor(),
            size = dimensions().spacing16x
        )
        Spacer(modifier = Modifier.size(MaterialTheme.wireDimensions.spacing8x))
        Text(
            modifier = Modifier.padding(end = dimensions().spacing4x),
            text = stringResource(R.string.asset_message_upload_in_progress_text),
            color = messageStyle.textColor(),
            style = MaterialTheme.wireTypography.subline01
        )
    }
}

@Composable
fun RestrictedAssetMessage(assetTypeIcon: Int, restrictedAssetMessage: String, messageStyle: MessageStyle, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(dimensions().messageAssetBorderRadius),
        border = BorderStroke(dimensions().spacing1x, MaterialTheme.wireColorScheme.divider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions().messageImageMaxWidth),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                modifier = Modifier
                    .padding(bottom = dimensions().spacing4x)
                    .size(height = dimensions().spacing24x, width = dimensions().spacing24x),
                painter = painterResource(
                    id = assetTypeIcon
                ),
                alignment = Alignment.Center,
                contentDescription = stringResource(R.string.content_description_image_message),
                colorFilter = ColorFilter.tint(messageStyle.textColor())
            )

            Text(
                text = restrictedAssetMessage,
                style = MaterialTheme.wireTypography.body01.copy(color = messageStyle.textColor()),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}

@Composable
fun RestrictedGenericFileMessage(fileName: String, fileSize: Long, messageStyle: MessageStyle, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(dimensions().messageAssetBorderRadius),
        border = BorderStroke(dimensions().spacing1x, MaterialTheme.wireColorScheme.divider)
    ) {
        val assetName = fileName.split(".").dropLast(1).joinToString(".")
        val assetDescription = provideAssetDescription(
            fileName.split(".").last(),
            fileSize
        )

        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions().spacing8x)
        ) {
            val (
                name, icon, size, message
            ) = createRefs()

            Text(
                text = assetName,
                style = MaterialTheme.wireTypography.body02,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier
                    .padding(bottom = dimensions().spacing4x)
                    .constrainAs(name) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }
            )

            Image(
                modifier = Modifier
                    .height(dimensions().spacing12x)
                    .width(dimensions().spacing12x)
                    .constrainAs(icon) {
                        top.linkTo(name.bottom)
                        start.linkTo(parent.start)
                    },
                painter = painterResource(
                    id = R.drawable.ic_file
                ),
                alignment = Alignment.Center,
                contentDescription = stringResource(R.string.content_description_image_message),
                colorFilter = ColorFilter.tint(messageStyle.textColor())
            )

            Text(
                text = assetDescription,
                style = MaterialTheme.wireTypography.body01.copy(color = messageStyle.textColor()),
                modifier = Modifier
                    .padding(start = dimensions().spacing4x)
                    .constrainAs(size) {
                        start.linkTo(icon.end)
                        top.linkTo(icon.top)
                        bottom.linkTo(icon.bottom)
                    }
            )

            Text(
                text = stringResource(id = R.string.prohibited_file_message),
                style = MaterialTheme.wireTypography.body01.copy(color = messageStyle.textColor()),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier
                    .padding(top = dimensions().spacing4x)
                    .constrainAs(message) {
                        start.linkTo(parent.start)
                        top.linkTo(icon.bottom)
                    }
            )
        }
    }
}

@Composable
fun getDownloadStatusText(assetTransferStatus: AssetTransferStatus): String = when (assetTransferStatus) {
    AssetTransferStatus.NOT_DOWNLOADED -> stringResource(R.string.asset_message_tap_to_download_text)
    AssetTransferStatus.UPLOAD_IN_PROGRESS -> stringResource(R.string.asset_message_upload_in_progress_text)
    AssetTransferStatus.DOWNLOAD_IN_PROGRESS -> stringResource(R.string.asset_message_download_in_progress_text)
    AssetTransferStatus.UPLOADED -> stringResource(R.string.asset_message_saved_externally_text)
    AssetTransferStatus.SAVED_INTERNALLY -> stringResource(R.string.asset_message_downloaded_internally_text)
    AssetTransferStatus.SAVED_EXTERNALLY -> stringResource(R.string.asset_message_saved_externally_text)
    AssetTransferStatus.FAILED_UPLOAD -> stringResource(R.string.asset_message_failed_upload_text)
    AssetTransferStatus.FAILED_DOWNLOAD -> stringResource(R.string.asset_message_failed_download_text)
    AssetTransferStatus.NOT_FOUND -> stringResource(R.string.asset_message_failed_download_text)
}

@Composable
private fun isNotClickable(assetTransferStatus: AssetTransferStatus) =
    assetTransferStatus == AssetTransferStatus.DOWNLOAD_IN_PROGRESS || assetTransferStatus == AssetTransferStatus.UPLOAD_IN_PROGRESS

@Suppress("MagicNumber")
@Stable
private fun provideAssetDescription(assetExtension: String, assetSizeInBytes: Long): String {
    return "${assetExtension.uppercase()} (${DeviceUtil.formatSize(assetSizeInBytes)})"
}

@PreviewMultipleThemes
@Composable
private fun PreviewMessageAsset() {
    WireTheme {
        MessageAsset(
            assetName = "Wire Logo",
            assetExtension = "png",
            assetSizeInBytes = 1000000,
            assetDataPath = null,
            onAssetClick = Clickable {},
            assetTransferStatus = AssetTransferStatus.NOT_DOWNLOADED,
            messageStyle = MessageStyle.NORMAL
        )
    }
}
