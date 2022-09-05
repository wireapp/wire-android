package com.wire.android.ui.home.messagecomposer

import androidx.compose.runtime.Composable
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireIconButton

@Composable
fun AdditionalOptionButton(isSelected: Boolean = false, onClick: () -> Unit) {
    WireIconButton(
        onButtonClicked = onClick,
        iconResource = R.drawable.ic_add,
        contentDescription = R.string.content_description_attachment_item,
        state = if (isSelected) WireButtonState.Selected else WireButtonState.Default,
    )
}
