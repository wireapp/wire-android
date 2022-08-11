package com.wire.android.ui.common.topappbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.wire.android.R
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun CommonTopAppBar(commonTopAppBarViewModel: CommonTopAppBarViewModel) {

    with (commonTopAppBarViewModel) {
        ongoingCallLabel(
            isMuted = callState.isMuted ?: false,
            shouldShow = callState.shouldShowOngoingCallLabel,
            onClick = ::openOngoingCallScreen
        )
    }
}

@Composable
private fun ongoingCallLabel(
    isMuted: Boolean,
    shouldShow: Boolean,
    onClick: () -> Unit
) {
    if (shouldShow) {
        val darkIcons = MaterialTheme.wireColorScheme.useDarkSystemBarIcons
        rememberSystemUiController().setStatusBarColor(
            color = MaterialTheme.wireColorScheme.ongoingCallLabelColor,
            darkIcons = darkIcons
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTheme.wireDimensions.ongoingCallLabelHeight)
                .background(MaterialTheme.wireColorScheme.ongoingCallLabelColor)
                .clickable { onClick() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row {
                // Microphone Icon
                Icon(
                    painter = painterResource(
                        id = if (isMuted) R.drawable.ic_microphone_white_muted
                        else R.drawable.ic_microphone_white
                    ),
                    contentDescription = stringResource(
                        id = if (isMuted) R.string.content_description_calling_call_muted
                        else R.string.content_description_calling_call_unmuted
                    ),
                    tint = MaterialTheme.wireColorScheme.ongoingCallLabelIconColor
                )

                // Camera Icon
                Icon(
                    painter = painterResource(id = R.drawable.ic_camera_white_paused),
                    contentDescription = stringResource(R.string.content_description_calling_call_paused_camera),
                    modifier = Modifier.padding(
                        start = MaterialTheme.wireDimensions.spacing8x,
                        end = MaterialTheme.wireDimensions.spacing8x
                    ),
                    tint = MaterialTheme.wireColorScheme.ongoingCallLabelIconColor
                )

                Text(
                    text = stringResource(id = R.string.calling_ongoing_call_return_to_call),
                    color = MaterialTheme.wireColorScheme.ongoingCallLabelTextColor,
                    style = MaterialTheme.wireTypography.title03,
                    textAlign = TextAlign.Left,
                )
            }
        }
    } else {
        clearStatusBarColor()
    }
}

@Composable
private fun clearStatusBarColor() {
    val backgroundColor = MaterialTheme.wireColorScheme.background
    val darkIcons = MaterialTheme.wireColorScheme.useDarkSystemBarIcons

    rememberSystemUiController().setSystemBarsColor(
        color = backgroundColor,
        darkIcons = darkIcons
    )
}

@Preview("is NOT muted")
@Composable
fun CommonTopAppBarCallIsNotMuted() {
    ongoingCallLabel(
        isMuted = false,
        shouldShow = true,
        onClick = { }
    )
}

@Preview("is muted")
@Composable
fun CommonTopAppBarCallIsMuted() {
    ongoingCallLabel(
        isMuted = false,
        shouldShow = true,
        onClick = { }
    )
}
