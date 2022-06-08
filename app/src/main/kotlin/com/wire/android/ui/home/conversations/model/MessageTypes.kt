package com.wire.android.ui.home.conversations.model

import android.graphics.Bitmap
import android.text.util.Linkify
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.rememberAsyncImagePainter
import com.wire.android.R
import com.wire.android.ui.common.LinkifyText
import com.wire.android.ui.common.WireCircularProgressIndicator
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.ConversationViewModel
import com.wire.android.ui.home.conversations.MessageItem
import com.wire.android.ui.home.conversations.mock.mockAssetMessage
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.getUriFromDrawable
import com.wire.android.util.toBitmap
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.Message.DownloadStatus.FAILED
import com.wire.kalium.logic.data.message.Message.DownloadStatus.IN_PROGRESS
import com.wire.kalium.logic.data.message.Message.DownloadStatus.NOT_DOWNLOADED
import com.wire.kalium.logic.data.message.Message.DownloadStatus.SAVED_EXTERNALLY
import com.wire.kalium.logic.data.message.Message.DownloadStatus.SAVED_INTERNALLY
import kotlin.math.roundToInt

// TODO: Here we actually need to implement some logic that will distinguish MentionLabel with Body of the message,
// waiting for the backend to implement mapping logic for the MessageBody
@Composable
internal fun MessageBody(messageBody: MessageBody, editTime: String? = null) {
    Column {
        if (editTime != null)
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.wireColorScheme.divider,
                        shape = RoundedCornerShape(dimensions().corner4x)
                    )
            ) {
                Text(
                    text = stringResource(R.string.label_message_status_edited_with_date, editTime),
                    color = MaterialTheme.wireColorScheme.labelText,
                    style = MaterialTheme.wireTypography.label03,
                    modifier = Modifier
                        .padding(
                            horizontal = dimensions().spacing4x,
                            vertical = dimensions().spacing2x
                        )
                )
            }
    }
    LinkifyText(
        text = messageBody.message.asString(),
        mask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES
    )
}

@Composable
internal fun DeletedMessage() {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.wireColorScheme.divider,
                shape = RoundedCornerShape(dimensions().corner4x)
            )
    ) {
        Text(
            text = stringResource(R.string.deleted_message_text),
            color = MaterialTheme.wireColorScheme.labelText,
            style = MaterialTheme.wireTypography.label03,
            modifier = Modifier
                .padding(
                    horizontal = dimensions().spacing4x,
                    vertical = dimensions().spacing2x
                )
        )
    }

}

@Composable
fun MessageImage(rawImgData: ByteArray?, imgParams: ImageMessageParams, onImageClick: () -> Unit) {
    Box(
        Modifier
            .clip(shape = RoundedCornerShape(dimensions().messageAssetBorderRadius))
            .clickable { onImageClick() }
    ) {
        val imageData: Bitmap? =
            if (rawImgData != null && rawImgData.size < ConversationViewModel.IMAGE_SIZE_LIMIT_BYTES) rawImgData.toBitmap() else null
        Image(
            painter = rememberAsyncImagePainter(imageData ?: getUriFromDrawable(LocalContext.current, R.drawable.ic_gallery)),
            alignment = Alignment.CenterStart,
            contentDescription = stringResource(R.string.content_description_image_message),
            modifier = Modifier
                .width(if (imageData != null) imgParams.normalizedWidth else dimensions().spacing24x)
                .height(if (imageData != null) imgParams.normalizedHeight else dimensions().spacing24x),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
internal fun MessageAsset(
    assetName: String,
    assetExtension: String,
    assetSizeInBytes: Long,
    onAssetClick: () -> Unit,
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
            .clickable { onAssetClick() }
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
                        text = getDownloadStatusText(assetDownloadStatus),
                        color = MaterialTheme.wireColorScheme.run {
                            if (assetDownloadStatus == FAILED) error else secondaryText
                        },
                        style = MaterialTheme.wireTypography.subline01
                    )
                    DownloadStatusIcon(assetDownloadStatus)
                }
            }
        }
    }
}

@Composable
private fun DownloadStatusIcon(assetDownloadStatus: Message.DownloadStatus) {
    return when (assetDownloadStatus) {
        IN_PROGRESS -> WireCircularProgressIndicator(
            progressColor = MaterialTheme.wireColorScheme.secondaryText,
            size = dimensions().spacing16x
        )
        SAVED_INTERNALLY -> Icon(
            painter = painterResource(id = R.drawable.ic_download),
            contentDescription = stringResource(R.string.content_description_download_icon),
            modifier = Modifier.size(dimensions().wireIconButtonSize),
            tint = MaterialTheme.wireColorScheme.secondaryText
        )
        SAVED_EXTERNALLY -> Icon(
            painter = painterResource(id = R.drawable.ic_check_tick),
            contentDescription = stringResource(R.string.content_description_check),
            modifier = Modifier.size(dimensions().wireIconButtonSize),
            tint = MaterialTheme.wireColorScheme.secondaryText
        )
        else -> {}
    }
}

@Composable
fun getDownloadStatusText(assetDownloadStatus: Message.DownloadStatus): String =
    when (assetDownloadStatus) {
        NOT_DOWNLOADED -> stringResource(R.string.asset_message_tap_to_download_text)
        SAVED_INTERNALLY -> stringResource(R.string.asset_message_downloaded_internally_text)
        IN_PROGRESS -> stringResource(R.string.asset_message_download_in_progress_text)
        SAVED_EXTERNALLY -> stringResource(R.string.asset_message_saved_externally_text)
        FAILED -> stringResource(R.string.asset_message_failed_download_text)
    }


@Suppress("MagicNumber")
private fun provideAssetDescription(assetExtension: String, assetSizeInBytes: Long): String {
    return when {
        assetSizeInBytes < 1000 -> "${assetExtension.uppercase()} ($assetSizeInBytes B)"
        assetSizeInBytes in 1000..999999 -> "${assetExtension.uppercase()} (${assetSizeInBytes / 1000} KB)"
        else -> "${assetExtension.uppercase()} (${((assetSizeInBytes / 1000000f) * 100.0).roundToInt() / 100.0} MB)" // 2 decimals round off
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

class ImageMessageParams(private val realImgWidth: Int, private val realImgHeight: Int) {
    // Image size normalizations to keep the ratio of the inline message image
    val normalizedWidth: Dp
        @Composable
        get() = dimensions().messageImageMaxWidth

    val normalizedHeight: Dp
        @Composable
        get() = Dp(normalizedWidth.value * realImgHeight.toFloat() / realImgWidth)
}

@Preview(showBackground = true)
@Composable
fun PreviewMessage() {
    MessageItem(mockMessageWithText, {}, {}, { _, _ -> })
}

@Preview(showBackground = true)
@Composable
fun PreviewDeletedMessage() {
    DeletedMessage()
}

@Preview(showBackground = true)
@Composable
fun PreviewAssetMessage() {
    MessageItem(mockAssetMessage, {}, {}, { _, _ -> })
}
