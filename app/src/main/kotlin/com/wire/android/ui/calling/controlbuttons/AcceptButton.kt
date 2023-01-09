package com.wire.android.ui.calling.controlbuttons

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.wirePrimaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions

@Composable
fun AcceptButton(
    modifier: Modifier = Modifier.size(dimensions().initiatingCallHangUpButtonSize),
    buttonClicked: () -> Unit
) {
    WirePrimaryButton(
        shape = CircleShape,
        modifier = modifier,
        colors = wirePrimaryButtonColors().copy(enabled = colorsScheme().callingAnswerButtonColor),
        onClick = buttonClicked,
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_call_accept),
                contentDescription = stringResource(id = R.string.content_description_calling_accept_call),
                tint = colorsScheme().onCallingAnswerButtonColor
            )
        }
    )
}
