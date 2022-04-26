package com.wire.android.ui.home.conversations.model

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.wire.android.ui.home.conversations.MessageItem
import com.wire.android.ui.home.conversations.mock.mockAssetMessage
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.getUriFromDrawable
import com.wire.android.util.toBitmap
import kotlin.math.roundToInt

// TODO: Here we actually need to implement some logic that will distinguish MentionLabel with Body of the message,
// waiting for the backend to implement mapping logic for the MessageBody
@Composable
internal fun MessageBody(messageBody: MessageBody) {
    Text(
        buildAnnotatedString {
            appendBody(messageBody = messageBody)
        }
    )
}

@Composable
fun MessageImage(rawImgData: ByteArray?, imgParams: ImageMessageParams) {
    Box(Modifier
        .clip(shape = RoundedCornerShape(MaterialTheme.wireDimensions.messageAssetBorderRadius))
        .clickable {
            // TODO: Add navigation to preview full screen mode here
        }) {
        Image(
            painter = rememberAsyncImagePainter(
                rawImgData?.toBitmap() ?: getUriFromDrawable(
                    LocalContext.current,
                    R.drawable.ic_gallery
                )
            ),
            alignment = Alignment.CenterStart,
            contentDescription = stringResource(R.string.content_description_image_message),
            modifier = Modifier.width(imgParams.normalizedWidth).height(imgParams.normalizedHeight),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
internal fun MessageAsset(assetName: String, assetExtension: String, assetSizeInBytes: Long, onAssetClick: () -> Unit) {
    val assetDescription = provideAssetDescription(assetExtension, assetSizeInBytes)
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.wireColorScheme.onPrimary,
                shape = RoundedCornerShape(MaterialTheme.wireDimensions.messageAssetBorderRadius)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline,
                shape = RoundedCornerShape(MaterialTheme.wireDimensions.messageAssetBorderRadius)
            )
            .clickable { onAssetClick() }
            .padding(MaterialTheme.wireDimensions.spacing8x)
    ) {
        Column {
            Text(
                text = assetName,
                style = MaterialTheme.wireTypography.body02,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            ConstraintLayout(Modifier.fillMaxWidth().padding(top = MaterialTheme.wireDimensions.spacing8x)) {
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
                        .padding(start = MaterialTheme.wireDimensions.spacing4x)
                        .constrainAs(description) {
                            top.linkTo(parent.top)
                            start.linkTo(icon.end)
                            bottom.linkTo(parent.bottom)
                        },
                    text = assetDescription,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    style = MaterialTheme.wireTypography.subline01
                )
                Text(
                    modifier = Modifier
                        .padding(start = MaterialTheme.wireDimensions.spacing8x)
                        .constrainAs(downloadStatus) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        },
                    text = stringResource(R.string.asset_message_download_text),
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    style = MaterialTheme.wireTypography.subline01
                )
            }
        }
    }
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
    append(messageBody.message)
}

class ImageMessageParams(private val realImgWidth: Int, private val realImgHeight: Int) {
    // Image size normalizations to keep the ratio of the inline message image
    val normalizedWidth: Dp
        @Composable
        get() = MaterialTheme.wireDimensions.messageImageMaxWidth

    val normalizedHeight: Dp
        @Composable
        get() = Dp(normalizedWidth.value * realImgHeight.toFloat() / realImgWidth)
}

@Preview(showBackground = true)
@Composable
fun PreviewMessage() {
    MessageItem(mockMessageWithText, {}, {})
}

@Preview(showBackground = true)
@Composable
fun PreviewAssetMessage() {
    MessageItem(mockAssetMessage, {}, {})
}
