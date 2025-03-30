package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.function.Function;

public class TeamCreationPage extends AndroidPage {

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Go back to new team creation and login view\"]/../..//*[@text='Create a Team']")
    private WebElement createATeamPageHeading;

    @AndroidFindBy(xpath = "//*[@text='Team Created']")
    private WebElement createATeamSuccessPageHeading;

    @AndroidFindBy(xpath = "//*[contains(@text,'EMAIL')]/..//*[@class='android.widget.EditText']")
    private WebElement emailInputField;

    @AndroidFindBy(xpath = "//*[@text='Terms of Use']")
    private WebElement termsOfUseHeading;

    @AndroidFindBy(xpath = "//*[contains(@text,'Terms of Use and Privacy Policy')]")
    private WebElement infoTextToU;

    @AndroidFindBy(xpath = "//*[@text='Cancel']")
    private WebElement cancelButton;

    @AndroidFindBy(xpath = "//*[@text='View ToU and Privacy Policy']")
    private WebElement viewToUButton;

    @AndroidFindBy(xpath = "//*[contains(@text,'FIRST NAME')]/..//*[@class='android.widget.EditText']")
    private WebElement firstNameInputField;

    @AndroidFindBy(xpath = "//*[contains(@text,'LAST NAME')]/..//*[@class='android.widget.EditText']")
    private WebElement lastNameInputField;

    @AndroidFindBy(uiAutomator = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"TEAM NAME\"))")
    private WebElement teamNameHeading;

    @AndroidFindBy(xpath = "//*[contains(@text,'TEAM NAME')]/..//*[@class='android.widget.EditText']")
    private WebElement teamNameInputField;

    @AndroidFindBy(uiAutomator = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"PASSWORD\"))")
    private WebElement passwordHeading;

    @AndroidFindBy(xpath = "//*[contains(@text,'PASSWORD')]/..//*[@class='android.widget.EditText']")
    private WebElement passwordInputField;

    @AndroidFindBy(uiAutomator = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"CONFIRM PASSWORD\"))")
    private WebElement confirmPasswordHeading;

    @AndroidFindBy(xpath = "//*[contains(@text,'CONFIRM PASSWORD')]/..//*[@class='android.widget.EditText']")
    private WebElement confirmPasswordInputField;

    @AndroidFindBy(xpath = "//*[contains(@text,'Use at least 8 characters')]/..//*[@content-desc='Show password']")
    private WebElement showPasswordEyeIcon;

    @AndroidFindBy(xpath = "//*[contains(@text,'Use at least 8 characters')]/..//*[@content-desc='Hide password']")
    private WebElement hidePasswordEyeIcon;

    @AndroidFindBy(xpath = "//*[@text='Resend code']")
    private WebElement resendCodeLink;

    @AndroidFindBy(xpath = "//*[@text='Resend code']/..//*[@class='android.widget.EditText']")
    private WebElement codeInputField;

    @AndroidFindBy(uiAutomator = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"Continue\"))")
    private WebElement continueButton;

    @AndroidFindBy(xpath = "//*[@text='Login']")
    private WebElement loginLink;

    @AndroidFindBy(xpath = "//*[@text='Continue']")
    private WebElement continueButtonToUAlert;

    @AndroidFindBy(xpath = "//*[@text='Get Started']")
    private WebElement getStartedButton;

    private final Function<String, String> textString = text -> String.format("//*[@text='%s']", text);

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[@text='%s']", text));

    public TeamCreationPage(WebDriver driver) {
        super(driver);
    }

    public boolean isTextVisible(String text) {
        return getDriver().findElement(By.xpath(textString.apply(text))).isDisplayed();
    }

    public boolean isCreateATeamPageVisible() {
        return createATeamPageHeading.isDisplayed();
    }

    public boolean isCreateATeamSuccessPageVisible() {
        return waitUntilElementVisible(createATeamSuccessPageHeading, getDefaultLookupTimeoutSeconds() * 2L);
    }

    public void enterEmailCredentials(String email) {
        emailInputField.isDisplayed();
        emailInputField.sendKeys(email);
    }

    public void clearEmailInputField() {
        emailInputField.clear();
    }

    public boolean isToUAlertVisible() {
        return waitUntilElementVisible(termsOfUseHeading);
    }

    public boolean isToUInfoTextVisible() {
        return infoTextToU.isDisplayed();
    }

    public boolean isToUCancelButtonVisible() {
        return cancelButton.isDisplayed();
    }

    public boolean isToUViewButtonVisible() {
        return viewToUButton.isDisplayed();
    }

    public boolean isToUContinueButtonVisible() {
        return continueButton.isDisplayed();
    }

    public void enterFirstNameCredentials(String name) {
        firstNameInputField.isDisplayed();
        firstNameInputField.click();
        firstNameInputField.sendKeys(name);
    }

    public void enterLastNameCredentials(String name) {
        lastNameInputField.isDisplayed();
        lastNameInputField.click();
        lastNameInputField.sendKeys(name);
    }

    public void enterTeamNameCredentials(String name) {
        waitUntilElementVisible(teamNameHeading);
        teamNameInputField.click();
        teamNameInputField.sendKeys(name);
    }
    public void clearPasswordCredentials(){
        passwordInputField.clear();
    }

    public void enterPasswordCredentials(String password) {
        waitUntilElementVisible(passwordHeading);
        passwordInputField.sendKeys(password);
    }

    public void clearConfirmPasswordCredentials(){
        confirmPasswordInputField.clear();
    }

    public void enterConfirmPasswordCredentials(String password) {
        waitUntilElementVisible(confirmPasswordHeading);
        confirmPasswordInputField.sendKeys(password);
    }

    public void tapShowPasswordIcon() {
        showPasswordEyeIcon.isDisplayed();
        showPasswordEyeIcon.click();
    }

    public void tapHidePasswordIcon() {
        hidePasswordEyeIcon.isDisplayed();
        hidePasswordEyeIcon.click();
    }

    public void tapContinue() {
        continueButton.isDisplayed();
        continueButton.click();
    }

    public void tapLoginLink() {
        loginLink.isDisplayed();
        loginLink.click();
    }

    public void tapContinueOnToUAlert() {
        continueButtonToUAlert.isDisplayed();
        continueButtonToUAlert.click();
    }

    public void tapGetStarted() {
        getStartedButton.isDisplayed();
        getStartedButton.click();
    }

    public boolean isVerifyCodePageVisible() {
        return waitUntilElementVisible(resendCodeLink);
    }

    public void inputVerificationCode(String code) {
        codeInputField.isDisplayed();
        codeInputField.sendKeys(code);
    }

    public void tapResendCodeLink() {
        resendCodeLink.isDisplayed();
        resendCodeLink.click();
    }

    public void clearCodeInputField() {
        codeInputField.clear();
    }

    public void scrollToTheBottom() {
        this.hideKeyboard();
        for (int i = 0; i < 3; ++i) {
            scroll(0,-1);
        }
    }

    public void scrollToTheTop() {
        this.hideKeyboard();
        for (int i = 0; i < 3; ++i) {
            scroll(0,9);
        }
    }
}
