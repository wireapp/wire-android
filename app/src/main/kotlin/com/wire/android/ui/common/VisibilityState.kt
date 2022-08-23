package com.wire.android.ui.common

import androidx.compose.runtime.Composable
import com.wire.android.ui.common.visbility.VisibilityState

@Composable
fun <T : Any> VisibilityState(
    state: T?,
    visible: @Composable (T) -> Unit,
) {
    if (state != null)
        visible(state)
}

// TODO kubaz rename to [VisibilityState] after resolving merge conflicts
@Composable
fun VisibilityStateExt(
    status: VisibilityState,
    content: @Composable () -> Unit,
) {
    if (status.isVisible) {
        content()
    }
}
