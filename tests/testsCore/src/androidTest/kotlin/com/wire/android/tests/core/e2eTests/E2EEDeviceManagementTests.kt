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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class E2EEDeviceManagementTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4323", "TC-4324")
    @Category("regression", "RC", "E2EEDeviceManagement")
    @Test
    fun givenIHaveCurrentAndPreviouslyAddedDevices_whenIOpenDevicesScreen_thenCurrentAndPreviouslyAddedDevicesAreDisplayed() {
        step("Given There is a team owner TeamOwner with team DeviceManagement") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "DeviceManagement",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team DeviceManagement with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "DeviceManagement",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team DeviceManagement") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "DeviceManagement"
            )
        }

        step("And User Member1 adds 2 devices") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                addDevice("user2Name", null, "Device2")
            }
        }

        step("And User TeamOwner adds 1 device") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("And User Member1 is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I see conversation TeamOwner in conversation list") {
            pages.conversationListPage.assertConversationVisible(teamOwner.name ?: "")
        }

        step("When I open Settings from the conversation list") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }

        step("And I open Manage your Devices") {
            pages.settingsPage.tapManageDevicesMenu()
        }

        step("Then I see my current device and previously added devices Device1 and Device2") {
            pages.devicesPage.apply {
                assertCurrentDeviceVisible()
                assertOtherDeviceVisible("Device1")
                assertOtherDeviceVisible("Device2")
            }
        }

        step("When I close Devices and return to the conversation list") {
            pages.devicesPage.closeDevicesScreen()
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickConversationsButtonOnMenuEntry()
                assertConversationListVisible()
            }
        }

        step("Then I see conversation TeamOwner in conversation list") {
            pages.conversationListPage.assertConversationVisible(teamOwner.name ?: "")
        }

        // TC-4324 - I want to see other users devices displayed in their user profile
        step("When I open TeamOwner's devices from their profile") {
            pages.conversationListPage.tapConversationNameInConversationList(teamOwner.name ?: "")
            pages.conversationViewPage.click1On1ConversationDetails(teamOwner.name ?: "")
            pages.connectedUserProfilePage.tapDevicesTab()
        }

        step("Then I see Desktop listed under devices in TeamOwner's profile") {
            pages.connectedUserProfilePage.assertDeviceVisible("Desktop")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4325")
    @Category("regression", "RC", "E2EEDeviceManagement")
    @Test
    fun givenCurrentDevice_whenIOpenManageDevices_thenDeviceIdAndAddedDateAreDisplayed() {
        step("Given There is a team owner TeamOwner with team DeviceManagement") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "DeviceManagement",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team DeviceManagement with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "DeviceManagement",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("When I open Manage your Devices from Settings") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.tapManageDevicesMenu()
        }

        step("Then I see my current device with its ID and added date") {
            pages.devicesPage.apply {
                assertCurrentDeviceVisible()
                assertCurrentDeviceIdAndAddedDateVisible()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4327")
    @Category("regression", "RC", "E2EEDeviceManagement")
    @Test
    fun givenADeviceWasRemovedFromAnotherClient_whenIOpenDevicesScreen_thenDeviceListIsUpdated() {
        step("Given There is a team owner TeamOwner with team DeviceManagement") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "DeviceManagement",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team DeviceManagement with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "DeviceManagement",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User Member1 adds 2 devices") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                addDevice("user2Name", null, "Device2")
            }
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I wait until Wire service notification disappears") {
            pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
        }

        step("When I open Manage your Devices from Settings") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.tapManageDevicesMenu()
        }

        step("And I see my current device and other devices Device1 and Device2") {
            pages.devicesPage.apply {
                assertCurrentDeviceVisible()
                assertOtherDeviceVisible("Device1")
                assertOtherDeviceVisible("Device2")
            }
        }

        step("When I close Devices and Member1's Device1 is removed from another client") {
            pages.devicesPage.closeDevicesScreen()
            backendClient.removeBackendClient(
                member1,
                backendClient.getBackendClientIds(member1, "Device1").single()
            )
        }

        step("And I reopen Manage your Devices") {
            pages.settingsPage.tapManageDevicesMenu()
        }

        step("Then Device1 is removed while the current device and Device2 remain visible") {
            pages.devicesPage.apply {
                assertCurrentDeviceVisible()
                assertOtherDeviceVisible("Device2")
                assertOtherDeviceNotVisible("Device1")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4328")
    @Category("regression", "RC", "E2EEDeviceManagement")
    @Test
    fun givenIAmLoggedIn_whenMyAccountIsUsedOnAnotherDevice_thenISeeAnAlert() {
        step("Given There is a team owner TeamOwner with team DeviceManagement") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "DeviceManagement",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team DeviceManagement with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "DeviceManagement",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("When User Member1 adds 1 device") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("Then I see an alert that my account was used on another device with its warning subtext") {
            pages.commonAppPage.apply {
                assertAccountUsedOnAnotherDeviceAlertVisible()
                assertAddedDeviceAlertSubtextVisible("remove the device and reset your password.")
            }
        }

        step("When I tap Manage Devices") {
            pages.commonAppPage.tapManageDevicesButton()
        }

        step("Then I see Manage Devices Page") {
            pages.devicesPage.assertManageDevicesPageVisible()
        }

        step("And I close Devices and see a connected conversation list") {
            pages.devicesPage.closeDevicesScreen()
            pages.conversationListPage.apply {
                assertConversationListVisible()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4329")
    @Category("regression", "RC", "E2EEDeviceManagement")
    @Test
    fun givenIHaveTwoLoggedInAccounts_whenMySecondAccountIsUsedOnAnotherDevice_thenISeeAnAlert() {
        step("Given There is a team owner TeamOwner with team DeviceManagement") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "DeviceManagement",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 to team DeviceManagement with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "DeviceManagement",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User Member1 is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I wait until Wire service notification disappears") {
            pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
        }

        step("And I open User Profile and see Member1 is my currently active account") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertCurrentAccountActive(member1.name ?: "")
            }
        }

        step("And I tap New Team or Account button") {
            pages.selfUserProfilePage.tapNewTeamOrAddAccountButton()
        }

        step("And User TeamOwner is me") {
            clientUserManager.setSelfUser(teamOwner)
        }

        loginToStagingAs(teamOwner)

        step("And I see conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }

        step("When User Member1 adds 1 device") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("Then I see an alert that my second account Member1 was used on another device") {
            pages.commonAppPage.apply {
                assertSecondAccountUsedOnAnotherDeviceAlertVisible(member1.name ?: "")
                assertAddedDeviceAlertSubtextVisible(
                    "remove the device (navigate to “Manage Devices” in the Settings section of this account), " +
                            "and reset your password."
                )
            }
        }

        step("When I tap Switch Account") {
            pages.commonAppPage.tapSwitchAccountButton()
        }

        step("Then I see Manage Devices Page") {
            pages.devicesPage.assertManageDevicesPageVisible()
        }

        step("And I close Devices and see a connected conversation list") {
            pages.devicesPage.closeDevicesScreen()
            pages.conversationListPage.apply {
                assertConversationListVisible()
                waitUntilWaitingForNetworkIsInvisible()
            }
        }

        step("Then I open User Profile and see Member1 is my currently active account") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertCurrentAccountActive(member1.name ?: "")
            }
        }
    }

    // Keeps the repeated staging login flow consistent across device-management scenarios.
    private fun loginToStagingAs(user: ClientUser) {
        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterUserIdentifier(user.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterUserPassword(user.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }
    }
}
