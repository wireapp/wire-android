package com.wire.android.ui.calling

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.waz.avs.VideoPreview
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun CallPreview(callState: CallState, onVideoPreviewCreated: (view: View) -> Unit) {
    Box {
        if (callState.isCameraOn) {
            AndroidView(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(),
                factory = {
                    val videoPreview = VideoPreview(it)
                    onVideoPreviewCreated(videoPreview)
                    videoPreview
                })
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (callState.conversationName) {
                    is ConversationName.Known -> callState.conversationName.name
                    is ConversationName.Unknown -> stringResource(id = callState.conversationName.resourceId)
                    else -> ""
                },
                style = androidx.compose.material3.MaterialTheme.wireTypography.title01,
                modifier = Modifier.padding(top = dimensions().spacing24x)
            )
            Text(
                text = stringResource(id = com.wire.android.R.string.calling_label_ringing_call),
                style = androidx.compose.material3.MaterialTheme.wireTypography.body01,
                modifier = Modifier.padding(top = dimensions().spacing8x)
            )
            if(!callState.isCameraOn)
                UserProfileAvatar(
                    userAvatarAsset = callState.avatarAssetId,
                    size = dimensions().initiatingCallUserAvatarSize,
                    modifier = Modifier.padding(top = dimensions().spacing16x)
                )
        }
    }
}
