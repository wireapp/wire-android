/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication.initialsync

sealed interface InitialSyncState {
    data object Loading : InitialSyncState
    data object Unavailable : InitialSyncState
    data class Completed(val shouldMoveToBackground: Boolean) : InitialSyncState
}
