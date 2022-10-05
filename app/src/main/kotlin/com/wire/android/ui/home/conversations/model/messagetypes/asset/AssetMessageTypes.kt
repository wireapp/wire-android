package com.wire.android.ui.home.conversations.model.messagetypes.asset

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.WireCircularProgressIndicator
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.message.Message
import kotlin.math.roundToInt

@Composable
internal fun MessageAsset(
    assetName: String,
    assetExtension: String,
    assetSizeInBytes: Long,
    onAssetClick: Clickable,
    assetUploadStatus: Message.UploadStatus,
    assetDownloadStatus: Message.DownloadStatus
) {
    val assetDescription = provideAssetDescription(assetExtension, assetSizeInBytes)
    Box(
        modifier = Modifier
            .padding(top = dimensions().spacing4x)
            .background(
                color = MaterialTheme.wireColorScheme.onPrimary,
                shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline,
                shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
            )
            .clickable(if (canNotClick(assetDownloadStatus, assetUploadStatus)) null else onAssetClick)
            .padding(dimensions().spacing8x)
    ) {
        Column {
            Text(
                text = assetName,
                style = MaterialTheme.wireTypography.body02,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            ConstraintLayout(
                Modifier
                    .fillMaxWidth()
                    .padding(top = dimensions().spacing8x)
            ) {
                val (icon, description, downloadStatus) = createRefs()
                Image(
                    modifier = Modifier
                        .constrainAs(icon) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            bottom.linkTo(parent.bottom)
                        },
                    painter = painterResource(R.drawable.ic_file),
                    contentDescription = stringResource(R.string.content_description_image_message),
                    colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.badge)
                )
                Text(
                    modifier = Modifier
                        .padding(start = dimensions().spacing4x)
                        .constrainAs(description) {
                            top.linkTo(parent.top)
                            start.linkTo(icon.end)
                            bottom.linkTo(parent.bottom)
                        },
                    text = assetDescription,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    style = MaterialTheme.wireTypography.subline01
                )
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .constrainAs(downloadStatus) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        },
                ) {
                    Text(
                        modifier = Modifier.padding(end = dimensions().spacing4x),
                        text = getDownloadStatusText(assetDownloadStatus, assetUploadStatus),
                        color = MaterialTheme.wireColorScheme.run {
                            if (assetDownloadStatus == Message.DownloadStatus.FAILED_DOWNLOAD ||
                                assetUploadStatus == Message.UploadStatus.FAILED_UPLOAD
                            ) error else secondaryText
                        },
                        style = MaterialTheme.wireTypography.subline01
                    )
                    DownloadStatusIcon(assetDownloadStatus, assetUploadStatus)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictedAssetMessage(assetTypeIcon: Int, restrictedAssetMessage: String) {
    Card(
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
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.secondaryText)
            )

            Text(
                text = restrictedAssetMessage,
                style = MaterialTheme.wireTypography.body01.copy(color = MaterialTheme.wireColorScheme.secondaryText),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictedGenericFileMessage(fileName: String, fileSize: Long) {
    Card(
        shape = RoundedCornerShape(dimensions().messageAssetBorderRadius),
        border = BorderStroke(dimensions().spacing1x, MaterialTheme.wireColorScheme.divider)
    ) {
        val assetName = fileName.split(".").dropLast(1).joinToString(".")
        val assetDescription = provideAssetDescription(
            fileName.split(".").last(), fileSize
        )

        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions().spacing8x),
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
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                    },
                painter = painterResource(
                    id = R.drawable.ic_file
                ),
                alignment = Alignment.Center,
                contentDescription = stringResource(R.string.content_description_image_message),
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.secondaryText),
            )

            Text(
                text = assetDescription,
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier
                    .padding(start = dimensions().spacing4x)
                    .constrainAs(size) {
                        start.linkTo(icon.end)
                        top.linkTo(name.bottom)
                    }
            )

            Text(
                text = stringResource(id = R.string.prohibited_file_message),
                style = MaterialTheme.wireTypography.body01.copy(color = MaterialTheme.wireColorScheme.secondaryText),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.constrainAs(message) {
                    end.linkTo(parent.end)
                    top.linkTo(name.bottom)
                }
            )
        }
    }
}

