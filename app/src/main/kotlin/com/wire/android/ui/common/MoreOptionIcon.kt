package com.wire.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.R
import com.wire.android.ui.common.button.WireIconButton

@Composable
fun MoreOptionIcon(
    onButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireIconButton(
        onButtonClicked = onButtonClicked,
        iconResource = R.drawable.ic_more,
        contentDescription = R.string.content_description_show_more_options,
        modifier = modifier
    )
}
