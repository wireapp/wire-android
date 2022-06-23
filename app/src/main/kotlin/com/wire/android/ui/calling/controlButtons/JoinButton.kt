package com.wire.android.ui.calling.controlButtons

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WirePrimaryButton

@Composable
fun JoinButton(buttonClick: () -> Unit) {
    WirePrimaryButton(
        onClick = buttonClick,
        fillMaxWidth = false,
        shape = RoundedCornerShape(dimensions().spacing12x),
        text = stringResource(R.string.calling_label_join_call),
        state = WireButtonState.Positive,
        modifier = Modifier
            .padding(
                top = dimensions().spacing8x,
                bottom = dimensions().spacing12x,
                end = dimensions().spacing8x
            ),
        contentPadding = PaddingValues(
            horizontal = dimensions().spacing12x,
            vertical = dimensions().spacing8x
        )
    )
}

@Preview
@Composable
fun JoinButtonPreview() {
    JoinButton {

    }
}
