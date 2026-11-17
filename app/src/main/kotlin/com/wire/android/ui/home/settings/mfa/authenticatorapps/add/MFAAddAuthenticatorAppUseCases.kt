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

package com.wire.android.ui.home.settings.mfa.authenticatorapps.add

import com.wire.android.ui.home.settings.mfa.AuthenticatorApp
import com.wire.android.ui.home.settings.mfa.MFAOverview
import com.wire.android.ui.home.settings.mfa.MFAOverviewUpdate
import com.wire.android.ui.home.settings.mfa.MFAStore
import com.wire.android.ui.home.settings.mfa.authenticatorapps.AuthenticatorAppMutationResult
import com.wire.android.ui.home.settings.mfa.authenticatorapps.MAX_AUTHENTICATOR_APPS
import com.wire.android.ui.home.settings.mfa.setup.DUMMY_TOTP_CODE
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private const val DUMMY_APP_OPERATION_DELAY_MILLIS = 500L

/** Checks the prototype code without generating or replacing recovery codes. */
class VerifyAuthenticatorAppCodeUseCase @Inject constructor() {
    suspend operator fun invoke(code: String): Boolean {
        delay(DUMMY_APP_OPERATION_DELAY_MILLIS)
        return code == DUMMY_TOTP_CODE
    }
}

class FinishAddingAuthenticatorAppUseCase @Inject constructor() {
    suspend operator fun invoke(enrollmentId: String, name: String): AuthenticatorAppAddedResult {
        require(enrollmentId.isNotBlank())
        require(name.isNotBlank())
        delay(DUMMY_APP_OPERATION_DELAY_MILLIS)
        return AuthenticatorAppAddedResult(enrollmentId, name.trim())
    }
}

class CompleteAuthenticatorAppAdditionUseCase(private val store: MFAStore) {
    suspend operator fun invoke(result: AuthenticatorAppAddedResult): AuthenticatorAppMutationResult = store.updateOverview { current ->
        val overview = current as? MFAOverview.Enabled
            ?: return@updateOverview MFAOverviewUpdate(result = AuthenticatorAppMutationResult.MFA_DISABLED)
        if (overview.authenticatorApps.any { it.id == result.appId }) {
            return@updateOverview MFAOverviewUpdate(result = AuthenticatorAppMutationResult.SUCCESS)
        }
        if (overview.authenticatorApps.size >= MAX_AUTHENTICATOR_APPS) {
            return@updateOverview MFAOverviewUpdate(result = AuthenticatorAppMutationResult.LIMIT_REACHED)
        }
        require(result.name.isNotBlank())
        val app = AuthenticatorApp(
            id = result.appId,
            name = result.name.trim(),
            addedOn = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        )
        MFAOverviewUpdate(
            overview = overview.copy(authenticatorApps = overview.authenticatorApps + app),
            result = AuthenticatorAppMutationResult.SUCCESS,
        )
    }
}
