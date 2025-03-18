package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.ConversationViewPage;
import com.wearezeta.auto.androidreloaded.pages.LegalHoldPage;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;

public class LegalHoldPageSteps {

    private final AndroidTestContext context;

    public LegalHoldPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private LegalHoldPage getLegalHoldPage() {
        return context.getPage(LegalHoldPage.class);
    }

    @When("^I see legal hold request modal$")
    public void iSeeLegalHoldRequestModal() {
        assertThat("Legal Hold modal is not visible.", getLegalHoldPage().isLegalHoldModalVisible());
    }

    @When("^I see legal hold deactivated modal$")
    public void iSeeLegalHoldDeactivatedModal() {
        assertThat("Legal Hold deactivated modal is not visible.", getLegalHoldPage().isLegalHoldDeactivatedModalVisible());
    }

    @When("^I tap OK button on legal hold deactivated modal$")
    public void iTapOkOnLegalHoldDeactivatedModal() {
        getLegalHoldPage().tapOkOnLegalHoldDeactivatedModal();
    }

    @When("^I do not see legal hold request modal$")
    public void iDoNotSeeLegalHoldRequestModal() {
        assertThat("Legal Hold modal is not visible.", getLegalHoldPage().isLegalHoldModalInvisible());
    }

    @When("^I see explanation \"(.*)\" on legal hold request modal$")
    public void iSeeTextLegalHoldModal(String text) {
        assertThat("Explanation is not visible.", getLegalHoldPage().isTextDisplayed(text));
    }

    @When("^I see explanation \"(.*)\" on legal hold deactivated modal$")
    public void iSeeTextLegalHoldDeactivatedModal(String text) {
        assertThat("Explanation is not visible.", getLegalHoldPage().isTextDisplayed(text));
    }

    @When("^I enter password my account password on legal hold request modal$")
    public void iEnterPasswordLegalHold() {
        final ClientUser self = context.getUsersManager().getSelfUserOrThrowError();
        getLegalHoldPage().enterPasswordLegalHold(self.getPassword());
    }

    @And("I clear the password input field on legal hold request modal")
    public void iClearThePasswordInputFieldOnLegalHoldRequestModal() {
        getLegalHoldPage().clearPasswordLegalHold();
    }

    @When("^I enter invalid password \"(.*)\" password on legal hold request modal$")
    public void iEnterInvalidPasswordLegalHold(String password) {
        getLegalHoldPage().enterPasswordLegalHold(password);
    }

    @When("^I do not see password field on legal hold request modal$")
    public void iDoNotSeePasswordFieldLegalHold() {
        assertThat("Password field is visible.", getLegalHoldPage().isPasswordFieldLegalHoldInvisible());
    }

    @When("^I accept Legal Hold request$")
    public void iAcceptLegalHoldRequest() {
        getLegalHoldPage().acceptLegalHoldRequest();
    }

    @When("^I delay Legal Hold request$")
    public void iDelayLegalHoldRequest() {
        getLegalHoldPage().delayLegalHoldRequest();
    }

    @When("^I see invalid password error message on legal hold request modal$")
    public void iSeePasswordErrorLegalHold() {
        assertThat("Error message is not visible.", getLegalHoldPage().isPasswordErrorLegalHoldVisible());
    }
}
