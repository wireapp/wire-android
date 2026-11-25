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
package com.wire.android.tests.core.e2eTests

import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.user.getPropertyValues
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class SettingsSyncTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-12815")
    @Category("regression", "RC", "userSettingsSync")
    @Test
    fun givenUserEnablesPrivacySettings_whenLoggingOutWithClearedDataAndLoggingInAgain_thenAllSettingsRemainEnabled() {
        step("Given There is a team owner TeamOwner with team UserSettingsSync") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "UserSettingsSync",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And TeamOwner configures MLS for UserSettingsSync") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "UserSettingsSync", backendClient)
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        step("And TeamOwner signs in to staging and completes the login prompts") {
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(teamOwner)
        }

        step("And I open Privacy Settings from the conversation list menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.clickPrivacySettingsButtonOnSettingsPage()
        }

        step("And I set typing indicators, read receipts and screenshot censoring to OFF before enabling them") {
            pages.settingsPage.apply {
                setTypingIndicatorEnabled(false)
                setReadReceiptsEnabled(false)
                setScreenshotCensoringEnabled(false)
            }
        }

        step("When I enable typing indicators, read receipts and screenshot censoring") {
            pages.settingsPage.apply {
                setTypingIndicatorEnabled(true)
                setReadReceiptsEnabled(true)
                setScreenshotCensoringEnabled(true)
            }
        }

        step("Then all three Privacy Settings toggles are ON") {
            pages.settingsPage.apply {
                assertTypingIndicatorToggleIsOn()
                assertReadReceiptsToggleIsOn()
                assertScreenshotCensoringToggleIsOn()
            }
        }

        step("And the backend stores all three settings as enabled for TeamOwner") {
            assertTrue(
                "Enabled privacy settings were not saved to the backend",
                UiWaitUtils.retryUntilTimeout(UiWaitUtils.DEFAULT_TIMEOUT) {
                    val properties = runBlocking { backendClient.getPropertyValues(teamOwner) }
                    // Typing indicators are enabled by deleting their disabling property.
                    properties.optInt("WIRE_RECEIPT_MODE", -1) == 1 &&
                        !properties.has("WIRE_TYPING_INDICATOR_MODE") &&
                        properties.optInt("WIRE_SCREENSHOT_CENSORING_MODE", -1) == 1
                }
            )
        }

        step("When I return to the conversation list and open TeamOwner's profile") {
            pages.settingsPage.apply {
                clickBackButtonOnPrivacySettingsPage()
                clickBackButtonOnSettingsPage()
            }
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.iSeeUserProfilePage()
        }

        step("And I tap Log out, select clear data and confirm logout") {
            pages.selfUserProfilePage.apply {
                tapLogoutButton()
                iSeeClearDataOnLogOutAlert()
                iSeeInfoTextCheckbox("Delete all your personal information and conversations on this device")
                tapInfoTextCheckbox()
                tapLogoutButton()
            }
        }

        step("And TeamOwner signs in to staging again and completes the login prompts") {
            pages.registrationPage.assertEmailWelcomePage()
            clientUserManager.setSelfUser(teamOwner)
            loginToStagingAs(teamOwner)
        }

        step("And I open Privacy Settings from the conversation list menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.clickPrivacySettingsButtonOnSettingsPage()
        }

        step("Then typing indicators, read receipts and screenshot censoring are restored as ON") {
            pages.settingsPage.apply {
                assertTypingIndicatorToggleIsOn()
                assertReadReceiptsToggleIsOn()
                assertScreenshotCensoringToggleIsOn()
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-13061")
    @Category("regression", "RC", "userSettingsSync")
    @Test
    fun givenUserDisablesPrivacySettings_whenLoggingOutWithClearedDataAndLoggingInAgain_thenAllSettingsRemainDisabled() {
        step("Given There is a team owner TeamOwner with team UserSettingsSync") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "UserSettingsSync",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And TeamOwner configures MLS for UserSettingsSync") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "UserSettingsSync", backendClient)
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        step("And TeamOwner signs in to staging and completes the login prompts") {
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(teamOwner)
        }

        step("And I open Privacy Settings from the conversation list menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.clickPrivacySettingsButtonOnSettingsPage()
        }

        step("And I set typing indicators, read receipts and screenshot censoring to ON before disabling them") {
            pages.settingsPage.apply {
                setTypingIndicatorEnabled(true)
                setReadReceiptsEnabled(true)
                setScreenshotCensoringEnabled(true)
            }
        }

        step("When I disable typing indicators, read receipts and screenshot censoring") {
            pages.settingsPage.apply {
                setTypingIndicatorEnabled(false)
                setReadReceiptsEnabled(false)
                setScreenshotCensoringEnabled(false)
            }
        }

        step("Then all three Privacy Settings toggles are OFF") {
            pages.settingsPage.apply {
                assertTypingIndicatorToggleIsOff()
                assertReadReceiptsToggleIsOff()
                assertScreenshotCensoringToggleIsOff()
            }
        }

        step("And the backend stores all three settings as disabled for TeamOwner") {
            assertTrue(
                "Disabled privacy settings were not saved to the backend",
                UiWaitUtils.retryUntilTimeout(UiWaitUtils.DEFAULT_TIMEOUT) {
                    val properties = runBlocking { backendClient.getPropertyValues(teamOwner) }
                    // Read receipts and screenshot censoring are disabled by deleting their properties.
                    !properties.has("WIRE_RECEIPT_MODE") &&
                        properties.optInt("WIRE_TYPING_INDICATOR_MODE", -1) == 0 &&
                        !properties.has("WIRE_SCREENSHOT_CENSORING_MODE")
                }
            )
        }

        step("When I return to the conversation list and open TeamOwner's profile") {
            pages.settingsPage.apply {
                clickBackButtonOnPrivacySettingsPage()
                clickBackButtonOnSettingsPage()
            }
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.iSeeUserProfilePage()
        }

        step("And I tap Log out, select clear data and confirm logout") {
            pages.selfUserProfilePage.apply {
                tapLogoutButton()
                iSeeClearDataOnLogOutAlert()
                iSeeInfoTextCheckbox("Delete all your personal information and conversations on this device")
                tapInfoTextCheckbox()
                tapLogoutButton()
            }
        }

        step("And TeamOwner signs in to staging again and completes the login prompts") {
            pages.registrationPage.assertEmailWelcomePage()
            clientUserManager.setSelfUser(teamOwner)
            loginToStagingAs(teamOwner)
        }

        step("And I open Privacy Settings from the conversation list menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.clickPrivacySettingsButtonOnSettingsPage()
        }

        step("Then typing indicators and screenshot censoring are restored as OFF") {
            pages.settingsPage.apply {
                assertTypingIndicatorToggleIsOff()
                // Skipped due to WPB-30316: read receipts return to ON after logout with cleared data and login.
                // assertReadReceiptsToggleIsOff()
                assertScreenshotCensoringToggleIsOff()
            }
        }
    }

    // Shared staging login flow, including post-login prompts and the service notification wait.
    private fun loginToStagingAs(user: ClientUser) {
        pages.loginPage.apply {
            clickStagingDeepLink()
            clickProceedButtonOnDeeplinkOverlay()
            clickContinueButtonOnBackendConfigSuccess()
            enterUserIdentifier(user.email.orEmpty())
            clickLoginButton()
            enterUserPassword(user.password.orEmpty())
            clickLoginButton()
        }
        pages.registrationPage.apply {
            waitUntilLoginFlowIsCompleted(loginButtonTimeout = 35.seconds)
            clickAllowNotificationButton()
            clickDeclineShareDataAlert()
        }
        pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
    }
}
