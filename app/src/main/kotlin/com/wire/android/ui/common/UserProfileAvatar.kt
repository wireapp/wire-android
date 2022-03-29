package com.wire.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.getDefaultAvatarUri
import com.wire.android.util.toBitmap


@Composable

fun UserProfileAvatar(
//TODO: in the future we would not pass here null, but while developing there may be users having no avatar asset
    avatarAssetByteArray: ByteArray? = null,
    status: UserStatus = UserStatus.NONE,
    isClickable: Boolean = false,
    size: Dp = MaterialTheme.wireDimensions.userAvatarDefaultSize,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .wrapContentSize()
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(isClickable) { onClick() } else Modifier)
            .wrapContentSize()
            .padding(MaterialTheme.wireDimensions.userAvatarClickablePadding)
    ) {
        AsyncImage(
            model = avatarAssetByteArray?.toBitmap() ?: getDefaultAvatarUri(LocalContext.current),
            contentDescription = stringResource(R.string.content_description_user_avatar),
            modifier = Modifier
                .padding(dimensions().userAvatarStatusBorderSize)
                .background(Color.Black, CircleShape)
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        UserStatusIndicator(
            status = status,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Preview
@Composable
fun UserProfileAvatarPreview() {
    UserProfileAvatar(status = UserStatus.BUSY) {}
}
