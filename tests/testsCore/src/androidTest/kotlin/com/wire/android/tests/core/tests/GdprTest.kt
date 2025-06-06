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

import Backend
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import com.wire.android.testSupport.uiautomatorutils.UiAutomatorSetup
import com.wire.android.tests.core.pages.ConversationPage
import com.wire.android.tests.core.pages.LoginPage
import com.wire.android.tests.core.pages.RegistrationPage
import com.wire.android.tests.core.pages.SettingsPage
import com.wire.android.tests.support.suite.Tag
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.utils.ClientUser


/*
This test works on the following conditions:
1) The dev/staging app is installed on the device/emulator.
*/
@RunWith(AndroidJUnit4::class)
@Tag("RC", "regression", "@TC-8694", "@registration")

class GdprTest {
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_DEV)
        // device = UiAutomatorSetup.start(UiAutomatorSetup.APP_STAGGING)
    }

    @After
    fun tearDown() {
        //  UiAutomatorSetup.stopApp()
    }

    @Test
    fun personalUsersendAnonymousDataAndSeeAnalyticsIdentifierInDebugSettings() {
        val registrationPage = RegistrationPage(device)
        val conversationPage = ConversationPage(device)
        val loginPage = LoginPage(device)
        val settingsPage = SettingsPage(device)


        val backendClient = Backend.loadBackend("STAGING")
        //val backendClient = Backend.loadBackend("BUND_NEXT_COLUMN_1")
        val clientUser = ClientUser()
        val registeredUser = backendClient?.createPersonalUserViaBackdoor(clientUser)


        registrationPage.assertEmailWelcomePage()
        loginPage.clickStagingDeepLink()
        loginPage.clickProceedButtonOnDeeplinkOverlay()
        loginPage.enterPersonalUserLoggingEmail(registeredUser?.email ?: "")
        loginPage.clickLoginButton()
        //loginPage.assertLoggingPageVisible()
        loginPage.enterPersonalUserLoginPassword(registeredUser?.password ?: "")
        loginPage.clickLoginButton()

//Thread.sleep(5000)
        registrationPage.clickAllowNotificationButton()
        registrationPage.setUserName(registeredUser?.uniqueUsername ?: "")
        loginPage.clickConfirmButtonOnUsernameSetupPage()
        registrationPage.clickAgreeShareDataAlert()
        registrationPage.assertConversationPageVisible()
        conversationPage.clickMainMenuButtonOnConversationVeiwPage()
        conversationPage.clickSettingsButtonOnMenuEntry()
        settingsPage.clickPrivacySettingsButtonOnSettingsPage()
       // Thread.sleep(5000)

        settingsPage.assertSendAnonymousUsageDataToggleIsOn()
        settingsPage.clickBackButtonOnPrivacySettingsPage()
        settingsPage.clickDebugSettingsButton()

        settingsPage.assertAnalyticsInitializedIsSetToTrue()

        settingsPage.assertAnalyticsTrackingIdentifierIsDispayed()

    }
}
