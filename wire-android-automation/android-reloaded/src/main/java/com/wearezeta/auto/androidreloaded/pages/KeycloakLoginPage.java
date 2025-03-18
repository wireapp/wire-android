package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class KeycloakLoginPage extends AndroidPage {

    @AndroidFindBy(xpath = "//android.view.View[@text='Username or email']/following-sibling::*[@class='android.widget.EditText']")
    private WebElement emailField;

    @AndroidFindBy(xpath = "//android.view.View[@text='Password']/following-sibling::*[@class='android.widget.EditText']")
    private WebElement passwordField;

    @AndroidFindBy(xpath = "//android.widget.Button[@text='Sign In']")
    private WebElement signInButton;

    public KeycloakLoginPage(WebDriver driver) {
        super(driver);
    }

    public void enterEmail(String oktaEmail) {
        waitUntilElementVisible(emailField, getDefaultLookupTimeoutSeconds() * 3L);
        emailField.sendKeys(oktaEmail);
    }

    public void enterPassword(String oktaPassword) {
        passwordField.click();
        passwordField.sendKeys(oktaPassword);
    }

    public void clickSignInButton() {
        waitUntilElementVisible(signInButton);
        signInButton.click();
    }
}
