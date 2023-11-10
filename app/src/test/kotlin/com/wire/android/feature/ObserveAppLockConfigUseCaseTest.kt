/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.configuration.AppLockTeamConfig
import com.wire.kalium.logic.feature.applock.AppLockTeamFeatureConfigObserver
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

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
    fun givenValidSessionAndAppLockedByTeam_whenObservingAppLock_thenSendEnforcedByTeamStatus() =
        runTest {
            val (_, useCase) = Arrangement()
                .withValidSession()
                .withTeamAppLockEnabled()
                .withAppLockedByCurrentUser()
                .arrange()

            val result = useCase.invoke()

            result.test {
                val appLockStatus = awaitItem()

                assertEquals(AppLockConfig.EnforcedByTeam(timeout), appLockStatus)
                awaitComplete()
            }
        }

    @Test
    fun givenValidSessionAndAppLockedByUserOnly_whenObservingAppLock_thenSendEnabledStatus() =
        runTest {
            val (_, useCase) = Arrangement()
                .withValidSession()
                .withTeamAppLockDisabled()
                .withAppLockedByCurrentUser()
                .arrange()

            val result = useCase.invoke()

            result.test {
                val appLockStatus = awaitItem()

                assertEquals(AppLockConfig.Enabled(timeout), appLockStatus)
                awaitComplete()
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
                awaitComplete()
            }
        }

    inner class Arrangement {

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        @MockK
        lateinit var currentSession: CurrentSessionUseCase

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var userSessionScope: UserSessionScope

        @MockK
        lateinit var appLockTeamFeatureConfigObserver: AppLockTeamFeatureConfigObserver

        val useCase by lazy {
            ObserveAppLockConfigUseCase(
                globalDataStore = globalDataStore,
                coreLogic = coreLogic,
                currentSession = currentSession
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withAppLockPasscodeSet(value: Boolean) = apply {
            every { globalDataStore.isAppLockPasscodeSetFlow() } returns flowOf(value)
        }

        fun arrange() = this to useCase

        fun withNonValidSession() = apply {
            coEvery { currentSession() } returns CurrentSessionResult.Failure.SessionNotFound
        }

        fun withValidSession() = apply {
            coEvery { currentSession() } returns CurrentSessionResult.Success(accountInfo)
        }

        fun withTeamAppLockEnabled() = apply {
            every { coreLogic.getSessionScope(any()) } returns userSessionScope
            every {
                userSessionScope.appLockTeamFeatureConfigObserver
            } returns appLockTeamFeatureConfigObserver
            every {
                appLockTeamFeatureConfigObserver.invoke()
            } returns flowOf(AppLockTeamConfig(true, timeout, false))
        }

        fun withTeamAppLockDisabled() = apply {
            every { coreLogic.getSessionScope(any()) } returns userSessionScope
            every {
                userSessionScope.appLockTeamFeatureConfigObserver
            } returns appLockTeamFeatureConfigObserver
            every {
                appLockTeamFeatureConfigObserver.invoke()
            } returns flowOf(AppLockTeamConfig(false, timeout, false))
        }

        fun withAppLockedByCurrentUser() = apply {
            every { globalDataStore.isAppLockPasscodeSetFlow() } returns flowOf(true)
        }

        fun withAppNonLockedByCurrentUser() = apply {
            every { globalDataStore.isAppLockPasscodeSetFlow() } returns flowOf(false)
        }
    }

    companion object {
        private val accountInfo = AccountInfo.Valid(UserId("userId", "domain"))
        private val timeout = 60.seconds
    }
}
