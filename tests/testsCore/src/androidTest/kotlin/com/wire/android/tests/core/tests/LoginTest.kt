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
import com.wire.android.tests.core.pages.LoginPage
import com.wire.android.tests.core.pages.RegistrationPage
import com.wire.android.tests.support.UiAutomatorSetup
import user.UserClient
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


/*
This test works on the following conditions:
1) The dev/staging app is installed on the device/emulator.
*/
@RunWith(AndroidJUnit4::class)
//@RC
class LoginTest {
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_DEV)
    }

    @Test
    fun openTheAppAndShouldSeeEmailFieldAndLoginWhenValid() {

    }
}
