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
import androidx.test.platform.app.InstrumentationRegistry
import backendUtils.team.TeamRoles
import backendUtils.team.deleteTeamMember
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.KeyboardUtils.closeKeyboardIfOpened
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@Suppress("LargeClass")
@RunWith(AndroidJUnit4::class)
class UpgradeVersion : BaseUiTest() {
    override val deletePersonalUsersAfterTest = true

    private var teamOwner: ClientUser? = null
    private var member1: ClientUser? = null
    private var member2: ClientUser? = null
    private lateinit var personalUser: ClientUser
    private lateinit var contact1: ClientUser
    private lateinit var contact2: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
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
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "UpgradeTeam",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("Team owner adds members to the team with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
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
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "UpgradeVersion",
                "user2Name,user3Name",
                "UpgradeTeam"
            )
        }

        step("Member 1 has a 1:1 conversation with Member 2 in the team") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user3Name",
                "UpgradeTeam"
            )
        }

        step("Member 1 is me") {
            member1 = clientUserManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
            member2 = clientUserManager.findUserBy("user3Name", ClientUserManager.FindBy.NAME_ALIAS)
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
                clickContinueButtonOnBackendConfigSuccess()
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
                    "UpgradeVersion"
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
                "user2Name"
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

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4565")
    @Category("regression", "RC", "upgrade")
    @Test
    fun givenTeamUserWithMessageHistory_whenUpgradingWire_thenConversationHistoryIsPreserved() {
        step("There is a team owner with a team named Migration") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Migration",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("Team owner adds Member 1 and Member 2 to the team") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Migration",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("Team owner has group conversation HappyMigration with Member 1 and Member 2") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "HappyMigration",
                "user2Name,user3Name",
                "Migration"
            )
        }

        step("Member 1 has a 1:1 conversation with Member 2") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user3Name",
                "Migration"
            )
        }

        step("Member 1 has a 1:1 conversation with Team Owner") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user1Name",
                "Migration"
            )
        }

        step("Member 2 and Team Owner add a test-service device") {
            testServiceHelper.apply {
                addDevice("user3Name", null, "Device1")
                addDevice("user1Name", null, "Device1")
            }
        }

        step("Member 1 is me") {
            teamOwner = clientUserManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
            member1 = clientUserManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
            member2 = clientUserManager.findUserBy("user3Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        loginAsMember1()

        step("I see all prepared conversations in the conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("HappyMigration")
                assertConversationVisible(member2?.name.orEmpty())
                assertConversationVisible(teamOwner?.name.orEmpty())
            }
        }

        step("I open group conversation HappyMigration") {
            pages.conversationListPage.clickGroupConversation("HappyMigration")
        }

        step("Member 2 and Team Owner send messages to HappyMigration") {
            testServiceHelper.apply {
                userSendMessageToConversation(
                    "user3Name",
                    "Hello!",
                    "Device1",
                    "HappyMigration"
                )
                userSendMessageToConversation(
                    "user1Name",
                    "Hello to you, too!",
                    "Device1",
                    "HappyMigration"
                )
            }
        }

        step("I see both received messages in HappyMigration") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
                assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
            }
        }

        step("Member 2 sends an image to HappyMigration") {
            testServiceHelper.contactSendsLocalImageConversation(
                context,
                "testing.jpg",
                "user3Name",
                "Device1",
                "HappyMigration"
            )
        }

        step("I scroll to the bottom and see the image") {
            pages.conversationViewPage.apply {
                scrollToBottomOfConversationScreen()
                assertImageIsVisible()
            }
        }

        step("I send message Hello as well") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello as well!")
                clickSendButton()
            }
            closeKeyboardIfOpened()
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Hello as well!")
        }

        step("I return to the conversation list") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("Member 2 and Team Owner each send a message to Member 1") {
            testServiceHelper.apply {
                userSendMessageToPersonalMlsConversation(
                    "user3Name",
                    "Hello!",
                    "Device1",
                    "user2Name"
                )
                userSendMessageToPersonalMlsConversation(
                    "user1Name",
                    "Hello to you, too!",
                    "Device1",
                    "user2Name"
                )
            }
        }

        step("I see one unread message for Member 2 and Team Owner") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
            pages.conversationListPage.apply {
                assertConversationHasUnreadMessagesCount(member2?.name.orEmpty(), "1")
                assertConversationHasUnreadMessagesCount(teamOwner?.name.orEmpty(), "1")
            }
        }

        step("I minimise Wire") {
            device.pressHome()
        }

        step("I upgrade Wire to the recent version") {
            val recentWireApkPath = InstrumentationRegistry.getArguments()
                .getString("newApkPath") ?: "/data/local/tmp/Wire.new.apk"
            UiAutomatorSetup.upgradeWireToRecentVersion(recentWireApkPath)
        }

        step("I wait until the upgraded app is fully logged in") {
            pages.registrationPage.waitUntilLoginFlowIsCompleted()
        }

        step("The unread messages and HappyMigration conversation remain visible") {
            pages.conversationListPage.apply {
                assertConversationHasUnreadMessagesCount(member2?.name.orEmpty(), "1")
                assertConversationHasUnreadMessagesCount(teamOwner?.name.orEmpty(), "1")
                assertGroupConversationVisible("HappyMigration")
            }
        }

        step("I open HappyMigration and see its message history") {
            pages.conversationListPage.clickGroupConversation("HappyMigration")
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Hello as well!")
                assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
                assertImageIsVisible()
                assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("Team Owner sends a message after the upgrade") {
            testServiceHelper.userSendMessageToConversation(
                "user1Name",
                "Hello after Migration!",
                "Device1",
                "HappyMigration"
            )
        }

        step("I see Team Owner's message after the upgrade") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(
                "Hello after Migration!"
            )
        }

        step("I send a message after the upgrade") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Migration was a success!")
                clickSendButton()
            }
        }

        step("I see my message after the upgrade") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation(
                "Migration was a success!"
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4571")
    @Category("regression", "RC", "upgrade")
    @Test
    fun givenPersonalUserWithMessageHistory_whenUpgradingWire_thenConversationHistoryIsPreserved() {
        step("There are three personal users where user1Name is me") {
            clientUserManager.createPersonalUsersByAliases(
                listOf("user1Name", "user2Name", "user3Name"),
                backendClient
            )
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user1Name,user2Name,user3Name")
            }
            personalUser = clientUserManager.findUserByNameOrNameAlias("user1Name")
            contact1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            contact2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            clientUserManager.setSelfUser(personalUser)
        }

        step("I am connected to Contact 1 and Contact 2") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user2Name,user3Name")
        }

        step("Contact 1 and Contact 2 add a test-service device") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                addDevice("user3Name", null, "Device1")
            }
        }

        step("I log in as the personal user using the current login flow") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
                enterPersonalUserLoggingEmail(personalUser.email.orEmpty())
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(personalUser.password.orEmpty())
                clickLoginButton()
            }
        }

        step("I complete the post-login prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("I see both contact conversations") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationVisible(contact1.name.orEmpty())
                assertConversationVisible(contact2.name.orEmpty())
            }
        }

        step("I open the conversation with Contact 1") {
            pages.conversationListPage.tapConversationNameInConversationList(contact1.name.orEmpty())
        }

        step("Contact 1 sends message Hello") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "user1Name"
            )
        }

        step("I see Contact 1's message") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("Contact 1 sends image testing.jpg") {
            testServiceHelper.contactSendsLocalImageConversation(
                context,
                "testing.jpg",
                "user2Name",
                "Device1",
                "user1Name"
            )
        }

        step("I scroll to the bottom and see the image") {
            pages.conversationViewPage.apply {
                scrollToBottomOfConversationScreen()
                assertImageIsVisible()
            }
        }

        step("I send message Hello to you too") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello to you, too!")
                clickSendButton()
            }
            closeKeyboardIfOpened()
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }

        step("I return to the conversation list") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("Contact 2 sends message Hello") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user3Name",
                "Hello!",
                "Device1",
                "user1Name"
            )
        }

        step("I see one unread message for Contact 2") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
            pages.conversationListPage.assertConversationHasUnreadMessagesCount(
                contact2.name.orEmpty(),
                "1"
            )
        }

        step("I minimise Wire") {
            device.pressHome()
        }

        step("I upgrade Wire to the recent version") {
            val recentWireApkPath = InstrumentationRegistry.getArguments()
                .getString("newApkPath") ?: "/data/local/tmp/Wire.new.apk"
            UiAutomatorSetup.upgradeWireToRecentVersion(recentWireApkPath)
        }

        step("I wait until the upgraded app is fully logged in") {
            pages.registrationPage.waitUntilLoginFlowIsCompleted()
        }

        step("Contact 2's unread message and Contact 1's conversation remain visible") {
            pages.conversationListPage.apply {
                assertConversationHasUnreadMessagesCount(contact2.name.orEmpty(), "1")
                assertConversationVisible(contact1.name.orEmpty())
            }
        }

        step("I open Contact 1's conversation and see its message history") {
            pages.conversationListPage.tapConversationNameInConversationList(contact1.name.orEmpty())
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Hello to you, too!")
                assertImageIsVisible()
                assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("Contact 1 sends a message after the upgrade") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user2Name",
                "Hello after Migration!",
                "Device1",
                "user1Name"
            )
        }

        step("I see Contact 1's message after the upgrade") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(
                "Hello after Migration!"
            )
        }

        step("I send a message after the upgrade") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Migration was a success!")
                clickSendButton()
            }
        }

        step("I see my message after the upgrade") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation(
                "Migration was a success!"
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4566")
    @Category("regression", "RC", "upgrade")
    @Test
    fun givenTeamUserWithSharedAssets_whenUpgradingWire_thenAssetsArePreserved() {
        step("There is a team owner with a team named Migration") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Migration",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("Team owner adds Member 1 and Member 2 to the team") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Migration",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("Member 2 and Team Owner add a test-service device") {
            testServiceHelper.apply {
                addDevice("user3Name", null, "Device1")
                addDevice("user1Name", null, "Device1")
            }
        }

        step("Member 1 is me") {
            teamOwner = clientUserManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
            member1 = clientUserManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
            member2 = clientUserManager.findUserBy("user3Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("Team owner has group conversation HappyMigration with Member 1 and Member 2") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "HappyMigration",
                "user2Name,user3Name",
                "Migration"
            )
        }

        step("Member 1 has a 1:1 conversation with Member 2") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user3Name",
                "Migration"
            )
        }

        step("Member 1 has a 1:1 conversation with Team Owner") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user1Name",
                "Migration"
            )
        }

        loginAsMember1()

        step("I see all prepared conversations in the conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("HappyMigration")
                assertConversationVisible(member2?.name.orEmpty())
                assertConversationVisible(teamOwner?.name.orEmpty())
            }
        }

        step("I open group conversation HappyMigration") {
            pages.conversationListPage.clickGroupConversation("HappyMigration")
        }

        step("Member 2 sends message Hello to HappyMigration") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello!",
                "Device1",
                "HappyMigration"
            )
        }

        step("Member 2 sends image testing.jpg to HappyMigration") {
            testServiceHelper.contactSendsLocalImageConversation(
                context,
                "testing.jpg",
                "user3Name",
                "Device1",
                "HappyMigration"
            )
        }

        step("I see the image") {
            pages.conversationViewPage.assertImageIsVisible()
        }

        step("Team Owner sends video testing.mp4 to HappyMigration") {
            testServiceHelper.contactSendsLocalVideoConversation(
                context,
                "testing.mp4",
                "user1Name",
                "Device1",
                "HappyMigration"
            )
        }

        step("I scroll to the bottom and see testing.mp4") {
            pages.conversationViewPage.apply {
                scrollToBottomOfConversationScreen()
                assertFileWithNameIsVisible("testing.mp4")
            }
        }

        step("Member 2 sends a 1 KB text file named qa_random.txt to HappyMigration") {
            testServiceHelper.contactSendsOneKbTextFileConversation(
                context,
                "qa_random.txt",
                "user3Name",
                "Device1",
                "HappyMigration"
            )
        }

        step("I scroll to the bottom and see qa_random.txt") {
            pages.conversationViewPage.apply {
                scrollToBottomOfConversationScreen()
                assertFileWithNameIsVisible("qa_random.txt")
            }
        }

        step("I minimise Wire") {
            device.pressHome()
        }

        step("I upgrade Wire to the recent version") {
            val recentWireApkPath = InstrumentationRegistry.getArguments()
                .getString("newApkPath") ?: "/data/local/tmp/Wire.new.apk"
            UiAutomatorSetup.upgradeWireToRecentVersion(recentWireApkPath)
        }

        step("I wait until the upgraded app is fully logged in") {
            pages.registrationPage.waitUntilLoginFlowIsCompleted()
        }

        step("HappyMigration remains visible after the upgrade") {
            pages.conversationListPage.assertGroupConversationVisible("HappyMigration")
        }

        step("I open HappyMigration and see all shared assets") {
            pages.conversationListPage.clickGroupConversation("HappyMigration")
            pages.conversationViewPage.apply {
                assertImageIsVisible()
                assertFileWithNameIsVisible("testing.mp4")
                assertFileWithNameIsVisible("qa_random.txt")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4567")
    @Category("regression", "RC", "upgrade")
    @Test
    fun givenTeamUserLeftGroup_whenUpgradingWire_thenHistoryRemainsVisibleAndNewMessagesAreNotReceived() {
        step("There is a team owner with a team named Migration") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Migration",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("Team owner adds Member 1 and Member 2 to the team") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Migration",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("Member 2 and Team Owner add a test-service device") {
            testServiceHelper.apply {
                addDevice("user3Name", null, "Device1")
                addDevice("user1Name", null, "Device1")
            }
        }

        step("Member 1 is me") {
            teamOwner = clientUserManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
            member1 = clientUserManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
            member2 = clientUserManager.findUserBy("user3Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("Team owner has group conversation HappyMigration with Member 1 and Member 2") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "HappyMigration",
                "user2Name,user3Name",
                "Migration"
            )
        }

        step("Member 1 has a 1:1 conversation with Member 2") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user3Name",
                "Migration"
            )
        }

        step("Member 1 has a 1:1 conversation with Team Owner") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user1Name",
                "Migration"
            )
        }

        loginAsMember1()

        step("I see all prepared conversations in the conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("HappyMigration")
                assertConversationVisible(member2?.name.orEmpty())
                assertConversationVisible(teamOwner?.name.orEmpty())
            }
        }

        step("I open group conversation HappyMigration") {
            pages.conversationListPage.clickGroupConversation("HappyMigration")
        }

        step("Member 2 and Team Owner send messages to HappyMigration") {
            testServiceHelper.apply {
                userSendMessageToConversation(
                    "user3Name",
                    "Hello!",
                    "Device1",
                    "HappyMigration"
                )
                userSendMessageToConversation(
                    "user1Name",
                    "Hello to you, too!",
                    "Device1",
                    "HappyMigration"
                )
            }
        }

        step("I see both received messages in HappyMigration") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
                assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
            }
        }

        step("Member 2 sends image testing.jpg to HappyMigration") {
            testServiceHelper.contactSendsLocalImageConversation(
                context,
                "testing.jpg",
                "user3Name",
                "Device1",
                "HappyMigration"
            )
        }

        step("I scroll to the bottom and see the image") {
            pages.conversationViewPage.apply {
                scrollToBottomOfConversationScreen()
                assertImageIsVisible()
            }
        }

        step("I send message Hello as well") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello as well!")
                clickSendButton()
            }
            closeKeyboardIfOpened()
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Hello as well!")
        }

        step("I open HappyMigration details and show more options") {
            pages.conversationViewPage.clickOnGroupConversationDetails("HappyMigration")
            pages.groupConversationDetailsPage.tapShowMoreOptionsButton()
        }

        step("I leave HappyMigration and confirm") {
            pages.groupConversationDetailsPage.apply {
                tapLeaveConversationButton()
                tapLeaveConversationConfirmButton()
            }
        }

        step("I see Member 2's conversation in the conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationVisible(member2?.name.orEmpty())
            }
        }

        step("I minimise Wire") {
            device.pressHome()
        }

        step("I upgrade Wire to the recent version") {
            val recentWireApkPath = InstrumentationRegistry.getArguments()
                .getString("newApkPath") ?: "/data/local/tmp/Wire.new.apk"
            UiAutomatorSetup.upgradeWireToRecentVersion(recentWireApkPath)
        }

        step("I wait until the upgraded app is fully logged in") {
            pages.registrationPage.waitUntilLoginFlowIsCompleted()
        }

        step("HappyMigration remains visible after the upgrade") {
            pages.conversationListPage.assertGroupConversationVisible("HappyMigration")
        }

        step("I open HappyMigration and see its history from before I left") {
            pages.conversationListPage.clickGroupConversation("HappyMigration")
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Hello as well!")
                assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
                assertImageIsVisible()
                assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("Member 2 sends a message after I left HappyMigration") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello after Migration",
                "Device1",
                "HappyMigration"
            )
        }

        step("I do not see the message sent after I left HappyMigration") {
            pages.conversationViewPage.assertMessageNotVisible("Hello after Migration")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4568")
    @Category("regression", "RC", "upgrade")
    @Test
    fun givenTeamUserClearedGroupContent_whenUpgradingWire_thenClearedContentDoesNotReturn() {
        step("There is a team owner with a team named Migration") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Migration",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("Team owner adds Member 1 and Member 2 to the team") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Migration",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("Member 2 and Team Owner add a test-service device") {
            testServiceHelper.apply {
                addDevice("user3Name", null, "Device1")
                addDevice("user1Name", null, "Device1")
            }
        }

        step("Member 1 is me") {
            teamOwner = clientUserManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
            member1 = clientUserManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
            member2 = clientUserManager.findUserBy("user3Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("Team owner has group conversation HappyMigration with Member 1 and Member 2") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "HappyMigration",
                "user2Name,user3Name",
                "Migration"
            )
        }

        step("Member 1 has a 1:1 conversation with Member 2") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user3Name",
                "Migration"
            )
        }

        step("Member 1 has a 1:1 conversation with Team Owner") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user1Name",
                "Migration"
            )
        }

        loginAsMember1()

        step("I see all prepared conversations in the conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("HappyMigration")
                assertConversationVisible(member2?.name.orEmpty())
                assertConversationVisible(teamOwner?.name.orEmpty())
            }
        }

        step("I open group conversation HappyMigration") {
            pages.conversationListPage.clickGroupConversation("HappyMigration")
        }

        step("Member 2 and Team Owner send messages to HappyMigration") {
            testServiceHelper.apply {
                userSendMessageToConversation(
                    "user3Name",
                    "Hello!",
                    "Device1",
                    "HappyMigration"
                )
                userSendMessageToConversation(
                    "user1Name",
                    "Hello to you, too!",
                    "Device1",
                    "HappyMigration"
                )
            }
        }

        step("I see both received messages in HappyMigration") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
                assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
            }
        }

        step("Member 2 sends image testing.jpg to HappyMigration") {
            testServiceHelper.contactSendsLocalImageConversation(
                context,
                "testing.jpg",
                "user3Name",
                "Device1",
                "HappyMigration"
            )
        }

        step("I scroll to the bottom and see the image") {
            pages.conversationViewPage.apply {
                scrollToBottomOfConversationScreen()
                assertImageIsVisible()
            }
        }

        step("I send message Hello as well") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello as well!")
                clickSendButton()
            }
            closeKeyboardIfOpened()
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation("Hello as well!")
        }

        step("I open HappyMigration details and show more options") {
            pages.conversationViewPage.clickOnGroupConversationDetails("HappyMigration")
            pages.groupConversationDetailsPage.tapShowMoreOptionsButton()
        }

        step("I clear the conversation content and confirm") {
            pages.groupConversationDetailsPage.apply {
                tapClearContentButton()
                tapClearContentConfirmButton()
            }
        }

        step("I see the conversation content was deleted message") {
            pages.groupConversationDetailsPage.assertToastMessageIsDisplayed(
                "Conversation content was deleted"
            )
        }

        step("I close the group conversation details") {
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
        }

        step("HappyMigration is in the foreground") {
            pages.conversationViewPage.assertGroupConversationInForeground("HappyMigration")
        }

        step("I close HappyMigration") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("I minimise Wire") {
            device.pressHome()
        }

        step("I upgrade Wire to the recent version") {
            val recentWireApkPath = InstrumentationRegistry.getArguments()
                .getString("newApkPath") ?: "/data/local/tmp/Wire.new.apk"
            UiAutomatorSetup.upgradeWireToRecentVersion(recentWireApkPath)
        }

        step("I wait until the upgraded app is fully logged in") {
            pages.registrationPage.waitUntilLoginFlowIsCompleted()
        }

        step("HappyMigration remains visible after the upgrade") {
            pages.conversationListPage.assertGroupConversationVisible("HappyMigration")
        }

        step("I open HappyMigration and do not see the cleared content") {
            pages.conversationListPage.clickGroupConversation("HappyMigration")
            pages.conversationViewPage.apply {
                assertMessageNotVisible("Hello as well!")
                assertMessageNotVisible("Hello to you, too!")
                assertImageNotVisible()
                assertMessageNotVisible("Hello!")
            }
        }

        step("Member 2 sends a message after the upgrade") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello after Migration",
                "Device1",
                "HappyMigration"
            )
        }

        step("I see the new message after the upgrade") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(
                "Hello after Migration"
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4569")
    @Category("regression", "RC", "upgrade")
    @Test
    fun givenConversationWithRemovedTeamMember_whenUpgradingWire_thenDeletedStatusIsPreserved() {
        step("There is a team owner with a team named Migration") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Migration",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("Team owner adds Member 1 and Member 2 to the team") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Migration",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("Member 2 and Team Owner add a test-service device") {
            testServiceHelper.apply {
                addDevice("user3Name", null, "Device1")
                addDevice("user1Name", null, "Device1")
            }
        }

        step("Member 1 is me") {
            teamOwner = clientUserManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
            member1 = clientUserManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
            member2 = clientUserManager.findUserBy("user3Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("Member 1 has a 1:1 conversation with Member 2") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user3Name",
                "Migration"
            )
        }

        step("Member 1 has a 1:1 conversation with Team Owner") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user1Name",
                "Migration"
            )
        }

        loginAsMember1()

        step("I see Member 2 and Team Owner in the conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationVisible(member2?.name.orEmpty())
                assertConversationVisible(teamOwner?.name.orEmpty())
            }
        }

        step("I open the conversation with Member 2") {
            pages.conversationListPage.tapConversationNameInConversationList(member2?.name.orEmpty())
        }

        step("Member 2 sends message Hello") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user3Name",
                "Hello!",
                "Device1",
                "user2Name"
            )
        }

        step("I see Member 2's message") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("I send message Hello to you too") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello to you, too!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello to you, too!")
            }
        }

        step("I close the keyboard and the conversation with Member 2") {
            closeKeyboardIfOpened()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("Team Owner removes Member 2 from Migration") {
            requireNotNull(teamOwner).deleteTeamMember(
                backendClient,
                requireNotNull(member2?.id) { "Member 2 has no backend user ID." }
            )
        }

        step("Member 2 has deleted status in the conversation list") {
            pages.conversationListPage.assertConversationDeletedStatusVisible(member2?.name.orEmpty())
        }

        step("I minimise Wire") {
            device.pressHome()
        }

        step("I upgrade Wire to the recent version") {
            val recentWireApkPath = InstrumentationRegistry.getArguments()
                .getString("newApkPath") ?: "/data/local/tmp/Wire.new.apk"
            UiAutomatorSetup.upgradeWireToRecentVersion(recentWireApkPath)
        }

        step("I wait until the upgraded app is fully logged in") {
            pages.registrationPage.waitUntilLoginFlowIsCompleted()
        }

        step("Member 2 remains deleted and Team Owner remains visible after the upgrade") {
            pages.conversationListPage.apply {
                assertConversationDeletedStatusVisible(member2?.name.orEmpty())
                assertConversationVisible(teamOwner?.name.orEmpty())
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4570")
    @Category("regression", "RC", "upgrade")
    @Test
    fun givenDeletedGroupConversation_whenUpgradingWire_thenConversationRemainsDeleted() {
        step("There is a team owner with a team named Migration") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Migration",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("Team owner adds Member 1 and Member 2 to the team") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Migration",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("Member 1 is me") {
            teamOwner = clientUserManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
            member1 = clientUserManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
            member2 = clientUserManager.findUserBy("user3Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("Member 2 and Team Owner add a test-service device") {
            testServiceHelper.apply {
                addDevice("user3Name", null, "Device1")
                addDevice("user1Name", null, "Device1")
            }
        }

        step("Team owner has group conversation HappyMigration with Member 1 and Member 2") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "HappyMigration",
                "user2Name,user3Name",
                "Migration"
            )
        }

        step("Member 1 has a 1:1 conversation with Member 2") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user3Name",
                "Migration"
            )
        }

        step("Member 1 has a 1:1 conversation with Team Owner") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user1Name",
                "Migration"
            )
        }

        loginAsMember1()

        step("I see all prepared conversations in the conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("HappyMigration")
                assertConversationVisible(member2?.name.orEmpty())
                assertConversationVisible(teamOwner?.name.orEmpty())
            }
        }

        step("I open group conversation HappyMigration") {
            pages.conversationListPage.clickGroupConversation("HappyMigration")
        }

        step("Member 2 and Team Owner send messages to HappyMigration") {
            testServiceHelper.apply {
                userSendMessageToConversation(
                    "user3Name",
                    "Hello!",
                    "Device1",
                    "HappyMigration"
                )
                userSendMessageToConversation(
                    "user1Name",
                    "Hello to you, too!",
                    "Device1",
                    "HappyMigration"
                )
            }
        }

        step("I see both received messages in HappyMigration") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
                assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
            }
        }

        step("I send message Hello as well") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello as well!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello as well!")
            }
        }

        step("I close the keyboard") {
            closeKeyboardIfOpened()
        }

        step("Team Owner deletes HappyMigration") {
            backendSetupHelper.userDeletesGroupConversation("user1Name", "HappyMigration")
        }

        step("HappyMigration is no longer visible") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationNotVisible("HappyMigration")
            }
        }

        step("I minimise Wire") {
            device.pressHome()
        }

        step("I upgrade Wire to the recent version") {
            val recentWireApkPath = InstrumentationRegistry.getArguments()
                .getString("newApkPath") ?: "/data/local/tmp/Wire.new.apk"
            UiAutomatorSetup.upgradeWireToRecentVersion(recentWireApkPath)
        }

        step("I wait until the upgraded app is fully logged in") {
            pages.registrationPage.waitUntilLoginFlowIsCompleted()
        }

        step("HappyMigration remains deleted and the one-to-one conversations remain visible") {
            pages.conversationListPage.apply {
                assertConversationNotVisible("HappyMigration")
                assertConversationVisible(teamOwner?.name.orEmpty())
                assertConversationVisible(member2?.name.orEmpty())
            }
        }
    }

    // Keeps the repeated staging login flow consistent across upgrade scenarios.
    private fun loginAsMember1() {
        step("I log in as Member 1 using the current login flow") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
                enterTeamMemberLoggingEmail(member1?.email.orEmpty())
                clickLoginButton()
                enterTeamMemberLoggingPassword(member1?.password.orEmpty())
                clickLoginButton()
            }
        }

        step("I complete the post-login prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }
    }
}
