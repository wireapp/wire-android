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
import backendUtils.BackendClient
import backendUtils.user.deleteUser
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.core.pages.AllPages
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.inject
import user.utils.ClientUser
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId

/*
This test works on the following conditions:
1) The dev/staging app is installed on the device/emulator.
*/
@RunWith(AndroidJUnit4::class)
class GdprTest : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    private lateinit var backendClient: BackendClient
    private var registeredUser: ClientUser? = null

    @Before
    fun setUp() {
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_INTERNAL)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        runCatching {
            registeredUser?.deleteUser(backendClient)
        }
    }

    @TestCaseId("TC-8705")
    @Category("gdpr", "regression", "testTest")
    @Test
    fun givenTeamUserAcceptsAnonymousDataSharing_whenConsentIsGiven_thenAnalyticsIdentifierIsVisibleInDebugSettings() {

        step("Create personal user via backend") {
            val clientUser = ClientUser()
            registeredUser = backendClient.createPersonalUserViaBackend(clientUser)
        }

        step("Verify welcome page and login via staging deep link") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterPersonalUserLoggingEmail(registeredUser?.email ?: "")
                clickLoginButton()
                enterPersonalUserLoginPassword(registeredUser?.password ?: "")
                clickLoginButton()
            }
        }

        step("Complete login flow and set username") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                setUserName(registeredUser?.uniqueUsername ?: "")
            }
            pages.loginPage.clickConfirmButtonOnUsernameSetupPage()
        }

        step("Give consent to share anonymous usage data and verify landing on conversation page") {
            pages.registrationPage.apply {
                clickAgreeShareDataAlert()
                assertConversationPageVisible()
            }
        }

        step("Navigate to settings from conversation list") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }

        step("Verify anonymous usage data toggle is ON in privacy settings") {
            pages.settingsPage.apply {
                clickPrivacySettingsButtonOnSettingsPage()
                assertSendAnonymousUsageDataToggleIsOn()
                clickBackButtonOnPrivacySettingsPage()
            }
        }

        step("Open debug settings and verify analytics identifier is visible") {
            pages.settingsPage.apply {
                clickDebugSettingsButton()
                assertAnalyticsInitializedIsSetToTrue()
                assertAnalyticsTrackingIdentifierIsDispayed()
            }
        }
    }
}
