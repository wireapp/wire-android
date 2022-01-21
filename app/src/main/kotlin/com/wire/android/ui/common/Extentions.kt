package com.wire.android.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

@Composable
fun Modifier.selectableBackground(isSelected: Boolean, onClick: () -> Unit): Modifier =
    this.selectable(
        selected = isSelected,
        onClick = { onClick() },
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple(bounded = true, color = MaterialTheme.colors.onBackground.copy(0.5f)),
        role = Role.Tab
    )
