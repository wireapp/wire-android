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

package com.wire.android.ui.home.settings.multifactorauthentication

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val DUMMY_LOADING_DELAY_MILLIS = 500L

data class MultiFactorAuthenticationStoreState(
    val overview: MultiFactorAuthenticationOverview? = null,
    val isRefreshing: Boolean = false,
    val failure: Exception? = null,
)

/** Owned by the MFA flow ViewModel, not the account's dependency graph. */
class MultiFactorAuthenticationStore(
    private val loadOverview: suspend () -> MultiFactorAuthenticationOverview,
) {
    @Inject
    constructor(loader: DummyMultiFactorAuthenticationLoader) : this(loader::invoke)

    private val _state = MutableStateFlow(MultiFactorAuthenticationStoreState())
    val state = _state.asStateFlow()
    private val refreshMutex = Mutex()
    private var dummyOverviewOverride: MultiFactorAuthenticationOverview? = null

    @Suppress("TooGenericExceptionCaught")
    suspend fun refresh() {
        // Multiple screens can request a refresh; reuse the operation already in progress.
        if (!refreshMutex.tryLock()) return
        try {
            _state.update { it.copy(isRefreshing = true, failure = null) }
            val overview = loadOverview()
            _state.value = MultiFactorAuthenticationStoreState(overview = dummyOverviewOverride ?: overview)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            _state.update { it.copy(failure = failure) }
        } finally {
            _state.update { it.copy(isRefreshing = false) }
            refreshMutex.unlock()
        }
    }

    suspend fun completeSetup(result: MultiFactorAuthenticationSetupResult) = refreshMutex.withLock {
        // Keep local prototype changes authoritative until the MFA area is closed.
        val overview = MultiFactorAuthenticationOverview.Enabled(
            authenticatorApps = listOf(
                AuthenticatorApp(
                    id = result.authenticatorId,
                    name = result.authenticatorName,
                    addedOn = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
                )
            ),
            recoveryCodesStatus = RecoveryCodesStatus(result.recoveryCodesCount, result.recoveryCodesCount),
        )
        dummyOverviewOverride = overview
        _state.value = MultiFactorAuthenticationStoreState(overview = overview)
    }

    suspend fun updateRecoveryCodesStatus(result: MultiFactorAuthenticationRecoveryCodesResult) = refreshMutex.withLock {
        val overview = _state.value.overview as? MultiFactorAuthenticationOverview.Enabled ?: return@withLock
        val updated = overview.copy(recoveryCodesStatus = RecoveryCodesStatus(result.generatedCount, result.generatedCount))
        dummyOverviewOverride = updated
        _state.value = MultiFactorAuthenticationStoreState(overview = updated)
    }

    suspend fun completeTurnOff() = refreshMutex.withLock {
        dummyOverviewOverride = MultiFactorAuthenticationOverview.Disabled
        _state.value = MultiFactorAuthenticationStoreState(overview = MultiFactorAuthenticationOverview.Disabled)
    }

    suspend fun completeAuthenticatorAppRename(result: AuthenticatorAppRenameResult) = refreshMutex.withLock {
        val overview = _state.value.overview as? MultiFactorAuthenticationOverview.Enabled ?: return@withLock
        if (overview.authenticatorApps.none { it.id == result.appId }) return@withLock
        val trimmedName = result.name.trim()
        require(trimmedName.isNotEmpty())
        val updated = overview.copy(
            authenticatorApps = overview.authenticatorApps.map { app ->
                if (app.id == result.appId) app.copy(name = trimmedName) else app
            },
        )
        dummyOverviewOverride = updated
        _state.value = MultiFactorAuthenticationStoreState(overview = updated)
    }

    suspend fun addAuthenticatorApp(result: AuthenticatorAppAddedResult): AuthenticatorAppMutationResult = refreshMutex.withLock {
        val overview = _state.value.overview as? MultiFactorAuthenticationOverview.Enabled
            ?: return@withLock AuthenticatorAppMutationResult.MFA_DISABLED
        if (overview.authenticatorApps.any { it.id == result.appId }) return@withLock AuthenticatorAppMutationResult.SUCCESS
        if (overview.authenticatorApps.size >= MAX_AUTHENTICATOR_APPS) return@withLock AuthenticatorAppMutationResult.LIMIT_REACHED
        require(result.name.isNotBlank())
        val app = AuthenticatorApp(
            id = result.appId,
            name = result.name.trim(),
            addedOn = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        )
        val updated = overview.copy(authenticatorApps = overview.authenticatorApps + app)
        dummyOverviewOverride = updated
        _state.value = MultiFactorAuthenticationStoreState(overview = updated)
        AuthenticatorAppMutationResult.SUCCESS
    }

    suspend fun removeAuthenticatorApp(appId: String): AuthenticatorAppMutationResult = refreshMutex.withLock {
        val overview = _state.value.overview as? MultiFactorAuthenticationOverview.Enabled
            ?: return@withLock AuthenticatorAppMutationResult.MFA_DISABLED
        if (overview.authenticatorApps.none { it.id == appId }) return@withLock AuthenticatorAppMutationResult.NOT_FOUND
        if (overview.authenticatorApps.size <= 1) return@withLock AuthenticatorAppMutationResult.LAST_APP
        val updated = overview.copy(authenticatorApps = overview.authenticatorApps.filterNot { it.id == appId })
        dummyOverviewOverride = updated
        _state.value = MultiFactorAuthenticationStoreState(overview = updated)
        AuthenticatorAppMutationResult.SUCCESS
    }
}

class DummyMultiFactorAuthenticationLoader @Inject constructor() {
    suspend operator fun invoke(): MultiFactorAuthenticationOverview {
        delay(DUMMY_LOADING_DELAY_MILLIS)
        return MultiFactorAuthenticationOverview.Disabled
    }
}
