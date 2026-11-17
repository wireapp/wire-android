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

package com.wire.android.ui.home.settings.mfa.authenticatorapps.edit

import com.wire.android.ui.home.settings.mfa.MFAOverview
import com.wire.android.ui.home.settings.mfa.MFAOverviewUpdate
import com.wire.android.ui.home.settings.mfa.MFAStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay

private const val DUMMY_RENAME_DELAY_MILLIS = 500L

class RenameAuthenticatorAppUseCase @Inject constructor() {
    suspend operator fun invoke(appId: String, name: String): AuthenticatorAppRenameResult {
        val trimmedName = name.trim()
        require(appId.isNotBlank())
        require(trimmedName.isNotEmpty())
        delay(DUMMY_RENAME_DELAY_MILLIS)
        return AuthenticatorAppRenameResult(appId, trimmedName)
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
