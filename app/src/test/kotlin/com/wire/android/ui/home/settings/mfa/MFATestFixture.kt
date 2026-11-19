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

import com.wire.kalium.logic.data.mfa.MFAEnrollment
import com.wire.kalium.logic.data.mfa.MFAFailure
import com.wire.kalium.logic.feature.mfa.CompleteMFASetupResult
import com.wire.kalium.logic.feature.mfa.CompleteMFASetupUseCase
import com.wire.kalium.logic.feature.mfa.StartMFASetupResult
import com.wire.kalium.logic.feature.mfa.StartMFASetupUseCase
import com.wire.kalium.logic.feature.mfa.VerifyMFASetupCodeResult
import com.wire.kalium.logic.feature.mfa.VerifyMFASetupCodeUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay

/** Canned Kalium responses for MFA presentation tests. */
internal class MFATestFixture {
    val enrollment = MFAEnrollment(
        id = "enrollment",
        account = "user@wire.example",
        setupKey = "test-key",
        authenticatorUri = "otpauth://totp/Wire:user%40wire.example?secret=test-key&issuer=Wire",
    )
    val codes = List(10) { "recovery-$it" }
    val startSetup = mockk<StartMFASetupUseCase>()
    val verifySetup = mockk<VerifyMFASetupCodeUseCase>()
    val completeSetup = mockk<CompleteMFASetupUseCase>()

    init {
        coEvery { startSetup(any()) } coAnswers { delayed(StartMFASetupResult.Success(enrollment)) }
        coEvery { verifySetup(any(), any()) } coAnswers { delayed(VerifyMFASetupCodeResult.Success(codes)) }
        coEvery { verifySetup(any(), "000000") } coAnswers {
            delayed(VerifyMFASetupCodeResult.Failure(MFAFailure.INVALID_CODE))
        }
        coEvery { completeSetup(any(), any()) } coAnswers { delayed(CompleteMFASetupResult.Success) }
    }

    private suspend fun <T> delayed(result: T): T {
        delay(500)
        return result
    }
}
