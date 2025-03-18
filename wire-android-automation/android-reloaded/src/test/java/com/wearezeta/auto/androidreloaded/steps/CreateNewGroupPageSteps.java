package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.CreateNewGroupPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class CreateNewGroupPageSteps {

    private final AndroidTestContext context;

    public CreateNewGroupPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private CreateNewGroupPage getCreateNewGroupPage() {
        return context.getPage(CreateNewGroupPage.class);
    }

    @When("^I see create new group details page$")
    public void iSeeCreateNewGroupDetailsPage() {
        getCreateNewGroupPage().isCreateNewGroupDetailsPageVisible();
    }

    @When("^I see create new group settings page$")
    public void iSeeCreateNewGroupSettingsPage() {
        getCreateNewGroupPage().isCreateNewGroupSettingsPageVisible();
    }

    @When("^I type new group name \"(.*)\"$")
    public void iTypeNewGroupName(String groupName) {
        getCreateNewGroupPage().typeNewGroupName(groupName);
    }

    @When("^I do not see the protocol dropdown$")
    public void iDoNotSeeProtocolDropdown() {
        assertThat("Protocol dropdown is visible.", getCreateNewGroupPage().isProtocolDropdownInvisible());
    }

    @When("^I see the protocol used for creating conversation is MLS$")
    public void iSeeProtocolUsedForCreatingConversationIsMLS() {
        assertThat("Protocol 'MLS' is not visible.", getCreateNewGroupPage().isProtocolMLSVisible());
    }

    @When("^I see the protocol used for creating conversation is Proteus")
    public void iSeeProtocolUsedForCreatingConversationIsProteus() {
        assertThat("Protocol 'Proteus' is not visible.", getCreateNewGroupPage().isProtocolProteusVisible());
    }

    @When("^I tap continue button on create new group details page$")
    public void tapContinueButtonOnGroupDetailsPage(){
        getCreateNewGroupPage().tapContinueButton();
    }

    @When("^I tap continue button on create new group settings page$")
    public void tapContinueButtonOnGroupSettingsPage(){
        getCreateNewGroupPage().tapContinueButton();
    }

    @When("^I close the group creation page through the back arrow$")
    public void iCloseGroupCreationPage() {
        getCreateNewGroupPage().closeGroupCreationPage();
    }

    @When("^I see alert informing me that group can not be created$")
    public void iSeeCanNotCreateGroupAlert() {
        assertThat("Cannot create group alert is not visible.", getCreateNewGroupPage().isCanNotCreateGroupAlertVisible());
    }

    @Then("^I see explanation that users can not join the same conversation in can not create group alert$")
    public void iSeeFailedMultipleAddError() {
        assertThat("Subtext is not visible.", getCreateNewGroupPage().isTextVisible("can’t join the same group conversation"));
        assertThat("Subtext is not visible.", getCreateNewGroupPage().isTextVisible("To create the group, remove affected participants."));
    }

    @When("^I see Learn more link in the can not create group alert$")
    public void iSeeLearnMoreLinkGroupCreationAlert() {
        assertThat("Learn more link is not visible.", getCreateNewGroupPage().isLearnMoreLinkVisible());
    }

    @When("^I see Edit Participants List button$")
    public void iSeeEditParticipantsListButton() {
        assertThat("Button '%s' is not visible.", getCreateNewGroupPage().isEditParticipantsButtonVisible());
    }

    @When("^I see Discard Group Creation button$")
    public void iSeeDiscardGroupCreationButton() {
        assertThat("Button '%s' is not visible.", getCreateNewGroupPage().isDiscardGroupCreationButtonVisible());
    }

    @When("^I tap Edit Participants List button$")
    public void iTapEditParticipantsListButton() {
        getCreateNewGroupPage().tapEditParticipantsListButton();
    }

    @When("^I tap Discard Group Creation button$")
    public void iTapDiscardGroupCreationButton() {
        getCreateNewGroupPage().tapDiscardGroupCreationButton();
    }

    @When("^I tap on Learn more link on group creation page alert$")
    public void iTapLearnMoreGroupCreation() {
        getCreateNewGroupPage().tapLearnMoreGroupCreation();
    }
}

