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

internal const val AUTHENTICATION_CODE_LENGTH = 6
private const val DUMMY_OPERATION_DELAY_MILLIS = 500L

data class MultiFactorAuthenticationEnrollment(
    val id: String,
    val account: String,
    val setupKey: String,
) {
    val authenticatorUri: String
        get() = "otpauth://totp/Wire:$account?secret=$setupKey&issuer=Wire&algorithm=SHA1&digits=6&period=30"
}

/** Local prototype operations. No real password validation or enrollment is performed. */
class StartMultiFactorAuthenticationSetupUseCase @Inject constructor() {
    suspend operator fun invoke(password: String): MultiFactorAuthenticationEnrollment {
        require(password.isNotBlank())
        delay(DUMMY_OPERATION_DELAY_MILLIS)
        return MultiFactorAuthenticationEnrollment(
            id = UUID.randomUUID().toString(),
            account = "demo@wire.example",
            setupKey = "JBSWY3DPEHPK3PXP",
        )
    }
}

class VerifyMultiFactorAuthenticationSetupCodeUseCase @Inject constructor() {
    suspend operator fun invoke(code: String): List<String>? {
        delay(DUMMY_OPERATION_DELAY_MILLIS)
        if (code != "284913") return null
        return listOf(
            "k7qm2-xpd9f-hwtr4", "b3np8-vz6x2-kem5q", "a7tdr-jw9hp-q4c6y", "hnm8v-s3bkf-e5gaw", "z2rh6-uyc9p-mr3zx",
            "t7ldq-4fn2k-w8bpe", "c6vyj-3hm7a-r9xts", "g5kd2-ne8wq-u4pzm", "y3fb7-lc6rv-h2jdk", "s9tx4-mw5ge-a8qnb",
        )
    }
}
