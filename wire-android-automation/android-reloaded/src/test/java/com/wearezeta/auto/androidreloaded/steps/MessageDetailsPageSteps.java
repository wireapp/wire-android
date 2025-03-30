package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.MessageDetailsPage;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MessageDetailsPageSteps {

    private final AndroidTestContext context;

    public MessageDetailsPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private MessageDetailsPage getMessageDetailsPage() {
        return context.getPage(MessageDetailsPage.class);
    }

    @When("^I tap on read receipts tab in message details$")
    public void iTapOnReadReceiptsTab() {
        getMessageDetailsPage().tapReadReceiptsTab();
    }

    @When("^I see (.*) read receipts in read receipts tab$")
    public void iSeeNumberOfReadReceiptsInTab(String amount) {
        assertThat("Amount of read receipts is not correct.", getMessageDetailsPage().getTextReadReceiptsTab(), containsString(amount));
    }

    @When("^I see user (.*) in the list of users that read my message$")
    public void iSeeUserReadMessage(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("User '%s' is not in list of users that read the message.", user), getMessageDetailsPage().isUserVisibleInList(user));
    }

    @When("^I do not see user (.*) in the list of users that read my message$")
    public void iDoNotSeeUserReadMessage(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("User '%s' is in list of users that read the message.", user), getMessageDetailsPage().isUserInvisibleInList(user));
    }

    @When("^I see (.*) reactions in reactions tab$")
    public void iSeeNumberOfReactionsInTab(String amount) {
        assertThat("Amount of reactions is not correct.", getMessageDetailsPage().getTextReactionsTab(), containsString(amount));
    }

    @When("^I see user (.*) in the list of users that reacted$")
    public void iSeeUserReacted(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("User '%s' is not in list of users that reacted.", user), getMessageDetailsPage().isUserVisibleInList(user));
    }
}
