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
import com.wire.android.tests.core.di.testModule
import com.wire.android.tests.core.pages.AllPages
import com.wire.android.tests.support.UiAutomatorSetup
import deleteDownloadedFilesContaining
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import service.TestServiceHelper
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.getValue

@RunWith(AndroidJUnit4::class)
class SSODeviceBackup : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.Companion.create {
        modules(testModule)
    }
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice

    lateinit var context: Context
    var teamOwner: ClientUser? = null
    var member1: ClientUser? = null

    var backendClient: BackendClient? = null
    val teamServiceHelper by lazy {
        TestServiceHelper()
    }
    val teamHelper by lazy {
        TeamHelper()
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_INTERNAL)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        teamOwner?.deleteTeam(backendClient!!)
        deleteDownloadedFilesContaining("Wire")
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @Test
    fun givenSSOTeamWithOkta_whenSettingUpNewDeviceAndRestoringBackup_thenMessageIsRestored() {

        runBlocking {
            teamServiceHelper.thereIsASSOTeamOwnerForOkta(context, "user1Name", "Messaging")

            teamServiceHelper.userAddsOktaUser("user1Name", "user2Name")
            teamServiceHelper.userXIsMe("user2Name")
        }

        teamOwner = teamHelper?.usersManager!!.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
        member1 = teamHelper?.usersManager!!.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
        val ssoCode = SSOServiceHelper.getSSOCode()

        pages.registrationPage.apply {
            assertEmailWelcomePage()
        }
        pages.loginPage.apply {
            clickStagingDeepLink()
            clickProceedButtonOnDeeplinkOverlay()
        }
        pages.chromePage.apply {
            clearChromeBrowserCache()
        }
        pages.loginPage.apply {
            enterSSOCodeOnSSOLoginTab(ssoCode)
            clickLoginButton()
        }
        pages.chromePage.apply {
            clickUseWithoutAccount()
        }
        pages.ssoPage.apply {
            enterOktaEmail(member1?.email ?: "")
            enterOktaPassword(member1?.password ?: "")
            tapOktaSignIn()
        }
        pages.registrationPage.apply {
            clickAllowNotificationButton()
            setUserName(member1?.uniqueUsername.orEmpty())
            clickConfirmButton()
            waitUntilLoginFlowIsCompleted()
            clickDeclineShareDataAlert()
        }
        pages.conversationListPage.apply {
            tapStartNewConversationButton()
        }
        pages.searchPage.apply {
            tapSearchPeopleField()
            typeUserNameInSearchField("user1Name")
            assertUsernameInSearchResultIs(teamOwner?.name ?: "")
            tapUsernameInSearchResult(teamOwner?.name ?: "")
        }
        pages.connectedUserProfilePage.apply {
            assertStartConversationButtonVisible()
            clickStartConversationButton()
        }
        pages.conversationViewPage.apply {
            assertConversationScreenVisible()
            typeMessageInInputField("Testing of the backup functionality")
            clickSendButton()
            assertSentMessageIsVisibleInCurrentConversation("Testing of the backup functionality")
            tapBackButtonToCloseConversationViewPage()
        }
        pages.connectedUserProfilePage.apply {
            tapCloseButtonOnConnectedUserProfilePage()
        }
        pages.conversationListPage.apply {
            clickCloseButtonOnNewConversationScreen()
            assertConversationListVisible()
        }
        pages.conversationListPage.apply {
            clickConversationsMenuEntry()
            clickSettingsButtonOnMenuEntry()
        }
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
        pages.registrationPage.apply {
            waitUntilLoginFlowIsCompleted()
            clickDeclineShareDataAlert()
        }
        pages.conversationListPage.apply {
            assertConversationIsVisibleWithTeamOwner(teamOwner?.name ?: "")
            tapConversationNameInConversationList(teamOwner?.name ?: "")
        }
        pages.conversationViewPage.apply {
            assertMessageNotVisible("Testing of the backup functionality")
            tapBackButtonToCloseConversationViewPage()
        }
        pages.conversationListPage.apply {
            clickConversationsMenuEntry()
            clickSettingsButtonOnMenuEntry()
        }
        pages.settingsPage.apply {
            openBackupAndRestoreConversationsMenu()
            iSeeBackupPageHeading()
            clickRestoreBackupButton()
            clickChooseBackupFileButton()
            selectBackupFileInDocumentsUI(teamHelper, "user2Name")
            waitUntilThisTextIsDisplayedOnBackupAlert("Conversations have been restored")
            clickOkButtonOnBackupAlert()
        }
        pages.conversationListPage.apply {
            assertConversationListVisible()
            assertConversationIsVisibleWithTeamOwner(teamOwner?.name ?: "")
            tapConversationNameInConversationList(teamOwner?.name ?: "")
        }
        pages.conversationViewPage.apply {
            assertMessageNotVisible("Testing of the backup functionality")
        }
    }
}
