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
package com.wire.android

import android.app.Application
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import co.touchlab.kermit.platformLogWriter
import com.wire.android.extensions.performClickWithNodeWithText
import com.wire.android.extensions.waitUntilExists
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.feature.SwitchAccountResult
import com.wire.android.ui.CurrentSessionErrorState
import com.wire.android.ui.GlobalAppState
import com.wire.android.ui.WireActivity
import com.wire.android.ui.WireActivityViewModel
import com.wire.android.util.DataDogLogger
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import com.wire.kalium.common.logger.CoreLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class WireActivityTest {

    @get:Rule
    val composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<WireActivity>, WireActivity> =
        createAndroidComposeRule<WireActivity>()

    @Before
    fun init() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        context.deleteDatabase("global-db") // GLOBAL_DB_NAME in FileNameUtil
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        initializeApplicationLoggingFrameworks()
    }

    @Ignore // TODO add other api mocks to not have flaky test
    @Test
    fun loginTest() = runTest {
        composeTestRule.waitUntilExists(R.string.label_login)
        composeTestRule.performClickWithNodeWithText(R.string.label_login)

        composeTestRule.waitUntilExists(R.string.login_title)

        composeTestRule.onNodeWithTag("userIdentifierInput").performTextInput("test@wire.com")
        composeTestRule.onNodeWithTag("PasswordInput").performTextInput("password")

        composeTestRule.onNodeWithTag("loginButton").performClick()

        composeTestRule.waitUntilExists(R.string.migration_title, timeoutMillis = 10_000)
    }

    @Test
    fun givenLoggedOutDialogWithoutNavHost_whenConfirming_thenActivityRecreatesNavigationGraph() {
        val removedClientTitle = composeTestRule.activity.getString(R.string.removed_client_error_title)
        val accountSwitch = mockk<AccountSwitchUseCase>()
        coEvery {
            accountSwitch(SwitchAccountParam.TryToSwitchToNextAccount)
        } returns SwitchAccountResult.NoOtherAccountToSwitch

        composeTestRule.runOnIdle {
            val viewModel = ViewModelProvider(composeTestRule.activity)[WireActivityViewModel::class.java]
            viewModel.setAccountSwitchForTest(accountSwitch)
            viewModel.setGlobalAppStateForTest(
                viewModel.globalAppState.copy(blockUserUI = CurrentSessionErrorState.RemovedClient)
            )
        }
        composeTestRule.waitUntilExists(R.string.removed_client_error_title)

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitUntilExists(R.string.removed_client_error_title)

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.label_ok)).performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText(removedClientTitle).fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.activityRule.scenario.onActivity {
            assertEquals(Lifecycle.State.RESUMED, it.lifecycle.currentState)
        }
        coVerify(exactly = 1) {
            accountSwitch(SwitchAccountParam.TryToSwitchToNextAccount)
        }
    }

    private fun WireActivityViewModel.setAccountSwitchForTest(accountSwitch: AccountSwitchUseCase) {
        javaClass.getDeclaredField("accountSwitch")
            .apply { isAccessible = true }
            .set(this, lazyOf(accountSwitch))
    }

    private fun WireActivityViewModel.setGlobalAppStateForTest(state: GlobalAppState) {
        javaClass.getDeclaredMethod("setGlobalAppState", GlobalAppState::class.java)
            .apply { isAccessible = true }
            .invoke(this, state)
    }

    private fun initializeApplicationLoggingFrameworks() {
        val config = KaliumLogger.Config(
            KaliumLogLevel.VERBOSE,
            listOf(DataDogLogger, platformLogWriter())
        )
        // 2. Initialize our internal logging framework
        AppLogger.init(config)
        CoreLogger.init(config)
        // 4. Everything ready, now we can log device info
        appLogger.i("Logger enabled")
    }
}
