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

package com.wire.android.ui.home.settings.mfa.authenticatorapps

import com.wire.android.ui.home.settings.mfa.AuthenticatorApp
import com.wire.android.ui.home.settings.mfa.MFAOverview
import com.wire.android.ui.home.settings.mfa.MFAStore
import com.wire.android.ui.home.settings.mfa.RecoveryCodesStatus
import com.wire.android.ui.home.settings.mfa.authenticatorapps.add.AuthenticatorAppAddedResult
import com.wire.android.ui.home.settings.mfa.authenticatorapps.add.CompleteAuthenticatorAppAdditionUseCase
import com.wire.android.ui.home.settings.mfa.authenticatorapps.remove.CompleteAuthenticatorAppRemovalUseCase
import com.wire.android.ui.home.settings.mfa.overview.RefreshMFAOverviewUseCase
import com.wire.android.ui.home.settings.mfa.turnoff.CompleteMFATurnOffUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthenticatorAppMutationsTest {
    private val primary = AuthenticatorApp("primary", "Primary", LocalDate(2026, 10, 14))
    private val overview = MFAOverview.Enabled(listOf(primary), RecoveryCodesStatus(3, 10))

    @Test
    fun `adding and removing preserve other apps and recovery codes across refreshes`() = runTest {
        val store = MFAStore()
        val refresh = RefreshMFAOverviewUseCase(store) { overview }
        val addition = CompleteAuthenticatorAppAdditionUseCase(store)
        val removal = CompleteAuthenticatorAppRemovalUseCase(store)
        refresh()
        assertEquals(AuthenticatorAppMutationResult.SUCCESS, addition(AuthenticatorAppAddedResult("backup", " Backup ")))
        val added = store.state.value.overview as MFAOverview.Enabled
        assertEquals(primary, added.authenticatorApps.first())
        assertEquals("Backup", added.authenticatorApps.last().name)
        assertEquals(overview.recoveryCodesStatus, added.recoveryCodesStatus)
        refresh()
        assertEquals(added, store.state.value.overview)

        assertEquals(AuthenticatorAppMutationResult.SUCCESS, removal(primary.id))
        val removed = store.state.value.overview as MFAOverview.Enabled
        assertEquals(listOf(added.authenticatorApps.last()), removed.authenticatorApps)
        assertEquals(overview.recoveryCodesStatus, removed.recoveryCodesStatus)
        refresh()
        assertEquals(removed, store.state.value.overview)
        assertEquals(AuthenticatorAppMutationResult.LAST_APP, removal("backup"))
        assertEquals(removed, store.state.value.overview)
    }

    @Test
    fun `the addition use case enforces the five-app limit and replayed results do not duplicate an app`() = runTest {
        val store = MFAStore()
        val addition = CompleteAuthenticatorAppAdditionUseCase(store)
        RefreshMFAOverviewUseCase(store) { overview }()
        repeat(4) { index ->
            assertEquals(
                AuthenticatorAppMutationResult.SUCCESS,
                addition(AuthenticatorAppAddedResult("backup-$index", "Backup $index")),
            )
        }
        val full = store.state.value.overview as MFAOverview.Enabled
        assertEquals(5, full.authenticatorApps.size)
        assertEquals(AuthenticatorAppMutationResult.LIMIT_REACHED, addition(AuthenticatorAppAddedResult("extra", "Extra")))
        assertEquals(AuthenticatorAppMutationResult.SUCCESS, addition(AuthenticatorAppAddedResult("backup-0", "Backup 0")))
        assertEquals(full, store.state.value.overview)
    }

    @Test
    fun `concurrent removals cannot remove the final authenticator`() = runTest {
        val store = MFAStore()
        RefreshMFAOverviewUseCase(store) {
            overview.copy(authenticatorApps = listOf(primary, primary.copy(id = "backup")))
        }()
        val firstRemoval = CompleteAuthenticatorAppRemovalUseCase(store)
        val secondRemoval = CompleteAuthenticatorAppRemovalUseCase(store)
        val first = async { firstRemoval("primary") }
        val second = async { secondRemoval("backup") }

        assertEquals(
            setOf(AuthenticatorAppMutationResult.SUCCESS, AuthenticatorAppMutationResult.LAST_APP),
            setOf(first.await(), second.await()),
        )
        val remaining = store.state.value.overview as MFAOverview.Enabled
        assertEquals(1, remaining.authenticatorApps.size)
        assertEquals(overview.recoveryCodesStatus, remaining.recoveryCodesStatus)
    }

    @Test
    fun `stale operations do not change unknown apps or re-enable MFA`() = runTest {
        val store = MFAStore()
        val addition = CompleteAuthenticatorAppAdditionUseCase(store)
        val removal = CompleteAuthenticatorAppRemovalUseCase(store)
        RefreshMFAOverviewUseCase(store) { overview }()
        assertEquals(AuthenticatorAppMutationResult.NOT_FOUND, removal("unknown"))
        assertEquals(overview, store.state.value.overview)
        CompleteMFATurnOffUseCase(store)()
        assertEquals(
            AuthenticatorAppMutationResult.MFA_DISABLED,
            addition(AuthenticatorAppAddedResult("backup", "Backup")),
        )
        assertEquals(AuthenticatorAppMutationResult.MFA_DISABLED, removal(primary.id))
        assertEquals(MFAOverview.Disabled, store.state.value.overview)
    }
}
