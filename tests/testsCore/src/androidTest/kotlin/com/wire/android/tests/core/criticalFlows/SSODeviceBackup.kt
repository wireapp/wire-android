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

import SSOServiceHelper.thereIsASSOTeamOwnerForOkta
import SSOServiceHelper.userAddsOktaUser
import SSOServiceHelper.userXIsMe
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import backendUtils.BackendClient
import backendUtils.team.TeamHelper
import backendUtils.team.deleteTeam
import com.wire.android.tests.core.pages.AllPages
import com.wire.android.tests.support.UiAutomatorSetup
import deleteDownloadedFilesContaining
import kotlinx.coroutines.runBlocking
import okta.OktaApiClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.test.inject
import service.TestServiceHelper
import uiautomatorutils.UiWaitUtils.WaitUtils.waitFor
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.getValue
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId

@RunWith(AndroidJUnit4::class)
class SSODeviceBackup : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private lateinit var oktaApiClient: OktaApiClient
    private lateinit var backendClient: BackendClient
    private lateinit var teamHelper: TeamHelper
    private lateinit var testServiceHelper: TestServiceHelper
    private var teamOwner: ClientUser? = null
    private var member1: ClientUser? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_INTERNAL)
        backendClient = BackendClient.loadBackend("STAGING")
        teamHelper = TeamHelper()
        SSOServiceHelper.usersManager = teamHelper.usersManager
        oktaApiClient = OktaApiClient()
        testServiceHelper = TestServiceHelper(teamHelper.usersManager)
    }

    @After
    fun tearDown() {
        runCatching { teamOwner?.deleteTeam(backendClient) }
        deleteDownloadedFilesContaining("Wire")
        oktaApiClient.cleanUp()
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8604")
    @Category("criticalFlow")
    @Test
    fun givenSSOTeamWithOkta_whenSettingUpNewDeviceAndRestoringBackup_thenMessageIsRestored() {
        step("Prepare backend SSO team in Okta (owner + member) and set current user") {
            runBlocking {
                testServiceHelper.thereIsASSOTeamOwnerForOkta(
                    context,
                    "user1Name",
                    "Messaging",
                    oktaApiClient
                )

                testServiceHelper.userAddsOktaUser("user1Name", "user2Name", oktaApiClient)

                testServiceHelper.userXIsMe("user2Name")

                teamOwner = teamHelper.usersManager.findUserBy(
                    "user1Name",
                    ClientUserManager.FindBy.NAME_ALIAS
                )
                member1 = teamHelper.usersManager.findUserBy(
                    "user2Name",
                    ClientUserManager.FindBy.NAME_ALIAS
                )
            }

            step("Get SSO code and wait for Okta app assignment sync") {
                val ssoCode = SSOServiceHelper.getSSOCode()
                waitFor(20) // Delay added to allow Okta app assignment to fully sync and avoid 403 error

                step("Start SSO login flow using SSO code") {
                    pages.registrationPage.apply {
                        assertEmailWelcomePage()
                    }
                    pages.loginPage.apply {
                        clickStagingDeepLink()
                        clickProceedButtonOnDeeplinkOverlay()
                    }
                    pages.loginPage.apply {
                        enterSSOCodeOnSSOLoginTab(ssoCode)
                        clickLoginButton()
                    }
                }

                step("Complete Okta login (email + password) and wait for Wire auth handoff") {
                    pages.ssoPage.apply {
                        waitUntilOktaPageLoaded()
                        enterOktaEmail(member1?.email ?: "")
                        enterOktaPassword(member1?.password ?: "")
                        tapOktaSignIn()
                        waitFor(5) // Wait for Okta → Wire auth handoff to finish; otherwise, setting up wire page will not succeed.
                    }
                }

                step("Finish first-time setup after login (notifications, username, privacy prompts)") {
                    pages.registrationPage.apply {
                        waitUntilLoginFlowIsCompleted()
                        clickAllowNotificationButton()
                        setUserName(member1?.uniqueUsername.orEmpty())
                        clickConfirmButton()
                        waitUntilLoginFlowIsCompleted()
                        clickDeclineShareDataAlert()
                    }
                }

                step("Start new conversation flow from conversation list") {
                    pages.conversationListPage.apply {
                        tapStartNewConversationButton()
                    }
                }

                step("Search for team owner and open their profile from search results") {
                    pages.searchPage.apply {
                        tapSearchPeopleField()
                        typeUniqueUserNameInSearchField(teamHelper, "user1Name")
                        assertUsernameInSearchResultIs(teamOwner?.name ?: "")
                        tapUsernameInSearchResult(teamOwner?.name ?: "")
                    }
                }

                step("Start conversation with team owner from connected user profile") {
                    pages.connectedUserProfilePage.apply {
                        assertStartConversationButtonVisible()
                        clickStartConversationButton()
                    }
                }

                step("Send message in conversation and verify it is visible") {
                    pages.conversationViewPage.apply {
                        assertConversationScreenVisible()
                        typeMessageInInputField("Testing of the backup functionality")
                        clickSendButton()
                        assertSentMessageIsVisibleInCurrentConversation("Testing of the backup functionality")
                        tapBackButtonToCloseConversationViewPage()
                    }
                }

                step("Exit profile/new conversation flow and return to conversation list") {
                    pages.connectedUserProfilePage.apply {
                        tapCloseButtonOnConnectedUserProfilePage()
                    }
                    pages.conversationListPage.apply {
                        clickCloseButtonOnNewConversationScreen()
                        assertConversationListVisible()
                    }
                }

                step("Open Settings from conversations menu entry") {
                    pages.conversationListPage.apply {
                        clickConversationsMenuEntry()
                        clickSettingsButtonOnMenuEntry()
                    }
                }

                step("Create backup and save backup file") {
                    pages.settingsPage.apply {
                        openBackupAndRestoreConversationsMenu()
                        iSeeBackupPageHeading()
                        clickCreateBackupButton()
                        clickBackUpNowButton()
                        iSeeBackupConfirmation("Conversations successfully saved")
                        iTapSaveFileButton()
                        iTapSaveInOSMenuButton()
                        iSeeBackupPageHeading()
                        clickBackButtonOnSettingsPage()
                    }
                }

                step("Navigate to self profile and log out with 'delete personal information' checked") {
                    pages.conversationListPage.apply {
                        clickConversationsMenuEntry()
                        clickConversationsButtonOnMenuEntry()
                        clickUserProfileButton()
                    }
                    pages.selfUserProfilePage.apply {
                        iSeeUserProfilePage()
                        tapLogoutButton()
                        iSeeClearDataOnLogOutAlert()
                        iSeeInfoTextCheckbox("Delete all your personal information and conversations on this device")
                        tapInfoTextCheckbox()
                        tapLogoutButton()
                    }
                }

                step("Start SSO login again using SSO code") {
                    pages.registrationPage.apply {
                        assertEmailWelcomePage()
                    }
                    pages.loginPage.apply {
                        clickStagingDeepLink()
                        clickProceedButtonOnDeeplinkOverlay()
                    }
                    pages.loginPage.apply {
                        enterSSOCodeOnSSOLoginTab(ssoCode)
                        clickLoginButton()
                    }

                    waitFor(5) // Wait for Okta → Wire auth handoff to finish;
                }

                step("Finish login flow after logout (decline share data)") {
                    pages.registrationPage.apply {
                        waitUntilLoginFlowIsCompleted()
                        clickDeclineShareDataAlert()
                    }
                }

                step("Open conversation with team owner and verify message is not visible before restore") {
                    pages.conversationListPage.apply {
                        assertConversationIsVisibleWithTeamOwner(teamOwner?.name ?: "")
                        tapConversationNameInConversationList(teamOwner?.name ?: "")
                    }
                    pages.conversationViewPage.apply {
                        assertMessageNotVisible("Testing of the backup functionality")
                        tapBackButtonToCloseConversationViewPage()
                    }
                }

                step("Open Settings again to restore backup") {
                    pages.conversationListPage.apply {
                        clickConversationsMenuEntry()
                        clickSettingsButtonOnMenuEntry()
                    }
                }

                step("Restore backup by selecting saved file and confirm restore completion") {
                    pages.settingsPage.apply {
                        openBackupAndRestoreConversationsMenu()
                        iSeeBackupPageHeading()
                        clickRestoreBackupButton()
                        clickChooseBackupFileButton()
                        selectBackupFileInDocumentsUI(teamHelper, "user2Name")
                        waitUntilThisTextIsDisplayedOnBackupAlert("Conversations have been restored")
                        clickOkButtonOnBackupAlert()
                    }
                }

                step("Open conversation again and verify message visibility after restore") {
                    pages.conversationListPage.apply {
                        assertConversationListVisible()
                        assertConversationIsVisibleWithTeamOwner(teamOwner?.name ?: "")
                        tapConversationNameInConversationList(teamOwner?.name ?: "")
                    }
                    pages.conversationViewPage.apply {
                        assertRestoredBackupMessageIsVisibleInCurrentConversation("Testing of the backup functionality")
                    }
                }
            }
        }
    }
}
