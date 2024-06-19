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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.work.testing.WorkManagerTestInitHelper
import co.touchlab.kermit.platformLogWriter
import com.wire.android.ui.WireActivity
import com.wire.android.util.DataDogLogger
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import com.wire.kalium.logic.CoreLogger
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class WireActivityTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<WireActivity>, WireActivity> =
        createAndroidComposeRule<WireActivity>()

    @Before
    fun init() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        context.deleteDatabase("global-db") // GLOBAL_DB_NAME in FileNameUtil
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        initializeApplicationLoggingFrameworks()
        hiltRule.inject()
    }

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

    private fun initializeApplicationLoggingFrameworks() {
        val config =
            KaliumLogger.Config.DEFAULT.apply {
                setLogLevel(KaliumLogLevel.VERBOSE)
                setLogWriterList(listOf(DataDogLogger, platformLogWriter()))
            }
        // 2. Initialize our internal logging framework
        AppLogger.init(config)
        CoreLogger.init(config)
        // 4. Everything ready, now we can log device info
        appLogger.i("Logger enabled")
    }
}
