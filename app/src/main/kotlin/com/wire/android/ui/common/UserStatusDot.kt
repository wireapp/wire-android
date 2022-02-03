package com.wire.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun UserStatusDot(status: UserStatus, modifier: Modifier = Modifier) {
    when (status) {
        UserStatus.AVAILABLE -> AvailableDot(modifier)
        UserStatus.BUSY -> BusyDot(modifier)
        UserStatus.AWAY -> AwayDot(modifier)
        UserStatus.NONE -> NoneDot()
    }
}

@Composable
private fun AvailableDot(modifier: Modifier) {
    Box(
        modifier = modifier
            .size(dimensions().userAvatarStatusSize)
            .clip(CircleShape)
            .background(Color.White)
            .padding(dimensions().userAvatarStatusBorderSize)
            .clip(CircleShape)
            .background(MaterialTheme.wireColorScheme.positive)
    )
}

@Composable
private fun BusyDot(modifier: Modifier) {
    Box(
        modifier = modifier
            .size(dimensions().userAvatarStatusSize)
            .clip(CircleShape)
            .background(Color.White)
            .padding(dimensions().userAvatarStatusBorderSize)
            .clip(CircleShape)
            .background(MaterialTheme.wireColorScheme.warning)
            .padding(
                top = dimensions().userAvatarBusyVerticalPadding,
                bottom = dimensions().userAvatarBusyVerticalPadding,
                start = dimensions().userAvatarBusyHorizontalPadding,
                end = dimensions().userAvatarBusyHorizontalPadding
            )
            .clip(RectangleShape)
            .background(Color.White)
    )
}

@Composable
private fun AwayDot(modifier: Modifier) {
    Box(
        modifier = modifier
            .size(dimensions().userAvatarStatusSize)
            .clip(CircleShape)
            .background(Color.White)
            .padding(dimensions().userAvatarStatusBorderSize)
            .clip(CircleShape)
            .background(MaterialTheme.wireColorScheme.error)
            .padding(dimensions().userAvatarStatusBorderSize)
            .clip(CircleShape)
            .background(Color.White)
    )
}

@Composable
private fun NoneDot() {

}

enum class UserStatus {
    AVAILABLE, BUSY, AWAY, NONE
}

@Preview(name = "AvailablePreview")
@Composable
fun AvailablePreview() {
    UserStatusDot(UserStatus.AVAILABLE)
}

@Preview(name = "BusyPreview")
@Composable
fun BusyPreview() {
    UserStatusDot(UserStatus.BUSY)
}

@Preview(name = "AwayPreview")
@Composable
fun AwayPreview() {
    UserStatusDot(UserStatus.AWAY)
}

@Preview(name = "NonePreview")
@Composable
fun NonePreview() {
    UserStatusDot(UserStatus.NONE)
}
