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

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import backendUtils.BackendClient
import backendUtils.team.TeamHelper
import backendUtils.team.deleteTeam
import backendUtils.user.deleteUser
import com.wire.android.testSupport.BuildConfig
import com.wire.android.tests.core.pages.AllPages
import com.wire.android.tests.support.UiAutomatorSetup
import kotlinx.coroutines.runBlocking
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
class PersonalAccountLifeCycle : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private var teamOwner: ClientUser? = null
    private var personalUser: ClientUser? = null
    private lateinit var backendClient: BackendClient
    private lateinit var teamHelper: TeamHelper
    private lateinit var testServiceHelper: TestServiceHelper

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_INTERNAL)
        backendClient = BackendClient.loadBackend("STAGING")
        teamHelper = TeamHelper()
        testServiceHelper = TestServiceHelper(teamHelper.usersManager)
    }

    @After
    fun tearDown() {
        teamOwner?.deleteTeam(backendClient)
        personalUser?.deleteUser(backendClient)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8609")
    @Category("criticalFlow", "testTest")
    @Test
    fun givenNoAccount_whenCreatingAndDeletingPersonalAccount_thenAccountIsRemoved() {
        step("Prepare backend users and device") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "chatFriend",
                "en_US",
                true,
                backendClient,
                context
            )

            teamOwner = teamHelper.usersManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
            personalUser = teamHelper.usersManager.findUserBy("user3Name", ClientUserManager.FindBy.NAME_ALIAS)

            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("Start personal account registration from welcome screen") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("Enter personal email and choose to create a personal account") {
            pages.registrationPage.apply {
                enterPersonalUserRegistrationEmail(personalUser?.email.orEmpty())
                clickLoginButton()
                clickCreateAccountButton()
                clickCreatePersonalAccountButton()
            }
        }

        step("Fill personal details and accept terms of use") {
            pages.registrationPage.apply {
                enterFirstName(personalUser?.name.orEmpty())
                enterPassword(personalUser?.password.orEmpty())
                enterConfirmPassword(personalUser?.password.orEmpty())

                clickShowPasswordEyeIcon()
                verifyConfirmPasswordIsCorrect(personalUser?.password.orEmpty())
                clickHidePasswordEyeIcon()

                checkIAgreeToShareAnonymousUsageData()
                clickContinueButton()

                assertTermsOfUseModalVisible()
                clickContinueButton()
            }
        }
        step("Fetch OTP from Inbucket to complete 2FA verification and complete registration") {
            val otp = runBlocking {
                InbucketClient.getVerificationCode(
                    personalUser?.email.orEmpty(),
                    BuildConfig.BACKENDCONNECTION_STAGING_INBUCKETURL,
                    BuildConfig.BACKENDCONNECTION_STAGING_INBUCKETPASSWORD,
                    BuildConfig.BACKENDCONNECTION_STAGING_INBUCKETUSERNAME
                )
            }

            pages.registrationPage.apply {
                enter2FAOnCreatePersonalAccountPage(otp)
                assertEnterYourUserNameInfoText()
                assertUserNameHelpText()
                setUserName(personalUser?.uniqueUsername.orEmpty())
                clickConfirmButton()

                waitUntilRegistrationFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
                assertConversationPageVisible()
            }
        }

        step("Send connection request to existing team owner") {
            pages.conversationListPage.tapStartNewConversationButton()

            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUniqueUserNameInSearchField(teamHelper, "user1Name")
                assertUsernameInSearchResultIs(teamOwner?.name ?: "")
                tapUsernameInSearchResult(teamOwner?.name ?: "")
            }

            pages.unconnectedUserProfilePage.clickConnectionRequestButton()
            pages.connectedUserProfilePage.assertToastMessageIsDisplayed("Connection request sent")

            pages.unconnectedUserProfilePage.clickCloseButtonOnUnconnectedUserProfilePage()
            pages.conversationListPage.clickCloseButtonOnNewConversationScreen()
            pages.conversationListPage
                .assertConversationNameWithPendingStatusVisibleInConversationList(teamOwner?.name ?: "")
        }

        step("Accept connection request via backend and start conversation") {
            runBlocking {
                val user = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
                backendClient.acceptAllIncomingConnectionRequests(user)
            }
            waitFor(1)
            pages.conversationListPage.apply {
                assertPendingStatusIsNoLongerVisible()
                tapConversationNameInConversationList(teamOwner?.name ?: "")
            }
        }

        step("Send message to team owner in 1:1 conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello Team Owner")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello Team Owner")
            }
        }

        step("Receive message from team owner via backend in 1:1 conversation") {
            testServiceHelper.userSendMessageToConversationObj(
                "user1Name",
                "Hello to you too!",
                "Device1",
                "user3Name",
                false
            )

            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello to you too!")
            }
        }

        step("Block the connected user from conversation details") {
            pages.conversationViewPage.click1On1ConversationDetails(teamOwner?.name ?: "")

            pages.connectedUserProfilePage.apply {
                clickShowMoreOptions()
                clickBlockOption()
                clickBlockButtonAlert()
                assertToastMessageIsDisplayed("${teamOwner?.name ?: ""} blocked")
                assertBlockedLabelVisible()
                assertUnblockUserButtonVisible()
                tapCloseButtonOnConnectedUserProfilePage()
            }

            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("Navigate to account settings from conversation list") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }

        waitFor(1)
        step("Verify personal account details in settings") {
            pages.settingsPage.apply {
                tapAccountDetailsButton()
                verifyDisplayedEmailAddress(personalUser?.email ?: "")
                verifyDisplayedDomain("staging.zinfra.io")
                verifyDisplayedProfileName(personalUser?.name ?: "")
                verifyDisplayedUserName(personalUser?.uniqueUsername ?: "")
            }
        }

        step("Delete personal account and confirm deletion") {
            pages.settingsPage.apply {
                tapDeleteAccountButton()
                assertDeleteAccountConfirmationModalIsDisplayed()
                tapContinueButtonOnDeleteAccountConfirmationModal()
                assertDeleteAccountConfirmationModalIsNoLongerVisible()
            }
        }
    }
}
