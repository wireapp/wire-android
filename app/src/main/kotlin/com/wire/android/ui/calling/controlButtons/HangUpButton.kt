package com.wire.android.ui.calling.controlButtons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions

@Composable
fun HangUpButton(onClick: () -> Unit) {
    IconButton(
        modifier = Modifier.width(MaterialTheme.wireDimensions.defaultCallingControlsSize),
        onClick = { onClick() },
    ) {
        Image(
            painter = painterResource(
                id = R.drawable.ic_hang_up
            ),
            contentDescription = stringResource(id = R.string.calling_hang_up_call)
        )
    }
}
