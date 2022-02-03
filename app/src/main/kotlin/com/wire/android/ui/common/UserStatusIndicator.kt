package com.wire.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.model.UserStatus
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun UserStatusIndicator(status: UserStatus, modifier: Modifier = Modifier) {
    when (status) {
        UserStatus.AVAILABLE -> AvailableDot(modifier)
        UserStatus.BUSY -> BusyDot(modifier)
        UserStatus.AWAY -> AwayDot(modifier)
        UserStatus.NONE -> None()
    }
}

@Composable
private fun AvailableDot(modifier: Modifier) {
    Box(
        modifier = modifier
            .size(dimensions().userAvatarStatusSize)
            .background(Color.White, CircleShape)
            .padding(dimensions().userAvatarStatusBorderSize)
            .background(MaterialTheme.wireColorScheme.positive, CircleShape)
    )
}

@Composable
private fun BusyDot(modifier: Modifier) {
    Box(
        modifier = modifier
            .size(dimensions().userAvatarStatusSize)
            .background(Color.White, CircleShape)
            .padding(dimensions().userAvatarStatusBorderSize)
            .background(MaterialTheme.wireColorScheme.warning, CircleShape)
            .padding(
                top = dimensions().userAvatarBusyVerticalPadding,
                bottom = dimensions().userAvatarBusyVerticalPadding,
                start = dimensions().userAvatarBusyHorizontalPadding,
                end = dimensions().userAvatarBusyHorizontalPadding
            )
            .background(Color.White)
    )
}

@Composable
private fun AwayDot(modifier: Modifier) {
    Box(
        modifier = modifier
            .size(dimensions().userAvatarStatusSize)
            .background(Color.White, CircleShape)
            .padding(dimensions().userAvatarStatusBorderSize)
            .background(MaterialTheme.wireColorScheme.error, CircleShape)
            .padding(dimensions().userAvatarStatusBorderSize)
            .background(Color.White, CircleShape)
    )
}

@Composable
private fun None() {
    //Display nothing
}

@Preview(name = "AvailablePreview")
@Composable
private fun AvailablePreview() {
    UserStatusIndicator(UserStatus.AVAILABLE)
}

@Preview(name = "BusyPreview")
@Composable
private fun BusyPreview() {
    UserStatusIndicator(UserStatus.BUSY)
}

@Preview(name = "AwayPreview")
@Composable
private fun AwayPreview() {
    UserStatusIndicator(UserStatus.AWAY)
}

@Preview(name = "NonePreview")
@Composable
private fun NonePreview() {
    UserStatusIndicator(UserStatus.NONE)
}
