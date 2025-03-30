package com.wearezeta.auto.androidreloaded.pages.external;

import com.wearezeta.auto.androidreloaded.pages.AndroidPage;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.function.Function;

public class ChromePage extends AndroidPage {

    @AndroidFindBy(id = "com.android.chrome:id/terms_accept")
    private WebElement acceptTermsButton;

    @AndroidFindBy(id = "com.android.chrome:id/negative_button")
    private WebElement noThanksButton;

    @AndroidFindBy(xpath = "//*[@text='Use without an account']")
    private WebElement useWithoutAccountButton;

    private final Function<String, String> textContainsString = text -> String.format("//*[contains(@text,'%s')]", text);

    private final Function<String, By> textContainsBy = text -> By.xpath(String.format("//*[contains(@text,'%s')]", text));

    public ChromePage(WebDriver driver) {
        super(driver);
    }

    public boolean isChromeLoginVisible() {
        return isElementPresentAndDisplayed(useWithoutAccountButton);
    }

    public void clickAccept() {
        acceptTermsButton.click();
    }

    public void clickNoThanks() {
        noThanksButton.click();
    }

    public void tapUseWithoutAccountButton() {
        waitUntilElementVisible(useWithoutAccountButton);
        useWithoutAccountButton.click();
    }

    public boolean isTextVisible(String text) {
        return getDriver().findElement(By.xpath(textContainsString.apply(text))).isDisplayed();
    }

    public boolean isTextInvisible(String text) {
        return isLocatorInvisible(textContainsBy.apply(text));
    }
}
