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

class RemoveAuthenticatorAppUseCase @Inject constructor() {
    suspend operator fun invoke(appId: String, password: String, code: String): Boolean {
        require(appId.isNotBlank())
        require(password.isNotBlank())
        delay(DUMMY_APP_OPERATION_DELAY_MILLIS)
        return code == DUMMY_TOTP_CODE
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
