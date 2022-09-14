package com.wire.android.ui.home.conversations.model

import android.graphics.Bitmap
import android.text.util.Linkify
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.rememberAsyncImagePainter
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.LinkifyText
import com.wire.android.ui.common.WireCircularProgressIndicator
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.MessageComposerViewModel
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.getUriFromDrawable
import com.wire.android.util.toBitmap
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.Message.DownloadStatus.DOWNLOAD_IN_PROGRESS
import com.wire.kalium.logic.data.message.Message.DownloadStatus.FAILED_DOWNLOAD
import com.wire.kalium.logic.data.message.Message.DownloadStatus.NOT_DOWNLOADED
import com.wire.kalium.logic.data.message.Message.DownloadStatus.SAVED_EXTERNALLY
import com.wire.kalium.logic.data.message.Message.DownloadStatus.SAVED_INTERNALLY
import com.wire.kalium.logic.data.message.Message.UploadStatus.FAILED_UPLOAD
import com.wire.kalium.logic.data.message.Message.UploadStatus.UPLOADED
import com.wire.kalium.logic.data.message.Message.UploadStatus.UPLOAD_IN_PROGRESS
import kotlin.math.roundToInt

// TODO: Here we actually need to implement some logic that will distinguish MentionLabel with Body of the message,
// waiting for the backend to implement mapping logic for the MessageBody
@Composable
internal fun MessageBody(messageBody: MessageBody, onLongClick: (() -> Unit)? = null) {
    LinkifyText(
        text = messageBody.message.asString(),
        mask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES,
        color = MaterialTheme.colorScheme.onBackground,
        onLongClick = onLongClick,
        modifier = Modifier.defaultMinSize(minHeight = dimensions().spacing20x),
        style = MaterialTheme.wireTypography.body01
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageImage(
    rawImgData: ByteArray?,
    imgParams: ImageMessageParams,
    assetUploadStatus: Message.UploadStatus,
    onImageClick: Clickable,
) {
    val imageData: Bitmap? = if (rawImgData != null && rawImgData.size < MessageComposerViewModel.IMAGE_SIZE_LIMIT_BYTES)
        rawImgData.toBitmap() else null
    Box(
        Modifier
            .clip(shape = RoundedCornerShape(dimensions().messageAssetBorderRadius))
            .background(
                color = MaterialTheme.wireColorScheme.onPrimary,
                shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
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
        when (assetUploadStatus) {
            // Default states, we try to draw the image
            Message.UploadStatus.NOT_UPLOADED -> {}
            Message.UploadStatus.UPLOADED -> {
                Image(
                    painter = rememberAsyncImagePainter(imageData ?: getUriFromDrawable(LocalContext.current, R.drawable.ic_gallery)),
                    contentDescription = stringResource(R.string.content_description_image_message),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(if (imageData != null) imgParams.normalizedWidth else dimensions().spacing24x)
                        .height(if (imageData != null) imgParams.normalizedHeight else dimensions().spacing24x),
                    alignment = Alignment.CenterStart,
                    contentScale = ContentScale.Crop
                )
            }
            // Trying to upload the asset
            Message.UploadStatus.UPLOAD_IN_PROGRESS -> {
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
                        text = stringResource(id = R.string.asset_message_upload_in_progress_text),
                        style = MaterialTheme.wireTypography.body01.copy(color = MaterialTheme.wireColorScheme.secondaryText),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }
            Message.UploadStatus.FAILED_UPLOAD -> {
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
                        text = stringResource(id = R.string.error_uploading_image_message),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.wireTypography.body01.copy(color = MaterialTheme.wireColorScheme.error)
                    )
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
fun RestrictedFileMessage(fileName: String, fileSize: Long) {
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
                name, icon, size, message) = createRefs()
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

            Text(text = assetDescription,
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier
                    .padding(start = dimensions().spacing4x)
                    .constrainAs(size) {
                        start.linkTo(icon.end)
                        top.linkTo(name.bottom)
                    })

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
            .clickable(onAssetClick)
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
                            if (assetDownloadStatus == FAILED_DOWNLOAD || assetUploadStatus == FAILED_UPLOAD) error else secondaryText
                        },
                        style = MaterialTheme.wireTypography.subline01
                    )
                    DownloadStatusIcon(assetDownloadStatus, assetUploadStatus)
                }
            }
        }
    }
}

@Composable
private fun DownloadStatusIcon(assetDownloadStatus: Message.DownloadStatus, assetUploadStatus: Message.UploadStatus) {
    return when {
        assetUploadStatus == UPLOAD_IN_PROGRESS || assetDownloadStatus == DOWNLOAD_IN_PROGRESS -> WireCircularProgressIndicator(
            progressColor = MaterialTheme.wireColorScheme.secondaryText,
            size = dimensions().spacing16x
        )
        assetUploadStatus == FAILED_UPLOAD -> {}
        assetDownloadStatus == SAVED_INTERNALLY -> Icon(
            painter = painterResource(id = R.drawable.ic_download),
            contentDescription = stringResource(R.string.content_description_download_icon),
            modifier = Modifier.size(dimensions().wireIconButtonSize),
            tint = MaterialTheme.wireColorScheme.secondaryText
        )
        assetDownloadStatus == SAVED_EXTERNALLY -> Icon(
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
        assetUploadStatus == UPLOAD_IN_PROGRESS -> stringResource(R.string.asset_message_upload_in_progress_text)
        assetUploadStatus == FAILED_UPLOAD -> stringResource(R.string.asset_message_failed_upload_text)
        assetDownloadStatus == NOT_DOWNLOADED -> stringResource(R.string.asset_message_tap_to_download_text)
        assetDownloadStatus == SAVED_INTERNALLY -> stringResource(R.string.asset_message_downloaded_internally_text)
        assetDownloadStatus == DOWNLOAD_IN_PROGRESS -> stringResource(R.string.asset_message_download_in_progress_text)
        assetDownloadStatus == SAVED_EXTERNALLY
                || assetUploadStatus == UPLOADED -> stringResource(R.string.asset_message_saved_externally_text)
        assetDownloadStatus == FAILED_DOWNLOAD -> stringResource(R.string.asset_message_failed_download_text)
        else -> ""
    }

private fun provideAssetDescription(assetExtension: String, assetSizeInBytes: Long): String {
    val oneKB = 1024L
    val oneMB = oneKB * oneKB
    return when {
        assetSizeInBytes < oneKB -> "${assetExtension.uppercase()} ($assetSizeInBytes B)"
        assetSizeInBytes in oneKB..oneMB -> "${assetExtension.uppercase()} (${assetSizeInBytes / oneKB} KB)"
        else -> "${assetExtension.uppercase()} (${((assetSizeInBytes / oneMB) * 100.0).roundToInt() / 100.0} MB)" // 2 decimals round off
    }
}

// TODO:we should provide the SpanStyle by LocalProvider to our Theme, later on
@Composable
private fun AnnotatedString.Builder.appendMentionLabel(label: String) {
    withStyle(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            background = MaterialTheme.colorScheme.primaryContainer,
        )
    ) {
        append("$label ")
    }
}

@Composable
private fun AnnotatedString.Builder.appendBody(messageBody: MessageBody) {
    append(messageBody.message.asString())
}

data class ImageMessageParams(private val realImgWidth: Int, private val realImgHeight: Int) {
    // Image size normalizations to keep the ratio of the inline message image
    val normalizedWidth: Dp
        @Composable
        get() = dimensions().messageImageMaxWidth

    val normalizedHeight: Dp
        @Composable
        get() = Dp(normalizedWidth.value * realImgHeight.toFloat() / realImgWidth)
}
