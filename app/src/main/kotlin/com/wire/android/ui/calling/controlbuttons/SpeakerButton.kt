package com.wire.android.ui.calling.controlbuttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.dimensions

@Composable
fun SpeakerButton(
    modifier: Modifier = Modifier.size(dimensions().defaultCallingControlsSize),
    isSpeakerOn: Boolean,
    onSpeakerButtonClicked: () -> Unit
) {
    IconButton(
        modifier = modifier,
        onClick = {}
    ) {
        Icon(
            modifier = Modifier
                .wrapContentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false, radius = dimensions().defaultCallingControlsSize / 2),
                    role = Role.Button,
                    onClick = { onSpeakerButtonClicked() }
                ),
            painter = painterResource(
                id = if (isSpeakerOn)
                    R.drawable.ic_speaker_on
                else R.drawable.ic_speaker_off
            ),
            contentDescription = stringResource(
                id = if (isSpeakerOn) R.string.content_description_calling_turn_speaker_off
                else R.string.content_description_calling_turn_speaker_on
            ),
            tint = Color.Unspecified
        )
    }
}

@Preview
@Composable
fun ComposableSpeakerButtonPreview() {
    SpeakerButton(isSpeakerOn = true, onSpeakerButtonClicked = { })
}
