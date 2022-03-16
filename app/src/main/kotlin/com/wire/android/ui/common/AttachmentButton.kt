package com.wire.android.ui.common

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        modifier = Modifier
            .width(dimensions().attachmentButtonSize)
            .wrapContentWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = modifier
                .size(dimensions().attachmentButtonSize)
                .background(MaterialTheme.wireColorScheme.primaryButtonEnabled, CircleShape)
                .padding(dimensions().userAvatarStatusBorderSize)
                .clickable { onClick() }
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = stringResource(R.string.content_description_user_avatar),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .padding(dimensions().userAvatarClickablePadding)
                    .align(Alignment.Center),
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.secondaryButtonEnabled)
            )
        }
        Text(
            text = text,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.wireTypography.title03,
            color = MaterialTheme.wireColorScheme.onBackground,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAttachmentButton() {
    AttachmentButton("Attach File", R.drawable.ic_attach_file) {
        Log.d("AttachmentButton", ">> Clicked")
    }
}
