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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import com.wire.android.tests.core.pages.RegistrationPage
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.suite.Tag
import kotlinx.coroutines.runBlocking
import org.junit.After
import user.UserClient
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/*
This test works on the following conditions:
1) The dev/staging app is installed on the device/emulator.
*/
@RunWith(AndroidJUnit4::class)
@Tag("RC", "regression", "@TC-8694", "@registration")
//@RC
class RegistrationTest {
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
       // device = UiAutomatorSetup.start(UiAutomatorSetup.APP_DEV)
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_STAGGING)

    }

    @After
    fun tearDown() {
        // This will always be called — even if test fails
      //  UiAutomatorSetup.stopApp()
    }

    @Test
    fun personalUserRegistrationFlow() {

        val registrationPage = RegistrationPage(device)
        val userInfo = UserClient.generateUniqueUserInfo()

        registrationPage.assertEmailWelcomePage()
        registrationPage.loginWithEmail(userInfo.email)
        registrationPage.assertAndClickLoginButton()
        registrationPage.clickCreateAccountButton()
        registrationPage.clickContinueButton()
        registrationPage.enterEmailOnCreatePersonalAccountPage(userInfo.email)
        registrationPage.assertAndClickContinueButtonOnCreatePersonalAccountPage()
        registrationPage.assertTermsOfUseModalVisible()  // Asserts all elements
        registrationPage.clickContinueButton()
        registrationPage.assertAndClickContinueButtonOnCreatePersonalAccountPage()
        registrationPage.enterPersonalDetails(
            firstName = userInfo.firstName,
            lastName = userInfo.lastName,
            password = userInfo.staticPassword,
            confirmPassword = userInfo.staticPassword
        )
        registrationPage.clickShowPasswordEyeIcon()
        registrationPage.verifyStaticPasswordIsCorrect()
        registrationPage.clickHidePasswordEyeIcon()
        registrationPage.clickContinueButton()
        val otp = runBlocking { InbucketClient.getVerificationCode(userInfo.email) }
        registrationPage.enter2FAOnCreatePersonalAccountPage(otp)
        registrationPage.assertAccountCreationSuccessMessage()
        registrationPage.clickGetStartedButton()
        registrationPage.assertUserNamePageIsVisible()
        registrationPage.assertEnterYourUserNameInfoText()
        registrationPage.assertUserNameHelpText()
        registrationPage.setUserName(userInfo.username)
        registrationPage.clickConfirmButton()
        registrationPage.clickAllowNotificationmButton()
        registrationPage.clickDeclineShareDataAlert()
        registrationPage.assertConversationPageVisible()

    }

}
