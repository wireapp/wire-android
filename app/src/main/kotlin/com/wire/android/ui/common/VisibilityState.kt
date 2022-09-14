package com.wire.android.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import com.wire.android.ui.common.visbility.VisibilityState

@Composable
fun <State : Any> VisibilityState(
    status: VisibilityState<State>,
    content: @Composable (State) -> Unit,
) {
    AnimatedVisibility(visible = status.isVisible && status.savedState != null) {
        content(status.savedState!!)
    }
}
