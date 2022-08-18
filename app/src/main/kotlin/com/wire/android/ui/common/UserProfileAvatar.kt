package com.wire.android.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.getUriFromDrawable
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

@Composable
fun UserProfileAvatar(
    avatarData: UserAvatarData = UserAvatarData(),
    size: Dp = MaterialTheme.wireDimensions.userAvatarDefaultSize,
    modifier: Modifier = Modifier,
    clickable: Clickable? = null
) {
    val painter = painter(avatarData)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .wrapContentSize()
            .clip(CircleShape)
            .clickable(clickable)
            .padding(MaterialTheme.wireDimensions.userAvatarClickablePadding)
    ) {
        Image(
            painter = painter,
            contentDescription = stringResource(R.string.content_description_user_avatar),
            modifier = Modifier
                .padding(dimensions().userAvatarStatusBorderSize)
                .background(MaterialTheme.wireColorScheme.divider, CircleShape)
                .size(size)
                .clip(CircleShape)
                .testTag("User avatar"),
            contentScale = ContentScale.Crop
        )
        UserStatusIndicator(
            status = avatarData.availabilityStatus,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

/**
 * Workaround to have profile avatar available for preview
 * @see [painter] https://developer.android.com/jetpack/compose/tooling
 */
@Composable
private fun painter(data: UserAvatarData): Painter =
    if (data.connectionState == ConnectionState.BLOCKED) {
        painterResource(id = R.drawable.ic_blocked_user_avatar)
    } else if (LocalInspectionMode.current || data.asset == null) {
        painterResource(id = R.drawable.ic_default_user_avatar)
    } else {
        data.asset.paint(getUriFromDrawable(LocalContext.current, R.drawable.ic_default_user_avatar))
    }


@Preview
@Composable
fun UserProfileAvatarPreview() {
    UserProfileAvatar(UserAvatarData(availabilityStatus = UserAvailabilityStatus.AVAILABLE))
}
