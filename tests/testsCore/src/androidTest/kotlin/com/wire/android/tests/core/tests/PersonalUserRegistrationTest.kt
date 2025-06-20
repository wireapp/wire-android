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
import com.wire.android.testSupport.uiautomatorutils.UiAutomatorSetup
import com.wire.android.tests.core.di.testModule
import com.wire.android.tests.core.pages.AllPages
import com.wire.android.tests.support.suite.Tag
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import user.UserClient

/*
This test works on the following conditions:
1) The dev/staging app is installed on the device/emulator.
*/
@RunWith(AndroidJUnit4::class)
@Tag("RC", "regression", "@TC-8694", "@registration")
class PersonalUserRegistrationTest : KoinTest {
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(testModule)
    }

    private val pages: AllPages by inject()
    private lateinit var device: UiDevice

    @Before
    fun setUp() {

        //device = UiAutomatorSetup.start(UiAutomatorSetup.APP_DEV)
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_STAGGING)
    }

    @After
    fun tearDown() {
        //  UiAutomatorSetup.stopApp()

    }

    @Test
    fun personalUserRegistrationFlow() {
        val userInfo = UserClient.generateUniqueUserInfo()


        pages.registrationPage.assertEmailWelcomePage()
        pages.registrationPage.enterPersonalUserRegistrationEmail(userInfo.email)
        pages.registrationPage.assertAndClickLoginButton()
        pages.registrationPage.clickCreateAccountButton()
        pages.registrationPage.clickCreatePersonalAccountButton()
        /*
        Existing personal registration flow(To be deleted soon)
        registrationPage.clickContinueButton()
        registrationPage.enterEmailOnCreatePersonalAccountPage(userInfo.email)
        registrationPage.assertAndClickContinueButtonOnCreatePersonalAccountPage()
        registrationPage.assertTermsOfUseModalVisible()  // Asserts all elements
        registrationPage.clickContinueButton()
        registrationPage.assertAndClickContinueButtonOnCreatePersonalAccountPage()
       */
        pages.registrationPage.enterFirstName(userInfo.name)
        pages.registrationPage.enterPassword(userInfo.staticPassword)
        pages.registrationPage.enterConfirmPassword(userInfo.staticPassword)
        pages.registrationPage.clickShowPasswordEyeIcon()
        pages.registrationPage.verifyStaticPasswordIsCorrect()
        pages.registrationPage.clickHidePasswordEyeIcon()
        pages.registrationPage.checkIagreeToShareAnonymousUsageData()
        pages.registrationPage.clickContinueButton()
        pages.registrationPage.assertTermsOfUseModalVisible()  // Asserts all elements
        pages.registrationPage.clickContinueButton()
        // These values are pulled from BuildConfig injected from secrets.json)
        val otp = runBlocking {
            InbucketClient.getVerificationCode(
                userInfo.email,
                BuildConfig.BACKENDCONNECTION_STAGING_INBUCKETURL,
                BuildConfig.BACKENDCONNECTION_STAGING_INBUCKETPASSWORD,
                BuildConfig.BACKENDCONNECTION_STAGING_INBUCKETUSERNAME
            )
        }
        pages.registrationPage.enter2FAOnCreatePersonalAccountPage(otp)
        //Sleep to be removed when new registration implementation is done
        Thread.sleep(4000)
        pages.registrationPage.assertEnterYourUserNameInfoText()
        pages.registrationPage.assertUserNameHelpText()
        pages.registrationPage.setUserName(userInfo.username)
        pages.registrationPage.clickConfirmButton()
        pages.registrationPage.waitUntilRegistrationFlowIsComplete()
        pages.registrationPage.clickAllowNotificationButton()
        pages.registrationPage.clickDeclineShareDataAlert()
        pages.registrationPage.assertConversationPageVisible()

    }

}



