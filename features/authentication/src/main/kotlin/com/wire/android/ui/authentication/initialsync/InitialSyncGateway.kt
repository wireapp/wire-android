/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication.initialsync

/**
 * Host bridge for the work which is necessarily tied to a logged-in account.
 *
 * The authentication feature owns the screen state and terminal-event policy. The host owns the
 * concrete sync observation, durable initial-sync marker and automated-login preference.
 */
fun interface InitialSyncGateway {
    suspend fun awaitInitialSync(): InitialSyncGatewayResult
}

sealed interface InitialSyncGatewayResult {
    data class Completed(val shouldMoveToBackground: Boolean) : InitialSyncGatewayResult
    data object Unavailable : InitialSyncGatewayResult
}
