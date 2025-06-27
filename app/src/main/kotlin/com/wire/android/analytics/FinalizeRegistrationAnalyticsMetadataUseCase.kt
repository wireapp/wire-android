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
package com.wire.android.analytics

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Finalize the registration process and analytics metadata in case there was enabled in the process.
 * Transfers the identifier to the logged in user and clean up the registration process.
 */
class FinalizeRegistrationAnalyticsMetadataUseCase @Inject constructor(
    private val globalDataStore: GlobalDataStore,
    @CurrentAccount private val currentAccount: UserId,
    @KaliumCoreLogic private val coreLogic: CoreLogic
) {
    suspend operator fun invoke() {
        if (globalDataStore.isAnonymousRegistrationEnabled().firstOrNull() == false) return

        val trackId = globalDataStore.getAnonymousRegistrationTrackId()
        if (!trackId.isNullOrBlank()) {
            coreLogic.getSessionScope(currentAccount).setNewUserTrackingIdentifier(trackId)
            globalDataStore.clearAnonymousRegistrationTrackId()
            globalDataStore.setAnonymousRegistrationEnabled(false)
        }
    }
}
