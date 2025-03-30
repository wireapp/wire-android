package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.common.FileHelper;
import com.wearezeta.auto.androidreloaded.common.PackageNameHolder;
import com.wearezeta.auto.androidreloaded.pages.AndroidPage;
import com.wearezeta.auto.androidreloaded.pages.CommonAppPage;
import com.wearezeta.auto.common.Config;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.log.ZetaLogger;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import java.io.File;
import java.time.Duration;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class CommonDeviceSteps {

    private static final Logger log = ZetaLogger.getLog(CommonDeviceSteps.class.getSimpleName());

    AndroidTestContext context;

    public CommonDeviceSteps(AndroidTestContext context) {
        this.context = context;
    }

    private AndroidPage getCommonPage() {
        return context.getPage(AndroidPage.class);
    }

    private static String getPath() {
        return Config.current().getAndroidApplicationPath(CommonDeviceSteps.class);
    }

    private static String getOldAppPath() {
        return Config.current().getOldAppPath(CommonDeviceSteps.class);
    }

    public static String isCountlyAvailable() {
        return Config.current().isCountlyAvailable(CommonDeviceSteps.class);
    }

    public static String getPackageName() {
        return PackageNameHolder.getPackageName();
    }

    @When("^I open (.*) backend deep link$")
    public void iOpenDeepLinkForCustomBackend(String backendType) {
        if (context.isDriverCreated()) {
            String deeplink = BackendConnections.get(backendType).getDeeplinkForAndroid();
            log.info("deeplink: " + deeplink);
            getCommonPage().openURL(deeplink);
        }
    }

    @When("^I open backend via deep link$")
    public void iOpenDefaultBackend() {
        if (context.isDriverCreated()) {
            String deeplink = BackendConnections.getDefault().getDeeplinkForAndroid();
            log.info(deeplink);
            getCommonPage().openURL(deeplink);
        }
    }

    @When("^I tap back button$")
    public void tapBackButton() {
        getCommonPage().navigateBack();
    }

    @When("^I tap back button (\\d+) times$")
    public void tapBackButtonXTimes(int times) {
        for (int i = 0; i < times; i++) {
            getCommonPage().navigateBack();
        }
    }

    @When("^I hide the keyboard$")
    public void hideKeyboard() {
        getCommonPage().hideKeyboard();
    }

    @Then("^I verify that Android clipboard content equals to \"(.*)\"$")
    public void IVerifyClipboardContent(String expectedMsg) {
        assertThat("The expected and the current clipboard contents are different", getCommonPage().getClipboard(), equalTo(expectedMsg));
    }

    @When("^I minimise Wire$")
    public void iMinimiseWire() {
        getCommonPage().minimizeApp();
    }

    @When("^I terminate Wire$")
    public void iTerminateWire() {
        getCommonPage().terminateApp(getPackageName());
    }

    @When("^I swipe the app away from background$")
    public void iSwipeTheAppAwayFromBackground() {
        context.getPage(CommonAppPage.class).swipeAppAwayFromBackground();

    }

    @When("^I restart Wire$")
    public void iRestartWire() {
        getCommonPage().activateApp(getPackageName());
    }

    @Given("I reinstall the old Wire Version$")
    public void iReinstallOldAppVersion() {
        getCommonPage().uninstallApp(getPackageName());
        getCommonPage().installApp(getOldAppPath(), true, true);
        getCommonPage().activateApp(getPackageName());
    }

    @When("^I upgrade Wire to the recent version$")
    public void iUpgradeWire() {
        getCommonPage().installApp(getPath(), true, true);
        getCommonPage().activateApp(getPackageName());
    }

    // File Sharing

    @When("^I wait up (\\d+) seconds? until file having name \"(.*)\" is downloaded to the device$")
    public void theXFileSavedInDownloadFolder(int timeoutSeconds, String fileFullName) {
        final String filePath = Config.current().getBuildPath(this.getClass()) +
                File.separator + fileFullName;
        final Wait<? extends RemoteWebDriver> wait = new FluentWait<>(context.getDriver())
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(500));
        boolean fileDownloaded = wait.until(drv -> {
            try {
                new FileHelper(drv).pullDownloadedFileFromSdcard(fileFullName);
            } catch (Exception e) {
                // do nothing
            }
            return new File(filePath).exists();
        });

        assertThat(
                String.format("Cannot pull downloaded file '%s' from the device", fileFullName), fileDownloaded);
    }

    @Given("^I remove the file \"(.*)\" from device's sdcard$")
    public void iRemoveRemoteFile(String fileFullName) {
        new FileHelper(context.getDriver()).removeFileFromSdcard(fileFullName);
    }

    // Joining conversation

    @When("^I open deep link for joining conversation (.*) that user (.*) has sent me$")
    public void iOpenDeepLinkForJoiningConversation(String conversationName, String nameAlias) {
        String deeplink = context.getCommonSteps().getClientDeepLinkForPublicConversation(nameAlias, conversationName);
        getCommonPage().openURL(deeplink);
        log.info(deeplink);
    }

    @When("^I remember deep link that User (.*) created for conversation (.*) as invite$")
    public void iRememberDeepLinkForInvite(String userNameAlias, String conversationName) {
        context.setRememberedInviteLink(context.getCommonSteps().userCreatesInviteDeeplink(userNameAlias, conversationName));
        log.info("Deeplink: " + context.getRememberedInviteLink());
    }

    @When("^I open remembered deep link for joining conversation$")
    public void iOpenRememberedDeepLinkForJoiningConversation() {
        log.info(context.getRememberedInviteLink());
        getCommonPage().openURL(context.getRememberedInviteLink());
    }
}