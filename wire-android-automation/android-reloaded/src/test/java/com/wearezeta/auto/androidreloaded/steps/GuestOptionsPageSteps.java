package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.GroupConversationDetailsPage;
import com.wearezeta.auto.androidreloaded.pages.GuestOptionsPage;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;

public class GuestOptionsPageSteps {

    private final AndroidTestContext context;

    public GuestOptionsPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private GuestOptionsPage getGuestOptionsPage() {
        return context.getPage(GuestOptionsPage.class);
    }

    @When("^I tap on the guest switch$")
    public void iTapGuestSwitch() {
        getGuestOptionsPage().tapOnGuestsSwitch();
    }

    @When("^I tap back button on guests options page$")
    public void iTapBackButton() {
        getGuestOptionsPage().tapBackButtonGuestOptions();
    }

    @When("^I tap on create link option on guests page$")
    public void iTapCreateGuestLinkButton() {
        getGuestOptionsPage().tapCreateGuestLinkButton();
    }

    @When("I select create without password to create the guest link")
    public void iSelectGuestLinkOptionWithoutPassword() {
        getGuestOptionsPage().iTapCreateLinkWithoutPasswordButton();
    }

    @When("I select create with password to create the guest link")
    public void iSelectGuestLinkOptionWithPassword() {
        getGuestOptionsPage().iTapCreateLinkWithPasswordButton();
    }

    @When("^I see guest link was created$")
    public void iSeeGuestlinkCreated() {
        assertThat("Guest link was not created.", getGuestOptionsPage().isGuestLinkCreated());
    }

    @When("^I see that guest link was created with password$")
    public void iSeeGuestLinkCreatedWithPassword() {
        assertThat("Guest link was not created with password.", getGuestOptionsPage().isGuestLinkCreatedWithPassword());
    }

    @When("^I do not see the guest link displayed on guests page$")
    public void iDoNotSeeGuestLink() {
        assertThat("Guest link is displayed but it should not.", getGuestOptionsPage().isGuestLinkInvisible());
    }

    @When("^I tap on copy link button on guests Link page$")
    public void iTapOnCopyLinkOption() {
        getGuestOptionsPage().tapCopyLinkButton();
    }

    @When("^I close guest page through the back arrow$")
    public void iCloseGuestPage() {
        getGuestOptionsPage().tapBackButtonGuestOptions();
    }

    @When("^I see create password for guestlinks page$")
    public void iSeeCreatePasswordPage() {
        assertThat("Create password page is not visible.", getGuestOptionsPage().isCreatePasswordPageVisible());
    }

    @When("^I type my password \"(.*)\" on guestlink page$")
    public void iTypePasswordGuestLinkPage(String password) {
        getGuestOptionsPage().iTypePassword(password);
    }

    @When("^I type my confirm password \"(.*)\" on guestlink page$")
    public void iTypeConfirmPasswordGuestLinkPage(String password) {
        getGuestOptionsPage().iTypeConfirmPassword(password);
    }

    @When("^I tap create Link button on guest link password page$")
    public void iTapCreateLinkOnGuestLinkPasswordPage() {
        getGuestOptionsPage().tapCreateGuestLinkButton();
    }
}
