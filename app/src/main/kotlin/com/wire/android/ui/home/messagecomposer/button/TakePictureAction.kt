package com.wire.android.ui.home.messagecomposer.button

import androidx.compose.runtime.Composable
import com.wire.android.R
import com.wire.android.ui.common.button.WireIconButton

@Composable
fun TakePictureAction() {
    WireIconButton(
        onButtonClicked = {},
        iconResource = R.drawable.ic_ping,
        contentDescription = R.string.content_description_ping_everyone
    )
}
