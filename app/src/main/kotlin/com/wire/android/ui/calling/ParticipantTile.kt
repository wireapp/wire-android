package com.wire.android.ui.calling

import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
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
import com.waz.avs.VideoRenderer
import com.wire.android.R
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.id.QualifiedID

@Composable
fun ParticipantTile(
    modifier: Modifier,
    participantTitleState: UICallParticipant,
    onGoingCallTileUsernameMaxWidth: Dp = 350.dp,
    avatarSize: Dp = dimensions().onGoingCallUserAvatarSize,
    isSelfUser: Boolean,
    onSelfUserVideoPreviewCreated: (view: View) -> Unit,
    onClearSelfUserVideoPreview: () -> Unit
) {
    var updatedModifier = modifier
    if (participantTitleState.isSpeaking) {
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
                    avatarData = UserAvatarData(participantTitleState.avatar)
                )
            }
            if (isSelfUser) {
                if (participantTitleState.isCameraOn) {
                    val context = LocalContext.current
                    AndroidView(factory = {
                        val videoPreview = VideoPreview(context).also(onSelfUserVideoPreviewCreated)
                        videoPreview
                    })
                } else onClearSelfUserVideoPreview()
            } else {
                if (participantTitleState.isCameraOn) {
                    val context = LocalContext.current
                    AndroidView(factory = {
                        VideoRenderer(context, participantTitleState.id.toString(), participantTitleState.clientId, false).apply {
                            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        }
                    })
                }
            }

            if (participantTitleState.isMuted) {
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
                        contentDescription = stringResource(R.string.content_description_calling_participant_muted)
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
                color = if (participantTitleState.isSpeaking) MaterialTheme.wireColorScheme.primary else Color.Black
            ) {
                Text(
                    color = Color.White,
                    style = MaterialTheme.wireTypography.label01,
                    modifier = Modifier.padding(3.dp),
                    text = participantTitleState.name,
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
        participantTitleState = UICallParticipant(
            id = QualifiedID("", ""),
            clientId = "client-id",
            name = "name",
            isMuted = true,
            isSpeaking = true,
            isCameraOn = true,
            avatar = null,
            membership = Membership.Admin
        ),
        onClearSelfUserVideoPreview = {},
        onSelfUserVideoPreviewCreated = {},
        isSelfUser = false
    )
}
