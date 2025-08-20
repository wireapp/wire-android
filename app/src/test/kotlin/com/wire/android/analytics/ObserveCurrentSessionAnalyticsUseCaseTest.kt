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
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.feature.analytics.model.AnalyticsProfileProperties
import com.wire.android.framework.TestUser
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.analytics.AnalyticsIdentifierResult
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.analytics.AnalyticsContactsData
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
import org.junit.jupiter.api.Assertions.assertEquals
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
            assertEquals(false, item.profileProperties().isTeamMember)
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
                setAnalyticsContactsData(TestUser.SELF_USER.id, Arrangement.ANALYTICS_CONTACTS_DATA)
                setObservingTrackingIdentifierStatus(AnalyticsIdentifierResult.ExistingIdentifier(Arrangement.CURRENT_TRACKING_IDENTIFIER))
                setSelfServerConfig(Arrangement.SERVER_CONFIG_PRODUCTION)
            }.arrange()

        // when
        useCase.invoke().test {
            // then
            val item = awaitItem()
            assertIs<AnalyticsIdentifierResult.ExistingIdentifier>(item.identifierResult)
            assertAnalyticsProfileProperties(Arrangement.ANALYTICS_CONTACTS_DATA, item.profileProperties())
        }
    }

    @Test
    fun givenStagingBackendApi_whenObservingCurrentSessionAnalytics_thenExistingIdentifierAnalyticsResultIsReturned() = runTest {
        // given
        val (_, useCase) = Arrangement()
            .withIsAnonymousUsageDataEnabled(true)
            .apply {
                setCurrentSession(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.SELF_USER.id)))
                setAnalyticsContactsData(TestUser.SELF_USER.id, Arrangement.ANALYTICS_CONTACTS_DATA)
                setObservingTrackingIdentifierStatus(AnalyticsIdentifierResult.ExistingIdentifier(Arrangement.CURRENT_TRACKING_IDENTIFIER))
                setSelfServerConfig(Arrangement.SERVER_CONFIG_STAGING)
            }.arrange()

        // when
        useCase.invoke().test {
            // then
            val item = awaitItem()
            assertIs<AnalyticsIdentifierResult.ExistingIdentifier>(item.identifierResult)
            assertAnalyticsProfileProperties(Arrangement.ANALYTICS_CONTACTS_DATA, item.profileProperties())
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
                    setAnalyticsContactsData(TestUser.SELF_USER.id, Arrangement.ANALYTICS_CONTACTS_DATA)
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
                assertAnalyticsProfileProperties(Arrangement.ANALYTICS_CONTACTS_DATA, item.profileProperties())
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
                    setAnalyticsContactsData(TestUser.SELF_USER.id, Arrangement.ANALYTICS_CONTACTS_DATA)
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
                assertAnalyticsProfileProperties(Arrangement.ANALYTICS_CONTACTS_DATA, item.profileProperties())
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
                    setAnalyticsContactsData(TestUser.SELF_USER.id, Arrangement.ANALYTICS_CONTACTS_DATA)
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
                assertAnalyticsProfileProperties(Arrangement.ANALYTICS_CONTACTS_DATA, item.profileProperties())
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
                setAnalyticsContactsData(TestUser.SELF_USER.id, Arrangement.ANALYTICS_CONTACTS_DATA)
                setObservingTrackingIdentifierStatus(AnalyticsIdentifierResult.ExistingIdentifier(Arrangement.CURRENT_TRACKING_IDENTIFIER))
                setSelfServerConfig(Arrangement.SERVER_CONFIG_PRODUCTION)
            }.arrange()

        // when
        useCase.invoke().test {
            // then
            val item = awaitItem()
            assertIs<AnalyticsIdentifierResult.ExistingIdentifier>(item.identifierResult)
            assertAnalyticsProfileProperties(Arrangement.ANALYTICS_CONTACTS_DATA, item.profileProperties())

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
        }
    }

    @Test
    fun givenRegistrationIdIsEnabled_whenObservingCurrentSessionAnalytics_thenEnableAnalyticsResultForRegistrationIsReturned() = runTest {
        // given
        val (_, useCase) = Arrangement().apply {
            setCurrentSession(CurrentSessionResult.Failure.SessionNotFound)
            withIsAnonymousRegistrationEnabledResult(true)
            withAnonymousRegistrationTrackingId("trackId")
        }.arrange()

        // when
        useCase.invoke().test {
            // then
            val item = awaitItem()
            assertIs<AnalyticsIdentifierResult.RegistrationIdentifier>(item.identifierResult)
            assertEquals(false, item.profileProperties().isTeamMember)
            assertEquals(null, item.manager)
        }
    }

    private fun assertAnalyticsProfileProperties(expected: AnalyticsContactsData, actual: AnalyticsProfileProperties) {
        assertEquals(expected.teamId, actual.teamId)
        assertEquals(expected.isTeamMember, actual.isTeamMember)
        assertEquals(expected.isEnterprise, actual.isEnterprise)
        assertEquals(expected.contactsSize, actual.contactsAmount)
        assertEquals(expected.teamSize, actual.teamMembersAmount)
    }

    private class Arrangement {

        @MockK
        private lateinit var userDataStore: UserDataStore

        @MockK
        private lateinit var userDataStoreProvider: UserDataStoreProvider

        @MockK
        private lateinit var analyticsIdentifierManager: AnalyticsIdentifierManager

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        private val currentSessionChannel = Channel<CurrentSessionResult>(Channel.UNLIMITED)

        private val analyticsTrackingIdentifierStatusChannel = Channel<AnalyticsIdentifierResult>(Channel.UNLIMITED)

        private val selfServerConfigChannel = Channel<SelfServerConfigUseCase.Result>(Channel.UNLIMITED)

        private var analyticsContactsData: MutableMap<UserId, AnalyticsContactsData> = mutableMapOf()

        private val getAnalyticsContactsData: (UserId) -> AnalyticsContactsData = {
            analyticsContactsData.getOrDefault(it, ANALYTICS_CONTACTS_DATA_DEFAULT)
        }

        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)
            withIsAnonymousRegistrationEnabledResult(false)
        }

        suspend fun setCurrentSession(result: CurrentSessionResult) {
            currentSessionChannel.send(result)
        }

        fun withAnonymousRegistrationTrackingId(trackId: String) = apply {
            coEvery { globalDataStore.getOrCreateAnonymousRegistrationTrackId() } returns trackId
        }

        fun withIsAnonymousRegistrationEnabledResult(result: Boolean) = apply {
            every { globalDataStore.isAnonymousRegistrationEnabled() } returns flowOf(result)
        }

        fun setAnalyticsContactsData(userId: UserId, data: AnalyticsContactsData) {
            analyticsContactsData[userId] = data
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
            getAnalyticsContactsData = getAnalyticsContactsData,
            observeAnalyticsTrackingIdentifierStatusFlow = {
                analyticsTrackingIdentifierStatusChannel.receiveAsFlow()
            },
            analyticsIdentifierManagerProvider = {
                analyticsIdentifierManager
            },
            userDataStoreProvider = userDataStoreProvider,
            currentBackend = {
                selfServerConfigChannel.receive()
            },
            globalDataStore = globalDataStore
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

            val ANALYTICS_CONTACTS_DATA = AnalyticsContactsData(
                teamId = "teamId",
                contactsSize = 12,
                teamSize = 13,
                isEnterprise = true,
                isTeamMember = true
            )

            val ANALYTICS_CONTACTS_DATA_DEFAULT = AnalyticsContactsData(
                teamId = null,
                contactsSize = null,
                teamSize = null,
                isEnterprise = null,
                isTeamMember = false
            )
        }
    }
}
