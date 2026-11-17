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

package com.wire.android.ui.home.settings.mfa.authenticatorapps.remove

import com.wire.android.ui.home.settings.mfa.MFAOverview
import com.wire.android.ui.home.settings.mfa.MFAOverviewUpdate
import com.wire.android.ui.home.settings.mfa.MFAStore
import com.wire.android.ui.home.settings.mfa.authenticatorapps.AuthenticatorAppMutationResult
import com.wire.android.ui.home.settings.mfa.setup.DUMMY_TOTP_CODE
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay

private const val DUMMY_APP_OPERATION_DELAY_MILLIS = 500L

class RemoveAuthenticatorAppUseCase @Inject constructor() {
    suspend operator fun invoke(appId: String, password: String, code: String): Boolean {
        require(appId.isNotBlank())
        require(password.isNotBlank())
        delay(DUMMY_APP_OPERATION_DELAY_MILLIS)
        return code == DUMMY_TOTP_CODE
    }
}

class CompleteAuthenticatorAppRemovalUseCase(private val store: MFAStore) {
    suspend operator fun invoke(appId: String): AuthenticatorAppMutationResult = store.updateOverview { current ->
        val overview = current as? MFAOverview.Enabled
            ?: return@updateOverview MFAOverviewUpdate(result = AuthenticatorAppMutationResult.MFA_DISABLED)
        if (overview.authenticatorApps.none { it.id == appId }) {
            return@updateOverview MFAOverviewUpdate(result = AuthenticatorAppMutationResult.NOT_FOUND)
        }
        if (overview.authenticatorApps.size <= 1) {
            return@updateOverview MFAOverviewUpdate(result = AuthenticatorAppMutationResult.LAST_APP)
        }
        MFAOverviewUpdate(
            overview = overview.copy(authenticatorApps = overview.authenticatorApps.filterNot { it.id == appId }),
            result = AuthenticatorAppMutationResult.SUCCESS,
        )
    }
}
