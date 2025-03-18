package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidDriverBuilder;
import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.SettingsPage;
import com.wearezeta.auto.common.CommonSteps;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.email.MailboxProvider;
import com.wearezeta.auto.common.email.handlers.ISupportsMessagesPolling;
import com.wearezeta.auto.common.email.messages.ActivationMessage;
import com.wearezeta.auto.common.email.messages.WireMessage;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class SettingsPageSteps {

    private final AndroidTestContext context;

    public SettingsPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private SettingsPage getSettingsPage() {
        return context.getPage(SettingsPage.class);
    }

    private CommonSteps getCommonSteps() {
        return context.getCommonSteps();
    }

    private static final Logger log = ZetaLogger.getLog(AndroidDriverBuilder.class.getSimpleName());

    // Main Menu

    @When("^I open the main navigation menu$")
    public void iOpenMainNavigationMenu() {
        getSettingsPage().tapMainMenu();
    }

    @When("^I tap on conversations menu entry$")
    public void iOpenConversations() {
        getSettingsPage().openConversations();
    }

    @When("^I tap on archive menu entry$")
    public void iOpenArchive() {
        getSettingsPage().openArchive();
    }

    @When("^I tap on Settings menu entry$")
    public void iOpenSettings() {
        getSettingsPage().openSettings();
    }

    @When("^I tap on Support menu entry$")
    public void iOpenSupport() {
        getSettingsPage().openSupport();
    }

    @When("^I close the settings through the back button$")
    public void iCloseSettings() {
        getSettingsPage().closeSettings();
    }

    @When("^I open report a bug menu$")
    public void iOpenReportBugMenu() {
        getSettingsPage().openReportBugMenu();
    }

    @When("^I see the app drawer opens where I can share my bug report$")
    public void iSeeAppDrawer() {
        assertThat("App drawer did not open.", getSettingsPage().isAppDrawerVisible());
    }

    // Settings

    @When("^I open my account details menu$")
    public void iOpenAccountDetailsMenu() {
        getSettingsPage().tapAccountDetailsMenu();
    }

    @When("^I open manage your devices menu$")
    public void iOpenManageDevicesMenu() {
        getSettingsPage().tapManageDevicesMenu();
    }

    @When("^I tap on lock with passcode toggle$")
    public void iTapLockWithPassCodeToggle() {
        getSettingsPage().iTapLockWithPasscodeToggle();
    }

    @When("^I see lock with passcode toggle is turned off$")
    public void iSeeLockWithPassCodeToggleIsOff() {
        assertThat("Lock with passcode toggle is not enabled", getSettingsPage().isLockWithPasscodeDisabled());
    }

    @When("^I see lock with passcode toggle is turned on$")
    public void iSeeLockWithPassCodeToggleIsOn() {
        assertThat("Lock with passcode toggle is not enabled", getSettingsPage().isLockWithPasscodeEnabled());
    }

    @When("^I see lock with passcode toggle can not be changed$")
    public void iSeePasscodeToggleIsNotClickable() {
        assertThat("Lock with passcode toggle is clickable.", getSettingsPage().isPasscodeToggleInvisible());
    }

    @When("^I open the debug menu$")
    public void iOpenDebugMenu() {
        getSettingsPage().tapDebugMenu();
    }

    @When("^I tap the logging toggle")
    public void iTapLoggingToggle() {
        getSettingsPage().tapLoggingToggle();
    }

    @When("^I see enable logging toggle is off")
    public void enableLoggingToggleOff() {
        assertThat("Enable logging toggle is on. ", getSettingsPage().isEnableLoggingToggleOff());
    }

    @When("^I see enable logging toggle is on")
    public void enableLoggingToggleOn() {
        assertThat("Enable logging toggle is off. ", getSettingsPage().isEnableLoggingToggleOn());
    }

    @When("^I see analytics initialized is set to (.*)$")
    public void iSeeAnalyticsInitialized(String state) {
        assertThat("Analytics initialized is not as expected.", getSettingsPage().isAnalyticsInitialized(state));
    }

    @When("^I see my Analytics tracking identifier$")
    public void iSeeAnalyticsIdentifier() {
        assertThat("Analytics initialized is not as expected.", getSettingsPage().getTextAnalyticsTrackingIdentifier(), not(""));
    }

    @When("^I open the Back up & Restore Conversations menu$")
    public void iOpenBackupMenu() {
        getSettingsPage().tapBackupMenu();
    }

    @When("^I tap Support in Settings$")
    public void iTapSupportInSettings() {
        getSettingsPage().tapSupportInSettings();
    }

    // Account Details

    @When("^I start activation email monitoring on mailbox (.*)")
    public void IStartActivationEmailMonitoringOnMbox(String mbox) throws Exception {
        ClientUser user = context.getUsersManager().findUserByEmailOrEmailAlias(mbox);

        final Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(WireMessage.ZETA_PURPOSE_HEADER_NAME, ActivationMessage.MESSAGE_PURPOSE);
        ISupportsMessagesPolling mailbox = MailboxProvider.getInstance(BackendConnections.get(user), user.getEmail());
        context.setActivationMessage(mailbox.getMessage(expectedHeaders, ActivationMessage.ACTIVATION_TIMEOUT));
    }

    @When("^I change email address to (.*) on Settings page$")
    public void IChangeEmailAddress(String newEmail) {
        newEmail = context.getUsersManager().replaceAliasesOccurrences(newEmail, ClientUsersManager.FindBy.EMAIL_ALIAS);
        getSettingsPage().changeEmailAddress(newEmail);
    }

    @Then("^I verify email address (.*) for (.*)")
    public void IVerifyEmail(String address, String user) throws Exception {
        if (context.getActivationMessage() == null) {
            throw new IllegalStateException("Activation email monitoring is expected to be running");
        }
        context.getCommonSteps().activateRegisteredUserByEmail(context.getActivationMessage());
        address = context.getUsersManager()
                .replaceAliasesOccurrences(address, ClientUsersManager.FindBy.EMAIL_ALIAS);
        final ClientUser dstUser = context.getUsersManager()
                .findUserByNameOrNameAlias(user);
        dstUser.setEmail(address);
        context.setActivationMessage(null);
    }

    @When("^I tap save button on email change view$")
    public void iTapEmailSaveButton() {
        getSettingsPage().tapSaveButton();
    }

    @When("^I see the notification about email change containing the \"(.*)\" is displayed$")
    public void iSeeMyNewEmail(String email) {
        email = context.getUsersManager().replaceAliasesOccurrences(email, ClientUsersManager.FindBy.EMAIL_ALIAS);
        assertThat("New email is not displayed.", getSettingsPage().isNewEmailDisplayed(email));
    }

    @When("^I see my profile name \"(.*)\" is displayed$")
    public void iSeeProfileName(String name) {
        name = context.getUsersManager().replaceAliasesOccurrences(name, ClientUsersManager.FindBy.NAME_ALIAS);
        getSettingsPage().isProfileNameVisible(name);
    }

    @When("^I tap on my profile name \"(.*)\" in Account Details$")
    public void iTapProflieName(String name) {
        name = context.getUsersManager().replaceAliasesOccurrences(name, ClientUsersManager.FindBy.NAME_ALIAS);
        getSettingsPage().tapProfileName(name);
    }

    @When("^I see edit profile name page$")
    public void iSeeEditProfileNamePage() {
        getSettingsPage().isProfileNameHeadingVisible();
        getSettingsPage().isEditBoxProfileNameVisible();
    }

    @When("^I do not see edit profile name page$")
    public void iDoNotSeeEditProfileNamePage() {
        getSettingsPage().isProfileNameHeadingInvisible();
        getSettingsPage().isEditBoxProfileNameInvisible();
    }

    @When("^I edit my profile name to \"(.*)\" in Account Details$")
    public void iEditMyProfileName(String newName) {
        getSettingsPage().editProfileName(newName);
    }

    @When("^I tap save button$")
    public void iTapSaveButton() {
        getSettingsPage().tapSaveButton();
    }

    @When("^I see toast message \"(.*)\" in Account Details$")
    public void iSeeToastMessage(String text) {
        assertThat("Toast message is not displayed.", getSettingsPage().isTextDisplayed(text));
    }

    @When("^I see my username \"(.*)\" is displayed$")
    public void iSeeUserName(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.UNIQUE_USERNAME_ALIAS);
        getSettingsPage().isUserNameVisible(userName);
    }

    @When("^I see my email address \"(.*)\" is displayed$")
    public void iSeeEmailAddress(String email) {
        email = context.getUsersManager().replaceAliasesOccurrences(email, ClientUsersManager.FindBy.EMAIL_ALIAS);
        getSettingsPage().isEmailVisible(email);
    }

    @Then("^I verify user's (.*) email on the backend is equal to (.*)")
    public void IVerifyEmailOnBackend(String user, String expectedValue) {
        getCommonSteps().userVerifiesEmail(user, expectedValue);
    }

    @When("^I tap my email address \"(.*)\" that is displayed$")
    public void iTapUserEmailAddress(String email) {
        email = context.getUsersManager().replaceAliasesOccurrences(email, ClientUsersManager.FindBy.EMAIL_ALIAS);
        getSettingsPage().tapUserEmail(email);
    }

    @When("^I see my team name \"(.*)\" is displayed$")
    public void iSeeTeamName(String teamName) {
        getSettingsPage().isTeamNameVisible(teamName);
    }

    @When("^I see my domain \"(.*)\" is displayed$")
    public void iSeeDomain(String domain) {
        getSettingsPage().isDomainVisible(domain);
    }

    @When("^I see reset password button$")
    public void iSeeResetPassword() {
        assertThat("Reset Password Button is not visible. ", getSettingsPage().isResetPasswordVisible());
    }

    @When("^I see delete account button$")
    public void iSeeDeleteAccount() {
        assertThat("Delete Account Button is not visible. ", getSettingsPage().isDeleteAccountVisible());
    }

    @When("^I do not see reset password button$")
    public void iDoNotSeeResetPassword() {
        assertThat("Reset Password Button is visible. ", getSettingsPage().isResetPasswordInvisible());
    }

    @When("^I do not see delete account alert confirmation")
    public void iDoNotSeeDeleteAccountAlert() {
        assertThat("Delete Account alert is visible. ", getSettingsPage().isDeleteAccountAlertInVisible());
    }

    @When("^I tap reset password button$")
    public void iTapResetPassword() {
        getSettingsPage().tapResetPasswordButton();
    }

    @When("^I tap delete account button$")
    public void iTapDeleteAccount() {
        getSettingsPage().tapDeleteAccountButton();
    }

    @When("^I tap continue button on delete account alert$")
    public void iTapContinueDeleteAccountButton() {
        getSettingsPage().tapContinueDeleteAccountButton();
    }

    @When("^I see delete account confirmation alert$")
    public void iSeeDeleteAccountAlert() {
        assertThat("delete account confirmation alert is not visible. ", getSettingsPage().isDeleteAccountAlertDisplayed());
    }

    // Privacy Settings

    @When("^I tap Privacy Settings menu$")
    public void iTapPrivacySettings() {
        getSettingsPage().tapPrivacySettingsButton();
    }

    @When("^I see read receipts are turned on$")
    public void readReceiptsTurnedOn() {
        assertThat("Read receipts are not enabled", getSettingsPage().areReadReceiptsEnabled());
    }

    @When("^I see read receipts are turned off$")
    public void readReceiptsTurnedf() {
        assertThat("Read receipts are enabled", getSettingsPage().areReadReceiptsDisabled());
    }

    @When("^I tap read receipts toggle")
    public void iTapReadReceiptsToggle() {
        getSettingsPage().tapReadReceiptsToggle();
    }

    @When("^I see send anonymous usage data switch is turned on$")
    public void iSeeSendAnonymousUsageDataSwitchOn() {
        assertThat("Send anonymous usage data switch is not enabled", getSettingsPage().isSendAnonymousUsageDataSwitchEnabled());
    }

    @When("^I see send anonymous usage data switch is turned off$")
    public void iSeeSendAnonymousUsageDataSwitchOff() {
        assertThat("Send anonymous usage data switch is not enabled", getSettingsPage().isSendAnonymousUsageDataSwitchDisabled());
    }

    // Network Settings

    @When("^I tap Network Settings menu$")
    public void iTapNetworkSetings() {
        getSettingsPage().tapNetworkSettingsButton();
    }

    @When("^I tap Websocket Connection button$")
    public void iTapWebsocketConnectionButton() {
        getSettingsPage().tapWebsocketConnectionButton();
    }

    @When("^I see Websocket switch is at \"(.*)\" state$")
    public void iSeeWebsocketSwitchOn(String state) {
        assertThat("Toggle is not in correct state.", getSettingsPage().isSwitchStateVisible(state));
    }

    @When("^I see that there is no option to enable or disable my websocket$")
    public void iSeeWebsocketSwitchEnabledByDefault() {
        assertThat("Websocket switch is visible, although it should be invisible.", getSettingsPage().isWebSocketSwitchInvisible());
    }

    //Backup

    @When("^I see Backup Page$")
    public void iSeeBackupPage() {
        assertThat("Backup Page is not open", getSettingsPage().isBackupPageVisible());
    }

    @When("^I see Backup Page Heading$")
    public void iSeeBackupPageHeading() {
        assertThat("Backup Page is not displayed", getSettingsPage().iSeeBackupPageHeading());
    }

    @When("^I tap on Create a Backup button$")
    public void iTapCreateBackupButton() {
        getSettingsPage().tapCreateBackupButton();
    }

    @When("^I tap on Restore from Backup button$")
    public void iTapRestoreBackupButton() {
        getSettingsPage().tapRestoreBackupButton();
    }

    @When("^I tap on Choose Backup button$")
    public void iTapChooseBackupButton() {
        getSettingsPage().tapChooseBackupFileButton();
    }

    @When("^I type my password (.*) to create my backup$")
    public void iTypeBackupPasswordCreate(String password) {
        getSettingsPage().typePasswordBackupCreate(password);
    }

    @When("^I type my password (.*) to restore my backup$")
    public void iTypeBackupPasswordRestore(String password) {
        getSettingsPage().typePasswordBackupRestore(password);
    }

    @When("^I tap on Back Up Now button$")
    public void iTapBackupNowButton() {
        getSettingsPage().tapBackupNowButton();
    }

    @When("^I wait until I see the message \"(.*)\" in backup alert")
    public void waitUntilMessageDisplayed(String message) {
        getSettingsPage().waitUntilTextDisplayed(message);
    }

    @When("^I tap on Save File button in backup alert$")
    public void iTapSaveNowButton() {
        getSettingsPage().tapSaveFileButton();
    }

    @When("^I tap on Save button in DocumentsUI$")
    public void iTapSaveInOSMenuButton() {
        getSettingsPage().tapSaveFileOSMenuButton();
    }

    @When("^I tap continue button on restore backup page$")
    public void iTapContinueButton() {
        getSettingsPage().tapContinueButtonRestoreBackup();
    }

    // Debug Menu

    @When("^I see my KeyPackages count is \"(.*)\" on debug screen$")
    public void iSeeKeyPackagesCount(String count) {
        assertThat("KeyPackages count is not as expected.", getSettingsPage().isCorrectAmountKeyPackagesDisplayed(count));
    }

    @When("^I remember the device id of the current device$")
    public void iRememberMyCurrentDevice() {
        String deviceID = getSettingsPage().getCurrentDeviceID();
        log.info(deviceID);
        // Remove spaces and "Device id: " from device ID text
        String finalDeviceID = deviceID.split(" ")[3];
        log.info(finalDeviceID);
        context.setCurrentDeviceId(finalDeviceID);
    }

    @When("^I remember the client id of the current device$")
    public void iRememberMyCurrentClientID() {
        String clientID = getSettingsPage().getCurrentClientID();
        log.info(clientID);
        String finalDeviceID = "";
        //FixMe: Remove if statement once we have "Proteus ID" also in release versions
        if (clientID.contains("Proteus")) {
            finalDeviceID = clientID.substring(12, 35).replace(" ", "");

        } else {
            finalDeviceID = clientID.substring(4, 27).replace(" ", "");
        }
        // Only grabbing the actual device ID from text entry and removing the spaces
        log.info(finalDeviceID);
        context.setCurrentDeviceId(finalDeviceID);
    }
}
