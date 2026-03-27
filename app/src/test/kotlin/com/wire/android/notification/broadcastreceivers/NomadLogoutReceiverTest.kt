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
package com.wire.android.notification.broadcastreceivers

import android.content.Context
import android.content.Intent
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NomadProfilesFeatureConfig
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.feature.SwitchAccountResult
import com.wire.android.util.SwitchAccountObserver
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class NomadLogoutReceiverTest {

    @Test
    fun `when logout broadcast received then logout sequence runs within receiver`() = runTest {
        val userId = UserId("user", "domain")
        val arrangement = Arrangement()
            .withCurrentSession(CurrentSessionResult.Success(AccountInfo.Valid(userId)))
            .arrange()

        val intent = mockk<Intent> { every { action } returns NomadLogoutReceiver.ACTION_LOGOUT }
        arrangement.receiver.receive(arrangement.context, intent)

        verify(exactly = 1) { arrangement.coreLogic.getSessionScope(userId) }
        coVerifyOrder {
            arrangement.currentSession()
            arrangement.logoutUseCase(LogoutReason.SELF_HARD_LOGOUT, true)
            arrangement.deleteSession(userId)
            arrangement.accountSwitch(SwitchAccountParam.TryToSwitchToNextAccount)
        }
        verify(exactly = 1) { arrangement.switchAccountObserver.noOtherAccountToSwitch() }
    }

    @Test
    fun `when nomad profiles are disabled then logout broadcast is ignored`() = runTest {
        val arrangement = Arrangement()
            .withNomadProfilesEnabled(false)
            .arrange()

        val intent = mockk<Intent> { every { action } returns NomadLogoutReceiver.ACTION_LOGOUT }
        arrangement.receiver.receive(arrangement.context, intent)
        advanceUntilIdle()

        coVerify(exactly = 0) {
            arrangement.currentSession()
            arrangement.logoutUseCase(any(), any())
            arrangement.deleteSession(any())
            arrangement.accountSwitch(any())
        }
        verify(exactly = 0) {
            arrangement.coreLogic.getSessionScope(any())
            arrangement.context.startActivity(any())
        }
    }

    @Test
    fun `when no current session exists then receiver does not continue logout flow`() = runTest {
        val arrangement = Arrangement()
            .withCurrentSession(CurrentSessionResult.Failure.SessionNotFound)
            .arrange()

        val intent = mockk<Intent> { every { action } returns NomadLogoutReceiver.ACTION_LOGOUT }
        arrangement.receiver.receive(arrangement.context, intent)
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.currentSession() }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
        coVerify(exactly = 0) {
            arrangement.logoutUseCase(any(), any())
            arrangement.deleteSession(any())
            arrangement.accountSwitch(any())
        }
        verify(exactly = 0) { arrangement.context.startActivity(any()) }
    }

    private class Arrangement {

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var currentSession: CurrentSessionUseCase

        @MockK
        lateinit var deleteSession: DeleteSessionUseCase

        @MockK
        lateinit var accountSwitch: AccountSwitchUseCase

        @MockK
        lateinit var switchAccountObserver: SwitchAccountObserver

        @MockK
        lateinit var userSessionScope: UserSessionScope

        @MockK
        lateinit var logoutUseCase: LogoutUseCase

        @MockK
        lateinit var nomadProfilesFeatureConfig: NomadProfilesFeatureConfig

        val context = mockk<Context>(relaxed = true)
        val receiver = NomadLogoutReceiver()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            every { context.applicationContext } returns context
            every { context.packageName } returns "com.wire.android"
            every { context.startActivity(any()) } just runs

            every { userSessionScope.logout } returns logoutUseCase
            coEvery { logoutUseCase(any(), any()) } returns Unit
            coEvery { deleteSession(any()) } returns DeleteSessionUseCase.Result.Success
            every { coreLogic.getGlobalScope().deleteSession } returns deleteSession
            coEvery { accountSwitch(any()) } returns SwitchAccountResult.NoOtherAccountToSwitch
            every { coreLogic.getSessionScope(any()) } returns userSessionScope
            every { nomadProfilesFeatureConfig.isEnabled() } returns true
        }

        fun arrange(): Arrangement {
            receiver.coreLogic = coreLogic
            receiver.currentSession = currentSession
            receiver.accountSwitch = accountSwitch
            receiver.switchAccountObserver = switchAccountObserver
            receiver.nomadProfilesFeatureConfig = nomadProfilesFeatureConfig
            return this
        }

        fun withCurrentSession(result: CurrentSessionResult) = apply {
            coEvery { currentSession() } returns result
        }

        fun withNomadProfilesEnabled(enabled: Boolean) = apply {
            every { nomadProfilesFeatureConfig.isEnabled() } returns enabled
        }
    }
}
