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

package com.wire.android.di

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.mfa.CompleteMFASetupUseCase
import com.wire.kalium.logic.feature.mfa.MFAScope
import com.wire.kalium.logic.feature.mfa.StartMFASetupUseCase
import com.wire.kalium.logic.feature.mfa.VerifyMFASetupCodeUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
class MFAUseCaseModule {
    @Provides
    fun scope(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount account: UserId): MFAScope =
        coreLogic.getSessionScope(account).mfa

    @Provides
    fun startSetup(scope: MFAScope): StartMFASetupUseCase = scope.startSetup

    @Provides
    fun verifySetup(scope: MFAScope): VerifyMFASetupCodeUseCase = scope.verifySetupCode

    @Provides
    fun completeSetup(scope: MFAScope): CompleteMFASetupUseCase = scope.completeSetup
}
