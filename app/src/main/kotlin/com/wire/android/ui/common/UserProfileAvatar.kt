package com.wire.android.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions

@Composable
fun UserProfileAvatar(
    avatarUrl: String = "",
    size: Dp = MaterialTheme.wireDimensions.userAvatarDefaultSize,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    ConstraintLayout(
        modifier = modifier
            .padding(10.dp)
            .size(size)
    ) {
        val (avatarImg, statusImg) = createRefs()
        Image(
            painter = painterResource(getAvatarAsDrawable(avatarUrl)),
            contentDescription = stringResource(R.string.content_description_user_avatar),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .background(Color.Black, CircleShape)
                .clickable { onClick.invoke() }
                .constrainAs(avatarImg) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color.Green) // TODO: Map UserStatus availability to the right icon shape/color once this logic exists in Kalium
                .constrainAs(statusImg) {
                    bottom.linkTo(avatarImg.bottom)
                    end.linkTo(avatarImg.end)
                }
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
    UserProfileAvatar(avatarUrl = "") {}
}
