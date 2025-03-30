package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;

public class DocumentsUIPage extends AndroidPage {

    public static final DefaultArtifactVersion VERSION_8_0 = new DefaultArtifactVersion("8.0.0");
    public static final DefaultArtifactVersion VERSION_8_1 = new DefaultArtifactVersion("8.1.0");
    public static final DefaultArtifactVersion VERSION_9_0 = new DefaultArtifactVersion("9.0");
    public static final DefaultArtifactVersion VERSION_12_0 = new DefaultArtifactVersion("12.0");
    public static final DefaultArtifactVersion VERSION_13_0 = new DefaultArtifactVersion("13.0");


    private String phoneModel = getPhoneModelType();

    private DefaultArtifactVersion osVersion = getOSVersion();

    @AndroidFindBy(xpath = "//android.widget.ImageButton[contains(@content-desc,'Show root')]")
    private WebElement fileSharingRootsButton;

    @AndroidFindBy(xpath = "//*[@text='Downloads']")
    private WebElement downloadsMenuItem;

    @AndroidFindBy(uiAutomator = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"TestingGallery\"))")
    private WebElement testingGalleryEntry;

    @AndroidFindBy(xpath = "//*[contains(@resource-id,'icon_thumbnail')] | //android.view.View[contains(@content-desc, 'Photo taken on')]")
    private WebElement image;

    @AndroidFindBy(xpath = "//*[contains(@text,'Add')] | //*[contains(@text,'Done')]")
    private WebElement addButtonFiles;

    private final Function<String, By> contentDescContains = text -> By.xpath(String.format("//android.widget.LinearLayout[contains(@content-desc,'%s')]", text));

    private final Function<String, By> textContains = text -> By.xpath(String.format("//*[contains(@text,'%s')]", text));
    
    private final Function<String, String> textString = text -> String.format("//*[@text='%s']", text);

    public DocumentsUIPage(WebDriver driver) {
        super(driver);
    }

    public void tapRootsMenu() {
        waitUntilElementVisible(fileSharingRootsButton);
        fileSharingRootsButton.click();
    }

    public void tapDownloads() {
        waitUntilElementVisible(downloadsMenuItem);
        downloadsMenuItem.click();
    }

    public boolean isImageContainingTextVisible(String text) {
        if (phoneModel.compareTo("samsung") > 0 && osVersion.compareTo(VERSION_8_0) > 0) {
            return waitUntilLocatorIsDisplayed(textContains.apply(text), Duration.ofSeconds(1));
        } else if (phoneModel.compareTo("samsung") > 0 && osVersion.compareTo(VERSION_9_0) > 0) {
            return waitUntilLocatorIsDisplayed(textContains.apply(text), Duration.ofSeconds(1));
        }  else if (phoneModel.compareTo("HUAWEI") > 0 && osVersion.compareTo(VERSION_8_1) > 0) {
            return waitUntilLocatorIsDisplayed(textContains.apply(text), Duration.ofSeconds(1));
        } else if (phoneModel.compareTo("Xiaomi") > 0 && osVersion.compareTo(VERSION_12_0) > 0) {
            return waitUntilLocatorIsDisplayed(textContains.apply(text), Duration.ofSeconds(1));
        } else if (phoneModel.compareTo("Pixel") > 0 && osVersion.compareTo(VERSION_13_0) > 0) {
            return waitUntilLocatorIsDisplayed(textContains.apply(text), Duration.ofSeconds(1));
        } else {
            return waitUntilLocatorIsDisplayed(contentDescContains.apply(text), Duration.ofSeconds(1));
        }
    }

    public void selectImageContaining(String text) {
        if (phoneModel.compareTo("samsung") > 0 && osVersion.compareTo(VERSION_8_0) > 0) {
            final WebElement fileOlderDevices = getDriver().findElement(textContains.apply(text));
            fileOlderDevices.click();
        } else if (phoneModel.compareTo("samsung") > 0 && osVersion.compareTo(VERSION_9_0) > 0) {
            final WebElement fileOlderDevices = getDriver().findElement(textContains.apply(text));
            fileOlderDevices.click();
        }  else if (phoneModel.compareTo("HUAWEI") > 0 && osVersion.compareTo(VERSION_8_1) > 0) {
            final WebElement fileOlderDevices = getDriver().findElement(textContains.apply(text));
            fileOlderDevices.click();
        } else if (phoneModel.compareTo("Xiaomi") > 0 && osVersion.compareTo(VERSION_12_0) > 0) {
            final WebElement fileOlderDevices = getDriver().findElement(textContains.apply(text));
            fileOlderDevices.click();
        } else if (phoneModel.compareTo("Pixel") > 0 && osVersion.compareTo(VERSION_13_0) > 0) {
            final WebElement fileOlderDevices = getDriver().findElement(textContains.apply(text));
            fileOlderDevices.click();
        } else {
            final WebElement fileNewerDevices = getDriver().findElement(contentDescContains.apply(text));
            fileNewerDevices.click();
        }
    }

    public void selectFileContaining(String text) {
        final WebElement fileToShare = getDriver().findElement(textContains.apply(text));
        fileToShare.click();
    }

    public void selectBackupFileContaining(String userName) {
        final WebElement fileToShare = getDriver().findElement(textContains.apply(userName));
        fileToShare.click();
    }

    public BufferedImage getRecentImageScreenshot() {
        waitUntilElementVisible(image);
        return getElementScreenshot(image);
    }

    public List<String> getQRCodeFromRecentImage() {
        return waitUntilElementContainsQRCode(image);
    }

    public void selectImageWithQRCode() {
        waitUntilElementContainsQRCode(image);
        image.click();
    }

    public void selectAddButtonFiles() {
        waitUntilElementVisible(addButtonFiles);
        addButtonFiles.click();
    }
}
