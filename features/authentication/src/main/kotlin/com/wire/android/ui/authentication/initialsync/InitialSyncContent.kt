/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication.initialsync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.wire.android.ui.common.SettingUpWireScreenContent

/**
 * The initial-sync surface intentionally remains non-dismissible. The host owns the terminal
 * Navigation 3 transition; this content emits it only after the gateway has awaited persistence.
 */
@Composable
fun InitialSyncRouteContent(
    viewModel: InitialSyncViewModel,
    onSyncCompleted: (shouldMoveToBackground: Boolean) -> Unit,
) {
    SettingUpWireScreenContent()

    val state = viewModel.state
    LaunchedEffect(state) {
        if (state is InitialSyncState.Completed) onSyncCompleted(state.shouldMoveToBackground)
    }
}
