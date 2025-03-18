package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.function.Function;

public class LoginPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@text='Login']")
    private WebElement loginPageHeading;

    @AndroidFindBy(xpath = "//*[@text='Forgot password?']")
    private WebElement forgotPasswordLink;

    @AndroidFindBy(xpath = "//*[@text='EMAIL']")
    private WebElement emailTab;

    @AndroidFindBy(xpath = "//*[@text='SSO LOGIN']")
    private WebElement SSOLoginTab;

    @AndroidFindBy(xpath = "//*[contains(@text,'EMAIL OR USERNAME')]/..//*[@class='android.widget.EditText']")
    private WebElement emailInputField;

    @AndroidFindBy(xpath = "//*[contains(@text,'PASSWORD')]/..//*[@class='android.widget.EditText']")
    private WebElement passwordInputField;

    @AndroidFindBy(xpath = "//*[@content-desc='Show password']")
    private WebElement showPasswordEyeIcon;

    @AndroidFindBy(xpath = "//*[@content-desc='Hide password']")
    private WebElement hidePasswordEyeIcon;

    @AndroidFindBy(xpath = "//*[@text='PROXY EMAIL OR USERNAME']/..//*[@class='android.widget.EditText']")
    private WebElement proxyUsernameInputField;

    @AndroidFindBy(xpath = "//*[@text='PROXY PASSWORD']/..//*[@class='android.widget.EditText']")
    private WebElement proxyPasswordInputField;

    @AndroidFindBy(xpath = "//*[@class='android.widget.Button']/..//*[@text='Login']")
    private WebElement loginButton;

    @AndroidFindBy(xpath = "//*[@text='Logging in…']")
    private WebElement loginButtonLoadingState;

    //TODO: ask for better locator for alert and alertText or add it
    @AndroidFindBy(xpath = "//*[@text='Invalid information']")
    private WebElement invalidInformationAlert;

    @AndroidFindBy(xpath = "//*[contains(@text,'These account credentials')]")
    private WebElement infoTextWrongCredentials;

    @AndroidFindBy(xpath = "//*[@text='OK']")
    private WebElement okButton;

    @AndroidFindBy(xpath = "//*[@text='Resend code']")
    private WebElement resendCodeLink;

    @AndroidFindBy(xpath = "//*[@text='Resend code']/..//*[@class='android.widget.EditText']")
    private WebElement codeInputField;

    private final Function<String, String> textString = text -> String.format("//*[@text='%s']", text);

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[@text='%s']", text));

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public boolean isTextVisible(String text) {
        return getDriver().findElement(By.xpath(textString.apply(text))).isDisplayed();
    }

    public boolean isLoginPageVisible() {
        return loginPageHeading.isDisplayed();
    }

    public void tapForgotPasswordLink() {
        forgotPasswordLink.isDisplayed();
        forgotPasswordLink.click();
    }

    public boolean isEmailLoginTabVisible() {
        return emailTab.isDisplayed();
    }

    public boolean isSSOLoginTabVisible() {
        return SSOLoginTab.isDisplayed();
    }

    public void tapEmailLoginTab() {
        emailTab.click();
    }

    public void tapSSOLoginTab() {
        waitUntilElementClickable(SSOLoginTab);
        SSOLoginTab.click();
    }

    public void clearEmailCredentials() {
        emailInputField.clear();
    }

    public void enterEmailCredentials(String email) {
        emailInputField.isDisplayed();
        emailInputField.click();
        emailInputField.sendKeys(email);
    }

    public void clearPasswordCredentials() {
        passwordInputField.clear();
    }

    public void enterPassword(String password) {
        passwordInputField.isDisplayed();
        passwordInputField.click();
        passwordInputField.sendKeys(password);
    }

    public void tapShowPasswordIcon() {
        showPasswordEyeIcon.isDisplayed();
        showPasswordEyeIcon.click();
    }

    public void tapHidePasswordIcon() {
        hidePasswordEyeIcon.isDisplayed();
        hidePasswordEyeIcon.click();
    }

    public String getTextPasswordInputField() {
        return passwordInputField.getText();
    }

    public void enterProxyUsername(String username) {
        proxyUsernameInputField.isDisplayed();
        proxyUsernameInputField.click();
        proxyUsernameInputField.sendKeys(username);
    }

    public void enterProxyPassword(String password) {
        proxyPasswordInputField.isDisplayed();
        proxyPasswordInputField.click();
        proxyPasswordInputField.sendKeys(password);
    }

    public void tapLoginButton() {
        loginButton.isDisplayed();
        loginButton.click();
    }

    public void waitUntilLoginButtonIsInvisible() {
        waitUntilElementInvisible(loginButton, Duration.ofSeconds(10));
    }

    public void waitUntilLoginButtonIsInvisibleAfterTap() {
        waitUntilElementInvisible(loginButtonLoadingState, Duration.ofSeconds(10));
    }

    public boolean isInvalidInformationAlertVisible() {
        return invalidInformationAlert.isDisplayed();
    }

    public String getTextAlert() {
        return infoTextWrongCredentials.getText();
    }

    public void tapOKButton() {
        okButton.isDisplayed();
        okButton.click();
    }

    public boolean isVerifyCodePageVisible() {
        return waitUntilElementVisible(resendCodeLink);
    }

    public void inputVerificationCode(String code) {
        codeInputField.isDisplayed();
        codeInputField.sendKeys(code);
    }
}
