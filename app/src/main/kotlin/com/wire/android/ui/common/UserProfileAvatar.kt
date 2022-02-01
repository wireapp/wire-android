package com.wire.android.ui.common

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions

@Composable
fun UserProfileAvatar(
    avatarUrl: String = "",
    size: Dp = MaterialTheme.wireDimensions.userAvatarDefaultSize,
    modifier: Modifier = Modifier,
    isClickable: Boolean = true,
    onClick: () -> Unit = {}
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .wrapContentSize()
            .clip(CircleShape)
            .then(if (isClickable) Modifier.clickable { onClick() } else Modifier)
            .wrapContentSize()
            .padding(MaterialTheme.wireDimensions.userAvatarClickablePadding)
    ) {
        Image(
            painter = painterResource(getAvatarAsDrawable(avatarUrl)),
            contentDescription = stringResource(R.string.content_description_user_avatar),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .background(Color.Black, CircleShape)
                .size(size)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Box(
                modifier = Modifier
                    .size(MaterialTheme.wireDimensions.userAvatarBorderSize)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Box(
                modifier = Modifier
                    .size(MaterialTheme.wireDimensions.userAvatarStatusSize)
                    .clip(CircleShape)
                    .background(Color.Green) //TODO: Map UserStatus availability to the right icon shape/color
            )
        }
    }
}

fun getAvatarAsDrawable(avatarUrl: String): Int {
    // TODO: Add the picture loading mechanism with the avatarUrl
    return R.drawable.ic_launcher_foreground
}

@Preview
@Composable
fun UserProfileAvatarPreview() {
    UserProfileAvatar(avatarUrl = "") {}
}
