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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.UserAvatarAsset
import com.wire.android.model.UserStatus
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.getUriFromDrawable

@Composable
fun UserProfileAvatar(
    userAvatarAsset: UserAvatarAsset? = null,
    status: UserStatus = UserStatus.NONE,
    isClickable: Boolean = false,
    size: Dp = MaterialTheme.wireDimensions.userAvatarDefaultSize,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val imageLoader = hiltViewModel<CurrentSessionViewModel>().wireSessionImageLoader

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .wrapContentSize()
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(isClickable) { onClick() } else Modifier)
            .wrapContentSize()
            .padding(MaterialTheme.wireDimensions.userAvatarClickablePadding)
    ) {
        Image(
            painter = imageLoader.paint(
                userAvatarAsset, getUriFromDrawable(
                    LocalContext.current,
                    R.drawable.ic_default_user_avatar
                )
            ),
            contentDescription = stringResource(R.string.content_description_user_avatar),
            modifier = Modifier
                .padding(dimensions().userAvatarStatusBorderSize)
                .background(MaterialTheme.wireColorScheme.divider, CircleShape)
                .size(size)
                .clip(CircleShape).testTag("User avatar"),
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
    UserProfileAvatar(
        status = UserStatus.BUSY
    ) {}
}
