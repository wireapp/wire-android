package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.function.Function;

public class CreateNewGroupPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@text='New Group']")
    private WebElement createNewGroupHeading;

    @AndroidFindBy(xpath = "//*[@text='Allow guests']")
    private WebElement allowGuestsSection;

    @AndroidFindBy(xpath = "//*[@text='Allow services']")
    private WebElement allowServicesSection;

    @AndroidFindBy(xpath = "//*[@text='Read receipts']")
    private WebElement readReceiptsSection;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Type group name\"]/..[@class='android.widget.EditText']")
    private WebElement newGroupNameInputField;

    @AndroidFindBy(xpath = "//*[@text='Protocol']/following-sibling::*[@text='MLS']")
    private WebElement protocolMLS;

    @AndroidFindBy(xpath = "//*[@text='Protocol']/following-sibling::*[@text='PROTEUS']")
    private WebElement protocolProteus;

    @AndroidFindBy(xpath = "//*[@content-desc='Change']")
    private WebElement protocolDropdown;

    @AndroidFindBy(xpath = "//*[contains(@text,'Continue')]")
    private WebElement continueButton;

    @AndroidFindBy(xpath = "//*[@text=\"Group can’t be created\"]")
    private WebElement canNotCreateGroupAlert;

    @AndroidFindBy(xpath = "//*[contains(@text,'Learn more')]")
    private WebElement learnMoreLink;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Go back to new conversation view\"]")
    private WebElement backArrow;

    @AndroidFindBy(xpath = "//*[contains(@text,'Group can’t be created')]//*[@class='android.widget.TextView']")
    private WebElement subTextCanNotCreateGroupAlert;

    @AndroidFindBy(xpath = "//*[@class='android.widget.Button']/preceding-sibling::*[@text='Edit Participants List']")
    private WebElement editParticipantsButton;

    @AndroidFindBy(xpath = "//*[@class='android.widget.Button']/preceding-sibling::*[@text='Discard Group Creation']")
    private WebElement discardGroupCreationButton;

    private final Function<String, String> textString = text -> String.format("//*[contains(@text,'%s')]", text);

    public CreateNewGroupPage(WebDriver driver) {
        super(driver);
    }

    public boolean isCreateNewGroupDetailsPageVisible() {
        return createNewGroupHeading.isDisplayed();
    }

    public boolean isCreateNewGroupSettingsPageVisible() {
        return allowGuestsSection.isDisplayed();
    }

    public void typeNewGroupName(String groupName) {
        newGroupNameInputField.isDisplayed();
        newGroupNameInputField.sendKeys(groupName);
    }

    public boolean isProtocolMLSVisible() {
        return protocolMLS.isDisplayed();
    }

    public boolean isProtocolProteusVisible() {
        return protocolProteus.isDisplayed();
    }

    public boolean isProtocolDropdownInvisible() {
        return waitUntilElementInvisible(protocolDropdown);
    }

    public void tapContinueButton() {
        continueButton.isDisplayed();
        continueButton.click();
    }

    public void closeGroupCreationPage() {
        backArrow.isDisplayed();
        backArrow.click();
    }

    public boolean isCanNotCreateGroupAlertVisible() {
        return canNotCreateGroupAlert.isDisplayed();
    }

    public boolean isTextVisible(String text) {
        return getDriver().findElement(By.xpath(textString.apply(text))).isDisplayed();
    }

    public boolean isEditParticipantsButtonVisible() {
        return editParticipantsButton.isDisplayed();
    }

    public boolean isDiscardGroupCreationButtonVisible() {
        return discardGroupCreationButton.isDisplayed();
    }

    public void tapEditParticipantsListButton() {
        editParticipantsButton.click();
    }

    public void tapDiscardGroupCreationButton() {
        discardGroupCreationButton.click();
    }

    public boolean isLearnMoreLinkVisible() {
        return learnMoreLink.isDisplayed();
    }

    public void tapLearnMoreGroupCreation() {
        learnMoreLink.isDisplayed();
        learnMoreLink.click();
    }
}
