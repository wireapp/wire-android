package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryIconButton
import com.wire.android.ui.common.button.WireTertiaryIconButton
import com.wire.android.ui.common.button.wireSendPrimaryButtonColors
import com.wire.android.ui.common.dimensions

@Composable
fun MessageEditActions(
    editButtonEnabled: Boolean = false,
    onEditSaveButtonClicked: () -> Unit = { },
    onEditCancelButtonClicked: () -> Unit = { },
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
    ) {

        Box( // we need to wrap it because button is smaller than minimum touch size so compose will add paddings to it to be 48dp anyway
            modifier = Modifier.size(width = dimensions().spacing64x, height = dimensions().spacing56x),
            contentAlignment = Alignment.CenterEnd
        ) {
            WireTertiaryIconButton(
                onButtonClicked = onEditCancelButtonClicked,
                iconResource = R.drawable.ic_close,
                contentDescription = R.string.content_description_close_button,
                shape = CircleShape,
                minHeight = dimensions().spacing40x,
                minWidth = dimensions().spacing40x,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.size(width = dimensions().spacing64x, height = dimensions().spacing56x),
            contentAlignment = Alignment.CenterStart
        ) {
            WirePrimaryIconButton(
                onButtonClicked = onEditSaveButtonClicked,
                iconResource = R.drawable.ic_check_tick,
                contentDescription = R.string.content_description_edit_the_message,
                state = if (editButtonEnabled) WireButtonState.Default else WireButtonState.Disabled,
                colors = wireSendPrimaryButtonColors(),
                blockUntilSynced = true,
                shape = CircleShape,
                minHeight = dimensions().spacing40x,
                minWidth = dimensions().spacing40x,
            )
        }
    }
}

@Preview
@Composable
fun PreviewMessageEditActionsEnabled() {
    MessageEditActions(true, {}, {})
}
@Preview
@Composable
fun PreviewMessageEditActionsDisabled() {
    MessageEditActions(false, {}, {})
}
