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
package com.wire.android.tests.core.tests

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import backendUtils.BackendClient
import backendUtils.team.TeamHelper
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.core.pages.AllPages
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.inject
import service.TestServiceHelper
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class UpgradeVersion : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private lateinit var backendClient: BackendClient
    private lateinit var teamHelper: TeamHelper
    private lateinit var testServiceHelper: TestServiceHelper
    private var member1: ClientUser? = null
    private var member2: ClientUser? = null

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
        cleanupCreatedUsers(backendClient, teamHelper.usersManager)
    }

    /**
     * Local runs should preinstall the old Wire APK, then push the new APK to /data/local/tmp/Wire.new.apk.
     * Push: adb push /path/to/new.apk /data/local/tmp/Wire.new.apk
     * Run: ./gradlew :tests:testsCore:connectedDebugAndroidTest \
     *   -Pandroid.testInstrumentationRunnerArguments.testCaseId=TC-8607 \
     *   -Pandroid.testInstrumentationRunnerArguments.newApkPath=/data/local/tmp/Wire.new.apk
     * CI installs the old APK before instrumentation starts because Android blocks in-test downgrades.
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8607")
    @Category("regression", "upgrade")
    @Test
    fun givenTeamUserWithConversationHistory_whenUpdatingFromPreviousWireVersion_thenHistoryIsPreserved() {
        step("There is a team owner with a team named UpgradeTeam") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "UpgradeTeam",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("Team owner adds members to the team with role Member") {
            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "UpgradeTeam",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("Team owner has a group conversation with members in the team") {
            testServiceHelper.userHasGroupConversationInTeam(
                "user1Name",
                "UpgradeVersion",
                "user2Name,user3Name",
                "UpgradeTeam"
            )
        }

        step("Member 1 has a 1:1 conversation with Member 2 in the team") {
            testServiceHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user3Name",
                "UpgradeTeam"
            )
        }

        step("Member 1 is me") {
            member1 = teamHelper.usersManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
            member2 = teamHelper.usersManager.findUserBy("user3Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I login as Member 1") {
            pages.loginPage.apply {
                enterTeamMemberLoggingEmail(member1?.email ?: "")
                clickLoginButton()
                enterTeamMemberLoggingPassword(member1?.password ?: "")
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

        step("Then I see conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
            }
        }

        step("And I see conversation UpgradeVersion in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("UpgradeVersion")
            }
        }

        step("And I see conversation with Member 2 in conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(member2?.name ?: "")
            }
        }

        step("When I tap on conversation name UpgradeVersion in conversation list") {
            pages.conversationListPage.apply {
                clickGroupConversation("UpgradeVersion")
            }
        }

        step("And Member 2 sends message to group conversation UpgradeVersion") {
            testServiceHelper.apply {
                addDevice("user3Name", null, "Device1")
                userSendMessageToConversation(
                    "user3Name",
                    "Hello!",
                    "Device1",
                    "UpgradeVersion",
                    false
                )
            }
        }

        step("Then I see the message from Member 2 in current conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("When I type a reply into the text input field and send it") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello as well!")
                clickSendButton()
            }
        }

        step("Then I see my reply in current conversation") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Hello as well!")
            }
        }

        step("When I tap the back arrow to go back to conversation list") {
            pages.conversationViewPage.apply {
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("And Member 2 sends message to Member 1") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user3Name",
                "Hello friend",
                "Device1",
                "user2Name",
                false
            )
        }

        step("And I wait until the notification popup disappears") {
            pages.notificationsPage.apply {
                waitUntilNotificationPopUpGone()
            }
        }

        step("Then I see conversation with Member 2 is having 1 unread messages in conversation list") {
            pages.conversationListPage.apply {
                assertConversationHasUnreadMessagesCount(member2?.name ?: "", "1")
            }
        }

        step("When I minimise Wire") {
            device.pressHome()
        }

        step("And I upgrade Wire to the recent version") {
            val recentWireApkPath = InstrumentationRegistry.getArguments()
                .getString("newApkPath") ?: "/data/local/tmp/Wire.new.apk"
            UiAutomatorSetup.upgradeWireToRecentVersion(recentWireApkPath)
        }

        step("Then I see conversation with Member 2 is having 1 unread messages in conversation list") {
            pages.conversationListPage.apply {
                assertConversationHasUnreadMessagesCount(member2?.name ?: "", "1")
            }
        }

        step("And I see conversation UpgradeVersion in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("UpgradeVersion")
            }
        }

        step("When I tap on conversation name UpgradeVersion in conversation list") {
            pages.conversationListPage.apply {
                clickGroupConversation("UpgradeVersion")
            }
        }

        step("Then I see the reply message in current conversation") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Hello as well!")
            }
        }

        step("And I see the message from Member 2 in current conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("When I type the final migration message into the text input field and send it") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Upgrade was a success!")
                clickSendButton()
            }
        }

        step("Then I see the final migration message in current conversation") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Upgrade was a success!")
            }
        }
    }
}
