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
@file:Suppress("ArgumentListWrapping")

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
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import user.UserClient
import uiautomatorutils.KeyboardUtils.closeKeyboardIfOpened
import uiautomatorutils.UiWaitUtils
import kotlin.time.Duration.Companion.seconds

/*
This test works on the following conditions:
1) The dev/staging app is installed on the device/emulator.
*/
@RunWith(AndroidJUnit4::class)
class PersonalUserRegistrationTest : BaseUiTest() {
    @Before
    fun setUp() {
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8694")
    @Category("regression", "RC", "registration")
    @Test
    fun givenUserWantsToRegister_whenTheyProvideValidDetails_thenAccountIsCreatedSuccessfully() {

        // create userInfo once, outside UI steps.
        val userInfo = UserClient.generateUniqueUserInfo()
        lateinit var otp: String

        step("Generate unique user data for registration") {
            // userInfo is created above (to avoid changing behavior).
            // This step is for Allure readability.
        }

        step("Open registration welcome screen") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("Enter email and start account creation flow") {
            pages.registrationPage.apply {
                enterPersonalUserRegistrationEmail(userInfo.email)
                clickLoginButton()
                clickCreateAccountButton()
                clickCreatePersonalAccountButton()
            }
        }

        step("Fill personal details (name + password) and validate password visibility toggles") {
            pages.registrationPage.apply {
                enterFirstName(userInfo.name)
                enterPassword(userInfo.password)
                enterConfirmPassword(userInfo.password)
                clickShowPasswordEyeIcon()
                verifyConfirmPasswordIsCorrect(userInfo.password)
                clickHidePasswordEyeIcon()

                closeKeyboardIfOpened()
            }
        }
        step("Accept anonymous usage data option and continue") {
            pages.registrationPage.apply {
                checkIAgreeToShareAnonymousUsageData()
            }
            pages.registrationPage.apply {
                clickContinueButton()
            }
        }

        step("Review Terms of Use modal and confirm") {
            pages.registrationPage.apply {
                assertTermsOfUseModalVisible() // Asserts all elements
                clickContinueButton()
            }
        }

        step("Retrieve OTP from Inbucket using backend credentials") {
            otp = runBlocking {
                InbucketClient.getVerificationCode(
                    userInfo.email,
                    backendClient.inbucketUrl,
                    backendClient.inbucketPassword,
                    backendClient.inbucketUsername
                )
            }
        }

        step("Enter OTP and complete account creation") {
            pages.registrationPage.apply {
                enter2FAOnCreatePersonalAccountPage(otp)
                UiWaitUtils.waitFor(5.seconds)

                assertEnterYourUserNameInfoText()
                assertUserNameHelpText()

                setUserName(userInfo.username)
                clickConfirmButton()
            }
        }

        step("Wait for registration flow to finish and handle post-registration prompts") {
            pages.registrationPage.apply {
                waitUntilRegistrationFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
                assertConversationPageVisible()
            }
        }
    }

    @Ignore("Stale zautomation case: TC-4493 targets the old login flow, which is no longer exposed by the current app.")
    @TestCaseId("TC-4493")
    @Category("regression", "RC", "registration")
    @Test
    fun givenUserWantsToRegisterWithOldLoginFlow_whenTheyProvideValidDetails_thenAccountIsCreatedSuccessfully() {
        // Intentionally skipped: the active app starts from the email verification flow covered by TC-8694.
    }

    @TestCaseId("TC-4852", "TC-4851")
    @Category("registration")
    @Ignore(
        "Blocked: column backend registration restrictions need column backend deep link/login fixture and welcome-page negative assertions."
    )
    @Test
    fun givenColumnBackendWelcomePage_whenRegistrationIsDisabled_thenTeamAndPersonalRegistrationAreHidden() = Unit
}
