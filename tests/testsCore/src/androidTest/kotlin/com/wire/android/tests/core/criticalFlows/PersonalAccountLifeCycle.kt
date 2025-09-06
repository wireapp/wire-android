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
import com.wire.android.testSupport.BuildConfig
import com.wire.android.tests.core.di.testModule
import com.wire.android.tests.core.pages.AllPages
import com.wire.android.tests.support.UiAutomatorSetup
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import service.TestServiceHelper
import uiautomatorutils.UiWaitUtils.WaitUtils.waitFor
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.getValue

@RunWith(AndroidJUnit4::class)
class PersonalAccountLifeCycle : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.Companion.create {
        modules(testModule)
    }
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice

    lateinit var context: Context
    var teamOwner: ClientUser? = null

    var personalUser: ClientUser? = null

    var backendClient: BackendClient? = null
    val teamHelper by lazy {
        TeamHelper()
    }
    val testServiceHelper by lazy {
        TestServiceHelper()
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        //  device = UiAutomatorSetup.start(UiAutomatorSetup.APP_DEV)
        // device = UiAutomatorSetup.start(UiAutomatorSetup.APP_STAGING)
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_INTERNAL)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        // To delete team
        teamOwner?.deleteTeam(backendClient!!)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @Test
    fun givenNoAccount_whenCreatingAndDeletingPersonalAccount_thenAccountIsRemoved() {
        teamHelper.usersManager.createTeamOwnerByAlias(
            "user1Name",
            "chatFriend",
            "en_US",
            true,
            backendClient!!,
            context
        )
        teamOwner = teamHelper.usersManager!!.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
        personalUser = teamHelper.usersManager!!.findUserBy("user3Name", ClientUserManager.FindBy.NAME_ALIAS)
        testServiceHelper.apply {
            addDevice("user1Name", null, "Device1")
        }

        pages.registrationPage.apply {
            assertEmailWelcomePage()
        }
        pages.loginPage.apply {
            clickStagingDeepLink()
            clickProceedButtonOnDeeplinkOverlay()
        }
        pages.registrationPage.apply {
            enterPersonalUserRegistrationEmail(personalUser?.email.orEmpty())
            clickLoginButton()
            clickCreateAccountButton()
            clickCreatePersonalAccountButton()
            enterFirstName(personalUser?.name.orEmpty())
            enterPassword(personalUser?.password.orEmpty())
            enterConfirmPassword(personalUser?.password.orEmpty())
            clickShowPasswordEyeIcon()
            verifyConfirmPasswordIsCorrect(personalUser?.password.orEmpty())
            clickHidePasswordEyeIcon()
            checkIAgreeToShareAnonymousUsageData()
            closeKeyBoard()
            clickContinueButton()
            assertTermsOfUseModalVisible() // Asserts all elements
            clickContinueButton()
            // These values are pulled from BuildConfig injected from secrets.json)
            val otp = runBlocking {
                InbucketClient.getVerificationCode(
                    personalUser?.email.orEmpty(),
                    BuildConfig.BACKENDCONNECTION_STAGING_INBUCKETURL,
                    BuildConfig.BACKENDCONNECTION_STAGING_INBUCKETPASSWORD,
                    BuildConfig.BACKENDCONNECTION_STAGING_INBUCKETUSERNAME
                )
            }
            enter2FAOnCreatePersonalAccountPage(otp)
            assertEnterYourUserNameInfoText()
            assertUserNameHelpText()
            setUserName(personalUser?.uniqueUsername.orEmpty())
            clickConfirmButton()
            waitUntilRegistrationFlowIsComplete()
            clickAllowNotificationButton()
            clickDeclineShareDataAlert()
            assertConversationPageVisible()
            pages.conversationListPage.apply {
                tapStartNewConversationButton()
            }
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUniqueUserNameInSearchField(teamHelper, "user1Name")
                assertUsernameInSearchResultIs(teamOwner?.uniqueUsername ?: "")
                tapUsernameInSearchResult(teamOwner?.name ?: "")
            }
                pages.unconnectedUserProfilePage.apply {
                    assertUserNameInUnconnectedUserProfilePage(teamOwner?.name ?: "")
                    clickConnectionRequestButton()
                }
                pages.connectedUserProfilePage.apply {
                    assertToastMessageIsDisplayed("Connection request sent")
                }
                pages.unconnectedUserProfilePage.apply {
                    clickCloseButtonOnUnconnectedUserProfilePage()
                }
                pages.conversationListPage.apply {
                    tapBackArrowButtonInsideSearchField()
                    clickCloseButtonOnNewConversationScreen()
                }
                pages.conversationListPage.apply {
                    assertConversationNameWithPendingStatusVisibleInConversationList(teamOwner?.name ?: "")
                }
                runBlocking {
                    val user = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
                    backendClient?.acceptAllIncomingConnectionRequests(user)
                }
                Thread.sleep(1000)
                pages.conversationListPage.apply {
                    assertPendingStatusIsNoLongerVisible()
                    tapConversationNameInConversationList(teamOwner?.name ?: "")
                }
                pages.conversationViewPage.apply {
                    typeMessageInInputField("Hello Team Owner")
                    clickSendButton()
                    assertSentMessageIsVisibleInCurrentConversation("Hello Team Owner")
                }
                val conversationName = "user3Name"
                testServiceHelper.userSendMessageToConversationObj(
                    "user1Name",
                    "Hello to you too!",
                    "Device1",
                    conversationName,
                    false
                )
                pages.conversationViewPage.apply {
                    assertReceivedMessageIsVisibleInCurrentConversation("Hello to you too!")
                    click1On1ConversationDetails(teamOwner?.name ?: "")
                }
                pages.connectedUserProfilePage.apply {
                    clickShowMoreOptions()
                    clickBlockOption()
                    clickBlockButtonAlert()
                    assertToastMessageIsDisplayed("${(teamOwner?.name ?: "")} blocked")
                    assertBlockedLabelVisible()
                    assertUnblockUserButtonVisible()
                    tapCloseButtonOnConnectedUserProfilePage()
                    pages.conversationViewPage.apply {
                        tapBackButtonOnConversationViewPage()
                    }
                    pages.conversationListPage.apply {
                        clickMainMenuButtonOnConversationPage()
                        clickSettingsButtonOnMenuEntry()
                    }
                    waitFor(1) // Simple wait
                    pages.settingsPage.apply {
                        tapAccountDetailsButton()
                        verifyDisplayedEmailAddress(personalUser?.email ?: "")
                        verifyDisplayedDomain("staging.zinfra.io")
                        verifyDisplayedProfileName(personalUser?.name ?: "")
                        verifyDisplayedUserName(personalUser?.uniqueUsername ?: "")
                        assertDeleteAccountButtonIsDisplayed()
                        tapDeleteAccountButton()
                        assertDeleteAccountConfirmationModalIsDisplayed()
                        tapContinueButtonOnDeleteAccountConfirmationModal()
                        assertDeleteAccountConfirmationModalIsNoLongerVisible()
                    }
                }
            }
        }
    }
