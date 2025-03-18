package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.function.Function;

public class RegistrationPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@text='Create a Personal Account']")
    private WebElement createPersonalAccountPageHeading;

    @AndroidFindBy(xpath = "//*[@text='Your Username']")
    private WebElement userNamePageHeading;

    @AndroidFindBy(xpath = "//*[@text='Create a Personal Account']")
    private WebElement createAPersonalAccountSuccessPageHeading;

    @AndroidFindBy(xpath = "//*[contains(@text,'EMAIL')]/..//*[@class='android.widget.EditText']")
    private WebElement emailInputField;

    @AndroidFindBy(xpath = "//*[contains(@text,'USERNAME')]/..//*[@class='android.widget.EditText']")
    private WebElement userNameInputField;

    @AndroidFindBy(xpath = "//*[@text='At least 2 characters, a-z, 0-9, “_”, “-” and “.”']")
    private WebElement userNameHelpText;

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

    @AndroidFindBy(xpath = "//*[@text='Confirm']")
    private WebElement confirmButton;

    @AndroidFindBy(xpath = "//*[@text='Continue']")
    private WebElement continueButton;

    @AndroidFindBy(xpath = "//*[@text='Get Started']")
    private WebElement getStartedButton;

    private final Function<String, String> textString = text -> String.format("//*[@text='%s']", text);

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[@text='%s']", text));

    public RegistrationPage(WebDriver driver) {
        super(driver);
    }

    public boolean isCreateAPersonalAccountHeadingVisible() {
        return createPersonalAccountPageHeading.isDisplayed();
    }

    public boolean isUserNamePageHeadingVisible() {
        return waitUntilElementVisible(userNamePageHeading, Timedelta.ofSeconds(15).asSeconds());
    }

    public boolean isUserNameHelpTextVisible() {
        return userNameHelpText.isDisplayed();
    }

    public boolean isCreateAPersonalAccountSuccessPageVisible() {
        return createAPersonalAccountSuccessPageHeading.isDisplayed();
    }

    public boolean isTextVisible(String text) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(textString.apply(text))));
    }

    public void enterEmailCredentials(String email) {
        emailInputField.isDisplayed();
        emailInputField.sendKeys(email);
    }

    public void enterUserName(String userName) {
        waitUntilElementVisible(userNameInputField, Timedelta.ofSeconds(15).asSeconds());
        userNameInputField.sendKeys(userName);
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

    public void enterPasswordCredentials(String password) {
        waitUntilElementVisible(passwordHeading);
        passwordInputField.isDisplayed();
        passwordInputField.click();
        passwordInputField.sendKeys(password);
    }

    public void enterConfirmPasswordCredentials(String password) {
        waitUntilElementVisible(confirmPasswordHeading);
        confirmPasswordInputField.isDisplayed();
        confirmPasswordInputField.click();
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

    public void tapConfirm() {
        confirmButton.isDisplayed();
        confirmButton.click();
    }

    public void tapContinue() {
        continueButton.isDisplayed();
        continueButton.click();
    }

    public void tapGetStarted() {
        getStartedButton.isDisplayed();
        getStartedButton.click();
    }

    public void seeGetStartedButton() {
        waitUntilElementClickable(getStartedButton);
    }

    public boolean isVerifyCodePageVisible() {
        return waitUntilElementVisible(resendCodeLink);
    }

    public void inputVerificationCode(String code) {
        codeInputField.isDisplayed();
        codeInputField.sendKeys(code);
    }

}
