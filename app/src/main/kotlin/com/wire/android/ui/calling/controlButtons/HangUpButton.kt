package com.wire.android.ui.calling.controlButtons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions

@Composable
fun HangUpButton(
    modifier: Modifier = Modifier.height(MaterialTheme.wireDimensions.defaultCallingControlsSize),
    onHangUpButtonClicked: () -> Unit
) {
    IconButton(
        modifier = modifier,
        onClick = onHangUpButtonClicked
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_hang_up),
            contentDescription = stringResource(id = R.string.calling_hang_up_call)
        )
    }
}

@Preview
@Composable
fun ComposableHangUpButtonPreview() {
    HangUpButton(onHangUpButtonClicked = { })
}
