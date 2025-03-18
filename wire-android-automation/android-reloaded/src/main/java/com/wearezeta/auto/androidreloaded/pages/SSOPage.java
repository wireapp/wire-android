package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SSOPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[contains(@text,'SSO CODE')]/..//*[@class='android.widget.EditText']")
    private WebElement SSOInputField;

    @AndroidFindBy(xpath = "//*[contains(@text,'SSO CODE')]/..//*[@class='android.widget.TextView'][2]")
    private WebElement SSOErrorMessage;

    @AndroidFindBy(xpath = "//*[@resource-id='okta-sign-in']")
    private WebElement oktaSignInPage;

    @AndroidFindBy(xpath = "//*[@text='Sign In']")
    private WebElement oktaSignInPageSignInLocator;

    @AndroidFindBy(xpath = "//*[@resource-id='okta-signin-username']")
    private WebElement oktaUsernameInputField;

    @AndroidFindBy(xpath = "//*[@text='Sign In']/following-sibling::*[@class='android.widget.EditText'][1]")
    private WebElement oktaUsernameInputFieldSecondLocator;

    @AndroidFindBy(xpath = "(//*[@class='android.widget.EditText'])[1]")
    private WebElement oktaUsernameInputFieldTempLocator;

    @AndroidFindBy(xpath = "//*[@resource-id='okta-signin-password']")
    private WebElement oktaPasswordInputField;

    @AndroidFindBy(xpath = "//*[@text='Sign In']/following-sibling::*[@class='android.widget.EditText'][2]")
    private WebElement oktaPasswordInputFieldSecondLocator;

    @AndroidFindBy(xpath = "(//*[@class='android.widget.EditText'])[2]")
    private WebElement oktaPasswordInputFieldTempLocator;

    @AndroidFindBy(xpath = "//*[@resource-id='okta-signin-submit']")
    private WebElement oktaSignInButton;

    @AndroidFindBy(xpath = "//*[@class='android.widget.Button' and @text='Sign In']")
    private WebElement oktaSignInButtonSecondLocator;

    @AndroidFindBy(xpath = "//*[@text='Unable to sign in']")
    private WebElement errorOkta;

    public SSOPage(WebDriver driver) {
        super(driver);
    }

    // SSO Section Login Page

    public void inputSsoCode(String ssocode) {
        SSOInputField.sendKeys(ssocode);
    }

    public void clearInputField() {
        SSOInputField.clear();
    }

    public String getTextErrorSSO() {
        return SSOErrorMessage.getText();
    }

    // SSO Section Okta Page

    public boolean waitUntilOktaSignInPageVisible() {
        if (isElementInvisible(oktaSignInPage, Timedelta.ofSeconds(5))) {
            return waitUntilElementVisible(oktaSignInPageSignInLocator);
        } else {
            return waitUntilElementVisible(oktaSignInPage);
        }
    }

    public boolean isOktaErrorVisible() {
        return errorOkta.isDisplayed();
    }

    //ToDo: Check if okta has resource-IDs again and change locators back from temp ones
    public void inputUsername(String username) {
//        if (isElementInvisible(oktaUsernameInputField, Timedelta.ofSeconds(1))) {
//            oktaUsernameInputFieldSecondLocator.sendKeys(username);
//        } else {
//            oktaUsernameInputField.sendKeys(username);
//        }
        oktaUsernameInputFieldTempLocator.sendKeys(username);
    }

    public void inputPassword(String password) {
//        if (isElementInvisible(oktaPasswordInputField, Timedelta.ofSeconds(1))) {
//            oktaPasswordInputFieldSecondLocator.sendKeys(password);
//        } else {
//            oktaPasswordInputField.sendKeys(password);
//        }
        oktaPasswordInputFieldTempLocator.sendKeys(password);
    }

    public void tapSignIn() {
        if (isElementInvisible(oktaSignInButton, Timedelta.ofSeconds(1))) {
            oktaSignInButtonSecondLocator.click();
        } else {
            oktaSignInButton.click();
        }
    }
}
