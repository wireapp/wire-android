package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ScalaAppPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@text='staging']")
    private WebElement stagingLink;

    @AndroidFindBy(xpath = "//*[@text='CONNECT' or @text='connect' or @text='Connect']")
    private WebElement acceptButtonCustomBEAlert;

    @AndroidFindBy(xpath = "//*[contains(@resource-id,'welcomeLoginButton')]")
    private WebElement loginButton;

    @AndroidFindBy(id = "customBackendEmailLoginButton")
    private WebElement emailLoginButton;

    @AndroidFindBy(xpath = "//*[contains(@resource-id,'get__sign_in__email')]")
    private WebElement idLoginInput;

    @AndroidFindBy(xpath = "//*[contains(@resource-id,'get__sign_in__password')]")
    private WebElement idPasswordInput;

    @AndroidFindBy(xpath = "//*[contains(@resource-id,'pcb__signin__email')]")
    public static WebElement idLoginButton;

    @AndroidFindBy(xpath = "//*[contains(@resource-id,'zb__first_launch__confirm')]")
    private WebElement okButton;

    @AndroidFindBy(xpath = "//*[contains(@text, 'first time')]")
    private WebElement overlayTitle;

    @AndroidFindBy(xpath = "//*[contains(@resource-id,'conversation_list_title')]")
    private WebElement conversationsListTitle;

    @AndroidFindBy(id = "alertTitle")
    private WebElement alertTitle;

    @AndroidFindBy(id = "android:id/button1")
    private WebElement iAgreeButton;

    @AndroidFindBy(xpath = "//*[contains(@resource-id,'image_container')]")
    private WebElement messageImage;

    @AndroidFindBy(id = "action_button")
    private WebElement audioPlayButton;

    @AndroidFindBy(xpath = "//*[contains(@resource-id,'cet__cursor')]")
    private WebElement idCursorEditText;

    @AndroidFindBy(xpath = "//*[contains(@resource-id,'cib__send')]")
    private WebElement idCursorSendButton;

    @AndroidFindBy(xpath = "//*[contains(@resource-id,'t_conversation_toolbar')]")
    private WebElement xpathStrConversationToolbar;

    @AndroidFindBy(xpath = "//*[contains(@resource-id,'gtv__participants__right__action')]")
    private WebElement menuButton;

    @AndroidFindBy(xpath = "//*[contains(@text,'Leave group…')]")
    private WebElement leaveGroupButton;

    @AndroidFindBy(xpath = "//*[contains(@text,'LEAVE GROUP')]")
    private WebElement leaveGroupButtonOverlay;

    @AndroidFindBy(xpath = "//*[contains(@text,'Clear content…')]")
    private WebElement clearContentButton;

    @AndroidFindBy(xpath = "//*[contains(@text,'CLEAR CONTENT')]")
    private WebElement clearContentButtonOverlay;

    private static final Function<String, By> byStrCustomBackendPillByName = backendName ->
            By.xpath(String.format("//*[contains(@resource-id,'customBackendWelcomeTextView') and contains(@text,'%s')]", backendName.toLowerCase()));

    private final By innerInputField = By.xpath("(//android.widget.EditText)[last()]");

    private final Function<String, By> xpathConversationName = name -> By.xpath(String.format("//*[contains(@text,'%s')]", name));

    private static final Function<String, By> byStrConversationMessageByText = text -> By.xpath(String.format("//*[contains(@resource-id,'text') and @text='%s']", text));

    private static final Function<String, By> byStrConversationSystemMessageByText = text -> By.xpath(String.format("//*[contains(@resource-id,'ttv__system_message__text') and @text='%s']", text));

    private static final Function<String, By> uiStatusPillText = text -> By.xpath(String.format("//*[contains(@resource-id,'status_pill_text') and @text='%s']", text.toUpperCase()));

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[@text='%s']", text));

    public ScalaAppPage(WebDriver driver) {
        super(driver);
    }

    protected WebElement retrieveEditElement(WebElement parent) {
        final List<WebElement> dstEdits = parent.findElements(innerInputField);
        return dstEdits.isEmpty() ? parent : dstEdits.get(0);
    }

    public void openStagingBackend() {
        waitUntilElementVisible(stagingLink);
        stagingLink.click();
    }

    public void acceptCustomBackendAlert() {
        acceptButtonCustomBEAlert.click();
    }

    public boolean isCustomBackendPillVisibleForBackend(String customBE) {
        return getDriver().findElement(byStrCustomBackendPillByName.apply(customBE)).isDisplayed();
    }

    public boolean isLoginButtonInvisible() {
        return waitUntilElementInvisible(loginButton);
    }

    public void tapLogin() {
        waitUntilElementVisible(loginButton, Timedelta.ofSeconds(15).asSeconds());
        loginButton.click();
        if  (!isLoginButtonInvisible()) {
            loginButton.click();
        }
    }

    public void tapOnLoginWithEmailButton() {
        waitUntilElementVisible(emailLoginButton);
        emailLoginButton.click();
    }

    public void setLogin(String login) {
        waitUntilElementVisible(idLoginInput);
        idLoginInput.clear();
        idLoginInput.click();
        final WebElement loginInput = retrieveEditElement(idLoginInput);
        loginInput.sendKeys(login);
    }

    public void setPassword(String password) {
        waitUntilElementVisible(idPasswordInput);
        idPasswordInput.clear();
        idPasswordInput.click();
        final WebElement passwordInput = retrieveEditElement(idPasswordInput);
        passwordInput.sendKeys(password);
    }

    public void logIn() {
        waitUntilElementVisible(idLoginButton);
        // Element ID changes after typing the password. Due to this issue, locators are directly in method
        getDriver().findElement(By.id("pcb__signin__email")).click();
    }

    public void tapOkButton(Timedelta timeout) {
        waitUntilElementVisible(okButton, timeout.asSeconds());
        okButton.click();
    }

    public boolean isOverlayInvisible() {
        return waitUntilElementInvisible(overlayTitle);
    }

    public boolean waitUntilConversationListOrAlertLoadedSuccessfully() {
        return isElementPresentAndDisplayed(conversationsListTitle) || isElementPresentAndDisplayed(alertTitle);
    }

    public void waitUntilAlertVisible() {
        waitUntilElementVisible(alertTitle);
    }

    public void tapIAgree() {
        iAgreeButton.isDisplayed();
        iAgreeButton.click();
    }

    public boolean isConversationVisible(String name) {
        return getDriver().findElement(xpathConversationName.apply(name)).isDisplayed();
    }

    public boolean isConversationInvisible(String name) {
        return isLocatorInvisible(xpathConversationName.apply(name));
    }

    public void tapListItem(String name) {
        getDriver().findElement(xpathConversationName.apply(name)).isDisplayed();
        getDriver().findElement(xpathConversationName.apply(name)).click();
    }

    public boolean waitUntilMessageWithTextVisible(String text) {
        return getDriver().findElement(byStrConversationMessageByText.apply(text)).isDisplayed();
    }

    public boolean waitUntilSystemMessageWithTextVisible(String text) {
        return getDriver().findElement(byStrConversationSystemMessageByText.apply(text)).isDisplayed();
    }

    public boolean isImageDisplayed() {
        return messageImage.isDisplayed();
    }

    public boolean isFileDisplayed(String fileName) {
        return getDriver().findElement(textBy.apply(fileName)).isDisplayed();
    }

    public boolean isAudioPlayButtonVisible()  {
        return audioPlayButton.isDisplayed();
    }

    public void tapOnTextInput() {
        idCursorEditText.click();
    }

    public void typeAndSendMessage(String message) {
        final WebElement cursorInput = retrieveEditElement(idCursorEditText);
        cursorInput.click();
        cursorInput.clear();
        cursorInput.sendKeys(message);
        this.hideKeyboard();
        idCursorSendButton.click();
    }

    public boolean hasStatusPillXNewMessagesIndicator(String amount){
        return getDriver().findElement(uiStatusPillText.apply(amount)).isDisplayed();
    }

    public void tapTopToolbarTitle() {
        xpathStrConversationToolbar.isDisplayed();
        xpathStrConversationToolbar.click();
        // Wait for animation
        Timedelta.ofSeconds(1).sleep();
    }

    public void tapMenuButton() {
        menuButton.isDisplayed();
        menuButton.click();
    }

    public void tapOnLeaveGroupButton() {
        leaveGroupButton.isDisplayed();
        leaveGroupButton.click();
    }

    public void tapOnClearContentButton() {
        clearContentButton.isDisplayed();
        clearContentButton.click();
    }

    public void tapOnLeaveGroupButtonOverlay() {
        leaveGroupButtonOverlay.isDisplayed();
        leaveGroupButtonOverlay.click();
    }

    public void tapOnClearContentButtonOverlay() {
        clearContentButtonOverlay.isDisplayed();
        clearContentButtonOverlay.click();
    }
}