@Composable
private fun DownloadStatusIcon(assetDownloadStatus: Message.DownloadStatus, assetUploadStatus: Message.UploadStatus) {
    return when {
        assetUploadStatus == Message.UploadStatus.UPLOAD_IN_PROGRESS ||
                assetDownloadStatus == Message.DownloadStatus.DOWNLOAD_IN_PROGRESS -> WireCircularProgressIndicator(
            progressColor = MaterialTheme.wireColorScheme.secondaryText,
            size = dimensions().spacing16x
        )
        assetUploadStatus == Message.UploadStatus.FAILED_UPLOAD -> {}
        assetDownloadStatus == Message.DownloadStatus.SAVED_INTERNALLY -> Icon(
            painter = painterResource(id = R.drawable.ic_download),
            contentDescription = stringResource(R.string.content_description_download_icon),
            modifier = Modifier.size(dimensions().wireIconButtonSize),
            tint = MaterialTheme.wireColorScheme.secondaryText
        )
        assetDownloadStatus == Message.DownloadStatus.SAVED_EXTERNALLY -> Icon(
            painter = painterResource(id = R.drawable.ic_check_tick),
            contentDescription = stringResource(R.string.content_description_check),
            modifier = Modifier.size(dimensions().wireIconButtonSize),
            tint = MaterialTheme.wireColorScheme.secondaryText
        )
        else -> {}
    }
}

@Composable
fun getDownloadStatusText(assetDownloadStatus: Message.DownloadStatus, assetUploadStatus: Message.UploadStatus): String =
    when {
        assetUploadStatus == Message.UploadStatus.UPLOAD_IN_PROGRESS -> stringResource(R.string.asset_message_upload_in_progress_text)
        assetUploadStatus == Message.UploadStatus.FAILED_UPLOAD -> stringResource(R.string.asset_message_failed_upload_text)
        assetDownloadStatus == Message.DownloadStatus.NOT_DOWNLOADED -> stringResource(R.string.asset_message_tap_to_download_text)
        assetDownloadStatus == Message.DownloadStatus.SAVED_INTERNALLY -> stringResource(R.string.asset_message_downloaded_internally_text)
        assetDownloadStatus == Message.DownloadStatus.DOWNLOAD_IN_PROGRESS ->
            stringResource(R.string.asset_message_download_in_progress_text)
        assetDownloadStatus == Message.DownloadStatus.SAVED_EXTERNALLY ||
                assetUploadStatus == Message.UploadStatus.UPLOADED -> stringResource(R.string.asset_message_saved_externally_text)
        assetDownloadStatus == Message.DownloadStatus.FAILED_DOWNLOAD -> stringResource(R.string.asset_message_failed_download_text)
        else -> ""
    }

@Composable
private fun canNotClick(assetDownloadStatus: Message.DownloadStatus, assetUploadStatus: Message.UploadStatus) =
    assetDownloadStatus == Message.DownloadStatus.DOWNLOAD_IN_PROGRESS || assetUploadStatus == Message.UploadStatus.UPLOAD_IN_PROGRESS

@Suppress("MagicNumber")
private fun provideAssetDescription(assetExtension: String, assetSizeInBytes: Long): String {
    val oneKB = 1024L
    val oneMB = oneKB * oneKB
    return when {
        assetSizeInBytes < oneKB -> "${assetExtension.uppercase()} ($assetSizeInBytes B)"
        assetSizeInBytes in oneKB..oneMB -> "${assetExtension.uppercase()} (${assetSizeInBytes / oneKB} KB)"
        else -> "${assetExtension.uppercase()} (${((assetSizeInBytes / oneMB) * 100.0).roundToInt() / 100.0} MB)" // 2 decimals round off
    }
}
