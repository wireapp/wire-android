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

import com.wire.kalium.logic.data.mfa.AuthenticatorApp
import com.wire.kalium.logic.data.mfa.MFAEnrollment
import com.wire.kalium.logic.data.mfa.MFAFailure
import com.wire.kalium.logic.feature.mfa.CompleteMFASetupResult
import com.wire.kalium.logic.feature.mfa.CompleteMFASetupUseCase
import com.wire.kalium.logic.feature.mfa.ConfirmMFAPasswordResult
import com.wire.kalium.logic.feature.mfa.ConfirmMFAPasswordUseCase
import com.wire.kalium.logic.feature.mfa.FinishAddingAuthenticatorAppResult
import com.wire.kalium.logic.feature.mfa.FinishAddingAuthenticatorAppUseCase
import com.wire.kalium.logic.feature.mfa.GenerateMFARecoveryCodesResult
import com.wire.kalium.logic.feature.mfa.GenerateMFARecoveryCodesUseCase
import com.wire.kalium.logic.feature.mfa.RemoveAuthenticatorAppResult
import com.wire.kalium.logic.feature.mfa.RemoveAuthenticatorAppUseCase
import com.wire.kalium.logic.feature.mfa.RenameAuthenticatorAppResult
import com.wire.kalium.logic.feature.mfa.RenameAuthenticatorAppUseCase
import com.wire.kalium.logic.feature.mfa.StartMFASetupResult
import com.wire.kalium.logic.feature.mfa.StartMFASetupUseCase
import com.wire.kalium.logic.feature.mfa.TurnOffMFAResult
import com.wire.kalium.logic.feature.mfa.TurnOffMFAUseCase
import com.wire.kalium.logic.feature.mfa.VerifyAuthenticatorAppCodeResult
import com.wire.kalium.logic.feature.mfa.VerifyAuthenticatorAppCodeUseCase
import com.wire.kalium.logic.feature.mfa.VerifyMFASetupCodeResult
import com.wire.kalium.logic.feature.mfa.VerifyMFASetupCodeUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate

/** Canned Kalium responses for MFA presentation tests. */
internal class MFATestFixture {
    val enrollment = MFAEnrollment("enrollment", "user@wire.example", "test-key")
    val codes = List(10) { "recovery-$it" }
    val app = AuthenticatorApp("phone", "Phone", LocalDate(2026, 10, 14))
    val startSetup = mockk<StartMFASetupUseCase>()
    val verifySetup = mockk<VerifyMFASetupCodeUseCase>()
    val completeSetup = mockk<CompleteMFASetupUseCase>()
    val verifyAuthenticator = mockk<VerifyAuthenticatorAppCodeUseCase>()
    val finishAdding = mockk<FinishAddingAuthenticatorAppUseCase>()
    val rename = mockk<RenameAuthenticatorAppUseCase>()
    val remove = mockk<RemoveAuthenticatorAppUseCase>()
    val confirmPassword = mockk<ConfirmMFAPasswordUseCase>()
    val generateCodes = mockk<GenerateMFARecoveryCodesUseCase>()
    val turnOff = mockk<TurnOffMFAUseCase>()

    init {
        coEvery { startSetup(any()) } coAnswers { delayed(StartMFASetupResult.Success(enrollment)) }
        coEvery { verifySetup(any(), any()) } coAnswers { delayed(VerifyMFASetupCodeResult.Success(codes)) }
        coEvery { verifySetup(any(), "000000") } coAnswers {
            delayed(VerifyMFASetupCodeResult.Failure(MFAFailure.INVALID_CODE))
        }
        coEvery { completeSetup(any(), any()) } coAnswers { delayed(CompleteMFASetupResult.Success) }
        coEvery { verifyAuthenticator(any(), any()) } coAnswers { delayed(VerifyAuthenticatorAppCodeResult.Success) }
        coEvery { verifyAuthenticator(any(), "000000") } coAnswers {
            delayed(VerifyAuthenticatorAppCodeResult.Failure(MFAFailure.INVALID_CODE))
        }
        coEvery { finishAdding(any(), any()) } coAnswers {
            delay(500)
            FinishAddingAuthenticatorAppResult.Success(app.copy(id = firstArg(), name = secondArg()))
        }
        coEvery { rename(any(), any()) } coAnswers {
            delay(500)
            RenameAuthenticatorAppResult.Success(app.copy(id = firstArg(), name = secondArg()))
        }
        coEvery { remove(any(), any(), any()) } coAnswers { delayed(RemoveAuthenticatorAppResult.Success) }
        coEvery { remove(any(), any(), "000000") } coAnswers {
            delayed(RemoveAuthenticatorAppResult.Failure(MFAFailure.INVALID_CODE))
        }
        coEvery { confirmPassword(any()) } coAnswers { delayed(ConfirmMFAPasswordResult.Success) }
        coEvery { generateCodes() } coAnswers { delayed(GenerateMFARecoveryCodesResult.Success(codes)) }
        coEvery { turnOff(any(), any()) } coAnswers { delayed(TurnOffMFAResult.Success) }
        coEvery { turnOff(any(), "000000") } coAnswers { delayed(TurnOffMFAResult.Failure(MFAFailure.INVALID_CODE)) }
    }

    private suspend fun <T> delayed(result: T): T {
        delay(500)
        return result
    }
}
