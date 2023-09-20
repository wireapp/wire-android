/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.calling.ongoing.participantsview

import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.waz.avs.VideoPreview
import com.waz.avs.VideoRenderer
import com.wire.android.R
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.QualifiedID

@Composable
fun ParticipantTile(
    modifier: Modifier,
    participantTitleState: UICallParticipant,
    onGoingCallTileUsernameMaxWidth: Dp = 350.dp,
    avatarSize: Dp = dimensions().onGoingCallUserAvatarSize,
    isSelfUser: Boolean,
    shouldFill: Boolean = true,
    isZoomingEnabled: Boolean = false,
    isSelfUserMuted: Boolean,
    isSelfUserCameraOn: Boolean,
    onSelfUserVideoPreviewCreated: (view: View) -> Unit,
    onClearSelfUserVideoPreview: () -> Unit
) {
    val alpha = if (participantTitleState.hasEstablishedAudio) ContentAlpha.high else ContentAlpha.medium
    Surface(
        modifier = modifier,
        color = colorsScheme().callingParticipantTileBackgroundColor,
        shape = RoundedCornerShape(dimensions().corner6x),
    ) {

        ConstraintLayout {
            val (avatar, userName, muteIcon) = createRefs()

            AvatarTile(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha)
                    .constrainAs(avatar) { },
                avatar = UserAvatarData(participantTitleState.avatar),
                avatarSize = avatarSize
            )

            if (isSelfUser) {
                CameraPreview(
                    isCameraOn = isSelfUserCameraOn,
                    onSelfUserVideoPreviewCreated = onSelfUserVideoPreviewCreated,
                    onClearSelfUserVideoPreview = onClearSelfUserVideoPreview
                )
            } else {
                OthersVideoRenderer(
                    participantId = participantTitleState.id.toString(),
                    clientId = participantTitleState.clientId,
                    isCameraOn = participantTitleState.isCameraOn,
                    isSharingScreen = participantTitleState.isSharingScreen,
                    shouldFill = shouldFill,
                    isZoomingEnabled = isZoomingEnabled
                )
            }

            MicrophoneTile(
                modifier = Modifier
                    .padding(
                        start = dimensions().spacing8x,
                        bottom = dimensions().spacing8x
                    )
                    .constrainAs(muteIcon) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                    },
                isMuted = if (isSelfUser) isSelfUserMuted else participantTitleState.isMuted,
                hasEstablishedAudio = participantTitleState.hasEstablishedAudio
            )

            UsernameTile(
                modifier = Modifier
                    .padding(bottom = dimensions().spacing8x)
                    .constrainAs(userName) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo((parent.end))
                    }
                    .widthIn(max = onGoingCallTileUsernameMaxWidth),
                name = participantTitleState.name,
                isSpeaking = participantTitleState.isSpeaking,
                hasEstablishedAudio = participantTitleState.hasEstablishedAudio
            )
        }
        TileBorder(participantTitleState.isSpeaking)
    }
}

@Composable
private fun cleanUpRendererIfNeeded(videoRenderer: VideoRenderer) {
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(videoRenderer, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                videoRenderer.destroyRenderer()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            videoRenderer.destroyRenderer()
        }
    }
}

@Composable
private fun TileBorder(isSpeaking: Boolean) {
    if (isSpeaking) {
        val color = MaterialTheme.wireColorScheme.primary
        val strokeWidth = dimensions().corner8x
        val cornerRadius = dimensions().corner10x

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasQuadrantSize = size
            drawRoundRect(
                color = color,
                size = canvasQuadrantSize,
                style = Stroke(width = strokeWidth.toPx()),
                cornerRadius = CornerRadius(
                    x = cornerRadius.toPx(),
                    y = cornerRadius.toPx()
                )
            )
        }
    }
}

@Composable
private fun CameraPreview(
    isCameraOn: Boolean,
    onSelfUserVideoPreviewCreated: (view: View) -> Unit,
    onClearSelfUserVideoPreview: () -> Unit
) {
    if (isCameraOn) {
        val context = LocalContext.current
        val videoPreview = remember {
            VideoPreview(context).apply {
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                setShouldFill(false)
            }
        }
        AndroidView(
            factory = { videoPreview },
            update = {
                onSelfUserVideoPreviewCreated(videoPreview)
            }
        )
    } else {
        onClearSelfUserVideoPreview()
    }
}

@Composable
private fun OthersVideoRenderer(
    participantId: String,
    clientId: String,
    isCameraOn: Boolean,
    isSharingScreen: Boolean,
    shouldFill: Boolean,
    isZoomingEnabled: Boolean
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val context = LocalContext.current
    val rendererFillColor = (colorsScheme().callingParticipantTileBackgroundColor.value shr 32).toLong()
    if (isCameraOn || isSharingScreen) {

        val videoRenderer = remember {
            VideoRenderer(
                context,
                participantId,
                clientId,
                false
            ).apply {
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                setFillColor(rendererFillColor)
                setShouldFill(shouldFill)
            }
        }

        cleanUpRendererIfNeeded(videoRenderer)

        AndroidView(
            modifier = Modifier
                .onSizeChanged {
                    size = it
                }
                .pointerInput(Unit) {
                    // enable zooming on full screen and when video is on
                    if (isZoomingEnabled) {
                        detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(1f, 3f)
                            val maxX = (size.width * (zoom - 1)) / 2
                            val minX = -maxX
                            offsetX = maxOf(minX, minOf(maxX, offsetX + gesturePan.x))
                            val maxY = (size.height * (zoom - 1)) / 2
                            val minY = -maxY
                            offsetY = maxOf(minY, minOf(maxY, offsetY + gesturePan.y))
                        }
                    }
                }
                .graphicsLayer(
                    scaleX = zoom,
                    scaleY = zoom,
                    translationX = offsetX,
                    translationY = offsetY
                ),

            factory = {
                val frameLayout = FrameLayout(it)
                frameLayout.addView(videoRenderer)
                frameLayout
            })
    }
}

