package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.*;


import java.time.Duration;
import java.util.function.Function;

public class SearchPage extends AndroidPage {

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Search people']")
    private WebElement searchFieldNewConversationPage;

    @AndroidFindBy(xpath = "//*[@class='android.widget.EditText']")
    private WebElement searchFieldSearchPage;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Show More']")
    private WebElement showMoreButtonSearchPage;

    @AndroidFindBy(xpath = "//*[@text='Learn more']")
    private WebElement learnMoreLink;

    @AndroidFindBy(xpath = "//*[@content-desc='Conversation search icon']")
    private WebElement searchIcon;

    @AndroidFindBy(xpath = "//*[contains(@text,'New Group')]")
    private WebElement newGroupButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[contains(@text,'Continue')]")
    private WebElement continueButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[contains(@text,'Add To Group')]")
    private WebElement addToGroupButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[contains(@text,'Remove From Group')]")
    private WebElement removeFromGroupButton;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Close new conversation view\"]")
    private WebElement closeButtonSearchPage;

    @AndroidFindBy(xpath = "//*[@content-desc='Conversation search icon']/..//*[@class='android.widget.Button']")
    private WebElement closeButtonSearchResultPage;

    private final Function<String, String> emptySearchHint = text -> String.format("//*[@text='%s']", text);

    private final Function<String, By> checkBoxUserSearchSuggestions = name -> By.xpath(String.format("//*[@text=\"%s\"]/preceding-sibling::*[@class='android.widget.CheckBox']", name));

    private final Function <String, String> searchUserString = name -> String.format("//android.view.View[@resource-id=\"User avatar\"]/following-sibling::*[@text=\"%s\"]", name);

    private final Function <String, By> searchUserBy = name -> By.xpath(String.format("//android.view.View[@resource-id=\"User avatar\"]/following-sibling::*[@text=\"%s\"]", name));

    private final Function<String, String> federatedLabelSearchResultString = name -> String.format("//*[@text=\"%s\"]/..//*[@text='Federated']", name);

    private final Function<String, By> federatedLabelSearchResultBy = name -> By.xpath(String.format("//*[@text=\"%s\"]/..//*[@text='Federated']", name));

    private final Function<String, String> subtitleDomainString = domain -> String.format("//*[@content-desc='Profile picture']/../..//*[contains(@text,'%s')]", domain);

    private final Function<String, By> subtitleDomainBy = domain -> By.xpath(String.format("//*[@content-desc='Profile picture']/../..//*[contains(@text,'%s')]", domain));

    private final Function<String, String> textString = text -> String.format("//*[@text=\"%s\"]", text);

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[@text=\"%s\"]", text));

    public SearchPage(WebDriver driver) {
        super(driver);
    }

    public void tapSearchField() {
        searchFieldNewConversationPage.isDisplayed();
        searchFieldNewConversationPage.click();
    }

    public void closeSearchPage() {
        closeButtonSearchPage.isDisplayed();
        closeButtonSearchPage.click();
    }

    public void closeSearchResultPage() {
        closeButtonSearchResultPage.isDisplayed();
        closeButtonSearchResultPage.click();
    }

    public boolean isSearchHintVisible(String text) {
        return getDriver().findElement(By.xpath(emptySearchHint.apply(text))).isDisplayed();
    }

    public boolean isSearchHintInvisible(String text) {
        WebElement searchHint = getDriver().findElement(By.xpath(emptySearchHint.apply(text)));
        return !searchHint.isDisplayed();
    }

    public boolean isLearnMoreLinkVisible() {
        return learnMoreLink.isDisplayed();
    }

    public boolean isLearnMoreLinkInvisible() {
        return !learnMoreLink.isDisplayed();
    }

    public boolean isUserInSearchSuggestionsListVisible(String user) {
        return getDriver().findElement(By.xpath(searchUserString.apply(user))).isDisplayed();
    }

    public boolean isUserInSearchSuggestionsListInvisible(String user) {
        return isLocatorInvisible(searchUserBy.apply(user));
    }

    public void iSelectUserInSearchSuggestionsList(String user) {
        final By oldLocator = checkBoxUserSearchSuggestions.apply(user);
        if (isLocatorInvisible(oldLocator, Timedelta.ofSeconds(1))) {
            getDriver().findElement(By.xpath(searchUserString.apply(user))).click();
        } else {
            getDriver().findElement(oldLocator).click();
        }
    }

    public boolean isNewGroupButtonInvisible() {
        return waitUntilElementInvisible(newGroupButton, Duration.ofSeconds(1));
    }

    public void tapCreateNewGroupButton () {
        newGroupButton.isDisplayed();
        newGroupButton.click();
    }

    public void tapContinue() {
        continueButton.isDisplayed();
        continueButton.click();
    }

    public void tapAddToGroup() {
        addToGroupButton.isDisplayed();
        addToGroupButton.click();
    }

    public void sendKeysUserNameSearchField(String userName) {
        searchFieldSearchPage.sendKeys(userName);
    }

    public void clearTextInSearch()  {
        searchFieldSearchPage.clear();
    }

    public boolean isUserNameSearchResultVisible(String result) {
        final By locator = searchUserBy.apply(result);
        waitUntilLocatorIsDisplayed(locator, Duration.ofSeconds(1));
        if (isLocatorInvisible(locator, Timedelta.ofSeconds(1))) {
            hideKeyboard();
            showMoreButtonSearchPage.click();
            scrollUntilElementVisible(locator, 0, -1);
        }
        return waitUntilLocatorIsDisplayed(locator, Duration.ofSeconds(1));
    }

    public boolean isUserNameSearchResultInvisible(String result) {
        final By locator = searchUserBy.apply(result);
        return isLocatorInvisible(locator);
    }

    public void tapUserNameSearchResult(String result) {
        getDriver().findElement(By.xpath(searchUserString.apply(result))).click();
    }

    // Federation

    public boolean isFederatedLabelForUserVisible(String user) {
        return getDriver().findElement(By.xpath(federatedLabelSearchResultString.apply(user))).isDisplayed();
    }

    public boolean isFederatedLabelForUserInvisible(String user) {
        final By locator = federatedLabelSearchResultBy.apply(user);
        return isLocatorInvisible(locator);
    }

    public boolean isDomainInSubtitleVisible(String domain) {
        return getDriver().findElement(By.xpath(subtitleDomainString.apply(domain))).isDisplayed();
    }

    public boolean isDomainInSubtitleInvisible(String domain) {
        final By locator = subtitleDomainBy.apply(domain);
        return isLocatorInvisible(locator);
    }

    // Federation End

    public void tapOnUserName(String userName) {
        getDriver().findElement(By.xpath(textString.apply(userName))).click();
    }
}
