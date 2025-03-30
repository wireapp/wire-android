package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.function.Function;

public class SettingsPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@content-desc='Main navigation']")
    private WebElement menuButton;

    @AndroidFindBy(xpath = "//*[@text='Conversations']")
    private WebElement conversationsMenu;

    @AndroidFindBy(xpath = "//*[@text='Archive']")
    private WebElement archiveMenu;

    @AndroidFindBy(xpath = "//*[@text='Manage your Devices']")
    private WebElement devicesMenu;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Settings']")
    private WebElement settingsMenu;

    @AndroidFindBy(xpath = "//*[@content-desc='Support']")
    private WebElement supportMenu;

    @AndroidFindBy(uiAutomator = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"Report Bug\"))")
    private WebElement reportBugMenu;

    // different devices have different resource-ids for the app drawer. Therefore, we have 2 locators, appDrawer and appDrawer2
    @AndroidFindBy(xpath = "//android.widget.TabHost[@resource-id='android:id/profile_tabhost']")
    private WebElement appDrawer;

    @AndroidFindBy(xpath = "//*[@resource-id='android:id/resolver_list']")
    private WebElement appDrawer2;

    @AndroidFindBy(xpath = "//*[@content-desc='Back button']")
    private WebElement closeButtonSettings;

    @AndroidFindBy(xpath = "//*[@text='Account Details']")
    private WebElement accountDetailsMenu;

    @AndroidFindBy(xpath = "//*[@text='Your profile name']")
    private WebElement headingProfileNamePage;

    @AndroidFindBy(xpath = "//*[@class='android.widget.EditText']")
    private WebElement editBoxProfileName;

    @AndroidFindBy(xpath = "//*[@text='Save']")
    private WebElement saveButton;

    @AndroidFindBy(xpath = "//*[@text='Reset Password']")
    private WebElement resetPasswordButton;

    @AndroidFindBy(xpath = "//*[@text='Delete Account']")
    private WebElement deleteAccountButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Continue']")
    private WebElement continueDeleteAccountButton;

    @AndroidFindBy(xpath = "//*[@text='Privacy Settings']")
    private WebElement privacySettings;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Send anonymous usage data']/following-sibling::*[@text='ON']")
    private WebElement anonymousUsageDataSwitchOn;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Send anonymous usage data']/following-sibling::*[@text='OFF']")
    private WebElement anonymousUsageDataSwitchOff;

    @AndroidFindBy(xpath = "//*[@text='Send read receipts']/following-sibling::*[@text='ON']")
    private WebElement readReceiptsSwitchOn;

    @AndroidFindBy(xpath = "//*[@text='Send read receipts']/following-sibling::*[@text='OFF']")
    private WebElement readReceiptsSwitchOff;

    @AndroidFindBy(xpath = "//*[@text='Send read receipts']/following-sibling::*[@class='android.view.View']")
    private WebElement readReceiptsToggle;

    @AndroidFindBy(xpath = "//*[@text='Network Settings']")
    private WebElement networkSettings;

    @AndroidFindBy(xpath = "//*[contains(@text,'Websocket')]/following-sibling::*[@class='android.view.View']")
    private WebElement websocketSwitch;

    @AndroidFindBy(xpath = "//*[@text='Keep Connection to Websocket']/following-sibling::*[@text='ON']")
    private WebElement websocketSwitchOnByDefault;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Lock with passcode']/following-sibling::*[@class='android.view.View']")
    private WebElement appLockToggle;

    @AndroidFindBy(xpath = "//android.widget.EditText")
    private WebElement emailInputField;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Lock with passcode']/following-sibling::*[@text='OFF']")
    private WebElement appLockToggleOff;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Lock with passcode']/following-sibling::*[@text='ON']")
    private WebElement appLockToggleOn;

    @AndroidFindBy(xpath = "//*[@content-desc='Support']")
    private WebElement supportInSettings;

    @AndroidFindBy(xpath = "//*[@text='Debug Settings']")
    private WebElement debugMenu;

    @AndroidFindBy(xpath = "//android.widget.ScrollView/android.view.View[1]")
    private WebElement loggingToggle;

    @AndroidFindBy(xpath = "//android.view.View[@clickable='true' and @checked='true']")
    private WebElement loggingToggleOn;

    @AndroidFindBy(xpath = "//android.view.View[@clickable='true' and @checked='false']")
    private WebElement loggingToggleOff;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Analytics Tracking Identifier']/following-sibling::*[@class='android.widget.TextView']")
    private WebElement analyticsTrackingIdentifier;

    @AndroidFindBy(xpath = "//*[@text='Back up & Restore Conversations']")
    private WebElement backupMenu;

    @AndroidFindBy(xpath = "//*[@text='Back up & Restore Conversations']")
    private WebElement backupPageHeading;

    @AndroidFindBy(xpath = "//*[@text='PASSWORD (OPTIONAL)']/..//*[@class='android.widget.EditText']")
    private WebElement passwordBackupCreate;

    @AndroidFindBy(xpath = "//*[@text='PASSWORD']/..//*[@class='android.widget.EditText']")
    private WebElement passwordBackupRestore;

    @AndroidFindBy(xpath = "//*[@text='Create a Backup']")
    private WebElement createBackupButton;

    @AndroidFindBy(xpath = "//*[@text='Restore from Backup']")
    private WebElement restoreBackupButton;

    @AndroidFindBy(xpath = "//*[@text='Choose Backup File']")
    private WebElement chooseBackupButton;

    @AndroidFindBy(xpath = "//*[@text='Back Up Now']")
    private WebElement backUpNowButton;

    @AndroidFindBy(xpath = "//*[@text='Save File']")
    private WebElement saveFileButton;

    @AndroidFindBy(xpath = "//*[@text='SAVE']")
    private WebElement saveButtonOSMenu;

    @AndroidFindBy(xpath = "//*[@text='Continue']")
    private WebElement continueButtonBackup;

    @AndroidFindBy(uiAutomator = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"Key-packages\"))")
    private WebElement keyPackagesEntry;

    @AndroidFindBy(xpath = "//*[contains(@text,'Device id')]")
    private WebElement deviceIDEntry;

    @AndroidFindBy(xpath = "//android.widget.TextView[contains(@text,'ID:')]")
    private WebElement clientID;

    private final Function<String, By> profileNameEntry = name -> By.xpath(String.format("//android.widget.TextView[@text='PROFILE NAME']/following-sibling::*[@text='%s']", name));

    private final Function<String, By> userNameEntry = name -> By.xpath(String.format("//android.widget.TextView[@text='USERNAME']/following-sibling::*[@text='%s']", name));

    private final Function<String, By> emailEntry = email -> By.xpath(String.format("//*[@text='EMAIL']/following-sibling::*[@text='%s']", email));

    private final Function<String, By> teamNameEntry = name -> By.xpath(String.format("//*[@text='TEAM']/following-sibling::*[@text='%s']", name));

    private final Function<String, By> domainEntry = domain -> By.xpath(String.format("//*[@text='DOMAIN']/following-sibling::*[@text='%s']", domain));

    private final Function<String, String> switchState = state -> String.format("//*[@text='%s']/following-sibling::*[@class='android.view.View']", state);

    private final Function<String, By> otherDevices = deviceName -> By.xpath(String.format("//android.view.View[@content-desc='Device item']/following-sibling::*[contains(@text,'%s')]", deviceName));

    private final Function<String, String> keyPackagesCount = count -> String.format("//*[@text='Key-packages count']/following-sibling::*[@text='%s']", count);

    private final Function<String, String> analyticsInitializedState = state -> String.format("//android.widget.TextView[@text='Analytics Initialized']/following-sibling::*[@text='%s']", state);

    private final Function<String, String> textString = text -> String.format("//*[@text='%s']", text);

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[@text='%s']", text));

    private final Function<String, By> newEmailNotification = email -> By.xpath(String.format("//android.widget.TextView[contains(@text, 'verification email') and contains(@text, '%s')]", email));

    public SettingsPage(WebDriver driver) {
        super(driver);
    }

    public boolean isTextDisplayed(String text) {
        return getDriver().findElement(By.xpath(textString.apply(text))).isDisplayed();
    }

    public void waitUntilTextDisplayed(String text) {
        waitUntilLocatorIsDisplayed(textBy.apply(text), Duration.ofSeconds(5));
    }

    public void tapMainMenu() {
        menuButton.isDisplayed();
        menuButton.click();
    }

    public void openConversations() {
        conversationsMenu.isDisplayed();
        conversationsMenu.click();
    }

    public void openArchive() {
        archiveMenu.isDisplayed();
        archiveMenu.click();
    }

    public void tapManageDevicesMenu() {
        devicesMenu.isDisplayed();
        devicesMenu.click();
    }

    public void openSettings() {
        settingsMenu.isDisplayed();
        settingsMenu.click();
    }

    public void closeSettings() {
        closeButtonSettings.isDisplayed();
        closeButtonSettings.click();
    }

    public void openSupport() {
        supportMenu.isDisplayed();
        supportMenu.click();
    }

    public void openReportBugMenu() {
        reportBugMenu.isDisplayed();
        reportBugMenu.click();
    }

    public boolean isAppDrawerVisible() {
        return isElementPresentAndDisplayed(appDrawer) || isElementPresentAndDisplayed(appDrawer2);
    }

    // Account Details

    public void changeEmailAddress(String newEmail) {
        emailInputField.clear();
        emailInputField.sendKeys(newEmail);
    }

    public boolean isNewEmailDisplayed(String email) {
        final By locator = newEmailNotification.apply(email);
        return waitUntilLocatorIsDisplayed(locator, Duration.ofSeconds(10));
    }

    public void tapAccountDetailsMenu() {
        waitUntilElementClickable(accountDetailsMenu);
        accountDetailsMenu.click();
    }

    public boolean isProfileNameVisible(String name) {
        final By locator = profileNameEntry.apply(name);
        return waitUntilLocatorIsDisplayed(locator, Duration.ofSeconds(2));
    }

    public void tapProfileName(String name) {
        waitUntilElementVisible(getDriver().findElement(profileNameEntry.apply(name)));
        getDriver().findElement(profileNameEntry.apply(name)).click();
    }

    public boolean isProfileNameHeadingVisible() {
        return waitUntilElementVisible(headingProfileNamePage);
    }

    public boolean isProfileNameHeadingInvisible() {
        return isElementInvisible(headingProfileNamePage, Timedelta.ofSeconds(2));
    }

    public boolean isEditBoxProfileNameVisible() {
        return waitUntilElementVisible(editBoxProfileName);
    }

    public boolean isEditBoxProfileNameInvisible() {
        return isElementInvisible(editBoxProfileName, Timedelta.ofSeconds(1));
    }

    public void editProfileName(String newName) {
        editBoxProfileName.clear();
        editBoxProfileName.sendKeys(newName);
    }

    public void tapSaveButton() {
        saveButton.isDisplayed();
        saveButton.click();
    }

    public boolean isUserNameVisible(String name){
        return getDriver().findElement(userNameEntry.apply(name)).isDisplayed();
    }

    public boolean isEmailVisible(String email){
        return getDriver().findElement(emailEntry.apply(email)).isDisplayed();
    }

    public boolean isTeamNameVisible(String teamName){
        return getDriver().findElement(teamNameEntry.apply(teamName)).isDisplayed();
    }

    public boolean isDomainVisible(String domain){
        return getDriver().findElement(domainEntry.apply(domain)).isDisplayed();
    }

    public boolean isResetPasswordVisible() {
        return resetPasswordButton.isDisplayed();
    }

    public boolean isDeleteAccountVisible() {
        return deleteAccountButton.isDisplayed();
    }

    public boolean isDeleteAccountAlertInVisible() {
        return waitUntilElementInvisible(continueDeleteAccountButton);
    }

    public boolean isResetPasswordInvisible() {
        return waitUntilElementInvisible(resetPasswordButton);
    }

    public void tapResetPasswordButton() {
        resetPasswordButton.isDisplayed();
        resetPasswordButton.click();
    }

    public void tapDeleteAccountButton() {
        deleteAccountButton.isDisplayed();
        deleteAccountButton.click();
    }

    public boolean isDeleteAccountAlertDisplayed() {
        return waitUntilElementVisible(deleteAccountButton);
    }

    public void tapContinueDeleteAccountButton() {
        continueDeleteAccountButton.isDisplayed();
        continueDeleteAccountButton.click();
    }

    public void tapUserEmail(String email) {
        getDriver().findElement(emailEntry.apply(email)).click();
    }

    // Privacy Settings

    public void tapPrivacySettingsButton() {
        privacySettings.isDisplayed();
        privacySettings.click();
    }

    public boolean areReadReceiptsEnabled() {
        return readReceiptsSwitchOn.isDisplayed();
    }

    public boolean areReadReceiptsDisabled() {
        return readReceiptsSwitchOff.isDisplayed();
    }

    public void tapReadReceiptsToggle() {
        readReceiptsToggle.isDisplayed();
        readReceiptsToggle.click();
    }

    public boolean isSendAnonymousUsageDataSwitchEnabled() {
        return waitUntilElementVisible(anonymousUsageDataSwitchOn);
    }

    public boolean isSendAnonymousUsageDataSwitchDisabled() {
        return waitUntilElementVisible(anonymousUsageDataSwitchOff);
    }

    // Network Settings

    public void tapNetworkSettingsButton() {
        networkSettings.isDisplayed();
        networkSettings.click();
    }

    public void tapWebsocketConnectionButton() {
        websocketSwitch.isDisplayed();
        websocketSwitch.click();
    }

    public boolean isSwitchStateVisible(String state) {
        return getDriver().findElement(By.xpath(switchState.apply(state))).isDisplayed();
    }

    public boolean isWebSocketSwitchInvisible() {
        return isElementInvisible(websocketSwitch, Timedelta.ofSeconds(1));
    }

    // Lock with passcode

    public void iTapLockWithPasscodeToggle() {
        waitUntilElementVisible(appLockToggle);
        appLockToggle.click();
    }

    public boolean isLockWithPasscodeDisabled() {
        return appLockToggleOff.isDisplayed();
    }

    public boolean isLockWithPasscodeEnabled() {
        return appLockToggleOn.isDisplayed();
    }

    public boolean isPasscodeToggleInvisible() {
        return isElementInvisible(appLockToggle, Timedelta.ofSeconds(1));
    }

    // Support in Settings (Not in main Menu)

    public void tapSupportInSettings() {
        supportInSettings.isDisplayed();
        supportInSettings.click();
    }

    // Backup

    public boolean isBackupPageVisible() {
        return backupPageHeading.isDisplayed();
    }

    public boolean iSeeBackupPageHeading() {
       return waitUntilElementClickable(backupPageHeading);
    }

    public void tapCreateBackupButton() {
        waitUntilElementClickable(createBackupButton);
        createBackupButton.click();
    }

    public void tapRestoreBackupButton() {
        waitUntilElementClickable(restoreBackupButton);
        restoreBackupButton.click();
    }

    public void tapChooseBackupFileButton() {
        waitUntilElementClickable(chooseBackupButton);
        chooseBackupButton.click();
    }

    public void typePasswordBackupCreate(String password) {
        waitUntilElementVisible(passwordBackupCreate);
        passwordBackupCreate.sendKeys(password);
    }

    public void typePasswordBackupRestore(String password) {
        waitUntilElementVisible(passwordBackupRestore);
        passwordBackupRestore.sendKeys(password);
    }

    public void tapBackupNowButton() {
        waitUntilElementClickable(backUpNowButton);
        backUpNowButton.click();
    }

    public void tapSaveFileButton() {
        saveFileButton.click();
    }

    public void tapSaveFileOSMenuButton() {
        saveButtonOSMenu.isDisplayed();
        saveButtonOSMenu.click();
    }

    public void tapContinueButtonRestoreBackup() {
        continueButtonBackup.click();
    }

    // Debug Menu

    public void tapDebugMenu() {
        debugMenu.isDisplayed();
        debugMenu.click();
    }

    public void tapLoggingToggle() {
        loggingToggle.isDisplayed();
        loggingToggle.click();
    }

    public boolean isEnableLoggingToggleOn() {
        return waitUntilElementVisible(loggingToggleOn);
    }

    public boolean isEnableLoggingToggleOff() {
        return waitUntilElementVisible(loggingToggleOff);
    }

    public boolean isAnalyticsInitialized(String state) {
        return getDriver().findElement(By.xpath(analyticsInitializedState.apply(state))).isDisplayed();
    }

    public String getTextAnalyticsTrackingIdentifier() {
        return analyticsTrackingIdentifier.getText();
    }

    public void tapBackupMenu() {
        waitUntilElementClickable(backupMenu);
        backupMenu.click();
    }

    public boolean isCorrectAmountKeyPackagesDisplayed(String count) {
        //scroll to keypackages entry
        keyPackagesEntry.isDisplayed();
        return getDriver().findElement(By.xpath(keyPackagesCount.apply(count))).isDisplayed();
    }

    public String getCurrentDeviceID() {
        return deviceIDEntry.getText();
    }

    public String getCurrentClientID() {
        return clientID.getText();
    }
}
