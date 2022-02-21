package com.wire.android.ui.common

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.theme.wireDimensions


@Composable
fun UserProfileAvatar(
    avatarUrl: String = "",
    status: UserStatus = UserStatus.NONE,
    size: Dp = MaterialTheme.wireDimensions.userAvatarDefaultSize,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .wrapContentSize()
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .wrapContentSize()
            .padding(MaterialTheme.wireDimensions.userAvatarClickablePadding)
    ) {
        Image(
            painter = painterResource(getAvatarAsDrawable(avatarUrl)),
            contentDescription = stringResource(R.string.content_description_user_avatar),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(dimensions().userAvatarStatusBorderSize)
                .background(Color.Black, CircleShape)
                .size(size)
        )
        UserStatusIndicator(
            status = status,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun UserProfileAvatar(
    avatarBitmap: Bitmap,
    status: UserStatus = UserStatus.NONE,
    isEnabled: Boolean = false,
    size: Dp = MaterialTheme.wireDimensions.userAvatarDefaultSize,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .wrapContentSize()
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(isEnabled) { onClick() } else Modifier)
            .wrapContentSize()
            .padding(MaterialTheme.wireDimensions.userAvatarClickablePadding)
    ) {
        Image(
            bitmap = avatarBitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.content_description_user_avatar),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(dimensions().userAvatarStatusBorderSize)
                .clip(CircleShape)
                .size(size)
        )
        UserStatusIndicator(
            status = status,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

fun getAvatarAsDrawable(avatarUrl: String): Int {
    // TODO: Add the picture loading mechanism with the avatarUrl
    return R.drawable.ic_launcher_foreground
}

@Preview
@Composable
fun UserProfileAvatarPreview() {
    UserProfileAvatar(avatarUrl = "", status = UserStatus.BUSY) {}
}
