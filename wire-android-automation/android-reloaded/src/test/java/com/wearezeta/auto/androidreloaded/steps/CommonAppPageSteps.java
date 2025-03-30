package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.common.PackageNameHolder;
import com.wearezeta.auto.androidreloaded.pages.CommonAppPage;
import com.wearezeta.auto.androidreloaded.pages.ConversationListPage;
import com.wearezeta.auto.androidreloaded.pages.external.ChromePage;
import com.wearezeta.auto.common.misc.Timedelta;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import com.wire.qa.picklejar.engine.exception.SkipException;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class CommonAppPageSteps {

    private final AndroidTestContext context;

    public CommonAppPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private CommonAppPage getCommonAppPage() {
        return context.getPage(CommonAppPage.class);
    }

    @When("^I see the Wire app is in foreground$")
    public void ISeeAppInForeground() {
        final String packageId = PackageNameHolder.getPackageName();
        assertThat("Wire is currently not in foreground", getCommonAppPage().isAppInForeground(packageId));
    }

    @When("^I see the Wire app is not in foreground$")
    public void ISeeAppNotInForeground() {
        final String packageId = PackageNameHolder.getPackageName();
            assertThat("Wire is currently not in foreground", getCommonAppPage().isAppInBackground(packageId));
    }

    @And("^I wait for (\\d+) seconds?")
    public void WaitForTime(int seconds) {
        if (seconds > 20) {
            context.startPinging();
        }
        Timedelta.ofSeconds(seconds).sleep();
        context.stopPinging();
    }

    @Given("^Test runs only on node \"(.*)\"$")
    public void testRunsOnlyOnNode(String browserName) {
        final UiAutomator2Options capabilities = new UiAutomator2Options();
        capabilities.setCapability("browserName", browserName);
        context.addCapabilities(capabilities);
    }

    @Given("^I enable Wi-Fi on the device$")
    public void iEnableWiFiMode() {
        context.isWiFiStateResetNeeded = true;
        getCommonAppPage().enableWifi();
    }

    @Given("^I disable Wi-Fi on the device$")
    public void iDisableWiFiMode() {
        context.isWiFiStateResetNeeded = true;
        getCommonAppPage().disableWifi();
    }

    @When("^I wait until Wifi is enabled again$")
    public void iWaitUntilWifiIsEnabledAgain() throws Exception {
        getCommonAppPage().waitUntilWifiIsEnabled();
        context.getPage(ConversationListPage.class).waitUntilWaitingForNetworkIsInvisible();
        context.getPage(ConversationListPage.class).waitUntilSyncBarInvisible();
    }

    @Given("^I clear cache of system browser$")
    public void iClearCacheOfSystemBrowser() {
        getCommonAppPage().clearCache();
    }

    // Alerts that can be displayed anywhere inside off the app

    @When("^I see alert informing me that my Team settings have changed$")
    public void iSeeTeamSettingsChangedAlert() {
        assertThat("Alert is not visible.", getCommonAppPage().isAlertTeamSettingsChangedVisible());
    }

    @When("^I see subtext \"(.*)\" in the Team settings change alert$")
    public void iSeeSubtextTeamSettingsChangeAlert(String subtext) {
        assertThat("Subtext is not visible.", getCommonAppPage().isTextTeamSettingsChangedAlertVisible(subtext));
    }

    @When("^I see Wire Enterprise alert$")
    public void iSeeWireEnterpriseAlert() {
        assertThat("Alert is not visible.", getCommonAppPage().isWireEnterpriseAlertVisible());
    }

    @When("^I see subtext \"(.*)\" in the Wire Enterprise alert$")
    public void iSeeSubtextEnterpriseAlert(String subtext) {
        assertThat("Subtext is not visible.", getCommonAppPage().isTextEnterpriseAlertVisible(subtext));
    }

    @When("^I see link \"(.*)\" in the Wire Enterprise alert$")
    public void iSeeLinkEnterpriseAlert(String link) {
        assertThat("Link is not visible.", getCommonAppPage().isTextEnterpriseAlertVisible(link));
    }

    @When("^I tap on Learn more link on the Enterprise alert$")
    public void iTapLearnMoreLinkEnterpriseAlert() {
        getCommonAppPage().tapLearnMoreLinkEnterpriseAlert();
    }

    @When("^I tap Upgrade now button on the Enterprise alert$")
    public void iTapUpgradeButtonOnEnterpriseAlert() {
        getCommonAppPage().tapUpgradeButtonEnterpriseAlert();
    }

    // Login to new client

    @When("^I see alert informing me that my account was used on another device$")
    public void iSeeAccountUsedOnAnotherDeviceAlert() {
        assertThat("Alert is not visible.", getCommonAppPage().isAccountUsedOnAnotherDeviceAlertVisible());
    }

    @When("^I see alert informing me that my second account \"(.*)\" was used on another device$")
    public void iSeeSecondAccountUsedOnAnotherDeviceAlert(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Alert is not visible.", getCommonAppPage().isSecondAccountUsedOnAnotherDeviceAlertVisible(user));
    }

    @When("^I see subtext \"(.*)\" in the added device alert$")
    public void iSeeSubtextUsedDeviceAlert(String subtext) {
        assertThat("Subtext is not visible.", getCommonAppPage().isTextUsedDeviceAlertVisible(subtext));
    }

    @When("^I tap OK button on Account was used on another device alert$")
    public void iTapOKButtonDevices() {
        getCommonAppPage().tapOKButtonDevicesAlert();
    }

    @When("^I tap Manage Devices button$")
    public void iTapManageDevicesButton() {
        getCommonAppPage().tapManageDevicesButton();
    }

    @When("^I tap Switch Account button$")
    public void iTapSwitchAccountButton() {
        getCommonAppPage().tapSwitchAccountButton();
    }

    // Deletion of account or removed device

    @When("^I see alert informing me that my device was removed$")
    public void iSeeDeviceRemovedAlert() {
        assertThat("Alert is not visible.", getCommonAppPage().isDeviceRemovedAlertVisible());
    }

    @When("^I see subtext \"(.*)\" in the removed device alert$")
    public void iSeeSubtextRemoveDeviceAlert(String subtext) {
        assertThat("Subtext is not visible.", getCommonAppPage().isTextRemoveDeviceAlertVisible(subtext));
    }

    @When("^I see alert informing me that my account was deleted$")
    public void iSeeDeletedAccountAlert() {
        assertThat("Alert is not visible.", getCommonAppPage().isDeletedAccountAlertVisible());
    }

    @When("^I see subtext \"(.*)\" in the deleted account alert$")
    public void iSeeSubtextDeletedAccountAlert(String subtext) {
        assertThat("Subtext is not visible.", getCommonAppPage().isTextDeletedAccountVisible(subtext));
    }

    @When("^I tap OK button on the alert$")
    public void iTapOkButton() {
        getCommonAppPage().tapOkButton();
    }

    @When("^I tap cancel button on the alert$")
    public void iTapCancelButton() {
        getCommonAppPage().tapCancelButton();
    }

    @When("^I see webpage with \"(.*)\" is in foreground$")
    public void iSeeWebPageWithURL(String url) {
        assertThat("Wrong URL in browser", context.getPage(ChromePage.class).isTextVisible(url));
    }

    @When("^I do not see webpage with \"(.*)\" is in foreground$")
    public void iDoNotSeeWebPageWithURL(String url) {
        assertThat("Wrong URL in browser", context.getPage(ChromePage.class).isTextInvisible(url));
    }

    @When("^I close the page through the X icon$")
    public void iCloseWebPage() {
        getCommonAppPage().closeWebPage();
    }

    @When("^I see alert informing me that something went wrong$")
    public void iSeeClearDataAlert() {
        assertThat("Alert is not visible.", getCommonAppPage().isSomethingWentWrongAlertVisible());
    }

    // Join Conversation

    @When("^I see join conversation alert$")
    public void iSeeJoinConversationAlert() {
        assertThat("Alert for joining conversation is not visible.", getCommonAppPage().isJoinConversationAlertVisible());
    }

    @When("^I see can not join conversation alert$")
    public void iSeeCanNotJoinConversationAlert() {
        assertThat("Alert for joining conversation is visible but should not.", getCommonAppPage().isCanNotJoinConversationAlertVisible());
    }

    @When("^I see subtext \"(.*)\" in the join conversation alert$")
    public void iSeeSubtextJoinConversationAlert(String subtext) {
        assertThat("Subtext in join conversation alert is not visible.", getCommonAppPage().isTextJoinConversationAlertVisible(subtext));
    }

    @When("^I see subtext \"(.*)\" in the can not join conversation alert$")
    public void iSeeSubtextCanNotJoinConversationAlert(String subtext) {
        assertThat("Subtext in join conversation alert is not visible.", getCommonAppPage().isTextCanNotJoinConversationAlertVisible(subtext));
    }

    @When("^I see conversation name \"(.*)\" in the join conversation alert$")
    public void iSeeConversationNameInAlert(String conversationName) {
        assertThat("Conversation name is not displayed in alert.", getCommonAppPage().isGroupNameDisplayedInJoinConversationAlert(conversationName));
    }

    @When("^I enter password \"(.*)\" on join conversation alert$")
    public void iEnterPasswordJoinConversationAlert(String password) {
        getCommonAppPage().enterPasswordJoinConversation(password);
    }

    @When("^I tap join button on join conversation alert$")
    public void iTapJoinButton() {
        getCommonAppPage().tapJoinConversationButton();
    }

    @When("^I tap cancel button on join conversation alert$")
    public void iTapCancelJoiningButton() {
        getCommonAppPage().tapCancelJoiningConversationButton();
    }

    @When("^I see invalid password error on join conversation alert$")
    public void iSeeInvalidPasswordJoinConversationAlert() {
        assertThat("Error is not displayed, but should.", getCommonAppPage().isInvalidPasswordJoinConversationErrorVisible());
    }

    // Bund feature flags

    @When("^User registration and team creation is enabled on the build$")
    public void isUserCreationEnabled() {
        if (PackageNameHolder.getPackageName().equals("com.wire.android.bund")) {
            throw new SkipException("Skip test because backend has SFT disabled");
        }
    }

    @When("^Notifications when the app is in the background are enabled on the build$")
    public void areNotificationsEnabled() {
        if (PackageNameHolder.getPackageName().equals("com.wire.android.bund")) {
            throw new SkipException("Skip test because backend has SFT disabled");
        }
    }

    @When("^Login with 3 accounts is enabled on the build$")
    public void isLoginWith3AccountsEnabled() {
        if (PackageNameHolder.getPackageName().equals("com.wire.android.bund")) {
            throw new SkipException("Skip test because backend has SFT disabled");
        }
    }
}
