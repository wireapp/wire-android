package com.wire.android.ui.calling

import android.view.View
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import com.waz.avs.VideoPreview
import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun ParticipantTile(
    modifier: Modifier,
    conversationName: ConversationName?,
    participantAvatar: ImageAsset.UserAvatarAsset?,
    onGoingCallTileUsernameMaxWidth: Dp = 350.dp,
    isMuted: Boolean,
    isCameraOn: Boolean,
    isActiveSpeaker: Boolean,
    avatarSize: Dp = dimensions().onGoingCallUserAvatarSize,
    onSelfUserVideoPreviewCreated: (view: View) -> Unit,
    onClearSelfUserVideoPreview: () -> Unit
) {
    var updatedModifier = modifier
    if (isActiveSpeaker) {
        updatedModifier = modifier
            .border(
                width = dimensions().spacing4x,
                color = MaterialTheme.wireColorScheme.primary,
                shape = RoundedCornerShape(dimensions().corner8x)
            )
            .padding(dimensions().spacing6x)
    }
    Surface(
        modifier = updatedModifier.padding(top = 0.dp),
        color = MaterialTheme.wireColorScheme.ongoingCallBackground,
        shape = RoundedCornerShape(dimensions().corner6x)
    ) {

        ConstraintLayout {
            val (avatar, userName, muteIcon) = createRefs()

            if (isCameraOn) {
                val context = LocalContext.current
                val view = remember { VideoPreview(context) }
                //TODO fix memory leak when the app goes to background with video turned on
                // https://issuetracker.google.com/issues/198012639
                // The issue is marked as fixed in the issue tracker,
                // but we are still getting it with our current compose version 1.2.0-beta01
                AndroidView(factory = {
                    onSelfUserVideoPreviewCreated(view)
                    view
                })
            } else {
                onClearSelfUserVideoPreview()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .constrainAs(avatar) { },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserProfileAvatar(
                        modifier = Modifier.padding(top = dimensions().spacing16x),
                        size = avatarSize,
                        avatarData = UserAvatarData(participantAvatar)
                    )
                }
            }

            if (isMuted) {
                Surface(
                    modifier = Modifier
                        .padding(
                            start = dimensions().spacing8x,
                            bottom = dimensions().spacing8x
                        )
                        .constrainAs(muteIcon) {
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                        },
                    color = Color.Black,
                    shape = RoundedCornerShape(dimensions().corner6x)
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(dimensions().spacing4x),
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_participant_muted),
                        tint = MaterialTheme.wireColorScheme.muteButtonColor,
                        contentDescription = stringResource(R.string.calling_content_description_participant_muted)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .padding(bottom = dimensions().spacing6x)
                    .constrainAs(userName) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo((parent.end))
                    }
                    .widthIn(max = onGoingCallTileUsernameMaxWidth),
                shape = RoundedCornerShape(dimensions().corner4x),
                color = if (isActiveSpeaker) MaterialTheme.wireColorScheme.primary else Color.Black
            ) {
                Text(
                    color = Color.White,
                    style = MaterialTheme.wireTypography.label01,
                    modifier = Modifier.padding(3.dp),
                    text = when (conversationName) {
                        is ConversationName.Known -> conversationName.name
                        is ConversationName.Unknown -> stringResource(id = conversationName.resourceId)
                        else -> ""
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview
@Composable
private fun ParticipantTilePreview() {
    ParticipantTile(
        modifier = Modifier.height(300.dp),
        isMuted = false,
        isCameraOn = false,
        conversationName = ConversationName.Known("Known Conversation"),
        onClearSelfUserVideoPreview = {},
        onSelfUserVideoPreviewCreated = {},
        participantAvatar = null,
        isActiveSpeaker = false
    )
}
