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
import backendUtils.team.getTeamByName
import backendUtils.team.TeamRoles
import backendUtils.team.updateUserProfileImage
import call.upgradeToEnterprisePlanResult
import com.wire.android.tests.core.BaseCallUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@Suppress("LargeClass")
@RunWith(AndroidJUnit4::class)
class ConferenceCallingRestrictionTests : BaseCallUiTest() {
    override val deletePersonalUsersAfterTest = true

    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser
    private lateinit var member2: ClientUser
    private lateinit var personalUser: ClientUser
    private lateinit var contact1: ClientUser
    private lateinit var guest: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        initCallTestHelpers()
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4277", "TC-4285", "TC-4276", "TC-4280")
    @Category("regression", "RC", "conferenceRestrictions")
    @Test
    fun givenIAmATeamMemberOfNonPayingTeam_whenIInitiateConferenceCall_thenISeeUpgradeInfoDialog() {
        step("Given There is a team owner TeamOwner with non paying team SuperTeam") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "SuperTeam",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds users Member1 and Member2 to team SuperTeam with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "SuperTeam",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation ConferenceCall with Member1 and Member2") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "ConferenceCall",
                "user2Name,user3Name",
                "SuperTeam"
            )
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
            backendClient.updateUserProfileImage(member1, context)
        }

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
                enterTeamMemberLoggingEmail(member1.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(member1.password ?: "")
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

        step("And I tap on conversation name ConferenceCall in conversation list") {
            pages.conversationListPage.clickGroupConversation("ConferenceCall")
        }

        step("When I tap start call button") {
            pages.conversationViewPage.iTapStartCallButton()
        }

        step("Then I see the feature unavailable alert with the team upgrade message") {
            pages.callingPage.assertFeatureUnavailableAlertVisible()
            pages.callingPage.assertFeatureUnavailableAlertSubtextVisible(
                "To start a conference call, your team needs to upgrade to the Enterprise plan."
            )
        }

        step("When I dismiss the alert, I do not see an ongoing group call") {
            pages.commonAppPage.tapOkButtonOnAlert()
            pages.callingPage.iDoNotSeeOngoingGroupCall()
        }

        // TC-4280 - I want to be informed as soon as my team is upgraded to paying team - as a team member
        step("And TeamOwner enables conference calling feature for team SuperTeam via backdoor") {
            runBlocking {
                callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
                    "user1Name",
                    "SuperTeam"
                )
            }
        }

        // Instances start after conference calling is enabled to avoid displaying the upgrade alert on Web.
        step("And TeamOwner and Member2 start Chrome calling instances") {
            runBlocking {
                callHelper.userXStartsInstance("user1Name,user3Name", "Chrome")
            }
        }

        step("And TeamOwner and Member2 accept the next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user1Name,user3Name")
            }
        }

        step("Then I see the Wire Enterprise alert with its upgrade confirmation and Learn more link") {
            pages.commonAppPage.assertWireEnterpriseAlertVisible()
            pages.commonAppPage.assertWireEnterpriseAlertTextVisible(
                "Your team was upgraded to Wire Enterprise, which gives you access to features such as " +
                        "conference calls and more."
            )
            pages.commonAppPage.assertWireEnterpriseAlertTextVisible("Learn more about Wire Enterprise")
        }

        step("When I open Learn more and dismiss Chrome prompts if visible") {
            pages.commonAppPage.tapLearnMoreWireEnterpriseLink()
            pages.chromePage.apply {
                dismissFirstRunIfVisible()
                dismissNotificationsPromptIfVisible()
                dismissCookieConsentIfVisible()
            }
        }

        step("Then I see webpage with wire.com in foreground") {
            pages.settingsPage.assertChromeUrlIsDisplayed("wire.com")
        }

        step("When I return to Wire and dismiss the Enterprise alert") {
            device.pressBack()
            pages.commonAppPage.tapOkButtonOnAlert()
        }

        // TC-4285 - I want to initiate a group conference call after my team is upgraded to paying team - as a team member
        // TC-4276 - I want to initiate an audio conference call (more than 2 participants) if I am a part of paying team
        step("When I tap start call button") {
            pages.conversationViewPage.iTapStartCallButton()
        }

        step("Then I see ongoing group call") {
            pages.callingPage.iSeeOngoingGroupCall()
        }

        step("And TeamOwner and Member2 verify call status changes to active within 90 seconds") {
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY(
                    "user1Name,user3Name",
                    "active",
                    90
                )
            }
        }

        step("And I see users TeamOwner and Member2 in ongoing group call") {
            callHelper.iSeeParticipantsInGroupCall("user1Name,user3Name")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4281", "TC-4283", "TC-4284", "TC-4288")
    @Category("regression", "RC", "conferenceRestrictions")
    @Test
    fun givenIAmATeamAdminOfNonPayingTeam_whenIInitiateConferenceCall_thenISeeUpgradeInfoDialog() {
        step("Given There is a team owner TeamOwner with non paying team SuperTeam") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "SuperTeam",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds users Member1 and Member2 to team SuperTeam with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "SuperTeam",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation ConferenceCall with Member1 and Member2") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "ConferenceCall",
                "user2Name,user3Name",
                "SuperTeam"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(teamOwner)
            backendClient.updateUserProfileImage(teamOwner, context)
        }

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
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
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

        step("And I tap on conversation name ConferenceCall in conversation list") {
            pages.conversationListPage.clickGroupConversation("ConferenceCall")
        }

        step("When I tap start call button") {
            pages.conversationViewPage.iTapStartCallButton()
        }

        step("Then I see the upgrade to Enterprise alert with the free-plan message") {
            pages.callingPage.assertUpgradeToEnterpriseAlertVisible()
            pages.callingPage.assertUpgradeToEnterpriseAlertSubtextVisible(
                "Your team is currently on the free Basic plan. Upgrade to Enterprise for access to " +
                        "features such as starting conferences and more."
            )
        }

        // TC-4284 - I want to see the wire pricing info page on clicking Learn more about wire pricing on dialog as a team admin
        step("When I open Learn more and dismiss Chrome prompts if visible") {
            pages.commonAppPage.tapLearnMoreWireEnterpriseLink()
            pages.chromePage.apply {
                dismissFirstRunIfVisible()
                dismissNotificationsPromptIfVisible()
                dismissCookieConsentIfVisible()
            }
        }

        step("Then I see webpage with teams.wire.com in foreground") {
            pages.settingsPage.assertChromeUrlIsDisplayed("teams.wire.com")
        }

        step("When I return to Wire and cancel the Enterprise alert") {
            device.pressBack()
            pages.commonAppPage.tapCancelButtonOnAlert()
        }

        // TC-4283 - I want to see group conversation after cancelling the upgrade dialog
        step("Then ConferenceCall remains in foreground without an ongoing group call") {
            pages.conversationViewPage.assertGroupConversationInForeground("ConferenceCall")
            pages.callingPage.iDoNotSeeOngoingGroupCall()
        }

        step("When I start a call and tap Upgrade now on the Enterprise alert") {
            pages.conversationViewPage.iTapStartCallButton()
            pages.commonAppPage.tapUpgradeNowButtonOnEnterpriseAlert()
        }

        step("Then I dismiss Chrome prompts and see teams.wire.com in foreground") {
            pages.chromePage.apply {
                dismissFirstRunIfVisible()
                dismissNotificationsPromptIfVisible()
                dismissCookieConsentIfVisible()
            }
            pages.settingsPage.assertChromeUrlIsDisplayed("teams.wire.com")
        }

        step("When I tap back button") {
            device.pressBack()
        }

        // TC-4288 - I want to initiate a group conference call after my team is upgraded to paying team - as a team admin
        step("And TeamOwner enables conference calling feature for team SuperTeam via backdoor") {
            runBlocking {
                callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
                    "user1Name",
                    "SuperTeam"
                )
            }
        }

        // Instances start after conference calling is enabled to avoid displaying the upgrade alert on Web.
        step("And Member1 and Member2 start Chrome calling instances") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name,user3Name", "Chrome")
            }
        }

        step("And Member1 and Member2 accept the next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user2Name,user3Name")
            }
        }

        step("Then I see the Wire Enterprise alert with its upgrade confirmation and Learn more link") {
            pages.commonAppPage.assertWireEnterpriseAlertVisible()
            pages.commonAppPage.assertWireEnterpriseAlertTextVisible(
                "Your team was upgraded to Wire Enterprise, which gives you access to features such as " +
                        "conference calls and more."
            )
            pages.commonAppPage.assertWireEnterpriseAlertTextVisible("Learn more about Wire Enterprise")
        }

        step("And I tap OK button on the alert") {
            pages.commonAppPage.tapOkButtonOnAlert()
        }

        step("When I tap start call button") {
            pages.conversationViewPage.iTapStartCallButton()
        }

        step("And Member1 and Member2 verify call status changes to active within 90 seconds") {
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY(
                    "user2Name,user3Name",
                    "active",
                    90
                )
            }
        }

        step("Then I see ongoing group call") {
            pages.callingPage.iSeeOngoingGroupCall()
        }

        step("When User Member1 and Member2 switch video on") {
            runBlocking {
                callingManager.switchVideoOn(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("And I wait for 2 seconds") {
            UiWaitUtils.waitFor(2.seconds)
        }

        step("Then I see users Member1 and Member2 in ongoing group video call") {
            callHelper.iSeeParticipantsInGroupVideoCall("user2Name,user3Name")
        }

        step("And I see video for users Member1 and Member2 is enabled") {
            callHelper.iSeeVideoForUsersEnabled("user2Name,user3Name")
        }

        step("And Users Member1 and Member2 verify to receive audio and video") {
            runBlocking {
                callingManager.verifyReceiveAudioAndVideo(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("And I tap hang up button") {
            pages.callingPage.iTapOnHangUpButton()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4278", "TC-4279")
    @Category("regression", "RC", "conferenceRestrictions")
    @Test
    fun givenIAmAPersonalUserOutsidePayingTeam_whenIInitiateConferenceCall_thenISeeFeatureUnavailableAlert() {
        step("Given There are personal users Name and Contact1") {
            clientUserManager.createPersonalUsersByAliases(
                listOf("user1Name", "user2Name"),
                backendClient
            )
            personalUser = clientUserManager.findUserByNameOrNameAlias("user1Name")
            contact1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user1Name")
                backendSetupHelper.usersSetUniqueUsername("user2Name")
            }
        }

        step("And Personal user Contact1 sets profile image") {
            backendClient.updateUserProfileImage(contact1, context)
        }

        step("And User Name is me") {
            clientUserManager.setSelfUser(personalUser)
            backendClient.updateUserProfileImage(personalUser, context)
        }

        step("And User Myself is connected to Contact1") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user2Name")
        }

        step("And User Myself has group conversation ConferenceCall with Contact1 as a personal user") {
            backendSetupHelper.userHasGroupConversationAsPersonalUser(
                "user1Name",
                "ConferenceCall",
                "user2Name"
            )
        }

        step("And Contact1 starts Chrome calling instance") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name", "Chrome")
            }
        }

        step("And Contact1 accepts the next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user2Name")
            }
        }

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
                enterPersonalUserLoggingEmail(personalUser.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(personalUser.password ?: "")
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

        step("And I tap on conversation name ConferenceCall in conversation list") {
            pages.conversationListPage.clickGroupConversation("ConferenceCall")
        }

        step("When I tap start call button") {
            pages.conversationViewPage.iTapStartCallButton()
        }

        step("Then I see the feature unavailable alert with the team upgrade message") {
            pages.callingPage.assertFeatureUnavailableAlertVisible()
            pages.callingPage.assertFeatureUnavailableAlertSubtextVisible(
                "To start a conference call, your team needs to upgrade to the Enterprise plan."
            )
        }

        step("When I dismiss the alert, I do not see an ongoing group call") {
            pages.commonAppPage.tapOkButtonOnAlert()
            pages.callingPage.iDoNotSeeOngoingGroupCall()
        }

        // TC-4279 - I want to initiate an audio call with 1 participants while I am not a part of paying team - as a personal user
        step("When I return to the conversation list and open Contact1") {
            device.pressBack()
            pages.conversationListPage.clickGroupConversation(contact1.name ?: "")
        }

        step("When I start a 1:1 call, the feature unavailable alert is not shown") {
            pages.conversationViewPage.iTapStartCallButton()
            pages.callingPage.assertFeatureUnavailableAlertNotVisible()
        }

        step("And Contact1 verifies waiting instance status changes to active within 30 seconds") {
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY("user2Name", "active", 30)
            }
        }

        step("Then I see Contact1 in the ongoing 1:1 call") {
            pages.callingPage.iSeeOngoingOneOnOneCall()
            pages.callingPage.iSeeParticipantInOngoingOneOnOneCall(contact1.name ?: "")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4286", "TC-4289")
    @Category("regression", "RC", "conferenceRestrictions")
    @Test
    fun givenIAmAPersonalGuestOutsidePayingTeam_whenIJoinAndInitiateConferenceCall_thenICanJoinButCannotInitiate() {
        step("Given There is a team owner TeamOwner with team SuperTeam") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "SuperTeam",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            runBlocking {
                backendClient.upgradeToEnterprisePlanResult(
                    backendClient.getTeamByName(teamOwner, "SuperTeam")
                )
            }
        }

        step("And I wait for 3 seconds") {
            UiWaitUtils.waitFor(3.seconds)
        }

        step("And TeamOwner enables conference calling feature for team SuperTeam via backdoor") {
            runBlocking {
                callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
                    "user1Name",
                    "SuperTeam"
                )
            }
        }

        step("And User TeamOwner adds users Member1 and Member2 to team SuperTeam with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "SuperTeam",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation ConferenceCall with Member1 and Member2") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "ConferenceCall",
                "user2Name,user3Name",
                "SuperTeam"
            )
        }

        step("And There is a personal user Guest") {
            clientUserManager.createPersonalUsersByAliases(listOf("user4Name"), backendClient)
            guest = clientUserManager.findUserByNameOrNameAlias("user4Name")
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user4Name")
            }
        }

        step("And User TeamOwner is connected to Guest") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user4Name")
        }

        step("And User TeamOwner adds user Guest to group conversation ConferenceCall") {
            backendSetupHelper.userXAddedContactsToGroupChat(
                "user1Name",
                "user4Name",
                "ConferenceCall"
            )
        }

        step("And User Guest is me") {
            clientUserManager.setSelfUser(guest)
            backendClient.updateUserProfileImage(guest, context)
        }

        step("And Member1 and Member2 start Chrome calling instances") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name,user3Name", "Chrome")
            }
        }

        step("And Member1 accepts the next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user2Name")
            }
        }

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
                enterPersonalUserLoggingEmail(guest.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(guest.password ?: "")
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

        step("And I tap on conversation name ConferenceCall in conversation list") {
            pages.conversationListPage.clickGroupConversation("ConferenceCall")
        }

        step("When User Member2 calls ConferenceCall") {
            runBlocking {
                callingManager.callGroupConversation("user3Name", "ConferenceCall")
            }
        }

        step("And I see incoming group call from group ConferenceCall") {
            pages.notificationsPage.iSeeIncomingGroupCall("ConferenceCall")
        }

        step("And I accept the call") {
            pages.callingPage.iAcceptCall()
        }

        step("And Member1 verifies waiting instance status changes to active within 60 seconds") {
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY("user2Name", "active", 60)
            }
        }

        step("And User Member1 and Member2 switch video on") {
            runBlocking {
                callingManager.switchVideoOn(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("And Users Member1 and Member2 verify to receive audio and video") {
            runBlocking {
                callingManager.verifyReceiveAudioAndVideo(
                    clientUserManager.splitAliases("user2Name,user3Name")
                )
            }
        }

        step("Then I see users Member1 and Member2 in ongoing group video call") {
            callHelper.iSeeParticipantsInGroupVideoCall("user2Name,user3Name")
        }

        step("And I see video for users Member1 and Member2 is enabled") {
            callHelper.iSeeVideoForUsersEnabled("user2Name,user3Name")
        }

        step("And Member1 stops calling") {
            runBlocking {
                callingManager.stopIncomingCall(clientUserManager.splitAliases("user2Name"))
            }
        }

        step("And I tap hang up button") {
            pages.callingPage.iTapOnHangUpButton()
        }

        step("And Member2 stops calling ConferenceCall") {
            runBlocking {
                callingManager.stopOutgoingCall(
                    clientUserManager.splitAliases("user3Name"),
                    "ConferenceCall"
                )
            }
        }

        step("And I wait for 5 seconds") {
            UiWaitUtils.waitFor(2.seconds)
        }

        // TC-4289 - I should not be able to initiate a conference call in a group I am added while I am not a part of paying team - as a personal user
        step("When I tap start call button") {
            pages.conversationViewPage.iTapStartCallButton()
        }

        step("Then I see the feature unavailable alert with the team upgrade message") {
            pages.callingPage.assertFeatureUnavailableAlertVisible()
            pages.callingPage.assertFeatureUnavailableAlertSubtextVisible(
                "To start a conference call, your team needs to upgrade to the Enterprise plan."
            )
        }

        step("When I dismiss the alert, I do not see an ongoing group call") {
            pages.commonAppPage.tapOkButtonOnAlert()
            pages.callingPage.iDoNotSeeOngoingGroupCall()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4287")
    @Category("regression", "conferenceRestrictions")
    @Test
    fun givenIAmPersonalUserOutsidePayingTeam_whenIJoinConferenceThroughGuestLink_thenICanJoinButCannotInitiate() {
        step("Given There is a team owner TeamOwner with team SuperTeam") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "SuperTeam",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            runBlocking {
                backendClient.upgradeToEnterprisePlanResult(
                    backendClient.getTeamByName(teamOwner, "SuperTeam")
                )
            }
        }

        step("And I wait for 3 seconds") {
            UiWaitUtils.waitFor(3.seconds)
        }

        step("And TeamOwner enables conference calling feature for team SuperTeam via backdoor") {
            runBlocking {
                callHelper.enableConferenceCallingFeatureViaBackdoorTeam(
                    "user1Name",
                    "SuperTeam"
                )
            }
        }

        step("And User TeamOwner adds users Member1 and Member2 to team SuperTeam with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "SuperTeam",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And User TeamOwner has group conversation ConferenceCall with Member1 and Member2 in team SuperTeam") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "ConferenceCall",
                "user2Name,user3Name",
                "SuperTeam"
            )
        }

        step("And User TeamOwner creates invite link for conversation ConferenceCall") {
            backendSetupHelper.userCreatesInviteLink("user1Name", "ConferenceCall")
        }

        step("And There is a personal user Guest") {
            clientUserManager.createPersonalUsersByAliases(listOf("user4Name"), backendClient)
            guest = clientUserManager.findUserByNameOrNameAlias("user4Name")
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user4Name")
            }
        }

        step("And User Guest is me") {
            clientUserManager.setSelfUser(guest)
            backendClient.updateUserProfileImage(guest, context)
        }

        step("And Member1 and Member2 start instances using Chrome") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name,user3Name", "Chrome")
            }
        }

        step("And Member2 accepts next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user3Name")
            }
        }

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
                enterPersonalUserLoggingEmail(guest.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(guest.password ?: "")
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

        step("When I minimise Wire") {
            device.pressHome()
        }

        step("And I open deep link for joining conversation ConferenceCall that user TeamOwner has sent me") {
            pages.commonAppPage.openDeepLink(
                backendSetupHelper.getClientDeepLinkForPublicConversation(
                    "user1Name",
                    "ConferenceCall"
                )
            )
        }

        step("And I tap join button on join conversation alert") {
            pages.commonAppPage.tapJoinConversationButton()
        }

        step("And User Member1 calls ConferenceCall") {
            runBlocking {
                callingManager.callGroupConversation("user2Name", "ConferenceCall")
            }
        }

        step("And I see incoming group call from group ConferenceCall") {
            pages.notificationsPage.iSeeIncomingGroupCall("ConferenceCall")
        }

        step("And I accept the call") {
            pages.callingPage.iAcceptCall()
        }

        step("And Member2 verifies that waiting instance status is changed to active in 10 seconds") {
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY("user3Name", "active", 10)
            }
        }

        step("And User Member2 verifies to send and receive audio") {
            runBlocking {
                callingManager.verifySendAndReceiveAudio(
                    clientUserManager.splitAliases("user3Name")
                )
            }
        }

        step("And User Member1 switches video on") {
            runBlocking {
                callingManager.switchVideoOn(clientUserManager.splitAliases("user2Name"))
            }
        }

        step("And I wait for 5 seconds") {
            UiWaitUtils.waitFor(5.seconds)
        }

        step("Then I see a QR code with Member1Email in video stream") {
            pages.callingPage.iSeeQrCodeContaining(member1.email ?: "")
        }

        step("And Member1 stops calling ConferenceCall") {
            runBlocking {
                callingManager.stopOutgoingCall(
                    clientUserManager.splitAliases("user2Name"),
                    "ConferenceCall"
                )
            }
        }

        step("And Member2 stops calling") {
            runBlocking {
                callingManager.stopIncomingCall(clientUserManager.splitAliases("user3Name"))
            }
        }

        step("And I tap hang up button") {
            pages.callingPage.iTapOnHangUpButton()
        }

        step("When I tap start call button") {
            pages.conversationViewPage.iTapStartCallButton()
        }

        step("Then I see the feature unavailable alert with the team upgrade message") {
            pages.callingPage.assertFeatureUnavailableAlertVisible()
            pages.callingPage.assertFeatureUnavailableAlertSubtextVisible(
                "To start a conference call, your team needs to upgrade to the Enterprise plan."
            )
        }

        step("When I dismiss the alert, I do not see an ongoing group call") {
            pages.commonAppPage.tapOkButtonOnAlert()
            pages.callingPage.iDoNotSeeOngoingGroupCall()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4290", "TC-4291")
    @Category("regression", "RC", "conferenceRestrictions")
    @Test
    fun givenIAmTeamMemberOfNonPayingTeam_whenIInitiateCallWithOneParticipant_thenAudioAndVideoCallCanBeCompleted() {
        step("Given There is a team owner TeamOwner with non paying team SuperTeam") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user3Name",
                "SuperTeam",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds users Member1 and Member2 to team SuperTeam with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user3Name",
                "user1Name,user2Name",
                "SuperTeam",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User Member1 has 1:1 conversation with Member2 in team SuperTeam") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "SuperTeam"
            )
        }

        step("And Member2 starts instance using Chrome") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name", "Chrome")
            }
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
            backendClient.updateUserProfileImage(member1, context)
        }

        step("And Member2 accepts next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user2Name")
            }
        }

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
                enterTeamMemberLoggingEmail(member1.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(member1.password ?: "")
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

        step("And I tap on conversation name Member2 in conversation list") {
            pages.conversationListPage.tapConversationNameInConversationList(member2.name ?: "")
        }

        step("When I start a 1:1 call, the feature unavailable alert is not shown") {
            pages.conversationViewPage.iTapStartCallButton()
            pages.callingPage.assertFeatureUnavailableAlertNotVisible()
        }

        step("And Member2 verifies that waiting instance status is changed to active in 30 seconds") {
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY("user2Name", "active", 30)
            }
        }

        step("And User Member2 verifies to send and receive audio") {
            runBlocking {
                callingManager.verifySendAndReceiveAudio(
                    clientUserManager.splitAliases("user2Name")
                )
            }
        }

        step("Then I see Member2 in the ongoing 1:1 call") {
            pages.callingPage.iSeeOngoingOneOnOneCall()
            pages.callingPage.iSeeParticipantInOngoingOneOnOneCall(member2.name ?: "")
        }

        // TC-4291 - I want to have a video call with 1 participants while I am not a part of paying team - as a team member
        step("When I turn camera on") {
            pages.callingPage.iTurnCameraOn()
        }

        step("And User Member2 switches video on") {
            runBlocking {
                callingManager.switchVideoOn(clientUserManager.splitAliases("user2Name"))
            }
        }

        step("And I wait for 2 seconds") {
            UiWaitUtils.waitFor(2.seconds)
        }

        step("Then I see user Member2 in ongoing group video call") {
            callHelper.iSeeParticipantsInGroupVideoCall("user2Name")
        }

        step("And User Member2 verifies to send and receive audio and video") {
            runBlocking {
                callingManager.verifySendAndReceiveAudioAndVideo(
                    clientUserManager.splitAliases("user2Name")
                )
            }
        }

        step("And I tap hang up button") {
            pages.callingPage.iTapOnHangUpButton()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4292")
    @Category("regression", "RC", "conferenceRestrictions")
    @Test
    fun givenIAmTeamOwnerOfNonPayingTeam_whenIInitiateGroupCall_thenUpgradeToEnterpriseAlertIsDisplayed() {
        step("Given There is a team owner TeamOwner with non paying team SuperTeam") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "SuperTeam",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team SuperTeam with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "SuperTeam",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation ConferenceCall with Member1 in team SuperTeam") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "ConferenceCall",
                "user2Name",
                "SuperTeam"
            )
        }

        step("And Member1 starts instance using Chrome") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name", "Chrome")
            }
        }

        step("And Member1 accepts next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user2Name")
            }
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(teamOwner)
            backendClient.updateUserProfileImage(teamOwner, context)
        }

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
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
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

        step("And I tap on conversation name ConferenceCall in conversation list") {
            pages.conversationListPage.clickGroupConversation("ConferenceCall")
        }

        step("When I tap start call button") {
            pages.conversationViewPage.iTapStartCallButton()
        }

        step("Then I see the upgrade to Enterprise alert with the free-plan message") {
            pages.callingPage.assertUpgradeToEnterpriseAlertVisible()
            pages.callingPage.assertUpgradeToEnterpriseAlertSubtextVisible(
                "Your team is currently on the free Basic plan. Upgrade to Enterprise for access to " +
                        "features such as starting conferences and more."
            )
        }

        step("When I cancel the alert, I do not see an ongoing group call") {
            pages.commonAppPage.tapCancelButtonOnAlert()
            pages.callingPage.iDoNotSeeOngoingGroupCall()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4293")
    @Category("regression", "RC", "conferenceRestrictions")
    @Test
    fun givenIAmNonPayingTeamMember_whenIInitiateAudioOrVideoCallInGroupWithOneParticipant_thenFeatureUnavailableAlertIsDisplayed() {
        step("Given There is a team owner TeamOwner with non paying team SuperTeam") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "SuperTeam",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team SuperTeam with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "SuperTeam",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation ConferenceCall with Member1 in team SuperTeam") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "ConferenceCall",
                "user2Name",
                "SuperTeam"
            )
        }

        step("And Member1 starts instance using Chrome") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name", "Chrome")
            }
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
            backendClient.updateUserProfileImage(member1, context)
        }

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
                enterTeamMemberLoggingEmail(member1.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(member1.password ?: "")
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

        step("And I tap on conversation name ConferenceCall in conversation list") {
            pages.conversationListPage.clickGroupConversation("ConferenceCall")
        }

        step("And I wait until Wire service notification disappears") {
            pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
        }

        step("When I tap start call button") {
            pages.conversationViewPage.iTapStartCallButton()
        }

        step("Then I see the feature unavailable alert with the team upgrade message") {
            pages.callingPage.assertFeatureUnavailableAlertVisible()
            pages.callingPage.assertFeatureUnavailableAlertSubtextVisible(
                "To start a conference call, your team needs to upgrade to the Enterprise plan."
            )
        }

        step("When I dismiss the alert, I do not see an ongoing group call") {
            pages.commonAppPage.tapOkButtonOnAlert()
            pages.callingPage.iDoNotSeeOngoingGroupCall()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4294")
    @Category("regression", "RC", "conferenceRestrictions")
    @Test
    fun givenIAmPersonalUser_whenIInitiateAudioOrVideoCallInGroupWithOneParticipant_thenFeatureUnavailableAlertIsDisplayed() {
        step("Given There are personal users Name and Contact") {
            clientUserManager.createPersonalUsersByAliases(
                listOf("user1Name", "user2Name"),
                backendClient
            )
            personalUser = clientUserManager.findUserByNameOrNameAlias("user1Name")
            contact1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user1Name,user2Name")
            }
        }

        step("And User Name is me") {
            clientUserManager.setSelfUser(personalUser)
            backendClient.updateUserProfileImage(personalUser, context)
        }

        step("And User Myself is connected to Contact") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user2Name")
        }

        step("And User Myself has group conversation Group with Contact as a personal user") {
            backendSetupHelper.userHasGroupConversationAsPersonalUser(
                "user1Name",
                "Group",
                "user2Name"
            )
        }

        step("And Contact starts instance using Chrome") {
            runBlocking {
                callHelper.userXStartsInstance("user2Name", "Chrome")
            }
        }

        step("And Contact accepts next incoming call automatically") {
            runBlocking {
                callHelper.userXAcceptsNextIncomingCallAutomatically("user2Name")
            }
        }

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
                enterPersonalUserLoggingEmail(personalUser.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(personalUser.password ?: "")
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

        step("And I tap on conversation name Group in conversation list") {
            pages.conversationListPage.clickGroupConversation("Group")
        }

        step("And I wait until Wire service notification disappears") {
            pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
        }

        step("When I tap start call button") {
            pages.conversationViewPage.iTapStartCallButton()
        }

        step("Then I see the feature unavailable alert with the team upgrade message") {
            pages.callingPage.assertFeatureUnavailableAlertVisible()
            pages.callingPage.assertFeatureUnavailableAlertSubtextVisible(
                "To start a conference call, your team needs to upgrade to the Enterprise plan."
            )
        }

        step("When I dismiss the alert, I do not see an ongoing group call") {
            pages.commonAppPage.tapOkButtonOnAlert()
            pages.callingPage.iDoNotSeeOngoingGroupCall()
        }
    }
}
