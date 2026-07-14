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
package com.wire.android.tests.core.criticalFlows

import SSOServiceHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import deleteDownloadedFilesContaining
import keycloak.KeycloakApiClient
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class SSODeviceBackup : BaseUiTest() {
    private lateinit var keycloakApiClient: KeycloakApiClient
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers("mobtown-lemon")
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        SSOServiceHelper.initialize(clientUserManager)
        keycloakApiClient = KeycloakApiClient(backendClient)
    }

    @After
    fun tearDown() {
        deleteDownloadedFilesContaining("Wire")
        runCatching { keycloakApiClient.cleanUp() }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8604")
    @Category("criticalFlow")
    @Test
    fun givenSSOTeamWithKeycloak_whenSettingUpNewDeviceAndRestoringBackup_thenMessageIsRestored() {
        var ssoCode = ""

        step("There is TeamOwner with team Messaging on mobtown-lemon backend wired to Keycloak SSO") {
            runBlocking {
                SSOServiceHelper.createKeycloakSsoTeamOwner(
                    context,
                    "user1Name",
                    "Messaging",
                    keycloakApiClient
                )
            }
        }

        step("User TeamOwner is available") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("User Member1 is created in Keycloak for SSO login") {
            runBlocking {
                SSOServiceHelper.addKeycloakSsoUsers(
                    "user1Name",
                    "user2Name",
                    keycloakApiClient
                )
            }
        }

        step("User Member1 is me") {
            SSOServiceHelper.setCurrentSsoUser("user2Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And I get SSO code for team Messaging") {
            ssoCode = SSOServiceHelper.getSsoCode()
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open mobtown-lemon deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink("mobtown-lemon")
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I start SSO login flow using the team SSO code") {
            pages.loginPage.apply {
                enterSSOCodeOnSSOLoginTab(ssoCode)
                clickLoginButton()
            }
        }

        step("And I complete Keycloak login as Member1") {
            pages.ssoPage.apply {
                waitUntilKeycloakPageLoaded()
                enterKeycloakEmail(member1.email ?: "")
                enterKeycloakPassword(member1.password ?: "")
                tapKeycloakSignIn()
            }
        }

        step("And I allow the notification permission prompt after login") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
            }
        }

        step("And I set Member1 username and confirm profile setup") {
            pages.registrationPage.apply {
                setUserName(member1.uniqueUsername.orEmpty())
                clickConfirmButton()
            }
        }

        step("And I decline the share data alert after login") {
            pages.registrationPage.apply {
                UiWaitUtils.waitFor(1.seconds)
                clickDeclineShareDataAlert()
            }
        }

        step("And I start a new conversation from conversation list") {
            pages.conversationListPage.apply {
                tapStartNewConversationButton()
            }
        }

        step("And I search for TeamOwner and open the user profile") {
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUniqueUserNameInSearchField(clientUserManager, "user1Name")
                assertUsernameInSearchResultIs(teamOwner.name ?: "")
                tapUsernameInSearchResult(teamOwner.name ?: "")
            }
        }

        step("And I start conversation with TeamOwner from the user profile") {
            pages.connectedUserProfilePage.apply {
                assertStartConversationButtonVisible()
                clickStartConversationButton()
            }
        }

        step("When I send the message Testing of the backup functionality") {
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
                typeMessageInInputField("Testing of the backup functionality")
                clickSendButton()
            }
        }

        step("Then I see the message Testing of the backup functionality in current conversation") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Testing of the backup functionality")
            }
        }

        step("And I tap back button to leave the conversation") {
            pages.conversationViewPage.apply {
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("And I return from the profile flow to conversation list") {
            pages.connectedUserProfilePage.apply {
                tapCloseButtonOnConnectedUserProfilePage()
            }
            pages.searchPage.apply {
                clickCloseButtonOnSearchInputField()
            }
            pages.conversationListPage.apply {
                clickCloseButtonOnNewConversationScreen()
                assertConversationListVisible()
            }
        }

        step("And I open Settings from conversations menu entry") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }

        step("And I open backup page from Settings") {
            pages.settingsPage.apply {
                openBackupAndRestoreConversationsMenu()
            }
            pages.backupPage.apply {
                iSeeBackupPageHeading()
            }
        }

        step("When I create a backup") {
            pages.backupPage.apply {
                clickCreateBackupButton()
                clickBackUpNowButton()
                iSeeBackupConfirmation("Conversations successfully saved")
            }
        }

        step("And I save the backup file to the device") {
            pages.backupPage.apply {
                iTapSaveFileButton()
                iTapSaveInOSMenuButton()
            }
        }

        step("Then I see backup page heading again and return to Settings") {
            pages.backupPage.apply {
                iSeeBackupPageHeading()
            }
            pages.settingsPage.apply {
                clickBackButtonOnSettingsPage()
            }
        }

        step("And I open my self profile from the conversations menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickConversationsButtonOnMenuEntry()
                clickUserProfileButtonNoPhoto()
            }
        }

        step("When I tap logout from my self profile and see the clear data alert") {
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                tapLogoutButton()
                iSeeClearDataOnLogOutAlert()
            }
        }

        step("And I choose to delete personal information from this device and log out") {
            pages.selfUserProfilePage.apply {
                iSeeInfoTextCheckbox("Delete all your personal information and conversations on this device")
                tapInfoTextCheckbox()
                tapLogoutButton()
            }
        }

        step("Then I see welcome screen before logging in again") {
            pages.registrationPage.apply {
                assertEmailWelcomePage(timeout = UiWaitUtils.MEDIUM_TIMEOUT)
            }
        }

        step("And I open mobtown-lemon deep link login flow again") {
            pages.loginPage.apply {
                clickStagingDeepLink("mobtown-lemon")
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I start SSO login flow again using the team SSO code") {
            pages.loginPage.apply {
                enterSSOCodeOnSSOLoginTab(ssoCode)
                clickLoginButton()
            }
        }

        step("And I complete post-login privacy prompt after logout") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickDeclineShareDataAlert()
            }
        }

        step("And I open the conversation with TeamOwner before restoring the backup") {
            pages.conversationListPage.apply {
                assertConversationIsVisibleWithTeamOwner(teamOwner.name ?: "")
                tapConversationNameInConversationList(teamOwner.name ?: "")
            }
        }

        step("Then I do not see the message Testing of the backup functionality in current conversation") {
            pages.conversationViewPage.apply {
                assertMessageNotVisible("Testing of the backup functionality")
            }
        }

        step("And I tap back button to leave the conversation before restore") {
            pages.conversationViewPage.apply {
                UiWaitUtils.waitFor(1.seconds)
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("And I open Settings from conversations menu entry again") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }

        step("And I open backup page again") {
            pages.settingsPage.apply {
                openBackupAndRestoreConversationsMenu()
            }
            pages.backupPage.apply {
                iSeeBackupPageHeading()
            }
        }

        step("When I restore the saved backup file") {
            pages.backupPage.apply {
                clickRestoreBackupButton()
                clickChooseBackupFileButton()
                selectBackupFileInDocumentsUI(clientUserManager, "user2Name")
            }
        }

        step("Then I see restore confirmation and accept it") {
            pages.backupPage.apply {
                waitUntilThisTextIsDisplayedOnBackupAlert("Conversations have been restored")
                clickOkButtonOnBackupAlert()
            }
        }

        step("And I open the conversation with TeamOwner after restore") {
            pages.conversationListPage.apply {
                assertConversationIsVisibleWithTeamOwner(teamOwner.name ?: "")
                tapConversationNameInConversationList(teamOwner.name ?: "")
            }
        }

        step("Then I see the restored message Testing of the backup functionality in current conversation") {
            pages.conversationViewPage.apply {
                assertRestoredBackupMessageIsVisibleInCurrentConversation("Testing of the backup functionality")
            }
        }
    }
}
