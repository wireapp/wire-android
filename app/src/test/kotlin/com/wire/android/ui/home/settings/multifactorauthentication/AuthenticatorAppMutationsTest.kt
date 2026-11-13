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

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthenticatorAppMutationsTest {
    private val primary = AuthenticatorApp("primary", "Primary", LocalDate(2026, 10, 14))
    private val overview = MultiFactorAuthenticationOverview.Enabled(listOf(primary), RecoveryCodesStatus(3, 10))

    @Test
    fun `adding and removing preserve other apps and recovery codes across refreshes`() = runTest {
        val store = MultiFactorAuthenticationStore { overview }
        store.refresh()
        assertEquals(AuthenticatorAppMutationResult.SUCCESS, store.addAuthenticatorApp(AuthenticatorAppAddedResult("backup", " Backup ")))
        val added = store.state.value.overview as MultiFactorAuthenticationOverview.Enabled
        assertEquals(primary, added.authenticatorApps.first())
        assertEquals("Backup", added.authenticatorApps.last().name)
        assertEquals(overview.recoveryCodesStatus, added.recoveryCodesStatus)
        store.refresh()
        assertEquals(added, store.state.value.overview)

        assertEquals(AuthenticatorAppMutationResult.SUCCESS, store.removeAuthenticatorApp(primary.id))
        val removed = store.state.value.overview as MultiFactorAuthenticationOverview.Enabled
        assertEquals(listOf(added.authenticatorApps.last()), removed.authenticatorApps)
        assertEquals(overview.recoveryCodesStatus, removed.recoveryCodesStatus)
        store.refresh()
        assertEquals(removed, store.state.value.overview)
        assertEquals(AuthenticatorAppMutationResult.LAST_APP, store.removeAuthenticatorApp("backup"))
        assertEquals(removed, store.state.value.overview)
    }

    @Test
    fun `the store enforces the five-app limit and replayed results do not duplicate an app`() = runTest {
        val store = MultiFactorAuthenticationStore { overview }
        store.refresh()
        repeat(4) { index ->
            assertEquals(
                AuthenticatorAppMutationResult.SUCCESS,
                store.addAuthenticatorApp(AuthenticatorAppAddedResult("backup-$index", "Backup $index")),
            )
        }
        val full = store.state.value.overview as MultiFactorAuthenticationOverview.Enabled
        assertEquals(5, full.authenticatorApps.size)
        assertEquals(AuthenticatorAppMutationResult.LIMIT_REACHED, store.addAuthenticatorApp(AuthenticatorAppAddedResult("extra", "Extra")))
        assertEquals(AuthenticatorAppMutationResult.SUCCESS, store.addAuthenticatorApp(AuthenticatorAppAddedResult("backup-0", "Backup 0")))
        assertEquals(full, store.state.value.overview)
    }

    @Test
    fun `concurrent removals cannot remove the final authenticator`() = runTest {
        val store = MultiFactorAuthenticationStore {
            overview.copy(authenticatorApps = listOf(primary, primary.copy(id = "backup")))
        }
        store.refresh()
        val first = async { store.removeAuthenticatorApp("primary") }
        val second = async { store.removeAuthenticatorApp("backup") }

        assertEquals(
            setOf(AuthenticatorAppMutationResult.SUCCESS, AuthenticatorAppMutationResult.LAST_APP),
            setOf(first.await(), second.await()),
        )
        val remaining = store.state.value.overview as MultiFactorAuthenticationOverview.Enabled
        assertEquals(1, remaining.authenticatorApps.size)
        assertEquals(overview.recoveryCodesStatus, remaining.recoveryCodesStatus)
    }

    @Test
    fun `stale operations do not change unknown apps or re-enable MFA`() = runTest {
        val store = MultiFactorAuthenticationStore { overview }
        store.refresh()
        assertEquals(AuthenticatorAppMutationResult.NOT_FOUND, store.removeAuthenticatorApp("unknown"))
        assertEquals(overview, store.state.value.overview)
        store.completeTurnOff()
        assertEquals(
            AuthenticatorAppMutationResult.MFA_DISABLED,
            store.addAuthenticatorApp(AuthenticatorAppAddedResult("backup", "Backup")),
        )
        assertEquals(AuthenticatorAppMutationResult.MFA_DISABLED, store.removeAuthenticatorApp(primary.id))
        assertEquals(MultiFactorAuthenticationOverview.Disabled, store.state.value.overview)
    }
}
