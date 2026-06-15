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

import InbucketClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.BackendClient
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.utils.ClientUser
import uiautomatorutils.KeyboardUtils.closeKeyboardIfOpened
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.iSeeSystemMessage
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class TeamCreationTest : BaseUiTest() {

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4558", "TC-4562")
    @Category("regression", "RC", "teamCreation")
    @Test
    fun givenIWantToBeAbleToCreateATeam_whenICompleteTeamCreation_thenTeamIsCreatedAndTeamOwnerSuccessfullyLogsIn() {
        lateinit var userInfo: ClientUser

        step("Given User user1Name is available for team creation") {
            userInfo = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And I see email verification welcome page") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging backend via deep link and proceed") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("When I enter my email and tap next on email verification welcome page") {
            pages.registrationPage.apply {
                enterPersonalUserRegistrationEmail(userInfo.email)
                clickLoginButton()
            }
        }

        step("And I start create team flow") {
            pages.registrationPage.apply {
                clickCreateAccountButton()
                clickCreateTeamButton()
            }
        }

        step("Then I see create team page") {
            pages.teamCreationPage.apply {
                assertCreateTeamPageVisible()
            }
        }

        step("When I enter team owner details on create team page") {
            pages.teamCreationPage.apply {
                enterEmail(userInfo.email)
                enterProfileName(userInfo.name)
                enterTeamName("SuperTeam")
                closeKeyboardIfOpened()
                enterPassword(userInfo.password)
                closeKeyboardIfOpened()
                enterConfirmPassword(userInfo.password)
                closeKeyboardIfOpened()
            }
        }

        step("And I select the first organization size option") {
            pages.teamCreationPage.apply {
                selectFirstOrganizationSizeOption()
                UiWaitUtils.waitFor(2.seconds)
            }
        }

        step("And I accept terms and anonymous usage data options") {
            pages.teamCreationPage.apply {
                checkIAcceptTermsAndConditions()
                checkIAgreeToShareAnonymousUsageData()
            }
        }

        step("And I tap continue button on create team page") {
            pages.teamCreationPage.apply {
                clickContinueButton()
            }
        }

        step("Then I see team verification code page") {
            pages.teamCreationPage.apply {
                assertYouHaveGotMailPageVisible()
            }
        }

        step("When I retrieve OTP from Inbucket and enter it on team verification page") {
            val otp = runBlocking {
                InbucketClient.getVerificationCode(
                    userInfo.email!!,
                    backendClient.inbucketUrl,
                    backendClient.inbucketPassword,
                    backendClient.inbucketUsername
                )
            }
            pages.teamCreationPage.apply {
                enterVerificationCode(otp)
            }
        }

        step("Then I see team created page") {
            pages.teamCreationPage.apply {
                assertTeamCreatedPageVisible()
            }
        }

        step("When I close team created page") {
            pages.teamCreationPage.apply {
                closeTeamCreatedPage()
            }
        }

        // TC-4562 I want to reach login page on submit email page while registering a team

        step("Then I login as TeamOwner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingPassword(userInfo.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Then I see username setup page") {
            pages.registrationPage.apply {
                assertEnterYourUserNameInfoText()
                assertUserNameHelpText()
            }
        }

        step("When I set username and confirm") {
            pages.registrationPage.apply {
                setUserName(userInfo.uniqueUsername)
                clickConfirmButton()
            }
        }

        step("And I decline share data alert after username confirmation") {
            pages.registrationPage.apply {
                clickDeclineShareDataAlert()
            }
        }

        step("Then I see conversation list") {
            pages.registrationPage.apply {
                assertConversationPageVisible()
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4559", "TC-4560", "TC-4561")
    @Category("regression", "RC", "teamCreation")
    @Test
    fun givenIWantToRegisterWithAnInvalidEmail_whenISubmitTheEmail_thenISeeAnError() {
        lateinit var userInfo: ClientUser

        step("Given User user1Name is available for team creation") {
            userInfo = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And I see email verification welcome page") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging backend via deep link and proceed") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("When I enter my email and tap next on email verification welcome page") {
            pages.registrationPage.apply {
                enterPersonalUserRegistrationEmail(userInfo.email)
                clickLoginButton()
            }
        }

        step("And I start create team flow") {
            pages.registrationPage.apply {
                clickCreateAccountButton()
                clickCreateTeamButton()
            }
        }

        step("Then I see create team page") {
            pages.teamCreationPage.apply {
                assertCreateTeamPageVisible()
            }
        }

        step("When I enter invalid email and valid team owner details on create team page") {
            pages.teamCreationPage.apply {
                enterEmail("smokester+invalid@wire")
                enterProfileName(userInfo.name)
                enterTeamName("SuperTeam")
                closeKeyboardIfOpened()
                enterPassword(userInfo.password)
                closeKeyboardIfOpened()
                enterConfirmPassword(userInfo.password)
                closeKeyboardIfOpened()
            }
        }

        step("And I accept terms and conditions") {
            pages.teamCreationPage.apply {
                checkIAcceptTermsAndConditions()
            }
        }

        step("And I submit create team form") {
            pages.teamCreationPage.apply {
                clickContinueButton()
            }
        }

        step("Then I see invalid email error message") {
            iSeeSystemMessage("Something went wrong. Please reload the page and try again.")
        }

        // TC-4561 I want to see an error when my passwords do not match

        step("When I clear email and confirm password then enter valid email and unmatched confirm password") {
            pages.teamCreationPage.apply {
                enterEmail("")
                enterEmail(userInfo.email)
                enterConfirmPassword("")
                enterConfirmPassword("unmatchedPassword")
                closeKeyboardIfOpened()
            }
        }

        step("And I submit create team form") {
            pages.teamCreationPage.apply {
                clickContinueButton()
            }
        }

        step("Then I see password mismatch error message") {
            iSeeSystemMessage("The password doesn’t match!")
        }

        // TC-4560 I want to see an error when I use an invalid password

        step("When I clear password fields and enter invalid password in both fields") {
            pages.teamCreationPage.apply {
                enterPassword("")
                enterPassword("invalidPassword")
                enterConfirmPassword("")
                enterConfirmPassword("invalidPassword")
                closeKeyboardIfOpened()
            }
        }

        step("And I submit create team form") {
            pages.teamCreationPage.apply {
                clickContinueButton()
            }
        }

        step("Then I see invalid password requirements error message") {
            iSeeSystemMessage(
                "Use at least 8 characters, with one lowercase letter, one capital letter, a number, and a special character."
            )
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4563")
    @Category("regression", "RC", "teamCreation")
    @Test
    fun givenIAmRegisteringATeam_whenIEnterAWrongVerificationCode_thenISeeAnError() {
        lateinit var userInfo: ClientUser

        step("Given User user1Name is available for team creation") {
            userInfo = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And I see email verification welcome page") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging backend via deep link and proceed") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("When I enter my email and tap next on email verification welcome page") {
            pages.registrationPage.apply {
                enterPersonalUserRegistrationEmail(userInfo.email)
                clickLoginButton()
            }
        }

        step("And I start create team flow") {
            pages.registrationPage.apply {
                clickCreateAccountButton()
                clickCreateTeamButton()
            }
        }

        step("Then I see create team page") {
            pages.teamCreationPage.apply {
                assertCreateTeamPageVisible()
            }
        }

        step("When I enter team owner details on create team page") {
            pages.teamCreationPage.apply {
                enterEmail(userInfo.email)
                enterProfileName(userInfo.name)
                enterTeamName("SuperTeam")
                closeKeyboardIfOpened()
                enterPassword(userInfo.password)
                closeKeyboardIfOpened()
                enterConfirmPassword(userInfo.password)
                closeKeyboardIfOpened()
            }
        }

        step("And I accept terms and conditions") {
            pages.teamCreationPage.apply {
                checkIAcceptTermsAndConditions()
            }
        }

        step("And I tap continue button on create team page") {
            pages.teamCreationPage.apply {
                clickContinueButton()
            }
        }

        step("Then I see team verification code page") {
            pages.teamCreationPage.apply {
                assertYouHaveGotMailPageVisible()
            }
        }

        step("When I enter incorrect OTP on team verification page") {
            val incorrectOtp = "0".repeat(6)
            pages.teamCreationPage.apply {
                enterVerificationCode(incorrectOtp)
            }
        }

        step("Then I see wrong verification code error message") {
            iSeeSystemMessage("Please retry, or request another code.")
        }
    }
}
