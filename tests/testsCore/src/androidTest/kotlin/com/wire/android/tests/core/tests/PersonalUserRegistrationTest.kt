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
package com.wire.android.tests.core.tests

import InbucketClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import com.wire.android.testSupport.BuildConfig
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.core.pages.AllPages
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.inject
import user.UserClient
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import uiautomatorutils.KeyboardUtils.closeKeyboardIfOpened
import uiautomatorutils.UiWaitUtils.WaitUtils.waitFor

/*
This test works on the following conditions:
1) The dev/staging app is installed on the device/emulator.
*/
@RunWith(AndroidJUnit4::class)
class PersonalUserRegistrationTest : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_INTERNAL)
    }

    @After
    fun tearDown() {
        //  UiAutomatorSetup.stopApp()
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8694")
    @Category("regression", "RC", "registration", "testTest")
    @Test
    fun givenUserWantsToRegister_whenTheyProvideValidDetails_thenAccountIsCreatedSuccessfully() {

        //  create userInfo once, outside UI steps.
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
                    BuildConfig.BACKENDCONNECTION_STAGING_INBUCKETURL,
                    BuildConfig.BACKENDCONNECTION_STAGING_INBUCKETPASSWORD,
                    BuildConfig.BACKENDCONNECTION_STAGING_INBUCKETUSERNAME
                )
            }
        }

        step("Enter OTP and complete account creation") {
            pages.registrationPage.apply {
                enter2FAOnCreatePersonalAccountPage(otp)
                waitFor(5)

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
}
