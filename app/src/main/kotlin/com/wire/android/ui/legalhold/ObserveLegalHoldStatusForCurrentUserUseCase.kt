/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.legalhold

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.legalhold.banner.LegalHoldUIState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.legalhold.LegalHoldState
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldRequestUseCase
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldRequestUseCase.Result.LegalHoldRequestAvailable
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ObserveLegalHoldStatusForCurrentUserUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke(): Flow<LegalHoldUIState> = withContext(dispatchers.io()) {
        coreLogic.getGlobalScope().session.currentSessionFlow()
            .flatMapLatest {
                when (it) {
                    is CurrentSessionResult.Success -> coreLogic.sessionScope(it.accountInfo.userId) {
                        combine(
                            observeLegalHoldRequest(),
                            observeLegalHoldForSelfUser()
                        ) { legalHoldRequestStatus: ObserveLegalHoldRequestUseCase.Result, legalHoldStatus: LegalHoldState ->
                            when {
                                legalHoldRequestStatus is LegalHoldRequestAvailable -> LegalHoldUIState.Pending
                                legalHoldStatus is LegalHoldState.Enabled -> LegalHoldUIState.Active
                                else -> LegalHoldUIState.None
                            }
                        }
                    }
                    else -> flowOf(LegalHoldUIState.None)
                }
            }
            .distinctUntilChanged()
    }
}
