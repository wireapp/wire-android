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

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.feature.analytics.model.AnalyticsProfileProperties
import com.wire.android.feature.analytics.model.AnalyticsResult
import com.wire.android.util.isHostValidForAnalytics
import com.wire.kalium.logic.data.analytics.AnalyticsIdentifierResult
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.analytics.AnalyticsContactsData
import com.wire.kalium.logic.feature.analytics.AnalyticsIdentifierManager
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
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

@Suppress("FunctionNaming", "LongParameterList")
fun ObserveCurrentSessionAnalyticsUseCase(
    currentSessionFlow: Flow<CurrentSessionResult>,
    getAnalyticsContactsData: suspend (UserId) -> AnalyticsContactsData,
    observeAnalyticsTrackingIdentifierStatusFlow: suspend (UserId) -> Flow<AnalyticsIdentifierResult>,
    analyticsIdentifierManagerProvider: (UserId) -> AnalyticsIdentifierManager,
    userDataStoreProvider: UserDataStoreProvider,
    globalDataStore: GlobalDataStore,
    currentBackend: suspend (UserId) -> SelfServerConfigUseCase.Result
) = object : ObserveCurrentSessionAnalyticsUseCase {

    private var previousAnalyticsResult: AnalyticsIdentifierResult? = null

    @Suppress("LongMethod")
    override fun invoke(): Flow<AnalyticsResult<AnalyticsIdentifierManager>> {
        return combine(
            currentSessionFlow,
            globalDataStore.isAnonymousRegistrationEnabled()
        ) { currentSession, isAnonymousRegistrationEnabled ->
            currentSession to isAnonymousRegistrationEnabled
        }.flatMapLatest { (currentSession, isAnonymousRegistrationEnabled) ->
            if (isAnonymousRegistrationEnabled) {
                println("ym. getting anonymous registration track id")
                val anonymousRegistrationTrackId = globalDataStore.getOrCreateAnonymousRegistrationTrackId()
                return@flatMapLatest flowOf(
                    AnalyticsResult<AnalyticsIdentifierManager>(
                        identifierResult = AnalyticsIdentifierResult.RegistrationIdentifier(anonymousRegistrationTrackId),
                        profileProperties = {
                            AnalyticsProfileProperties(
                                isTeamMember = false,
                                teamId = null,
                                contactsAmount = null,
                                teamMembersAmount = null,
                                isEnterprise = null
                            )
                        },
                        manager = null
                    )
                )
            }

            if (currentSession is CurrentSessionResult.Success && currentSession.accountInfo.isValid()) {
                println("ym. getting current session track id")
                val userId = currentSession.accountInfo.userId
                val analyticsIdentifierManager = analyticsIdentifierManagerProvider(userId)
                combine(
                    observeAnalyticsTrackingIdentifierStatusFlow(userId)
                        .filter { currentIdentifierResult ->
                            val currentResult = (currentIdentifierResult as? AnalyticsIdentifierResult.Enabled)
                            val previousResult = (previousAnalyticsResult as? AnalyticsIdentifierResult.Enabled)

                            currentIdentifierResult != previousAnalyticsResult &&
                                    currentResult?.identifier != previousResult?.identifier
                        },
                    userDataStoreProvider.getOrCreate(userId).isAnonymousUsageDataEnabled(),
                ) { analyticsIdentifierResult, enabled ->
                    previousAnalyticsResult = analyticsIdentifierResult

                    val isProdBackend = when (val serverConfig = currentBackend(userId)) {
                        is SelfServerConfigUseCase.Result.Success -> serverConfig.serverLinks.isHostValidForAnalytics()
                        is SelfServerConfigUseCase.Result.Failure -> false
                    }

                    val identifierResult = if (enabled && isProdBackend) {
                        analyticsIdentifierResult
                    } else {
                        AnalyticsIdentifierResult.Disabled
                    }

                    AnalyticsResult(
                        identifierResult = identifierResult,
                        profileProperties = {
                            getAnalyticsContactsData(userId).let { analyticsContactsData ->
                                AnalyticsProfileProperties(
                                    isTeamMember = analyticsContactsData.isTeamMember,
                                    teamId = analyticsContactsData.teamId,
                                    contactsAmount = analyticsContactsData.contactsSize,
                                    teamMembersAmount = analyticsContactsData.teamSize,
                                    isEnterprise = analyticsContactsData.isEnterprise
                                )
                            }
                        },
                        manager = analyticsIdentifierManager
                    )
                }
            } else {
                println("ym. no trackid for this guy")
                flowOf(
                    AnalyticsResult<AnalyticsIdentifierManager>(
                        identifierResult = AnalyticsIdentifierResult.Disabled,
                        profileProperties = {
                            AnalyticsProfileProperties(
                                isTeamMember = false,
                                teamId = null,
                                contactsAmount = null,
                                teamMembersAmount = null,
                                isEnterprise = null
                            )
                        },
                        manager = null
                    )
                )
            }
        }.distinctUntilChanged()
    }
}
