package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.function.Function;

public class MessageDetailsPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[contains(@text,'REACTIONS')]")
    private WebElement reactionsTab;

    @AndroidFindBy(xpath = "//*[contains(@text,'READ RECEIPTS')]")
    private WebElement readReceiptsTab;

    private final Function<String, String> textString = text -> String.format("//*[@text=\"%s\"]", text);

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[@text=\"%s\"]", text));

    public MessageDetailsPage(WebDriver driver) {
        super(driver);
    }

    public void tapReadReceiptsTab() {
        readReceiptsTab.click();
    }

    public String getTextReadReceiptsTab() {
        return readReceiptsTab.getText();
    }

    public String getTextReactionsTab() {
        return reactionsTab.getText();
    }

    public boolean isUserVisibleInList(String user) {
        return getDriver().findElement(By.xpath(textString.apply(user))).isDisplayed();
    }

    public boolean isUserInvisibleInList(String user) {
        return isLocatorInvisible(textBy.apply(user));
    }
}
