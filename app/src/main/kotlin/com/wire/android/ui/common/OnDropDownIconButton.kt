package com.wire.android.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R


@Composable
fun OnDropDownIconButton(onDropDownClick: () -> Unit) {
    IconButton(
        onClick = onDropDownClick
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = stringResource(R.string.content_description_drop_down_icon),
        )
    }
}
