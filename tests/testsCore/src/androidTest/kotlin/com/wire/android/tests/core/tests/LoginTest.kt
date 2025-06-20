/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.tests.core.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import com.wire.android.testSupport.uiautomatorutils.UiAutomatorSetup
import com.wire.android.tests.core.di.testModule
import com.wire.android.tests.core.pages.AllPages
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject


/*
This test works on the following conditions:
1) The dev/staging app is installed on the device/emulator.
*/
@RunWith(AndroidJUnit4::class)
//@RC
class LoginTest : KoinTest {
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(testModule)
    }
    private lateinit var device: UiDevice
    private val pages: AllPages by inject()


    @Before
    fun setUp() {
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_DEV)
    }

    @Test
    fun openTheAppAndShouldSeeEmailFieldAndLoginWhenValid() {


        }
    }

