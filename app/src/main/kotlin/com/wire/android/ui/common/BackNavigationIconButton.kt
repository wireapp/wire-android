package com.wire.android.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R

@Composable
fun BackNavigationIconButton(onBackButtonClick: () -> Unit) {
    IconButton(
        onClick = onBackButtonClick
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = stringResource(R.string.content_description_back_button),
        )
    }
}
