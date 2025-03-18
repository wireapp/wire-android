package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.function.Function;

public class ArchiveListPage extends AndroidPage{

    @AndroidFindBy(xpath = "//*[@text='Unarchive']")
    private WebElement moveOutOfArchiveButton;

    @AndroidFindBy(xpath = "//*[@text='Clear Content…']")
    private WebElement clearContentButton;

    private final Function<String, String> archivedConversationName = name -> String.format("//android.widget.TextView[@text='%s']", name);

    private final Function<String, By> archivedConversationNameBy = name -> By.xpath(String.format("//android.widget.TextView[@text='%s']", name));

    private final Function<String, String> toastMessage = text -> String.format("//*[@text='%s']", text);

    public ArchiveListPage(WebDriver driver) {
        super(driver);
    }

    public boolean isArchivedConversationDisplayed(String name) {
        return getDriver().findElement(By.xpath(archivedConversationName.apply(name))).isDisplayed();
    }

    public boolean isArchivedConversationInvisible(String name) {
        final By locator = archivedConversationNameBy.apply(name);
        return isLocatorInvisible(locator);
    }

    public void tapArchivedConversationName(final String name) {
        getDriver().findElement(By.xpath(archivedConversationName.apply(name))).click();
    }

    public void longTapArchivedConversationName(String name) {
        final WebElement locator = getDriver().findElement(By.xpath(archivedConversationName.apply(name)));
        longTap(locator);
    }

    public void tapMoveOutOfArchiveButton() {
        waitUntilElementVisible(moveOutOfArchiveButton);
        moveOutOfArchiveButton.click();
    }

    public boolean isToastMessageDisplayed(String text) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(toastMessage.apply(text))));
    }
}
