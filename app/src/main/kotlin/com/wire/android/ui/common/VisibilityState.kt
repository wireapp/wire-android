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
fun <State: Any> VisibilityStateExt(
    status: VisibilityState<State>,
    content: @Composable (State) -> Unit,
) {
    if (status.isVisible && status.savedState != null) {
        content(status.savedState!!)
    }
}
