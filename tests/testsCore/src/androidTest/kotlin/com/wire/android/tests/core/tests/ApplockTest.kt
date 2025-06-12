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
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.wire.android.testSupport.backendConnections.team.createTeamOwnerViaBackdoor
import com.wire.android.testSupport.uiautomatorutils.UiAutomatorSetup
import com.wire.android.tests.core.pages.ConversationPage
import com.wire.android.tests.core.pages.LoginPage
import com.wire.android.tests.core.pages.RegistrationPage
import com.wire.android.tests.core.pages.SettingsPage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)

class ApplockTest {

    private lateinit var device: UiDevice

    lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

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
    fun setAppLockForAppAndVerifyAppIsLockedAfter1MinuteInTheBackground() {
        val registrationPage = RegistrationPage(device)
        val conversationPage = ConversationPage(device)
        val loginPage = LoginPage(device)
        val settingsPage = SettingsPage(device)
        val backendClient = Backend.loadBackend("STAGING")
        val clientUser = ClientUser()
//        val registeredUser = backendClient?.createPersonalUserViaBackdoor(clientUser)
        val registeredUser = runBlocking {
            backendClient?.createTeamOwnerViaBackdoor(
                clientUser,
                "FullTeam",
                "en_US",
                true,
                context
            )
        }

        println("---- Registered User -----")
        println(registeredUser)

        registrationPage.assertEmailWelcomePage()
        //loginPage.clickStagingDeepLink()
        //loginPage.clickProceedButtonOnDeeplinkOverlay()
        loginPage.enterPersonalUserLoggingEmail(registeredUser?.email ?: "")
        loginPage.clickLoginButton()
        //loginPage.assertLoggingPageVisible()
        loginPage.enterPersonalUserLoginPassword(registeredUser?.password ?: "")
        loginPage.clickLoginButton()

        //Thread.sleep(3000)


        registrationPage.waitUntilLoginFlowIsComplete()

//Thread.sleep(10000)
        registrationPage.clickAllowNotificationButton()
      //  registrationPage.setUserName(registeredUser?.uniqueUsername ?: "")
      //  loginPage.clickConfirmButtonOnUsernameSetupPage()
        registrationPage.clickAgreeShareDataAlert()
        registrationPage.assertConversationPageVisible()
        Thread.sleep(20000)

    }
}


