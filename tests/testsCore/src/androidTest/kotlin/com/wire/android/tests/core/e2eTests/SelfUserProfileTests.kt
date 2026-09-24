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
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class SelfUserProfileTests : BaseUiTest() {

    private lateinit var teamOwner: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4536")
    @Category("regression", "RC", "selfUserProfile")
    @Test
    fun givenMyStatusIsBusy_whenIChangeItToNone_thenNoneStatusIsDisplayed() {
        step("Given There is a team owner TeamOwner with team StatusChange") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "StatusChange",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User TeamOwner is me") {
            clientUserManager.setSelfUser(teamOwner)
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

        step("And I open User Profile Page and see change status options") {
            pages.conversationListPage.apply {
                waitUntilWireServiceNotificationDisappears()
                clickUserProfileButton()
            }
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertAvailabilityOptionsVisible()
            }
        }

        step("And I change my status to Busy and confirm the change") {
            pages.selfUserProfilePage.apply {
                changeAvailabilityStatus("None", "Busy")
                confirmStatusChange()
                assertAvailabilityStatusSelected("Busy")
            }
        }

        step("When I change my status from Busy to None") {
            pages.selfUserProfilePage.changeAvailabilityStatus("Busy", "None")
        }

        step("Then I see information about changing my status to None") {
            pages.selfUserProfilePage.assertStatusChangeInfoText(
                "You will receive notifications for incoming calls and for messages according to the Notifications " +
                    "setting in each conversation."
            )
        }

        step("And I confirm the status change and see my status is set to None") {
            pages.selfUserProfilePage.apply {
                confirmStatusChange()
                assertAvailabilityStatusSelected("None")
            }
        }
    }
}
