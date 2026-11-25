/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.authentication.login.mfa

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.wire.android.R
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.coroutines.flow.filterNotNull

@Composable
internal fun MFARecoveryLoginSnackbarEffect(
    homeEntryId: WireNavEntryId?,
    sessionId: WireSessionId?,
    enabled: Boolean,
    pendingNotice: (WireSessionId) -> RecoveryCodeLoginNotice?,
    consumeNotice: (RecoveryCodeLoginNotice) -> Boolean,
    onManageCodes: (RecoveryCodeLoginNotice) -> Unit,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalContext.current.resources
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val manageCodesLabel = stringResource(R.string.mfa_login_manage_codes)
    val currentPendingNotice by rememberUpdatedState(pendingNotice)
    val currentConsumeNotice by rememberUpdatedState(consumeNotice)
    val currentOnManageCodes by rememberUpdatedState(onManageCodes)

    LaunchedEffect(homeEntryId, sessionId, enabled, lifecycle, snackbarHostState) {
        if (!enabled || homeEntryId == null || sessionId == null) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { currentPendingNotice(sessionId) }
                .filterNotNull()
                .collect { notice ->
                    if (currentConsumeNotice(notice)) {
                        val result = snackbarHostState.showSnackbar(
                            message = resources.getQuantityString(
                                R.plurals.mfa_login_recovery_code_notice,
                                notice.remainingCodes,
                                notice.remainingCodes,
                            ),
                            actionLabel = manageCodesLabel,
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            currentOnManageCodes(notice)
                        }
                    }
                }
        }
    }
}
