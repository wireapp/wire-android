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
import java.util.UUID
import kotlinx.coroutines.delay

private const val DUMMY_RECOVERY_OPERATION_DELAY_MILLIS = 500L
private const val RECOVERY_CODE_COUNT = 10
private const val RECOVERY_CODE_LENGTH = 15
private const val RECOVERY_CODE_GROUP_LENGTH = 5

class ConfirmMultiFactorAuthenticationPasswordUseCase @Inject constructor() {
    suspend operator fun invoke(password: String) {
        require(password.isNotBlank())
        delay(DUMMY_RECOVERY_OPERATION_DELAY_MILLIS)
    }
}

/** Generates fresh prototype values, not credentials accepted by a backend. */
class GenerateMultiFactorAuthenticationRecoveryCodesUseCase @Inject constructor() {
    suspend operator fun invoke(): List<String> {
        delay(DUMMY_RECOVERY_OPERATION_DELAY_MILLIS)
        return List(RECOVERY_CODE_COUNT) {
            UUID.randomUUID().toString().replace("-", "").take(RECOVERY_CODE_LENGTH)
                .chunked(RECOVERY_CODE_GROUP_LENGTH).joinToString("-")
        }
    }
}
