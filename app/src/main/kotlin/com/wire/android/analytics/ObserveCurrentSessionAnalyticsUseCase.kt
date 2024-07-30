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
package com.wire.android.analytics

import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.feature.analytics.model.AnalyticsResult
import com.wire.kalium.logic.data.analytics.AnalyticsIdentifierResult
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.analytics.AnalyticsIdentifierManager
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

interface ObserveCurrentSessionAnalyticsUseCase {

    /**
     * Observes a flow of AnalyticsResult of type AnalyticsIdentifierManager
     * returning the current result for analytics:
     * - newly generated / existing / migration
     *
     * to be used in analytics user profile device setting.
     */
    operator fun invoke(): Flow<AnalyticsResult<AnalyticsIdentifierManager>>
}

@Suppress("FunctionNaming")
fun ObserveCurrentSessionAnalyticsUseCase(
    currentSessionFlow: Flow<CurrentSessionResult>,
    isUserTeamMember: suspend (UserId) -> Boolean,
    observeAnalyticsTrackingIdentifierStatusFlow: suspend (UserId) -> Flow<AnalyticsIdentifierResult>,
    analyticsIdentifierManagerProvider: (UserId) -> AnalyticsIdentifierManager,
    userDataStoreProvider: UserDataStoreProvider
) = object : ObserveCurrentSessionAnalyticsUseCase {

    private var previousAnalyticsResult: AnalyticsIdentifierResult? = null

    override fun invoke(): Flow<AnalyticsResult<AnalyticsIdentifierManager>> =
        currentSessionFlow
        .flatMapLatest {
            if (it is CurrentSessionResult.Success && it.accountInfo.isValid()) {
                val userId = it.accountInfo.userId
                val isTeamMember = isUserTeamMember(userId)
                val analyticsIdentifierManager = analyticsIdentifierManagerProvider(userId)

                combine(
                    observeAnalyticsTrackingIdentifierStatusFlow(userId)
                        .filter { currentIdentifierResult ->
                            val currentResult = (currentIdentifierResult as? AnalyticsIdentifierResult.Enabled)
                            val previousResult = (previousAnalyticsResult as? AnalyticsIdentifierResult.Enabled)

                            currentIdentifierResult != previousAnalyticsResult &&
                                    currentResult?.identifier != previousResult?.identifier
                        },
                    userDataStoreProvider.getOrCreate(userId).isAnonymousUsageDataEnabled()
                ) { identifierResult, enabled ->
                    previousAnalyticsResult = identifierResult

                    if (enabled) {
                        AnalyticsResult(
                            identifierResult = identifierResult,
                            isTeamMember = isTeamMember,
                            manager = analyticsIdentifierManager
                        )
                    } else {
                        AnalyticsResult(
                            identifierResult = AnalyticsIdentifierResult.Disabled,
                            isTeamMember = isTeamMember,
                            manager = analyticsIdentifierManager
                        )
                    }
                }
            } else {
                flowOf(
                    AnalyticsResult<AnalyticsIdentifierManager>(
                        identifierResult = AnalyticsIdentifierResult.Disabled,
                        isTeamMember = false,
                        manager = null
                    )
                )
            }
        }
        .distinctUntilChanged()
}
