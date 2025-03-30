package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.AndroidPage;
import com.wearezeta.auto.common.backend.Backend;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class CommonBackendSteps {

    private static final Logger log = ZetaLogger.getLog(CommonBackendSteps.class.getSimpleName());

    AndroidTestContext context;

    public CommonBackendSteps(AndroidTestContext context) {
        this.context = context;
    }

    private AndroidPage getCommonPage() {
        return context.getPage(AndroidPage.class);
    }

    // User creation - Personal Users

    @Given("^There (?:is a|are) personal users? (.*)$")
    public void thereAreUsers(String nameAliases) {
        final List<String> userNames = context.getCommonSteps().thereArePersonalUsers(nameAliases).stream()
                .map(ClientUser::getName)
                .collect(Collectors.toList());
        context.getCommonSteps().usersSetUniqueUsername(String.join(",", userNames));
    }

    @Given("^Personal user (.*) sets profile image$")
    public void personalUserSetsProfileImage(String nameAliases) {
        context.getCommonSteps().userChangesUserAvatarPicture(nameAliases);
    }

    @Given("^There (?:is a|are) (\\d+) users? where (.*) is me$")
    public void thereAreNUsersWhereXIsMe(int count, String myNameAlias) throws Exception {
        context.getCommonSteps().thereAreNPersonalUsersWhereXIsMe(count, myNameAlias);
        context.getCommonSteps().userChangesUserAvatarPicture(myNameAlias);
        context.getCommonSteps().usersSetUniqueUsername(myNameAlias);
    }

    @Given("^There (?:is a|are) users? (.*) on (.*) backend$")
    public void thereAreNUsersOnCustomBackend(String nameAliases, String backend) {
        if (backend.equals("custom")) {
            backend = "QA Demo";
        }
        context.getCommonSteps().thereArePersonalUsersOnCustomBackend(nameAliases, backend);
    }

    // User creation - Team Users

    @Given("^There is a team owner \"(.*)\" with team \"(.*)\"$")
    public void thereIsATeamOwner(String userAlias, String teamName) {
        context.getCommonSteps().thereIsATeamOwner(userAlias, teamName, true);
        context.getCommonSteps().upgradeToEnterprisePlanResult(userAlias, teamName);
    }

    @Given("^There is a team owner \"(.*)\" with non paying team \"(.*)\"$")
    public void thereIsATeamOwnerWithNonPayingTeam(String userAlias, String teamName) {
        context.getCommonSteps().thereIsATeamOwner(userAlias, teamName, true);
    }

    @Given("^There is a team owner \"(.*)\" with team \"(.*)\" on (.*) backend$")
    public void thereIsATeamOwnerOnCustomBackend(String userAlias, String teamName, String backendName) {
        Backend backend = BackendConnections.get(backendName);
        context.getCommonSteps().thereIsATeamOwner(userAlias, teamName, backend);
    }

    @Given("^There is a team owner \"(.*)\" with SSO team \"(.*)\" configured for okta$")
    public void thereIsASSOTeamOwnerForOkta(String userAlias, String teamName) {
        context.getCommonSteps().thereIsASSOTeamOwnerForOkta(userAlias, teamName);
    }

    @Given("^There is a QA-Fixed-SSO team owner (.*) with email (.*) and password (.*)$")
    public void thereIsAQAFixedSSOUser(String name, String email, String password) {
        context.getCommonSteps().thereIsAKnownTeamOwner(name, email, password, BackendConnections.get("QA-Fixed-SSO"));
    }

    @Given("^There is a known user (.*) with email (.*) and password (.*)$")
    public void thereIsAKnownUser(String name, String email, String password) {
        context.getCommonSteps().thereIsAKnownUser(name, email, password, BackendConnections.getDefault());
    }

    @Given("^User (.*) adds users? (.*) to team \"(.*)\" with role (Owner|Admin|Member|Partner|External)( and without unique usernames?)?$")
    public void userXAddsUsersToTeam(String userNameAlias, String nameAliases, String teamName, String role,
                                     String hasNoHandle) {
        boolean hasHandle = hasNoHandle == null;
        if(role.equals("External")) {
            role = "Partner";
        }
        context.getCommonSteps().userXAddsUsersToTeam(userNameAlias, nameAliases, teamName, role, hasHandle);
    }

    @Given("^User (.*) removes users? (.*) from team (.*)")
    public void UserXRemovesUsersFromTeam(String userNameAlias, String userAliases, String teamName) {
        context.getCommonSteps().userXRemovesUsersFromTeam(userNameAlias, userAliases, teamName);
    }

    // User creation - SSO Users

    @Given("^User (.*) adds users? (.*) to okta$")
    public void userAddsOktaUser(String ownerAlias, String userNameAliases) {
        context.getCommonSteps().userAddsOktaUser(ownerAlias, userNameAliases);
    }

    @Given("^User (.*) adds users? (.*) to keycloak for E2EI$")
    public void userAddsKeycloakUserForE2EI(String ownerNameAlias, String userNameAliases) {
        context.getCommonSteps().userAddsKeycloakUserForE2EI(ownerNameAlias, userNameAliases);
    }

    @Given("^User (.*) adds users? (.*) to okta and SCIM$")
    public void userAddsUserToOktaAndSCIM(String ownerNameAlias, String userNameAliases) {
        context.getCommonSteps().userAddsUserToOktaAndSCIM(ownerNameAlias, userNameAliases);
    }

    @Given("^User (.*) adds users? (.*) via SCIM$")
    public void userAddsUserViaSCIM(String ownerNameAlias, String userNameAliases) {
        context.getCommonSteps().userAddsUserViaSCIM(ownerNameAlias, userNameAliases);
    }

    @Then("^I print all created users in the execution log$")
    public void IPrintAllCreatedUsers() {
        context.getCommonSteps().printAllCreatedUsers();
    }

    // User creation - Setting Me as a User

    @Given("^User (.*) is [Mm]e$")
    public void UserXIsMe(String nameAlias) {
        context.getCommonSteps().userXIsMe(nameAlias);
        context.getCommonSteps().userChangesUserAvatarPicture(nameAlias);
    }

    @Given("^SSO user (\\w+) is [Mm]e$")
    public void SSOUserXIsMe(String nameAlias) {
        context.getCommonSteps().userXIsMe(nameAlias);
    }

    // Conversations

    @Given("^User (.*) has 1:1 conversation with (.*) in team \"(.*)\"")
    public void UserHas1to1ChatInTeam(String chatOwnerNameAlias, String otherParticipantAlias, String teamName) {
        context.getCommonSteps().userHas1on1ConversationInTeam(chatOwnerNameAlias, otherParticipantAlias, teamName);
    }

    @Given("^User (.*) has group conversation (.*) with (.*) in team \"(.*)\"")
    public void UserHasGroupChatWithContacts(String chatOwnerNameAlias,
                                             String chatName, String otherParticipantsNameAliases, String teamName) {
        context.getCommonSteps().userHasGroupConversationInTeam(chatOwnerNameAlias, chatName, otherParticipantsNameAliases,
                teamName);
    }

    @When("^User (.*) adds user (.*) to group conversation \"(.*)\"$")
    public void UserAddsUserToGroupChat(String user, String userToBeAdded, String group) {
        context.getCommonSteps().userXAddedContactsToGroupChat(user, userToBeAdded, group);
    }

    @Given("^(.*) leave[s]* group conversation (.*)$")
    public void userLeavesGroupChat(String userName, String chatName) {
        context.getCommonSteps().userXLeavesGroupChat(userName, chatName);
    }

    @Given("^User (.*) removes user (.*) from group conversation \"(.*)\"$")
    public void UserXRemovesUserYFromGroup(String userWhoRemoves, String userToBeRemoved, String group) {
        userWhoRemoves = context.getUsersManager().findUserByNameOrNameAlias(userWhoRemoves).getName();
        userToBeRemoved = context.getUsersManager().findUserByNameOrNameAlias(userToBeRemoved).getName();
        group = context.getUsersManager().replaceAliasesOccurrences(group, ClientUsersManager.FindBy.NAME_ALIAS);
        context.getCommonSteps().userXRemoveUserFromGroupConversation(userWhoRemoves, userToBeRemoved, group);
    }

    @When("^Group admin user (.*) deletes conversation (.*)$")
    public void deleteConversation(String userToNameAlias, String dstConversationName) {
        context.getCommonSteps().userXDeletesConversation(userToNameAlias, dstConversationName);
    }

    @When("^User (.*) (archives|unarchives) conversation \"(.*)\"")
    public void archiveConversationWithUser(String userToNameAlias, String action, String dstConvoName) {
        switch (action) {
            case "archives":
                context.getCommonSteps().userSetsArchivedStateForConversation(userToNameAlias, dstConvoName, true);
                break;
            case "unarchives":
                context.getCommonSteps().userSetsArchivedStateForConversation(userToNameAlias, dstConvoName, false);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown action: %s", action));
        }
    }

    // Creating Group conversations personal User

    @Given("^User (.*) has group conversation (.*) with (.*) as a personal user$")
    public void UserHasGroupConversationWithContacts(String chatOwnerNameAlias, String chatName,
                                                     String otherParticipantsNameAliases) {
        context.getCommonSteps()
                .userHasGroupChatWithContacts(chatOwnerNameAlias,
                        chatName, otherParticipantsNameAliases);
    }

    // GuestLinks

    @When("^User (.*) creates invite link for conversation (.*)")
    public void userCreatesInviteLink(String userNameAlias, String conversationName) {
        String inviteLink = context.getCommonSteps().userCreatesInviteLink(userNameAlias, conversationName);
        context.setRememberedInviteLink(inviteLink);
        log.info("Invite link: " + context.getRememberedInviteLink());
    }

    @When("^User (.*) creates invite link with password \"(.*)\" for conversation (.*)")
    public void userCreatesInviteLinkWithPassword(String userNameAlias, String password, String conversationName) {
        String inviteLink = context.getCommonSteps().userCreatesInviteLinkWithPassword(userNameAlias, conversationName, password);
        context.setRememberedInviteLink(inviteLink);
        log.info("Invite link: " + context.getRememberedInviteLink());
    }

    @When("^User (.*) revokes invite link for conversation (.*)")
    public void userRevokesInviteLink(String userNameAlias, String conversationName) {
        context.getCommonSteps().userRevokesInviteLink(userNameAlias, conversationName);
    }

    // Connections

    @Given("^User (.*) is connected to (.*)$")
    public void UserIsConnectedTo(String userFromNameAlias, String usersToNameAliases) {
        System.out.println("userFromAlias: " + userFromNameAlias);
        System.out.println("usersToNameAliases: " + usersToNameAliases);
        context.getCommonSteps().userIsConnectedTo(userFromNameAlias, usersToNameAliases);
    }

    // Services

    @Given("^User (.*) (en|dis)ables (.*) services? for team (.*)$")
    public void userWhitelistsService(String ownerOrAdminAlias, String action, String commaSeparatedServiceAliases,
                                      String teamName) {
        context.getCommonSteps().userSwitchesUsersServicesForTeam(ownerOrAdminAlias,
                action.equals("en"), commaSeparatedServiceAliases, teamName);
    }

    @Given("^User (.*) adds bot (.*) to conversation (.*)$")
    public void userAddsBotToConversation(String userWhoAdds, String botToAdd, String chatName) {
        context.getCommonSteps().userAddsBotToConversation(userWhoAdds, botToAdd, chatName);
    }

    // Conference Calling

    @Given("^TeamOwner \"(.*)\" enables conference calling feature for team (.*) via backdoor$")
    public void TeamOwnerEnablesConferenceCallingViaBackdoor(String alias, String teamName) {
        context.getCommonSteps().enableConferenceCallingFeatureViaBackdoorTeam(alias, teamName);
    }

    // Team Configurations

    @When("^Team Owner (.*) enables forced Self deleting messages for team (.*) with timeout of (\\d+) (seconds?|minutes?|days?|weeks?)$")
    public void userOwnerForceEnablesSelfDeletingMessagesForTeam(String adminUserAlias, String teamName, int timeOut, String timeUnit) {
        long timeoutInSeconds;
        if (timeUnit.startsWith("second")) {
            timeoutInSeconds = timeOut;
        } else if (timeUnit.startsWith("minute")) {
            timeoutInSeconds = Duration.ofMinutes(timeOut).getSeconds();
        } else if (timeUnit.startsWith("day")) {
            timeoutInSeconds = Duration.ofDays(timeOut).getSeconds();
        } else if (timeUnit.startsWith("week")) {
            timeoutInSeconds = Duration.ofDays(timeOut).getSeconds() * 7;
        } else {
            throw new IllegalStateException("Timeout should be defined in seconds, minutes, days or weeks");
        }
        context.getCommonSteps().unlockSelfDeletingMessagesFeature(adminUserAlias, teamName);
        context.getCommonSteps().enableForcedSelfDeletingMessages(adminUserAlias, teamName, timeoutInSeconds);
    }

    @When("^User (.*) disables forced Self deleting messages for team (.*)")
    public void userOwnerDisablesForceSelfDeletingMessagesForTeam(String adminUserAlias, String teamName) {
        context.getCommonSteps().unlockSelfDeletingMessagesFeature(adminUserAlias, teamName);
        context.getCommonSteps().disableForcedSelfDeletingMessages(adminUserAlias, teamName);
    }

    @When("^User (.*) disables File Sharing for team (.*)$")
    public void userOwnerDisablesFileSharingForTeam(String adminUserAlias, String teamName) {
        context.getCommonSteps().unlockFileSharingFeature(adminUserAlias, teamName);
        context.getCommonSteps().disableFileSharingFeature(adminUserAlias, teamName);
    }

    @When("^User (.*) enables File Sharing for team (.*)$")
    public void userOwnerEnablesFileSharingForTeam(String adminUserAlias, String teamName) {
        context.getCommonSteps().enableFileSharingFeature(adminUserAlias, teamName);
    }

    @Given("^User (.*) enables force app lock feature for team (.*) with timeout of (\\d+) seconds$")
    public void userEnablesForceApplock(String adminUserAlias, String teamName, int seconds) {
        //  Note: the amount of seconds must be at least 30
        context.getCommonSteps().enableForceAppLockFeature(adminUserAlias, teamName, seconds);
    }

   // region MLS

    @Given("^User (.*) has MLS conversation \"(.*)\" with (.*)$")
    public void userHasMLSGroupChat(String chatOwnerNameAlias, String chatName, String participantAliases) {
        context.getCommonSteps().userHasMLSGroupConversation(chatOwnerNameAlias, chatName, participantAliases);
    }

    @Given("^Conversation (.*) from user (.*) uses (mls|proteus) protocol$")
    public void getConvoInfo(String chatName, String chatOwnerAlias, String protocol) {
        JSONObject c = context.getCommonSteps().getConversationInfo(chatOwnerAlias, chatName);
        String convoProtocol = c.getJSONArray("found").getJSONObject(0).getString("protocol");
        assertThat("Wrong protocol", convoProtocol, equalTo(protocol));
    }

    @Given("^Admin user (.*) now migrates conversations of team (.*) to MLS via backdoor$")
    public void migrateTeamNowToMLS(String adminUserAlias, String teamName) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS'Z'");
        LocalDateTime currentTime = LocalDateTime.now().minusHours(2);
        String date = currentTime.format(formatter);
        System.out.println("Migration start and finalise time: " + date);
        context.getCommonSteps().migrateTeamToMLS(adminUserAlias, teamName, date, date);
    }

    @Given("^User (.*) configures MLS for team \"(.*)\"$")
    public void enableMLSForTeam(String adminUserAlias, String teamName) {
        context.getCommonSteps().configureMLSForBund(adminUserAlias, teamName);
    }

    @Given("^Admin user (.*) enables MLS for team (.*) and locks state$")
    public void enableMLSForTeamAndLockState(String adminUserAlias, String teamName) {
        context.getCommonSteps().enableMLSFeatureTeam(adminUserAlias, teamName, 1, List.of(1), "mls", List.of("mls", "proteus"), true);
    }

    @Given("^Admin user (.*) disables MLS for team (.*) via backdoor$")
    public void disableMLSForTeam(String adminUserAlias, String teamName) {
        context.getCommonSteps().disableMLSFeatureTeam(adminUserAlias, teamName);
    }

    @When("^Users? (.*) claims? (\\d+) key packages$")
    public void claimKeyPackages(String userAliases, int amount) {
        context.getCommonSteps().claimKeyPackages(userAliases, amount);
    }

    @When("^User (.*) verifies to have (\\d+) remaining key packages$")
    public void checkKeyPackages(String userAlias, int amount) {
        assertThat("Wrong amount of remaining key packages", context.getCommonSteps().getRemainingKeyPackagesCount(userAlias), is(amount));
    }

    // 2FA

    @When("^Admin user (.*) disables 2 Factor Authentication for team (.*)$")
    public void userOwnerDisablesGuestLinksForTeam(String adminUserAlias, String teamName) {
        context.getCommonSteps().disable2FAuthentication(adminUserAlias, teamName);
    }

    @When("^Admin user (.*) unlocks 2F Authentication for team (.*)$")
    public void userOwnerUnlocks2FAuthenticationForTeam(String adminUserAlias, String teamName) {
        context.getCommonSteps().unlock2FAuthentication(adminUserAlias, teamName);
    }

    // TeamSearchVisibility

    @Given("^TeamOwner \"(.*)\" sets the search behaviour for SearchVisibilityInbound to SearchableByAllTeams for team (.*)$")
    public void TeamOwnerEnablesSearchInbound(String alias, String teamName) {
        //SearchableByAllTeams = enabled
        context.getCommonSteps().enableSearchVisibilityInbound(alias, teamName);
    }

    @Given("^TeamOwner \"(.*)\" sets the search behaviour for SearchVisibilityInbound to SearchableByOwnTeam for team (.*)$")
    public void TeamOwnerDisablesSearchInbound(String alias, String teamName) {
        //SearchableByOwnTeam = disabled
        context.getCommonSteps().disableSearchVisibilityInbound(alias, teamName);
    }

    @Given("^TeamOwner \"(.*)\" enables the search behaviour for TeamSearchVisibility for team (.*)$")
    public void TeamOwnerEnablesSearchOutbound(String alias, String teamName) {
        context.getCommonSteps().enableTeamSearchVisibilityOutbound(alias, teamName);
    }

    @Given("^TeamOwner \"(.*)\" sets the search behaviour for TeamSearchVisibility to SearchVisibilityStandard for team (.*)$")
    public void TeamOwnerSetsSearchOutboundSearchVisibilityStandard(String alias, String teamName) {
        context.getCommonSteps().setTeamSearchVisibilityOutboundStandard(alias, teamName);
    }

    @Given("^TeamOwner \"(.*)\" sets the search behaviour for TeamSearchVisibility to SearchVisibilityNoNameOutsideTeam for team (.*)$")
    public void TeamOwnerSetsSearchOutboundSearchVisibilityNoNameOutsideTeam(String alias, String teamName) {
        context.getCommonSteps().setTeamSearchVisibilityOutboundNoNameOutsideTeam(alias, teamName);
    }

    // region E2EI

    @When("^Admin user (.*) enables E2EI with ACME server for team \"(.*)\"$")
    public void enableE2EIForTeam(String adminUserAlias, String teamName) {
        context.getCommonSteps().enableE2EIFeatureTeam(adminUserAlias, teamName);
    }

    @When("^Admin user (.*) enables E2EI with insecure ACME server for team \"(.*)\"$")
    public void enableE2EIForTeamWithInsecureACME(String adminUserAlias, String teamName) {
        context.getCommonSteps().enableE2EIFeatureTeamWithInsecureACME(adminUserAlias, teamName);
    }

    @When("^Admin of (.*) backend revokes remembered certificate on ACME server$")
    public void revokeRememberedCertificate(String backendName) throws CertificateException {
        String pem = context.getRememberedCertificate();
        log.info("PEM: " + pem);
        byte [] decoded = Base64.getDecoder().decode(pem
                .replaceAll("-----BEGIN CERTIFICATE-----", "")
                .replaceAll("-----END CERTIFICATE-----", "")
                .replaceAll("\n", "")
                .replaceAll(" ", "")
                .strip());

        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(decoded));
        String serialNumber = "0x" + certificate.getSerialNumber().toString(16);
        log.info("Serial number (Hex):" + serialNumber);
        log.info("Serial number (Dec):" + certificate.getSerialNumber());
        context.getCommonSteps().revokeCertificate(backendName, serialNumber, certificate.getSerialNumber());
    }

    // Legal Hold

    @When("^User (.*) (un)?registers legal hold service with team \"(.*)\"$")
    public void registerLegalHoldService(String userAlias, String unregister, String teamName) {
        if(unregister == null) {
            context.getCommonSteps().registerLegalHoldService(userAlias, teamName);
        } else {
            context.getCommonSteps().unregisterLegalHoldService(userAlias, teamName);
        }
    }

    @When("^Admin user (.*) sends Legal Hold request for user (.*)$")
    public void adminUserXSendsLegalHoldRequest(String adminUserNameAlias, String userNameAlias) {
        context.getCommonSteps().adminSendsLegalHoldRequestForUser(adminUserNameAlias, userNameAlias);
    }

    @When("^User (.*) accepts pending Legal Hold request$")
    public void userXAcceptsPendingLegalHoldRequest(String userNameAlias) {
        context.getCommonSteps().userAcceptsLegalHoldRequest(userNameAlias);
    }

    @When("^Admin user (.*) turns off Legal Hold for user (.*)$")
    public void adminUserXTurnsOffLegalHold(String adminUserNameAlias, String userNameAlias) {
        context.getCommonSteps().adminTurnsOffLegalHoldForUser(adminUserNameAlias, userNameAlias);
    }

    // region foma

    /*
     * The following 3 Methods can be used during the setup of a test when working with FOMA environment
     * These methods will check for 60 seconds if the needed pods are available and if we can start the testcase.
     */
    @Given("^I wait until the (federator|brig|galley) pod on (.*) is available$")
    public void waitUntilPodIsAvailable(String service, String backendName) throws Exception {
        context.getCommonSteps().waitUntilPodIsAvailable(backendName, service);
    }

    @Given("^I wait until the ingress pod on (.*) is available$")
    public void waitUntilIngressPodIsAvailable(String backendName) throws Exception {
        context.getCommonSteps().waitUntilIngressPodIsAvailable(backendName);
    }

    @Given("^I wait until the SFT pod on (.*) is available$")
    public void waitUntilSFTPodIsAvailable(String backendName) throws Exception {
        context.getCommonSteps().waitUntilSFTPodIsAvailable(backendName);
    }

    /*
     * This Method will turn the Federator for Federated environments on or off.
     * Turning the federator off will disable federation for the selected environment.
     */
    @Given("^Federator for backend (.*) is turned (on|off)$")
    public void turnFederatorforFederatedEnvironmentOnOrOff(String backendName, String status) throws Exception {
        if (status.equals("on")) {
            context.getCommonSteps().turnFederatorInBackendOn(backendName);
            context.getCommonSteps().checkPodsStatusOn(backendName, "federator");
        } else {
            context.getCommonSteps().turnFederatorInBackendOff(backendName);
            context.getCommonSteps().checkPodsStatusOff(backendName, "federator");
        }
    }

    /*
     * This Method will turn Brig for Federated environments on or off.
     * Turning Brig off will disable searching for Users across federated environments.
     * Brig might also break sending messages across federated environments.
     */
    @Given("^Brig for backend (.*) is turned (on|off)$")
    public void turnBrigForFederatedEnvironmentOnOrOff(String backendName, String status) throws Exception {
        if (status.equals("on")) {
            context.getCommonSteps().turnBrigInBackendOn(backendName);
            context.getCommonSteps().checkPodsStatusOn(backendName, "brig");
        } else {
            context.getCommonSteps().turnBrigInBackendOff(backendName);
            context.getCommonSteps().checkPodsStatusOff(backendName, "brig");
        }
    }

    /*
     * This Method will turn Galley for Federated environments on or off.
     * Turning Galley off will disable sending messages across federated environments.
     */
    @Given("^Galley for backend (.*) is turned (on|off)$")
    public void turnGalleyForFederatedEnvironmentOnOrOff(String backendName, String status) throws Exception {
        if (status.equals("on")) {
            context.getCommonSteps().turnGalleyInBackendOn(backendName);
            context.getCommonSteps().checkPodsStatusOn(backendName, "galley");
        } else {
            context.getCommonSteps().turnGalleyInBackendOff(backendName);
            context.getCommonSteps().checkPodsStatusOff(backendName, "galley");
        }
    }

    /*
     * This Method will turn Ingress for Federated environments on or off.
     * Turning Ingress off will make the webapp unavailable and also federation unavailable.
     * Note: Turning Ingress on and off takes some time. Some waiting time is recommended for usage in Testcases.
     */
    @Given("^Ingress for backend (.*) is turned (on|off)$")
    public void turnIngressForFederatedEnvironmentOnOrOff(String backendName, String status) throws Exception {
        if (status.equals("on")) {
            context.getCommonSteps().turnIngressInBackendOn(backendName);
            context.getCommonSteps().checkIngressPodsStatusOn(backendName);
        } else {
            context.getCommonSteps().turnIngressInBackendOff(backendName);
            context.getCommonSteps().checkIngressPodsStatusOff(backendName);
        }
    }

    /*
     * This Method will turn SFT for Federated environments on or off.
     * Turning SFT off will disable SFT Calling between federated environments.
     */
    @Given("^SFT for backend (.*) is turned (on|off)$")
    public void turnSFTForFederatedEnvironmentOnOrOff(String backendName, String status) throws Exception {
        if (status.equals("on")) {
            context.getCommonSteps().turnSFTInBackendOn(backendName);
            context.getCommonSteps().checkSFTPodStatusOn(backendName);
        } else {
            context.getCommonSteps().turnSFTInBackendOff(backendName);
            context.getCommonSteps().checkSFTPodStatusOff(backendName);
        }
    }

    // Changing search configs
    @Then("^The search policy is (.*) with no team level restriction from (.*) backend to (.*) backend$")
    public void searchPolicyCheck(String searchPolicy, String fromBackend, String toBackend) {
        String toDomain = BackendConnections.get(toBackend).getDomain();
        assertThat("Search policy is not correct",
                context.getCommonSteps().getSearchPolicy(fromBackend),
                containsString("\"domain\":\"" + toDomain + "\","
                        + "\"restriction\":{\"tag\":\"allow_all\",\"value\":null},"
                        + "\"search_policy\":\"" + searchPolicy + "\""));
    }

    // Changing federation status

    @When("^Backend (.*) federates with (.*) backend$")
    public void federateBackend(String backendName, String toBackendName) {
        context.getCommonSteps().federateBackends(backendName, toBackendName);
    }

    @When("^Backends (.*) and (.*) both federate$")
    public void federateBothBackends(String backendName1, String backendName2) {
        context.getCommonSteps().federateBackends(backendName1, backendName2);
        context.getCommonSteps().federateBackends(backendName2, backendName1);
    }

    @When("^Backend (.*) defederates with (.*) backend$")
    public void defederateBackend(String backendName, String toBackendName) {
        context.getCommonSteps().defederateBackends(backendName, toBackendName);
    }

    @When("^Backends (.*) and (.*) both defederate$")
    public void defederateBothBackends(String backendName1, String backendName2) {
        context.getCommonSteps().defederateBackends(backendName1, backendName2);
        context.getCommonSteps().defederateBackends(backendName2, backendName1);
    }
}
