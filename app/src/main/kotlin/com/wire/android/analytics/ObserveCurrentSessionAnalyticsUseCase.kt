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
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.analytics.AnalyticsIdentifierResult
import com.wire.kalium.logic.feature.analytics.AnalyticsIdentifierManager
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive

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
    coreLogic: CoreLogic,
    userDataStoreProvider: UserDataStoreProvider,
    globalDataStore: GlobalDataStore,
) = object : ObserveCurrentSessionAnalyticsUseCase {

    @Suppress("LongMethod")
    override fun invoke(): Flow<AnalyticsResult<AnalyticsIdentifierManager>> = combine(
        currentSessionFlow,
        globalDataStore.isAnonymousRegistrationEnabled(),
    ) { currentSession, registrationEnabled -> currentSession to registrationEnabled }
        .flatMapLatest { (currentSession, registrationEnabled) ->
            if (registrationEnabled) {
                val trackId = globalDataStore.getOrCreateAnonymousRegistrationTrackId()
                flowOf(
                    AnalyticsResult<AnalyticsIdentifierManager>(
                        AnalyticsIdentifierResult.RegistrationIdentifier(trackId),
                        profileProperties = { EMPTY_ANALYTICS_PROFILE },
                        manager = null,
                    )
                )
            } else {
                val account = (currentSession as? CurrentSessionResult.Success)?.accountInfo
                if (account?.isValid() == true) {
                    coreLogic.observeValidSession<AnalyticsResult<AnalyticsIdentifierManager>>(account.userId) {
                        val session = this
                        channelFlow {
                            // Reset on each admitted generation, even if the identifier and AccountInfo are unchanged.
                            var previousIdentifier: AnalyticsIdentifierResult? = null
                            val manager = admittedAnalyticsManager(session.analyticsIdentifierManager)
                            combine(
                                session.observeAnalyticsTrackingIdentifierStatus().filter { identifier ->
                                    val current = identifier as? AnalyticsIdentifierResult.Enabled
                                    val previous = previousIdentifier as? AnalyticsIdentifierResult.Enabled
                                    identifier != previousIdentifier && current?.identifier != previous?.identifier
                                },
                                userDataStoreProvider.getOrCreate(account.userId).isAnonymousUsageDataEnabled(),
                            ) { identifier, enabled ->
                                previousIdentifier = identifier
                                val production = when (val config = session.users.serverLinks()) {
                                    is SelfServerConfigUseCase.Result.Success -> config.serverLinks.isHostValidForAnalytics()
                                    is SelfServerConfigUseCase.Result.Failure -> false
                                }
                                // Analytics consumes this callback later: emit a value snapshot, never a resource read.
                                val contacts = session.getAnalyticsContactsData()
                                val profile = AnalyticsProfileProperties(
                                    isTeamMember = contacts.isTeamMember,
                                    teamId = contacts.teamId,
                                    contactsAmount = contacts.contactsSize,
                                    teamMembersAmount = contacts.teamSize,
                                    isEnterprise = contacts.isEnterprise,
                                )
                                AnalyticsResult(
                                    identifierResult = if (enabled && production) identifier else AnalyticsIdentifierResult.Disabled,
                                    profileProperties = { profile },
                                    manager = manager,
                                )
                            }.collect { send(it) }
                        }
                    }.map { it ?: inactiveAnalyticsResult() }
                } else {
                    flowOf(inactiveAnalyticsResult())
                }
            }
        }.distinctUntilChanged()
}

private val EMPTY_ANALYTICS_PROFILE = AnalyticsProfileProperties(false, null, null, null, null)

private fun inactiveAnalyticsResult() = AnalyticsResult<AnalyticsIdentifierManager>(
    identifierResult = AnalyticsIdentifierResult.Disabled,
    profileProperties = { EMPTY_ANALYTICS_PROFILE },
    manager = null,
)

/** Deferred analytics commands remain children of the exact observation that supplied the manager. */
private fun CoroutineScope.admittedAnalyticsManager(delegate: AnalyticsIdentifierManager): AnalyticsIdentifierManager =
    object : AnalyticsIdentifierManager {
        override suspend fun onMigrationComplete() = runAnalyticsAction { delegate.onMigrationComplete() }

        override suspend fun propagateTrackingIdentifier(identifier: String) =
            runAnalyticsAction { delegate.propagateTrackingIdentifier(identifier) }
    }

private suspend fun CoroutineScope.runAnalyticsAction(action: suspend () -> Unit) {
    currentCoroutineContext().ensureActive()
    if (!isActive) return
    val command = async { action() }
    try {
        command.await()
    } catch (cancellation: CancellationException) {
        currentCoroutineContext().ensureActive()
        if (isActive) throw cancellation
        // The supplying observation was revoked; a stale analytics callback has no work to do.
    } finally {
        command.cancel()
    }
}
