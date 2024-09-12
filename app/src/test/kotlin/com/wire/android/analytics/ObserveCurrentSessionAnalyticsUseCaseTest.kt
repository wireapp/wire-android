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

import app.cash.turbine.test
import com.wire.android.assertIs
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.framework.TestUser
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.analytics.AnalyticsIdentifierResult
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.analytics.AnalyticsIdentifierManager
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.Test

class ObserveCurrentSessionAnalyticsUseCaseTest {

    @Test
    fun givenThereIsNoValidSession_whenObservingCurrentSessionAnalytics_thenDisabledAnalyticsResultIsReturned() = runTest {
        // given
        val (_, useCase) = Arrangement().apply {
            setCurrentSession(CurrentSessionResult.Failure.SessionNotFound)
        }.arrange()

        // when
        useCase.invoke().test {
            // then
            val item = awaitItem()
            assertIs<AnalyticsIdentifierResult.Disabled>(item.identifierResult)
            assertEquals(false, item.isTeamMember)
            assertEquals(null, item.manager)
        }
    }

    @Test
    fun givenThereIsAValidSession_whenObservingCurrentSessionAnalytics_thenExistingIdentifierAnalyticsResultIsReturned() = runTest {
        // given
        val (_, useCase) = Arrangement()
            .withIsAnonymousUsageDataEnabled(true)
            .apply {
                setCurrentSession(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.SELF_USER.id)))
                setIsTeamMember(TestUser.SELF_USER.id)
                setObservingTrackingIdentifierStatus(AnalyticsIdentifierResult.ExistingIdentifier(Arrangement.CURRENT_TRACKING_IDENTIFIER))
                setSelfServerConfig(Arrangement.SERVER_CONFIG_PRODUCTION)
            }.arrange()

        // when
        useCase.invoke().test {
            // then
            val item = awaitItem()
            assertIs<AnalyticsIdentifierResult.ExistingIdentifier>(item.identifierResult)
            assertEquals(true, item.isTeamMember)
        }
    }

    @Test
    fun givenStagingBackendApi_whenObservingCurrentSessionAnalytics_thenExistingIdentifierAnalyticsResultIsReturned() = runTest {
        // given
        val (_, useCase) = Arrangement()
            .withIsAnonymousUsageDataEnabled(true)
            .apply {
                setCurrentSession(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.SELF_USER.id)))
                setIsTeamMember(TestUser.SELF_USER.id)
                setObservingTrackingIdentifierStatus(AnalyticsIdentifierResult.ExistingIdentifier(Arrangement.CURRENT_TRACKING_IDENTIFIER))
                setSelfServerConfig(Arrangement.SERVER_CONFIG_STAGING)
            }.arrange()

        // when
        useCase.invoke().test {
            // then
            val item = awaitItem()
            assertIs<AnalyticsIdentifierResult.ExistingIdentifier>(item.identifierResult)
            assertEquals(true, item.isTeamMember)
        }
    }

    @Test
    fun givenThereIsAValidSessionAndDisabledUsageData_whenObservingCurrentSessionAnalytics_thenDisabledAnalyticsResultIsReturned() =
        runTest {
            // given
            val (_, useCase) = Arrangement()
                .withIsAnonymousUsageDataEnabled(false)
                .apply {
                    setCurrentSession(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.SELF_USER.id)))
                    setIsTeamMember(TestUser.SELF_USER.id)
                    setObservingTrackingIdentifierStatus(
                        AnalyticsIdentifierResult.ExistingIdentifier(Arrangement.CURRENT_TRACKING_IDENTIFIER)
                    )
                    setSelfServerConfig(Arrangement.SERVER_CONFIG_PRODUCTION)
                }.arrange()

            // when
            useCase.invoke().test {
                // then
                val item = awaitItem()
                assertIs<AnalyticsIdentifierResult.Disabled>(item.identifierResult)
                assertEquals(true, item.isTeamMember)
                assertEquals(true, item.manager != null)
            }
        }

    @Test
    fun givenThereIsAValidSessionButCustomBackend_whenObservingCurrentSessionAnalytics_thenDisabledAnalyticsResultIsReturned() =
        runTest {
            // given
            val (_, useCase) = Arrangement()
                .withIsAnonymousUsageDataEnabled(true)
                .apply {
                    setCurrentSession(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.SELF_USER.id)))
                    setIsTeamMember(TestUser.SELF_USER.id)
                    setObservingTrackingIdentifierStatus(
                        AnalyticsIdentifierResult.ExistingIdentifier(Arrangement.CURRENT_TRACKING_IDENTIFIER)
                    )
                    setSelfServerConfig(
                        Arrangement.SERVER_CONFIG_PRODUCTION.copy(
                            serverLinks = Arrangement.SERVER_CONFIG_PRODUCTION.serverLinks.copy(links = ServerConfig.DUMMY)
                        )
                    )
                }.arrange()

            // when
            useCase.invoke().test {
                // then
                val item = awaitItem()
                assertIs<AnalyticsIdentifierResult.Disabled>(item.identifierResult)
                assertEquals(true, item.isTeamMember)
                assertEquals(true, item.manager != null)
            }
        }

    @Test
    fun givenThereIsAValidSessionButFailureOnCustomBackend_whenObservingCurrentSessionAnalytics_thenDisabledAnalyticsResultIsReturned() =
        runTest {
            // given
            val (_, useCase) = Arrangement()
                .withIsAnonymousUsageDataEnabled(true)
                .apply {
                    setCurrentSession(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.SELF_USER.id)))
                    setIsTeamMember(TestUser.SELF_USER.id)
                    setObservingTrackingIdentifierStatus(
                        AnalyticsIdentifierResult.ExistingIdentifier(Arrangement.CURRENT_TRACKING_IDENTIFIER)
                    )
                    setSelfServerConfig(SelfServerConfigUseCase.Result.Failure(CoreFailure.Unknown(null)))
                }.arrange()

            // when
            useCase.invoke().test {
                // then
                val item = awaitItem()
                assertIs<AnalyticsIdentifierResult.Disabled>(item.identifierResult)
                assertEquals(true, item.isTeamMember)
                assertEquals(true, item.manager != null)
            }
        }

    @Test
    fun givenUserSwitchAccount_whenObservingCurrentSessionAnalytics_thenExistingIdentifierAnalyticsResultIsReturned() = runTest {
        // given
        val (arrangement, useCase) = Arrangement()
            .withIsAnonymousUsageDataEnabled(true)
            .apply {
                setCurrentSession(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.SELF_USER.id)))
                setIsTeamMember(TestUser.SELF_USER.id)
                setObservingTrackingIdentifierStatus(AnalyticsIdentifierResult.ExistingIdentifier(Arrangement.CURRENT_TRACKING_IDENTIFIER))
                setSelfServerConfig(Arrangement.SERVER_CONFIG_PRODUCTION)
            }.arrange()

        // when
        useCase.invoke().test {
            // then
            val item = awaitItem()
            assertIs<AnalyticsIdentifierResult.ExistingIdentifier>(item.identifierResult)
            assertEquals(true, item.isTeamMember)

            // when changing user
            arrangement.setCurrentSession(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.OTHER_USER.id)))
            arrangement.setObservingTrackingIdentifierStatus(
                AnalyticsIdentifierResult.ExistingIdentifier(Arrangement.OTHER_TRACKING_IDENTIFIER)
            )
            arrangement.setSelfServerConfig(Arrangement.SERVER_CONFIG_PRODUCTION)
            arrangement.withIsAnonymousUsageDataEnabled(true)

            // then
            val nextItem = awaitItem()
            assertIs<AnalyticsIdentifierResult.ExistingIdentifier>(nextItem.identifierResult)
            assertEquals(false, nextItem.isTeamMember)
        }
    }

    private class Arrangement {

        @MockK
        private lateinit var userDataStore: UserDataStore

        @MockK
        private lateinit var userDataStoreProvider: UserDataStoreProvider

        @MockK
        private lateinit var analyticsIdentifierManager: AnalyticsIdentifierManager

        private val currentSessionChannel = Channel<CurrentSessionResult>(Channel.UNLIMITED)

        private val analyticsTrackingIdentifierStatusChannel = Channel<AnalyticsIdentifierResult>(Channel.UNLIMITED)

        private val selfServerConfigChannel = Channel<SelfServerConfigUseCase.Result>(Channel.UNLIMITED)

        private val teamMembers = mutableSetOf<UserId>()

        private val isTeamMember: (UserId) -> Boolean = { teamMembers.contains(it) }

        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        suspend fun setCurrentSession(result: CurrentSessionResult) {
            currentSessionChannel.send(result)
        }

        fun setIsTeamMember(userId: UserId) {
            teamMembers.add(userId)
        }

        suspend fun setObservingTrackingIdentifierStatus(result: AnalyticsIdentifierResult) {
            analyticsTrackingIdentifierStatusChannel.send(result)
        }

        suspend fun setSelfServerConfig(result: SelfServerConfigUseCase.Result) {
            selfServerConfigChannel.send(result)
        }

        fun withIsAnonymousUsageDataEnabled(result: Boolean): Arrangement = apply {
            every { userDataStoreProvider.getOrCreate(any()) } returns userDataStore
            coEvery { userDataStore.isAnonymousUsageDataEnabled() } returns flowOf(result)
        }

        var useCase: ObserveCurrentSessionAnalyticsUseCase = ObserveCurrentSessionAnalyticsUseCase(
            currentSessionFlow = currentSessionChannel.receiveAsFlow(),
            isUserTeamMember = isTeamMember,
            observeAnalyticsTrackingIdentifierStatusFlow = {
                analyticsTrackingIdentifierStatusChannel.receiveAsFlow()
            },
            analyticsIdentifierManagerProvider = {
                analyticsIdentifierManager
            },
            userDataStoreProvider = userDataStoreProvider,
            currentBackend = {
                selfServerConfigChannel.receive()
            }
        )

        fun arrange() = this to useCase

        companion object {
            const val CURRENT_TRACKING_IDENTIFIER = "abcd-1234"
            const val OTHER_TRACKING_IDENTIFIER = "aaaa-bbbb-1234"

            val SERVER_CONFIG_PRODUCTION = SelfServerConfigUseCase.Result.Success(
                serverLinks = ServerConfig(
                    id = "server_id",
                    links = ServerConfig.PRODUCTION,
                    metaData = ServerConfig.MetaData(
                        federation = false,
                        commonApiVersion = CommonApiVersionType.New,
                        domain = null
                    )
                )
            )

            val SERVER_CONFIG_STAGING = SERVER_CONFIG_PRODUCTION.copy(
                serverLinks = SERVER_CONFIG_PRODUCTION.serverLinks.copy(
                    links = ServerConfig.STAGING
                )
            )
        }
    }
}
