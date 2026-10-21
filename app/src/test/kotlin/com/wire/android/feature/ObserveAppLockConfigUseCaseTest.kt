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
package com.wire.android.feature

import app.cash.turbine.test
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.session.FakeSessionAdmission
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.AppLockTeamConfig
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.applock.AppLockTeamFeatureConfigObserver
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertSame
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ObserveAppLockConfigUseCaseTest {

    @Test
    fun givenNoValidSession_whenObservingAppLock_thenSendDisabledStatus() = runTest {
        val (_, useCase) = Arrangement()
            .withNonValidSession()
            .arrange()

        val result = useCase.invoke()

        result.test {
            val appLockStatus = awaitItem()

            assertEquals(AppLockConfig.Disabled(timeout), appLockStatus)
            awaitComplete()
        }
    }

    @Test
    fun givenInvalidSession_whenObservingAppLock_thenSendDisabledStatus() = runTest {
        val (_, useCase) = Arrangement()
            .withInvalidSession()
            .arrange()

        val result = useCase.invoke()

        result.test {
            val appLockStatus = awaitItem()

            assertEquals(AppLockConfig.Disabled(timeout), appLockStatus)
            awaitComplete()
        }
    }

    @Test
    fun givenValidSessionAndAppLockedByTeam_whenObservingAppLock_thenSendEnforcedByTeamStatus() =
        runTest {
            val (_, useCase) = Arrangement()
                .withValidSession()
                .withTeamAppLockEnabled()
                .withAppLockedByCurrentUser(false)
                .arrange()

            val result = useCase.invoke()

            result.test {
                val appLockStatus = awaitItem()

                assertEquals(AppLockConfig.EnforcedByTeam(timeout), appLockStatus)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun givenValidSessionAndAppLockedByUserOnly_whenObservingAppLock_thenSendEnabledStatus() =
        runTest {
            val (_, useCase) = Arrangement()
                .withValidSession()
                .withTeamAppLockDisabled()
                .withAppLockedByCurrentUser(true)
                .arrange()

            val result = useCase.invoke()

            result.test {
                val appLockStatus = awaitItem()

                assertEquals(AppLockConfig.Enabled(timeout), appLockStatus)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun givenValidSessionAndAppNotLockedByUserNorTeam_whenObservingAppLock_thenSendDisabledStatus() =
        runTest {
            val (_, useCase) = Arrangement()
                .withValidSession()
                .withTeamAppLockDisabled()
                .withAppNonLockedByCurrentUser()
                .arrange()

            val result = useCase.invoke()
            result.test {
                val appLockStatus = awaitItem()

                assertEquals(AppLockConfig.Disabled(timeout), appLockStatus)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `enforced config survives pending admission then replacement and invalidation reach the collector`() = runTest {
        val (arrangement, useCase) = Arrangement().withTeamAppLockEnabled().withAppLockedByCurrentUser(false).arrange()
        val current = MutableStateFlow<CurrentSessionResult>(CurrentSessionResult.Success(accountInfo))
        coEvery { arrangement.coreLogic.getGlobalScope().session.currentSessionFlow() } returns current
        arrangement.admission.publish(userId, null)
        useCase().test {
            runCurrent()
            expectNoEvents()
            arrangement.admission.publish(userId, arrangement.userSessionScope)
            assertEquals(AppLockConfig.EnforcedByTeam(timeout), awaitItem())
            arrangement.admission.publish(userId, null)
            runCurrent()
            expectNoEvents()
            assertTrue(arrangement.userSessionScope in arrangement.admission.drained)
            arrangement.admission.publish(userId, arrangement.userSessionScope)
            assertEquals(AppLockConfig.EnforcedByTeam(timeout), awaitItem())
            current.value = CurrentSessionResult.Success(accountInfoInvalid)
            assertEquals(AppLockConfig.Disabled(timeout), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
    }

    @Test
    fun `enabled config is retained through unavailable revision and caller cancellation drains it`() = runTest {
        val (arrangement, useCase) = Arrangement().withValidSession().withTeamAppLockDisabled()
            .withAppLockedByCurrentUser(true).arrange()
        useCase().test {
            assertEquals(AppLockConfig.Enabled(timeout), awaitItem())
            arrangement.admission.publish(userId, null)
            runCurrent()
            expectNoEvents()
            arrangement.admission.publish(userId, arrangement.userSessionScope)
            assertEquals(AppLockConfig.Enabled(timeout), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(2, arrangement.admission.drained.size)
    }

    @Test
    fun `genuine app lock observer failure propagates`() = runTest {
        val (arrangement, useCase) = Arrangement().withValidSession().withTeamAppLockEnabled()
            .withAppLockedByCurrentUser(false).arrange()
        val failure = IllegalStateException("config failed")
        every { arrangement.appLockTeamFeatureConfigObserver() } throws failure
        useCase().test { assertSame(failure, awaitError()) }
    }

    inner class Arrangement {

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var userSessionScope: UserSessionScope

        @MockK
        lateinit var appLockTeamFeatureConfigObserver: AppLockTeamFeatureConfigObserver

        internal val admission by lazy { FakeSessionAdmission(coreLogic) { userSessionScope } }

        val useCase by lazy {
            ObserveAppLockConfigUseCase(
                globalDataStore = globalDataStore,
                coreLogic = coreLogic
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            admission
        }

        fun arrange() = this to useCase

        fun withNonValidSession() = apply {
            coEvery { coreLogic.getGlobalScope().session.currentSessionFlow() } returns
                    flowOf(CurrentSessionResult.Failure.SessionNotFound)
        }

        fun withInvalidSession() = apply {
            coEvery { coreLogic.getGlobalScope().session.currentSessionFlow() } returns
                    flowOf(CurrentSessionResult.Success(accountInfoInvalid))
        }

        fun withValidSession() = apply {
            coEvery { coreLogic.getGlobalScope().session.currentSessionFlow() } returns
                    flowOf(CurrentSessionResult.Success(accountInfo))
        }

        fun withTeamAppLockEnabled() = apply {
            every {
                userSessionScope.appLockTeamFeatureConfigObserver
            } returns appLockTeamFeatureConfigObserver
            every {
                appLockTeamFeatureConfigObserver.invoke()
            } returns flowOf(AppLockTeamConfig(true, timeout, false))
        }

        fun withTeamAppLockDisabled() = apply {
            every {
                userSessionScope.appLockTeamFeatureConfigObserver
            } returns appLockTeamFeatureConfigObserver
            every {
                appLockTeamFeatureConfigObserver.invoke()
            } returns flowOf(AppLockTeamConfig(false, timeout, false))
        }

        fun withAppLockedByCurrentUser(state: Boolean) = apply {
            every { globalDataStore.isAppLockPasscodeSetFlow() } returns flowOf(state)
        }

        fun withAppNonLockedByCurrentUser() = apply {
            every { globalDataStore.isAppLockPasscodeSetFlow() } returns flowOf(false)
        }
    }

    companion object {
        private val userId = UserId("userId", "domain")
        private val accountInfo = AccountInfo.Valid(userId)
        private val accountInfoInvalid = AccountInfo.Invalid(userId, LogoutReason.DELETED_ACCOUNT)
        private val timeout = 60.seconds
    }
}
