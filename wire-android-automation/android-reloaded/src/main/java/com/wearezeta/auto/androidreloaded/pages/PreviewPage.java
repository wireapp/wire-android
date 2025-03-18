package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Function;


public class PreviewPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@class='android.widget.ImageView']")
    private WebElement imagePreview;

    @AndroidFindBy(xpath = "//*[@class='android.widget.Button']/preceding-sibling::*[@text='Send']")
    private WebElement imagePreviewSendButton;

    private final Function<String, String> textString = text -> String.format("//*[contains(@text,'%s')]", text);

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[contains(@text,'%s')]", text));

    public PreviewPage(WebDriver driver) {
        super(driver);
    }

    public boolean isImagePreviewPageVisible() {
        return waitUntilElementVisible(imagePreview);
    }

    public boolean isAssetPreviewPageVisible(String fileName) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(textString.apply(fileName))));
    }

    public void tapSendButtonImagePreviewPage() {
        imagePreviewSendButton.click();
    }
}
