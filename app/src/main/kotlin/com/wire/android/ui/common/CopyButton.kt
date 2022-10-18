package com.wire.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryIconButton

@Composable
fun CopyButton(onCopyClicked: () -> Unit, modifier: Modifier = Modifier) {
    WireSecondaryIconButton(
        onButtonClicked = onCopyClicked,
        iconResource = R.drawable.ic_copy,
        contentDescription = R.string.content_description_copy,
        modifier = modifier
    )
}
