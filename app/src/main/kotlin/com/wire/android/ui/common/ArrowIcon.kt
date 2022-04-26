package com.wire.android.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions

@Composable
fun ArrowRightIcon(
    modifier: Modifier = Modifier
) {
    ArrowIcon(
        arrowIcon = R.drawable.ic_arrow_right,
        contentDescription = R.string.content_description_right_arrow,
        modifier = modifier
    )
}

@Composable
fun ArrowLeftIcon(
    modifier: Modifier = Modifier
) {
    ArrowIcon(
        arrowIcon = R.drawable.ic_arrow_left,
        contentDescription = R.string.content_description_left_arrow,
        modifier = modifier
    )
}

@Composable
private fun ArrowIcon(
    @DrawableRes arrowIcon: Int,
    @StringRes contentDescription: Int,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(id = arrowIcon),
        contentDescription = stringResource(contentDescription),
        modifier = modifier
            .size(MaterialTheme.wireDimensions.wireIconButtonSize)
            .then(modifier)
    )
}
