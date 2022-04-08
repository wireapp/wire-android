package com.wire.android.ui.calling.controlButtons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.rememberAsyncImagePainter
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.getUriFromDrawable

@Composable
fun DrawSpeakerButton() {
    Image(
        modifier = Modifier.width(MaterialTheme.wireDimensions.defaultCallingControlsSize),
        painter = rememberAsyncImagePainter(getUriFromDrawable(LocalContext.current, R.drawable.ic_speaker_off)),
        contentDescription = stringResource(id = R.string.calling_turn_speaker_on_off)
    )
}
