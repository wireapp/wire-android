package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.function.Function;

public class WelcomePage extends AndroidPage {

    @AndroidFindBy(xpath = "//android.view.View[@resource-id='loginButton']")
    private WebElement loginButtonWelcomePage;
    
    @AndroidFindBy(xpath = "//*[@text='Proceed']")
    private WebElement proceedButton;

    @AndroidFindBy(xpath = "//*[@text='Login']")
    private WebElement loginButton;

    @AndroidFindBy(xpath = "//*[@text='Create a Team']")
    private WebElement createATeamButton;

    @AndroidFindBy(xpath = "//*[@text='Create a Personal Account']")
    private WebElement createAPersonalAccountLink;

    private final Function<String, String> textContainsString = text -> String.format("//*[contains(@text,'%s')]", text);

    public WelcomePage(WebDriver driver) {
        super(driver);
    }

    public boolean isWelcomePageVisible() {
        return waitUntilElementVisible(loginButtonWelcomePage);
    }

    public boolean isTextDisplayed(String text) {
        return getDriver().findElement(By.xpath(textContainsString.apply(text))).isDisplayed();
    }

    public void tapProceedButton() {
        proceedButton.isDisplayed();
        proceedButton.click();
    }

    public boolean isProceedButtonVisible() {
        return !isElementInvisible(proceedButton, Timedelta.ofSeconds(1));
    }

    public void tapLoginButton() {
        loginButton.click();
    }

    public void tapCreateATeamButton() {
        createATeamButton.click();
    }

    public boolean isCreateATeamButtonInvisible() {
        return waitUntilElementInvisible(createATeamButton, Duration.ofSeconds(1));
    }

    public void tapCreateAPersonalAccountLink() {
        createAPersonalAccountLink.click();
    }

    public boolean isCreateAPersonalAccountLinkInvisible() {
        return waitUntilElementInvisible(createAPersonalAccountLink, Duration.ofSeconds(1));
    }
}
