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
import backendUtils.client.getBackendClientIds
import backendUtils.client.removeBackendClient
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import logger.WireTestLogger
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class MultiAccountSessionScope : BaseUiTest() {

    private val log = WireTestLogger.getLog("MetroSessionScope")
    private var primaryUser: ClientUser? = null
    private var secondaryUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @TestCaseId("TC-11259")
    @Category("regression", "RC", "multiAccountSessionScope")
    @Test
    fun givenLoggedInAccount_whenSecondAccountHitsTooManyDevicesAndLoginIsCancelled_thenPreviousSessionIsRestored() {
        step("Prepare staging users and make the second account reach the client limit") {
            prepareSecondaryUserWithClientLimit(teamName = "SessionScopeMetroLimit")
        }

        step("Login the first account") {
            loginUser(primaryUser)
        }

        step("Open add account flow from the self profile") {
            openAddAccountFlow()
        }

        step("Login the second account until too many devices screen is shown") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
                enterUserIdentifier(secondaryUser?.email.orEmpty())
                clickLoginButton()
                enterUserPassword(secondaryUser?.password.orEmpty())
                clickLoginButton()
                assertRemoveDeviceScreenVisible()
            }
        }

        step("Cancel remove-device login and verify previous session is restored") {
            device.pressBack()
            pages.selfUserProfilePage.apply {
                assertCancelLoginDialogVisible()
                confirmCancelLogin()
            }
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    @TestCaseId("TC-11260")
    @Category("regression", "RC", "multiAccountSessionScope")
    @Test
    fun givenTwoLoggedInAccounts_whenSwitchingFromProfile_thenConversationListUsesSelectedSession() {
        step("Prepare staging users") {
            prepareTeamUsers(teamName = "SessionScopeMetroSwitch")
        }

        step("Login the first account") {
            loginUser(primaryUser)
        }

        step("Add and login the second account") {
            openAddAccountFlow()
            loginUser(secondaryUser)
        }

        step("Switch back to the first account from the self profile") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                tapOtherAccountByName(primaryUser?.name.orEmpty())
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("Verify the second account is available after switching") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                tapOtherAccountByName(secondaryUser?.name.orEmpty())
            }
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    @TestCaseId("TC-11261")
    @Category("regression", "RC", "multiAccountSessionScope")
    @Test
    fun givenSingleLoggedInAccount_whenCurrentClientIsRemovedAndUserLogsInAgain_thenMlsSessionIsRestored() {
        step("Prepare staging users") {
            prepareTeamUsers(teamName = "SessionScopeMetroRemovedSingle")
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("Login the first account") {
            loginUser(primaryUser)
        }

        step("Remove the current client from the backend") {
            removeCurrentBackendClient(primaryUser)
        }

        step("Confirm removed-device dialog and verify login screen opens") {
            pages.selfUserProfilePage.apply {
                assertRemovedDeviceDialogVisible()
                confirmRemovedDeviceDialog()
            }
            pages.registrationPage.assertAuthEntryVisible()
        }

        step("Log the removed user in again without restarting the app") {
            loginUser(primaryUser, configureStagingBackend = false)
        }

        step("Open the existing MLS conversation from the recreated session") {
            pages.conversationListPage.tapConversationNameInConversationList(secondaryUser?.name.orEmpty())
        }

        step("Receive an MLS message after the new client joins by external commit") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user2Name",
                POST_RELOGIN_MLS_MESSAGE,
                "Device1",
                "user1Name"
            )
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(POST_RELOGIN_MLS_MESSAGE)
        }

        step("Send an MLS message from the recreated session") {
            pages.conversationViewPage.apply {
                typeMessageInInputField(POST_RELOGIN_REPLY)
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation(POST_RELOGIN_REPLY)
            }
        }

        step("Verify the test-service device receives and decrypts the MLS reply") {
            testServiceHelper.assertMessageReceivedInPersonalMlsConversation(
                receiverAlias = "user2Name",
                deviceName = "Device1",
                conversationWithAlias = "user1Name",
                message = POST_RELOGIN_REPLY,
            )
        }
    }

    @TestCaseId("TC-11262")
    @Category("regression", "RC", "multiAccountSessionScope")
    @Test
    fun givenTwoLoggedInAccounts_whenCurrentClientIsRemovedRemotely_thenNextSessionIsRestored() {
        step("Prepare staging users") {
            prepareTeamUsers(teamName = "SessionScopeMetroRemovedSwitch")
        }

        step("Login the first account") {
            loginUser(primaryUser)
        }

        step("Add and login the second account") {
            openAddAccountFlow()
            loginUser(secondaryUser)
        }

        step("Remove the current second-account client from the backend") {
            removeCurrentBackendClient(secondaryUser)
        }

        step("Confirm removed-device dialog and verify the remaining session is restored") {
            pages.selfUserProfilePage.apply {
                assertRemovedDeviceDialogVisible()
                confirmRemovedDeviceDialog()
            }
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    @TestCaseId("TC-11263")
    @Category("regression", "RC", "multiAccountSessionScope")
    @Test
    fun givenTwoLoggedInAccounts_whenLoggingOutCurrentAccount_thenNextSessionIsRestored() {
        step("Prepare staging users") {
            prepareTeamUsers(teamName = "SessionScopeMetroLogoutSwitch")
        }

        step("Login the first account") {
            loginUser(primaryUser)
        }

        step("Add and login the second account") {
            openAddAccountFlow()
            loginUser(secondaryUser)
        }

        step("Logout the current second account") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                tapLogoutButton()
                iSeeClearDataOnLogOutAlert()
                tapLogoutButton()
            }
        }

        step("Verify the remaining session is restored") {
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    @TestCaseId("TC-11264")
    @Category("regression", "RC", "multiAccountSessionScope")
    @Test
    fun givenNoLoggedInAccount_whenLoginHitsTooManyDevicesAndIsCancelled_thenLoginScreenOpens() {
        step("Prepare staging user with the client limit reached") {
            prepareSecondaryUserWithClientLimit(teamName = "SessionScopeMetroLimitFreshCancel")
        }

        step("Login until too many devices screen is shown") {
            loginSecondaryUserUntilRemoveDeviceScreen()
        }

        step("Cancel remove-device login and verify auth flow opens") {
            device.pressBack()
            pages.selfUserProfilePage.apply {
                assertCancelLoginDialogVisible()
                confirmCancelLogin()
            }
            UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA, clearData = false)
            pages.registrationPage.assertEmailWelcomePage()
        }
    }

    @TestCaseId("TC-11265")
    @Category("regression", "RC", "multiAccountSessionScope")
    @Test
    fun givenNoLoggedInAccount_whenRemovingDeviceFromTooManyDevices_thenLoginCompletes() {
        step("Prepare staging user with the client limit reached") {
            prepareSecondaryUserWithClientLimit(teamName = "SessionScopeMetroLimitFreshRemove")
        }

        step("Login until too many devices screen is shown") {
            loginSecondaryUserUntilRemoveDeviceScreen()
        }

        step("Remove one existing device and verify login completes") {
            pages.loginPage.apply {
                assertRemoveDeviceListVisible()
                clickFirstRemoveDeviceButton()
                assertRemoveDeviceDialogVisible()
                enterRemoveDevicePassword(secondaryUser?.password.orEmpty())
                confirmRemoveDevice()
            }
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                waitUntilConversationPageVisibleDismissingPostLoginPrompts(timeout = POST_REMOVE_DEVICE_LOGIN_TIMEOUT)
            }
        }
    }

    private fun prepareTeamUsers(teamName: String) {
        backendSetupHelper.createTeamOwnerByAlias(
            "user1Name",
            teamName,
            "en_US",
            true,
            backendClient,
            context
        )
        backendSetupHelper.userXAddsUsersToTeam(
            "user1Name",
            "user2Name",
            teamName,
            TeamRoles.Member,
            backendClient,
            context,
            true
        )
        primaryUser = clientUserManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
        secondaryUser = clientUserManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
    }

    private fun loginUser(user: ClientUser?, configureStagingBackend: Boolean = true) {
        pages.loginPage.apply {
            if (configureStagingBackend) {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
            enterUserIdentifier(user?.email.orEmpty())
            clickLoginButton()
            enterUserPassword(user?.password.orEmpty())
            clickLoginButton()
        }
        pages.registrationPage.apply {
            waitUntilLoginFlowIsCompleted()
            clickAllowNotificationButton()
            clickDeclineShareDataAlert()
            assertConversationPageVisible()
        }
    }

    private fun openAddAccountFlow() {
        pages.conversationListPage.clickUserProfileButton()
        pages.selfUserProfilePage.apply {
            iSeeUserProfilePage()
            tapNewAccountButton()
        }
    }

    private fun removeCurrentBackendClient(user: ClientUser?) {
        val clientUser = requireNotNull(user) { "Cannot remove backend client for a missing user." }
        val clientId = backendClient.getBackendClientIds(clientUser).singleOrNull()
            ?: throw AssertionError("Expected exactly one backend client for ${clientUser.name}.")
        backendClient.removeBackendClient(clientUser, clientId)
    }

    private fun prepareSecondaryUserWithClientLimit(teamName: String) {
        prepareTeamUsers(teamName)
        val user = requireNotNull(secondaryUser) { "Secondary user was not created." }
        log.info("Preparing too-many-devices setup for user2Name email=${user.email} id=${user.id} backend=${user.backendName}")
        repeat(EXISTING_CLIENTS_LIMIT) { index ->
            testServiceHelper.addDevice("user2Name", null, "ExistingDevice${index + 1}")
            val clientIds = backendClient.getBackendClientIds(user)
            log.info("After ExistingDevice${index + 1}: backend client count=${clientIds.size}, ids=$clientIds")
        }
    }

    private fun loginSecondaryUserUntilRemoveDeviceScreen() {
        pages.loginPage.apply {
            clickStagingDeepLink()
            clickProceedButtonOnDeeplinkOverlay()
            clickContinueButtonOnBackendConfigSuccess()
            enterUserIdentifier(secondaryUser?.email.orEmpty())
            clickLoginButton()
            enterUserPassword(secondaryUser?.password.orEmpty())
            clickLoginButton()
            assertRemoveDeviceScreenVisible()
        }
    }

    private companion object {
        const val EXISTING_CLIENTS_LIMIT = 7
        const val POST_RELOGIN_MLS_MESSAGE = "MLS after client re-registration"
        const val POST_RELOGIN_REPLY = "MLS reply after client re-registration"
        val POST_REMOVE_DEVICE_LOGIN_TIMEOUT = 120.seconds
    }
}
