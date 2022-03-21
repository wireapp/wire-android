package com.wire.android.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun AttachmentButton(
    text: String = "",
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(CircleShape)
            .padding(dimensions().spacing16x)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(dimensions().attachmentButtonSize)
                .background(MaterialTheme.wireColorScheme.primaryButtonEnabled, CircleShape)
                .padding(dimensions().spacing2x)
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = stringResource(R.string.content_description_attachment_item),
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .padding(dimensions().spacing8x)
                    .align(Alignment.Center),
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.secondaryButtonEnabled)
            )
        }
        Text(
            text = text,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.wireTypography.button03,
            color = MaterialTheme.wireColorScheme.onBackground,
            modifier = Modifier.requiredSizeIn(minWidth = dimensions().spacing40x, maxWidth = dimensions().spacing64x)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAttachmentButton() {
    AttachmentButton("Share Location", R.drawable.ic_location) { }
}
