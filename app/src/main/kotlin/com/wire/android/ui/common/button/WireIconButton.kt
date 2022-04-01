package com.wire.android.ui.common.button

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.dimensions

@Composable
fun WireIconButton(
    onButtonClicked: () -> Unit,
    @DrawableRes iconResource: Int,
    @StringRes contentDescription: Int,
    modifier: Modifier = Modifier
) {
    WireSecondaryButton(
        onClick = onButtonClicked,
        leadingIcon = {
            Icon(
                painter = painterResource(id = iconResource),
                contentDescription = stringResource(contentDescription),
                modifier = modifier
                    .size(dimensions().wireIconButtonSize)
            )
        },
        minHeight = dimensions().spacing32x,
        minWidth = dimensions().spacing40x,
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
        leadingIconAlignment = IconAlignment.Center,
        fillMaxWidth = false
    )
}
