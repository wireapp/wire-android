package com.wire.android.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.theme.wireTypography

@Composable
fun FloatingActionButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit) = { Icon(Icons.Filled.Add, "") },
    onClick: () -> Unit
) {
    ExtendedFloatingActionButton(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = icon,
        text = {
            Text(
                text = text,
                style = MaterialTheme.wireTypography.button01
            )
        },
        onClick = onClick,
        modifier = modifier
    )
}
