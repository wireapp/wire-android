package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.CallingPage;
import com.wearezeta.auto.common.backend.Backend;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.imagecomparator.QRCode;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.management.InstanceNotFoundException;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class CallingPageSteps {

    private final AndroidTestContext context;

    private static final Logger log = ZetaLogger.getLog(CallingPageSteps.class.getSimpleName());

    public CallingPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private CallingPage getCallingPage() {
        return context.getPage(CallingPage.class);
    }

    @When("(.*) starts? instances? using (.*)$")
    public void userXStartsInstance(String callees,
                                    String callingServiceBackend) throws Exception {
        context.startPinging();
        context.getCallingManager().
                startInstances(context.getUsersManager().splitAliases(callees),
                        callingServiceBackend, "Android", context.getScenario().getName());
        context.stopPinging();
    }

    @When("(.*) starts? 2FA instances? using (.*)$")
    public void UserXStarts2FAInstance(String callees, String callingServiceBackend) throws Exception {
        context.startPinging();
        List<String> calleeNames = context.getUsersManager().splitAliases(callees);
        for (String calleeName : calleeNames) {
            ClientUser user = context.getUsersManager().findUserByNameOrNameAlias(calleeName);
            Backend backend = BackendConnections.get(user);

            if (callingServiceBackend.contains("zcall")) {
                String teamID = backend.getAllTeams(user).get(0).getId();
                backend.unlock2FAuthenticationFeature(teamID);
                backend.disable2FAuthenticationFeature(teamID);
                context.getCallingManager().startInstances(Collections.singletonList(calleeName), callingServiceBackend,
                        "Android", context.getScenario().getName());
                backend.enable2FAuthenticationFeature(teamID);
                backend.lock2FAuthenticationFeature(teamID);
            } else {
                String verificationCode = backend.getVerificationCode(user);
                log.info("verificationCode: " + verificationCode);
                context.getCallingManager().startInstance(calleeName, verificationCode,
                        callingServiceBackend, "Android", context.getScenario().getName());
            }
        }
        context.stopPinging();
    }

    @When("^User (.*) calls (.*)$")
    public void userXCallsToConversationY(String callerName, String conversationName) throws Exception {
        context.getCallingManager().callToConversation(callerName, conversationName);
    }

    @When("^I see incoming call from (.*)$")
    public void iSeeIncomingCallFromUser(String callerName) {
        callerName = context.getUsersManager().replaceAliasesOccurrences(callerName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("Incoming call from %s is not visible", callerName), getCallingPage().isIncomingCallFromUserVisible(callerName));
    }

    @When("^I see incoming group call from group (.*) with (.*) calling$")
    public void iSeeIncomingGroupCallFromUser(String groupName, String callerName) {
        callerName = context.getUsersManager().replaceAliasesOccurrences(callerName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("Incoming group call for group %s from %s is not visible", groupName, callerName), getCallingPage().isIncomingGroupCallFromUserVisible(groupName, callerName));
    }

    @When("^I see incoming group call from group (.*)$")
    public void iSeeIncomingGroupCall(String groupName) {
        assertThat(String.format("Incoming group call for group %s is not visible", groupName), getCallingPage().isIncomingGroupCallVisible(groupName));
    }

    @When("^I accept the call$")
    public void iAcceptCall(){
        getCallingPage().acceptCall();
    }

    @When("^I decline the call$")
    public void iDeclineCall(){
        getCallingPage().declineCall();
    }

    @When("^(.*) accepts? next incoming call automatically$")
    public void userXAcceptsNextIncomingCallAutomatically(String callees) throws Exception {
        context.getCallingManager().acceptNextCall(context.getUsersManager().splitAliases(callees));
    }

    @When("^I tap start call button$")
    public void iStartCall(){
        getCallingPage().startCall();
    }

    @When("^I see start call alert$")
    public void iSeeStartCallAlert(){
        assertThat("Start call alert is not displayed.", getCallingPage().isStartCallAlertDisplayed());
    }

    @When("^I tap call button on start call alert")
    public void iTapStartCallButtonOnAlert(){
        getCallingPage().tapStartCallButtonAlert();
    }

    @When("^(.*) stops? calling( (.*))?$")
    public void UserXStopsCallsToUserY(String instanceUsers, String outgoingCall, String conversationName)
            throws Exception {
        if (outgoingCall == null) {
            context.getCallingManager()
                    .stopIncomingCall(context
                            .getUsersManager().splitAliases(instanceUsers));
        } else {
            context.getCallingManager()
                    .stopOutgoingCall(context.getUsersManager()
                            .splitAliases(instanceUsers), conversationName);
        }
    }

    // 1:1 Calls

    @When("^I see ongoing 1:1 call$")
    public void iSeeOngoing1On1Call() {
        assertThat("Ongoing call is not visible.", getCallingPage().isOngoingCallVisible());
    }

    @When("^I do not see ongoing 1:1 call$")
    public void iDoNotSeeOngoing1On1Call() {
        assertThat("Ongoing call is still visible.", getCallingPage().isOngoingCallInvisible());
    }

    @When("^I see (.*) in ongoing 1:1 call$")
    public void iSeeParticipantInCall(String participant) {
        participant = context.getUsersManager().replaceAliasesOccurrences(participant, ClientUsersManager.FindBy.NAME_ALIAS);
        getCallingPage().isOtherParticipantVisible(participant);
    }

    @When("^I see user (.*) in ongoing 1:1 video call$")
    public void iSeeParticipantInVideoCall(String participants) {
        for (String participant : context.getUsersManager().splitAliases(participants)) {
            participant = context.getUsersManager().replaceAliasesOccurrences(participant, ClientUsersManager.FindBy.NAME_ALIAS);
            getCallingPage().isOtherParticipantVideoTileVisible(participant);
        }
    }

    // Group Calls

    @When("^I see ongoing group call$")
    public void iSeeOngoingGroupCall() {
        assertThat("Ongoing call is not visible.", getCallingPage().isOngoingCallVisible());
    }

    @When("^I do not see ongoing group call$")
    public void iDoNotSeeOngoingGroup() {
        assertThat("Ongoing call is still visible.", getCallingPage().isOngoingCallInvisible());
    }

    @When("^I see users? (.*) in ongoing group call$")
    public void iSeeParticipantsInGroupCall(String participants) {
        for (String participant : context.getUsersManager().splitAliases(participants)) {
            participant = context.getUsersManager().replaceAliasesOccurrences(participant, ClientUsersManager.FindBy.NAME_ALIAS);
            getCallingPage().isOtherParticipantVisible(participant);
        }
    }

    @When("^I do not see users? (.*) in ongoing group call$")
    public void iDoNotSeeParticipantsInGroupCall(String participants) {
        for (String participant : context.getUsersManager().splitAliases(participants)) {
            participant = context.getUsersManager().replaceAliasesOccurrences(participant, ClientUsersManager.FindBy.NAME_ALIAS);
            getCallingPage().isOtherParticipantInvisible(participant);
        }
    }

    @When("^I see users? (.*) in ongoing group video call$")
    public void iSeeParticipantsInGroupVideoCall(String participants) {
        for (String participant : context.getUsersManager().splitAliases(participants)) {
            participant = context.getUsersManager().replaceAliasesOccurrences(participant, ClientUsersManager.FindBy.NAME_ALIAS);
            getCallingPage().isOtherParticipantVideoTileVisible(participant);
        }
    }

    @Then("(.*) verif(?:y|ies) that waiting instance status is changed to (.*) in (\\d+) seconds?$")
    public void userVerifiesCallStatusToUserY(String callees, String expectedStatuses, int timeoutSeconds) throws Exception {
        context.startPinging();
        context.getCallingManager()
                .verifyAcceptingCallStatus(
                        context.getUsersManager().splitAliases(callees),
                        expectedStatuses, timeoutSeconds);
        context.stopPinging();
    }

    @Then("Users? (.*) verif(?:y|ies) that call status to (.*) is changed to (.*) in (\\d+) seconds?$")
    public void userXVerifesCallStatusToUserY(String caller,
                                              String conversationName, String expectedStatuses, int timeoutSeconds)
            throws Exception {
        context.getCallingManager().verifyCallingStatus(caller, conversationName,
                expectedStatuses, timeoutSeconds);
    }

    @Then("^Users? (.*) verif(?:ies|y) to send and receive audio$")
    public void userVerifiesAudio(String callees) throws Exception {
        context.getCallingManager().verifySendAndReceiveAudio(callees);
    }

    @Then("^Users? (.*) verif(?:ies|y) to send and receive audio and video$")
    public void userVerifiesAudioAndVideo(String callees) throws Exception {
        context.getCallingManager().verifySendAndReceiveAudioAndVideo(callees);
    }

    @Then("^Users? (.*) verif(?:ies|y) to have CBR connection$")
    public void UserXVerifesHavingCbrConnections(String callees)
            throws Exception {
        context.getCallingManager().verifyCbrConnections(callees);
    }

    @Then("^I see a QR code with (.*) in video stream$")
    public void iSeeQRCodeInVideoStream(String userEmailQRCode) throws Exception {

        BufferedImage actualImage = context.getPage(CallingPage.class).waitUntilVideoGridContainsQRCode();
        assertThat("Remote video is not present", actualImage, notNullValue());

        context.addAdditionalScreenshots(actualImage);
        if (userEmailQRCode.contains("Email")) {
            userEmailQRCode = context.getUsersManager().findUserByEmailOrEmailAlias(userEmailQRCode).getEmail();
        } else if (userEmailQRCode.contains("Name")) {
            userEmailQRCode = context.getUsersManager().findUserByNameOrNameAlias(userEmailQRCode).getName();
        }
        // check qr code in video
        assertThat("QR code in video image does not match", QRCode.readCode(actualImage), equalTo(userEmailQRCode));
    }

    @Then("^I unmute myself$")
    public void iUnmuteMyself() {
        getCallingPage().tapUnmute();
    }

    @When("^I turn camera on$")
    public void iTurnCameraOn() {
        getCallingPage().turnCameraOn();
    }

    @When("Users? (.*) switch(?:es)? video on$")
    public void userSwitchesVideoOn(String callees) throws Exception {
        final List<String> users = context.getUsersManager().splitAliases(callees);
            context.getCallingManager().switchVideoOn(users);
    }

    @When("Users? (.*) (un)?mutes? their microphone$")
    public void userXSwitchesOffMicrophone(String callees, String state) throws InstanceNotFoundException {
        final List<String> users = context.getUsersManager().splitAliases(callees);
        if (state == null) {
            context.getCallingManager().muteMicrophone(users);
            Timedelta.ofMillis(500).sleep();
        } else {
            context.getCallingManager().unmuteMicrophone(users);
        }
    }

    @When("^I minimise the ongoing call$")
    public void minimiseOngoingCall() {
        getCallingPage().minimiseCall();
    }

    @When("^I restore the ongoing call$")
    public void restoreOngoingCall() {
        getCallingPage().restoreCall();
    }

    @When("^I tap hang up button$")
    public void iTapHangUp() {
        getCallingPage().tapHangUp();
    }

    @When("^I see join button in group conversation view$")
    public void iSeeJoinButtonGroupConversation() {
        assertThat("Join button is not visible in conversation view",getCallingPage().isJoinButtonInConversationVisible());
    }

    // Alerts

    @When("^I see alert informing me that this feature is unavailable$")
    public void iSeeFeatureAlert() {
        assertThat("Alert is not visible", getCallingPage().isFeatureUnavailableAlertVisible());
    }

    @When("^I see upgrade to enterprise alert$")
    public void iSeeUpgradeToEnterpriseAlert() {
        assertThat("Alert is not visible", getCallingPage().isUpgradeToEnterpriseAlertVisible());
    }

    @When("^I do not see alert informing me that this feature is unavailable$")
    public void iDoNotSeeFeatureAlert() {
        assertThat("Alert is not visible", getCallingPage().isFeatureUnavailableAlertInvisible());
    }

    @When("^I see subtext of feature unavailable alert containing \"(.*)\"$")
    public void iSeeSubtextFeatureUnavailableAlert(String text) {
        assertThat("Subtext does not contain right information.", getCallingPage().isSubTextFeatureUnavailableAlertVisible(text));
    }

    @When("^I see subtext of upgrade to Enterprise alert containing \"(.*)\"$")
    public void iSeeSubtextUpgradeToEnterpriseAlert(String text) {
        assertThat("Subtext does not contain right information.", getCallingPage().isSubTextUpgradeToEnterpriseAlertVisible(text));
    }

    // Federation

    @Then("^I see federated guest icon for user \"(.*)\" on (incoming|outgoing|ongoing) call overlay$")
    public void iSeeFederatedGuestLabelForCall(String user, String status) {
        assertThat(String.format("Federated Guest label on %s calling overlay for user '%s' is not visible", status, user), getCallingPage().isFederatedLabelVisible());
    }

    @Then("^I do not see federated guest icon for user \"(.*)\" on (incoming|outgoing) call overlay$")
    public void iDoNotSeeFederatedGuestLabelForCall(String user, String status) {
        assertThat(String.format("Federated Guest label on %s calling overlay for user '%s' is not visible.", status, user), getCallingPage().isFederatedLabelInvisible());
    }

    @Then("^I see classified domain label with text \"(.*)\" on (incoming|outgoing|ongoing) call overlay$")
    public void iSeeClassifiedBannerTextForCall(String text, String status) {
        assertThat(String.format("Classified banner on %s calling overlay does not have correct text.", status), getCallingPage().isTextDisplayed(text));
    }

    @Then("^I do not see classified domain label with text \"(.*)\" on (incoming|outgoing|ongoing) call overlay$")
    public void iDoNotSeeClassifiedBannerTextForCall(String text, String status) {
        assertThat(String.format("Classified banner on %s calling overlay does not have correct text.", status), getCallingPage().isTextInvisible(text));
    }
}