@Composable
private fun AvatarTile(
    modifier: Modifier,
    avatar: UserAvatarData,
    avatarSize: Dp
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UserProfileAvatar(
            size = avatarSize,
            avatarData = avatar
        )
    }
}

@Composable
private fun UsernameTile(
    modifier: Modifier,
    name: String,
    isSpeaking: Boolean,
    hasEstablishedAudio: Boolean
) {
    val color = if (isSpeaking) MaterialTheme.wireColorScheme.primary else Color.Black
    val nameLabelColor = if (hasEstablishedAudio) Color.White else colorsScheme().secondaryText

    ConstraintLayout(modifier = modifier) {
        val (nameLabel, connectingLabel) = createRefs()

        Surface(
            modifier = Modifier.constrainAs(nameLabel) {
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(connectingLabel.start)
            },
            shape = RoundedCornerShape(
                topStart = dimensions().corner4x,
                bottomStart = dimensions().corner4x,
                topEnd = if (hasEstablishedAudio) dimensions().corner4x else 0.dp,
                bottomEnd = if (hasEstablishedAudio) dimensions().corner4x else 0.dp,
            ),
            color = color
        ) {
            Text(
                color = nameLabelColor,
                style = MaterialTheme.wireTypography.label01,
                modifier = Modifier.padding(dimensions().spacing4x),
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!hasEstablishedAudio) {
            Surface(
                modifier = Modifier.constrainAs(connectingLabel) {
                    start.linkTo(nameLabel.end)
                    top.linkTo(nameLabel.top)
                    bottom.linkTo(nameLabel.bottom)
                },
                shape = RoundedCornerShape(
                    topEnd = dimensions().corner4x,
                    bottomEnd = dimensions().corner4x
                ),
                color = color
            ) {
                Text(
                    color = colorsScheme().error,
                    style = MaterialTheme.wireTypography.label01,
                    modifier = Modifier.padding(
                        top = dimensions().spacing4x,
                        bottom = dimensions().spacing4x,
                        end = dimensions().spacing4x
                    ),
                    text = stringResource(id = R.string.participant_tile_call_connecting_label),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MicrophoneTile(
    modifier: Modifier,
    isMuted: Boolean,
    hasEstablishedAudio: Boolean
) {
    if (isMuted && hasEstablishedAudio) {
        Surface(
            modifier = modifier,
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
}

@Preview("Default view")
@Composable
fun PreviewParticipantTile() {
    ParticipantTile(
        modifier = Modifier.height(300.dp),
        participantTitleState = UICallParticipant(
            id = QualifiedID("", ""),
            clientId = "client-id",
            name = "user name",
            isMuted = true,
            isSpeaking = false,
            isCameraOn = false,
            isSharingScreen = false,
            avatar = null,
            membership = Membership.Admin,
            hasEstablishedAudio = true
        ),
        onClearSelfUserVideoPreview = {},
        onSelfUserVideoPreviewCreated = {},
        isSelfUser = false,
        isSelfUserMuted = false,
        isSelfUserCameraOn = false
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantTalking() {
    ParticipantTile(
        modifier = Modifier.height(300.dp),
        participantTitleState = UICallParticipant(
            id = QualifiedID("", ""),
            clientId = "client-id",
            name = "long user name to be displayed in participant tile during a call",
            isMuted = false,
            isSpeaking = true,
            isCameraOn = false,
            isSharingScreen = false,
            avatar = null,
            membership = Membership.Admin,
            hasEstablishedAudio = true
        ),
        onClearSelfUserVideoPreview = {},
        onSelfUserVideoPreviewCreated = {},
        isSelfUser = false,
        isSelfUserMuted = false,
        isSelfUserCameraOn = false
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantConnecting() {
    ParticipantTile(
        modifier = Modifier
            .height(350.dp)
            .width(200.dp),
        participantTitleState = UICallParticipant(
            id = QualifiedID("", ""),
            clientId = "client-id",
            name = "Oussama2",
            isMuted = true,
            isSpeaking = false,
            isCameraOn = false,
            isSharingScreen = false,
            avatar = null,
            membership = Membership.Admin,
            hasEstablishedAudio = false
        ),
        onClearSelfUserVideoPreview = {},
        onSelfUserVideoPreviewCreated = {},
        isSelfUser = false,
        isSelfUserMuted = false,
        isSelfUserCameraOn = false
    )
}
