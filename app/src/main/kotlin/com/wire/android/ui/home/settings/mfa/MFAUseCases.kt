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

package com.wire.android.ui.home.settings.mfa

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class ObserveMFAOverviewUseCase(
    private val store: MFAStore,
) {
    operator fun invoke(): Flow<MFAStoreState> = store.state
}

class RefreshMFAOverviewUseCase(
    private val store: MFAStore,
    private val loadOverview: suspend () -> MFAOverview,
) {
    @Suppress("TooGenericExceptionCaught")
    suspend operator fun invoke() = store.withRefresh {
        store.setRefreshStatus(isRefreshing = true, hasRefreshError = false)
        var hasRefreshError = false
        try {
            store.publishRefreshedOverview(loadOverview())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            hasRefreshError = true
        } finally {
            store.setRefreshStatus(isRefreshing = false, hasRefreshError = hasRefreshError)
        }
    }
}

class CompleteMFASetupUseCase(private val store: MFAStore) {
    suspend operator fun invoke(result: MFASetupResult) = store.updateOverview {
        val overview = MFAOverview.Enabled(
            authenticatorApps = listOf(
                AuthenticatorApp(
                    id = result.authenticatorId,
                    name = result.authenticatorName,
                    addedOn = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
                )
            ),
            recoveryCodesStatus = RecoveryCodesStatus(result.recoveryCodesCount, result.recoveryCodesCount),
        )
        MFAOverviewUpdate(overview, Unit)
    }
}

class UpdateMFARecoveryCodesStatusUseCase(private val store: MFAStore) {
    suspend operator fun invoke(result: MFARecoveryCodesResult) = store.updateOverview { current ->
        val overview = current as? MFAOverview.Enabled
        MFAOverviewUpdate(
            overview = overview?.copy(recoveryCodesStatus = RecoveryCodesStatus(result.generatedCount, result.generatedCount)),
            result = Unit,
        )
    }
}

class CompleteMFATurnOffUseCase(private val store: MFAStore) {
    suspend operator fun invoke() = store.updateOverview {
        MFAOverviewUpdate(MFAOverview.Disabled, Unit)
    }
}

class CompleteAuthenticatorAppRenameUseCase(private val store: MFAStore) {
    suspend operator fun invoke(result: AuthenticatorAppRenameResult) = store.updateOverview { current ->
        val overview = current as? MFAOverview.Enabled
            ?: return@updateOverview MFAOverviewUpdate(result = Unit)
        if (overview.authenticatorApps.none { it.id == result.appId }) return@updateOverview MFAOverviewUpdate(result = Unit)
        val trimmedName = result.name.trim()
        require(trimmedName.isNotEmpty())
        MFAOverviewUpdate(
            overview = overview.copy(
                authenticatorApps = overview.authenticatorApps.map { app ->
                    if (app.id == result.appId) app.copy(name = trimmedName) else app
                },
            ),
            result = Unit,
        )
    }
}
