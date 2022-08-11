package com.wire.android.ui.common

import androidx.compose.runtime.Composable

@Composable
fun <T : Any> VisibilityState(
    state: T?,
    visible: @Composable (T) -> Unit,
) {
    if (state != null)
        visible(state)
}
