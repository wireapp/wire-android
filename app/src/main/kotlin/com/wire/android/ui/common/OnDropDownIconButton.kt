package com.wire.android.ui.common

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R


@Composable
fun OnDropDownIconButton(onDropDownClick: () -> Unit) {
    IconButton(
        onClick = onDropDownClick
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_dropdown_icon),
            contentDescription = stringResource(R.string.content_description_drop_down_icon),
        )
    }
}
