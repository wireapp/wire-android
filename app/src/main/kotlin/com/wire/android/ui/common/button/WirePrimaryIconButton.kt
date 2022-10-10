package com.wire.android.ui.common.button

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.theme.wireDimensions

@Composable
fun WirePrimaryIconButton(
    onButtonClicked: () -> Unit,
    @DrawableRes iconResource: Int,
    @StringRes contentDescription: Int,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.buttonCornerSize),
    minHeight: Dp = dimensions().spacing32x,
    minWidth: Dp = dimensions().spacing40x,
    state: WireButtonState = WireButtonState.Default,
    colors: WireButtonColors = wirePrimaryButtonColors(),
    blockUntilSynced: Boolean = false,
    modifier: Modifier = Modifier
) {
    WirePrimaryButton(
        onClick = onButtonClicked,
        leadingIcon = {
            Icon(
                painter = painterResource(id = iconResource),
                contentDescription = stringResource(contentDescription),
                modifier = Modifier.size(dimensions().wireIconButtonSize)
            )
        },
        shape = shape,
        minHeight = minHeight,
        minWidth = minWidth,
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
        leadingIconAlignment = IconAlignment.Center,
        state = state,
        colors = colors,
        blockUntilSynced = blockUntilSynced,
        fillMaxWidth = false,
        modifier = modifier
    )
}
