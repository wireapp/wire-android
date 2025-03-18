package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.androidreloaded.common.PackageNameHolder;
import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.appmanagement.ApplicationState;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import java.time.Duration;
import java.util.function.Function;

public class CommonAppPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[contains(@text,'Wire Enterprise')]")
    private WebElement enterpriseAlertHeading;

    @AndroidFindBy(xpath = "//*[contains(@text,'Learn more about Wire')]")
    private WebElement learnMoreLinkEnterpriseAlert;

    @AndroidFindBy(xpath = "//*[@text='Upgrade now']")
    private WebElement upgradeNowButton;

    @AndroidFindBy(xpath = "//*[@text='Your account was used on']")
    private WebElement accountUsedAlertHeading;

    @AndroidFindBy(xpath = "//*[@text='Manage Devices']")
    private WebElement manageDevicesButton;

    @AndroidFindBy(xpath = "//*[@text='Switch Account']")
    private WebElement switchAccountButton;

    @AndroidFindBy(xpath = "//*[@text='Removed Device']")
    private WebElement deviceRemovedAlertHeading;

    @AndroidFindBy(xpath = "//*[@text='Deleted account']")
    private WebElement deletedAccountAlertHeading;

    @AndroidFindBy(xpath = "//*[@text='OK']")
    private WebElement okButton;

    @AndroidFindBy(xpath = "//*[@text='Cancel']")
    private WebElement cancelButton;

    @AndroidFindBy(xpath = "//*[contains(@resource-id,'url_bar')]")
    private WebElement urlWebPage;

    @AndroidFindBy(xpath = "//android.widget.ImageButton[@content-desc='Close tab']")
    private WebElement closeButton;

    @AndroidFindBy(xpath = "//*[@text='Something went wrong']")
    private WebElement somethingWentWrongHeading;

    @AndroidFindBy(xpath = "//*[contains(@text,'Team Settings Changed')]")
    private WebElement headingTeamSettingsChange;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Join conversation?']")
    private WebElement joinConversationAlertHeading;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Unable to join conversation']")
    private WebElement canNotJoinConversationAlertHeading;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Conversation Password']/..//*[@class='android.widget.EditText']")
    private WebElement passwordFieldJoinConversation;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Join']")
    private WebElement joinConversationButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Cancel']")
    private WebElement cancelJoiningConversationButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Invalid password']")
    private WebElement invalidPasswordJoinConversationError;

    private final Function<String, String> subtextEnterpriseAlert = subtext -> String.format("//*[contains(@text,'Wire Enterprise')]/..//*[contains(@text,'%s')]", subtext);

    private final Function<String, String> subTextRemoveDeviceAlert = subtext -> String.format("//*[contains(@text,'Removed Device')]/..//*[contains(@text,'%s')]", subtext);

    private final Function<String, String> subTextAddedDeviceAlert = subtext -> String.format("//*[contains(@text,'Your account')]/..//*[contains(@text,'%s')]", subtext);

    private final Function<String, String> subTextDeletedAccountAlert = subtext -> String.format("//*[contains(@text,'Deleted account')]/..//*[@text='%s']", subtext);

    private final Function<String, String> subTextTeamSettingChangedAlert = subtext -> String.format("//*[contains(@text,'Team Settings Changed')]/..//*[contains(@text,'%s')]", subtext);

    private final Function<String, By> secondAccountUsedAlertHeading = userName-> By.xpath(String.format("//*[contains(@text,'Your account “%s')]", userName));

    private final Function<String, String> subtextJoinConversationAlert = subtext -> String.format("//android.widget.TextView[@text='Join conversation?']/..//android.widget.TextView[contains(@text,'%s')]", subtext);

    private final Function<String, String> subtextCanNotJoinConversationAlert = subtext -> String.format("//android.widget.TextView[@text='Unable to join conversation']/..//android.widget.TextView[contains(@text,'%s')]", subtext);

    private final Function<String, String> conversationNameJoinConversationAlert = name -> String.format("//android.widget.TextView[@text='Join conversation?']/..//android.widget.TextView[contains(@text,'%s')]", name);

    public CommonAppPage(WebDriver driver) {
        super(driver);
    }

    public void enableWifi() {
        AndroidPage.executeShell(getDriver(), "svc wifi enable");
    }

    public void disableWifi() {
        AndroidPage.executeShell(getDriver(), "svc wifi disable");
    }

    public void waitUntilWifiIsEnabled() throws Exception {
        final int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++){
            String status = (String) AndroidPage.executeShell(getDriver(), "dumpsys wifi | grep \"Wi-Fi is\"");
            log.info(status);
            if (status.contains("enabled")) {
                break;
            } else if (i == (maxRetries - 1)) {
                throw new Exception("Enabling Wifi was not successful");
            }
        }
    }

    public void clearCache() {
        String id = (String) AndroidPage.executeShell(getDriver(), "getprop ro.boot.serialno");
        String trimmedId = id.replace("\n", "");
        if (trimmedId.equals("ce091829205f7a3704")) {
            AndroidPage.executeShell(getDriver(), "pm clear org.lineageos.jelly");
        } else if (trimmedId.equals("25181JEGR05249")) {
            AndroidPage.executeShell(getDriver(), "pm clear app.vanadium.browser");
        } else {
            AndroidPage.executeShell(getDriver(), "pm clear com.android.chrome");
        }
    }

    public boolean isAppInForeground(String packageId) {
        return isAppInForeground(packageId, Timedelta.ofSeconds(5));
    }

    private boolean isAppInForeground(String packageId, Timedelta timeout) {
        return isAppInForeground(getDriver(), packageId, timeout);
    }

    public boolean isAppInForeground(AndroidDriver driver, String packageId, Timedelta timeout) {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
                .withTimeout(Duration.ofSeconds(5))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class);
        try {
            return wait.until(drv -> {
                boolean isAppInForeground = getDriver().queryAppState(packageId) == ApplicationState.RUNNING_IN_FOREGROUND;
                if (isAppInForeground) {
                    return true;
                } else {
                    log.info("App is not in foreground. Current app is " + getDriver().getCurrentPackage());
                }
                return false;
            });
        } catch (TimeoutException e) {
            return false;
        }
    }

    public boolean isAppInBackground(String packageId) {
        return isAppInBackground(getDriver(), packageId,  Timedelta.ofSeconds(5));
    }

    public boolean isAppInBackground(AndroidDriver driver, String packageId,  Timedelta timeout) {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
                .withTimeout(Duration.ofSeconds(5))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class);
        try {
            return wait.until(drv -> {
                boolean isAppNotInForeground = getDriver().queryAppState(packageId) == ApplicationState.RUNNING_IN_BACKGROUND;
                if (isAppNotInForeground) {
                    return true;
                } else {
                    log.info("App is still in foreground");
                }
                return false;
            });
        } catch (TimeoutException e) {
            return false;
        }
    }

    public void swipeAppAwayFromBackground() {
        getDriver().pressKey(new KeyEvent(AndroidKey.APP_SWITCH));
        final int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            scroll(-0, -1);
            if (!isAppInBackground(PackageNameHolder.getPackageName())) {
                break;
            }
        }
        getDriver().pressKey(new KeyEvent(AndroidKey.BACK));
    }

    public boolean isAccountUsedOnAnotherDeviceAlertVisible() {
        return accountUsedAlertHeading.isDisplayed();
    }

    public boolean isSecondAccountUsedOnAnotherDeviceAlertVisible(String userName) {
        return waitUntilElementVisible(getDriver().findElement(secondAccountUsedAlertHeading.apply(userName)));
    }

    public boolean isTextUsedDeviceAlertVisible(String text) {
        return getDriver().findElement(By.xpath(subTextAddedDeviceAlert.apply(text))).isDisplayed();
    }

    public boolean isTextTeamSettingsChangedAlertVisible(String text) {
        return getDriver().findElement(By.xpath(subTextTeamSettingChangedAlert.apply(text))).isDisplayed();
    }

    public boolean isAlertTeamSettingsChangedVisible() {
        return waitUntilElementVisible(headingTeamSettingsChange);
    }

    public boolean isWireEnterpriseAlertVisible() {
        return waitUntilElementVisible(enterpriseAlertHeading);
    }

    public boolean isTextEnterpriseAlertVisible(String text) {
        return getDriver().findElement(By.xpath(String.format(subtextEnterpriseAlert.apply(text)))).isDisplayed();
    }

    public void tapLearnMoreLinkEnterpriseAlert() {
        learnMoreLinkEnterpriseAlert.click();
    }

    public void tapUpgradeButtonEnterpriseAlert() {
        waitUntilElementVisible(upgradeNowButton);
        upgradeNowButton.click();
    }

    public void tapManageDevicesButton() {
        manageDevicesButton.isDisplayed();
        manageDevicesButton.click();
    }

    public void tapOKButtonDevicesAlert() {
        okButton.isDisplayed();
        okButton.click();
    }

    public void tapSwitchAccountButton() {
        switchAccountButton.isDisplayed();
        switchAccountButton.click();
    }

    public boolean isDeviceRemovedAlertVisible() {
        return deviceRemovedAlertHeading.isDisplayed();
    }

    public boolean isTextRemoveDeviceAlertVisible(String text) {
        return getDriver().findElement(By.xpath(subTextRemoveDeviceAlert.apply(text))).isDisplayed();
    }

    public boolean isDeletedAccountAlertVisible() {
        return deletedAccountAlertHeading.isDisplayed();
    }

    public boolean isTextDeletedAccountVisible(String text) {
        return getDriver().findElement(By.xpath(subTextDeletedAccountAlert.apply(text))).isDisplayed();
    }

    public void tapOkButton() {
        okButton.isDisplayed();
        okButton.click();
    }

    public void tapCancelButton() {
        cancelButton.isDisplayed();
        cancelButton.click();
    }

    public void closeWebPage() {
        closeButton.isDisplayed();
        closeButton.click();
    }

    public boolean isSomethingWentWrongAlertVisible() {
        return somethingWentWrongHeading.isDisplayed();
    }

    public boolean isJoinConversationAlertVisible() {
        return waitUntilElementVisible(joinConversationAlertHeading);
    }

    public boolean isCanNotJoinConversationAlertVisible() {
        return waitUntilElementVisible(canNotJoinConversationAlertHeading);
    }

    public boolean isTextJoinConversationAlertVisible(String text) {
        return getDriver().findElement(By.xpath(subtextJoinConversationAlert.apply(text))).isDisplayed();
    }

    public boolean isTextCanNotJoinConversationAlertVisible(String text) {
        return getDriver().findElement(By.xpath(subtextCanNotJoinConversationAlert.apply(text))).isDisplayed();
    }

    public boolean isGroupNameDisplayedInJoinConversationAlert(String groupName) {
        return getDriver().findElement(By.xpath(conversationNameJoinConversationAlert.apply(groupName))).isDisplayed();
    }

    public void enterPasswordJoinConversation(String password) {
        passwordFieldJoinConversation.sendKeys(password);
    }

    public void tapJoinConversationButton() {
        joinConversationButton.click();
    }

    public void tapCancelJoiningConversationButton() {
        cancelJoiningConversationButton.click();
    }

    public boolean isInvalidPasswordJoinConversationErrorVisible() {
        return invalidPasswordJoinConversationError.isDisplayed();
    }
}
