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
package com.wire.android.session

import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.PrepareUserSessionResult
import com.wire.kalium.logic.UserSessionPreparationFailure
import com.wire.kalium.logic.UserSessionPreparationState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow

/**
 * The Android-side boundary for every operation that needs a user database.
 *
 * This class intentionally does not start or cache a second migration operation. Each caller
 * delegates to Kalium, which single-flights concurrent preparation for the physical database.
 */
@SingleIn(AppScope::class)
class UserSessionPreparationGate @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
) {
    suspend fun prepare(userId: UserId): AppUserSessionPreparationResult =
        coreLogic.prepareUserSession(userId).toAppResult()

    fun observe(userId: UserId): Flow<UserSessionPreparationState> =
        coreLogic.observeUserSessionPreparation(userId)
}

sealed interface AppUserSessionPreparationResult {
    data class Ready(val sessionScope: UserSessionScope) : AppUserSessionPreparationResult

    data class Failed(
        val reason: UserSessionPreparationFailure,
        val canRetry: Boolean,
    ) : AppUserSessionPreparationResult
}

internal fun PrepareUserSessionResult.toAppResult(): AppUserSessionPreparationResult = when (this) {
    is PrepareUserSessionResult.Success -> AppUserSessionPreparationResult.Ready(sessionScope)
    is PrepareUserSessionResult.Failure -> AppUserSessionPreparationResult.Failed(
        reason = reason,
        canRetry = reason is UserSessionPreparationFailure.InsufficientStorage ||
                reason is UserSessionPreparationFailure.TemporarilyUnavailable,
    )
}
