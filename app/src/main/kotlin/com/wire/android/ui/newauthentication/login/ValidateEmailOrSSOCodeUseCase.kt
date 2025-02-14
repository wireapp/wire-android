/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.newauthentication.login

import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.sso.ValidateSSOCodeResult
import com.wire.kalium.logic.feature.auth.sso.ValidateSSOCodeUseCase
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/**
 * Validates the input for a SSO code or an email address valid format.
 */
@ViewModelScoped
class ValidateEmailOrSSOCodeUseCase @Inject constructor(
    val validateEmail: ValidateEmailUseCase,
    val validateSSOCode: ValidateSSOCodeUseCase
) {
    /**
     * Validates the input for a SSO code or an email address valid format.
     */
    operator fun invoke(input: String): Result {
        return when {
            input.startsWith(ValidateSSOCodeUseCase.SSO_CODE_WIRE_PREFIX) -> {
                if (validateSSOCode(input) is ValidateSSOCodeResult.Valid) {
                    Result.ValidSSOCode
                } else {
                    Result.InvalidInput
                }
            }

            validateEmail(input) -> {
                Result.ValidEmail
            }

            else -> Result.InvalidInput
        }
    }

    sealed class Result {
        data object ValidSSOCode : Result()
        data object ValidEmail : Result()
        data object InvalidInput : Result()
    }
}
