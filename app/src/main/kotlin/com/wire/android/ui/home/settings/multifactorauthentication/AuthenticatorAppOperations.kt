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
import kotlinx.coroutines.delay

private const val DUMMY_APP_OPERATION_DELAY_MILLIS = 500L

/** Checks the prototype code without generating or replacing recovery codes. */
class VerifyAuthenticatorAppCodeUseCase @Inject constructor() {
    suspend operator fun invoke(code: String): Boolean {
        delay(DUMMY_APP_OPERATION_DELAY_MILLIS)
        return code == "284913"
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
        return code == "284913"
    }
}

class CompleteAuthenticatorAppAdditionUseCase(private val store: MultiFactorAuthenticationStore) {
    suspend operator fun invoke(result: AuthenticatorAppAddedResult): AuthenticatorAppMutationResult = store.addAuthenticatorApp(result)
}

class CompleteAuthenticatorAppRemovalUseCase(private val store: MultiFactorAuthenticationStore) {
    suspend operator fun invoke(appId: String): AuthenticatorAppMutationResult = store.removeAuthenticatorApp(appId)
}
