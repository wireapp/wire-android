package com.wearezeta.auto.common;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.wearezeta.auto.common.backend.Backend;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.backend.HttpRequestException;
import com.wearezeta.auto.common.backend.models.*;
import com.wearezeta.auto.common.backend.models.Label;
import com.wearezeta.auto.common.credentials.Credentials;
import com.wearezeta.auto.common.email.messages.ActivationMessage;
import com.wearezeta.auto.common.testservice.TestServiceClient;
import com.wearezeta.auto.common.testservice.ReplyHashGenerator;
import com.wearezeta.auto.common.testservice.models.Mention;
import com.wearezeta.auto.common.imagecomparator.QRCode;
import com.wearezeta.auto.common.legalhold.LegalHoldServiceClient;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;
import com.wearezeta.auto.common.misc.URLTransformer;
import com.wearezeta.auto.common.mixpanel.MixPanelAPIClient;
import com.wearezeta.auto.common.mixpanel.MixPanelMockAPIClient;
import com.wearezeta.auto.common.sso.KeycloakAPIClient;
import com.wearezeta.auto.common.sso.OktaAPIClient;
import com.wearezeta.auto.common.service.*;
import com.wearezeta.auto.common.sso.ScimClient;
import com.wearezeta.auto.common.stripe.StripeAPIClient;
import com.wearezeta.auto.common.usrmgmt.*;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager.FindBy;
import org.apache.commons.lang3.BooleanUtils;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.text.ParseException;
import java.time.Duration;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;

public final class CommonSteps {

    public static final Timedelta DEFAULT_WAIT_UNTIL_INTERVAL = Timedelta.ofMillis(1000);
    public static final Timedelta DEFAULT_WAIT_UNTIL_TIMEOUT = Timedelta.ofSeconds(10);
    private static final String DEFAULT_LOCALE = "en_US";
    private static final Timedelta CUSTOMER_WAIT_UNTIL_INTERVAL = Timedelta.ofMillis(5000);
    private static final Timedelta CUSTOMER_WAIT_UNTIL_TIMEOUT = Timedelta.ofSeconds(240);
    public static final String DEFAULT_AUTOMATION_MESSAGE = "1 message";
    private static final Logger log = ZetaLogger.getLog(CommonSteps.class.getSimpleName());
    private static final Timedelta BACKEND_USER_SYNC_TIMEOUT = Timedelta.ofSeconds(180);
    private static final Timedelta PICTURE_CHANGE_TIMEOUT = Timedelta.ofSeconds(15);
    private static final String USER_DETAIL_NOT_SET = "NOT_SET";
    private static final StripeAPIClient stripeAPIClient = new StripeAPIClient();
    private static final MixPanelAPIClient mixPanelClient = new MixPanelMockAPIClient();
    private static final Duration MIXPANEL_TIMEOUT = Duration.ofMinutes(10);
    public static final String WIRE_RECEIPT_MODE = "WIRE_RECEIPT_MODE";
    private static JSONObject lastMixpanelResponse = new JSONObject();
    private static String lastMixpanelDistrictId = "";
    private static String lastMixpanelEvent = "";
    public static final String FIRST_AVAILABLE_DEVICE = null;
    public static final Timedelta NO_EXPIRATION = Timedelta.ofSeconds(0);
    public static String KUBECTLPATH = "/usr/local/bin/";
    static {
        if (System.getenv( "WORKSPACE") != null) {
            KUBECTLPATH =  System.getenv("WORKSPACE") + File.separator;
        }
    }

    private final ClientUsersManager usersManager;
    private TestServiceClient testServiceClient;
    private final OktaAPIClient oktaAPIClient;
    private final KeycloakAPIClient keycloakAPIClient;
    private final ScimClient scimClient;

    private final Map<String, String> profilePictureV3SnapshotsMap = new HashMap<>();
    private final Map<String, String> profilePictureV3PreviewSnapshotsMap = new HashMap<>();
    private final Map<String, Optional<String>> recentMessageIds = new HashMap<>();
    private String identityProviderId;

    private final List<String> customDomains = new ArrayList<>();

    public Map<String, String> defederatedBackends = new HashMap<>();
    public List<String> touchedFederator = new ArrayList<>();
    public List<String> touchedBrig = new ArrayList<>();
    public List<String> touchedGalley = new ArrayList<>();
    public List<String> touchedIngress = new ArrayList<>();
    public List<String> touchedSFT = new ArrayList<>();
    private boolean isOldTestService = false;

    public CommonSteps(ClientUsersManager usersManager) {
        this.usersManager = usersManager;
        this.oktaAPIClient = new OktaAPIClient();
        String defaultBackendName = Config.common().getBackendType(CommonUtils.class);
        this.keycloakAPIClient = new KeycloakAPIClient(defaultBackendName);
        useNewTestService();
        this.scimClient = new ScimClient(defaultBackendName);
    }

    public void cleanUpBackends() {
        log.fine("Clean up push tokens from the backends");
        getUsersManager().getCreatedUsers().parallelStream().forEach((user) -> {
            try {
                Backend backend = BackendConnections.get(user);
                backend.getOtrClients(user).parallelStream().forEach((c) -> {
                    try {
                        backend.removeOtrClient(user, c);
                    } catch (Exception e) {
                        log.fine(String.format("Could not remove client for user %s: %s",
                                user.getName(),
                                e.getMessage()));
                    }
                });
            } catch (Exception e) {
                log.fine("Issue on backend cleanup: " + e.getMessage());
            }
        });
        log.fine("Deactivate LH devices and delete teams from backends");
        getUsersManager().getAllTeamOwners().forEach((user) -> {
            Backend backend = BackendConnections.get(user);
            if (!user.isHarcoded()) {
                if (user.hasRegisteredLegalHoldService()) {
                    try {
                        log.fine("Unregister LH for team with id " + user.getTeamId());
                        backend.unregisterLegalHoldService(user, user.getTeamId());
                    } catch (Exception e) {
                        log.fine("Problem with getting team members: " + e.getMessage());
                    }
                }
                try {
                    for (Team team : backend.getAllTeams(user)) {
                        backend.deleteTeam(user, team);
                    }
                } catch (Exception e) {
                    log.fine(String.format("Error while deleting teams for owner '%s': %s", user.getName(), e.getMessage()));
                }
            } else {
                log.fine("Delete all no-owner members of hardcoded team from backends");
                // If team owner is hardcoded we delete all members
                List<TeamMember> members = backend.getTeamMembers(user);
                String teamId = backend.getAllTeams(user).get(0).getId();
                for (TeamMember member : members) {
                    try {
                        backend.deleteTeamMember(user, teamId, member.getUserId());
                    } catch (Exception e) {
                        log.severe("Could not delete team member " + member.getUserId());
                    }
                }
                log.fine("Logout team owner to remove cookie and avoid being banned");
                backend.logout(user);
            }
        });
        getUsersManager().getAllTeamOwners().clear();
        log.fine("Delete custom domains");
        for (String customDomain : customDomains) {
            deleteCustomBackend(customDomain);
        }
    }

    public static JSONObject createJsonFromMapping(String[][] mappingAsJson) {
        final JSONObject json = new JSONObject();
        Stream.of(mappingAsJson).forEach(e -> json.put(e[0], e[1]));
        return json;
    }

    private ClientUsersManager getUsersManager() {
        return this.usersManager;
    }

    private ClientUser toClientUser(String nameAlias) {
        return getUsersManager().findUserByNameOrNameAlias(nameAlias);
    }

    private String toConvoId(String ownerAlias, String convoName) {
        return toConvoObj(ownerAlias, convoName).getId();
    }

    private String toConvoId(ClientUser owner, String convoName) {
        return toConvoObj(owner, convoName).getId();
    }

    public int getConversationMessageTimer(ClientUser member, String convoName) {
        return toConvoObj(member, convoName).getMessageTimerInMilliseconds();
    }

    private Conversation toConvoObj(String ownerAlias, String convoName) {
        return toConvoObj(toClientUser(ownerAlias), convoName);
    }

    private Conversation toConvoObj(ClientUser owner, String convoName) {
        convoName = getUsersManager().replaceAliasesOccurrences(convoName, ClientUsersManager.FindBy.NAME_ALIAS);
        Backend backend = BackendConnections.get(owner);
        return backend.getConversationByName(owner, convoName);
    }

    public String getTeamId(String adminAlias) {
        final ClientUser adminUser = toClientUser(adminAlias);
        Backend backend = BackendConnections.get(adminUser);
        return backend.getTeamId(adminUser);
    }

    public void connectionRequestIsSentTo(String userFromNameAlias, String usersToNameAliases) {
        ClientUser userFrom = toClientUser(userFromNameAlias);
        Backend backend = BackendConnections.get(userFrom);
        for (String userToNameAlias : getUsersManager().splitAliases(usersToNameAliases)) {
            ClientUser userTo = toClientUser(userToNameAlias);
            backend.sendConnectionRequest(userFrom, userTo);
        }
    }

    public void userAcceptsConnectionRequestFrom(String asUserAlias, String userFromNameAliases) {
        final ClientUser asUser = toClientUser(asUserAlias);
        Backend backend = BackendConnections.get(asUser);
        for (String userFromNameAlias : getUsersManager().splitAliases(userFromNameAliases)) {
            ClientUser userFrom = toClientUser(userFromNameAlias);
            backend.acceptIncomingConnectionRequest(asUser, userFrom);
        }
    }

    public void userIsConnectedTo(String userFromNameAlias, String usersToNameAliases) {
        final ClientUser asUser = toClientUser(userFromNameAlias);
        Backend fromBackend = BackendConnections.get(asUser);
        for (String userToName : getUsersManager().splitAliases(usersToNameAliases)) {
            final ClientUser usrTo = toClientUser(userToName);
            fromBackend.sendConnectionRequest(asUser, usrTo);
            // no need to create new backend object when both users exist on same backend
            if(fromBackend.getBackendName().equals(usrTo.getBackendName())) {
                fromBackend.acceptIncomingConnectionRequest(usrTo, asUser);
            } else {
                // create backend instance for other (federated) backend user
                Backend receivingBackend = BackendConnections.get(usrTo);
                receivingBackend.acceptIncomingConnectionRequest(usrTo, asUser);
            }
        }
    }

    public void userHasGroupChatWithContacts(String chatOwnerNameAlias,
                                             String chatName, String otherParticipantsNameAliases) {
        ClientUser chatOwner = toClientUser(chatOwnerNameAlias);
        Backend backend = BackendConnections.get(chatOwner);
        final List<ClientUser> participants = getUsersManager().splitAliases(otherParticipantsNameAliases)
                .stream()
                .map(this::toClientUser)
                .collect(Collectors.toList());
        backend.createGroupConversation(chatOwner, participants, chatName);
    }

    public void userHasGroupConversationInTeam(String chatOwnerNameAlias, @Nullable String chatName,
                                               @Nullable String otherParticipantsNameAlises, String teamName) {
        List<ClientUser> participants = null;
        ClientUser chatOwner = toClientUser(chatOwnerNameAlias);
        if (otherParticipantsNameAlises != null) {
            participants = getUsersManager()
                    .splitAliases(otherParticipantsNameAlises)
                    .stream()
                    .map(this::toClientUser)
                    .collect(Collectors.toList());
        }
        Backend backend = BackendConnections.get(chatOwner);
        final Team dstTeam = backend.getTeamByName(chatOwner, teamName);
        backend.createTeamConversation(chatOwner, participants, chatName, dstTeam);
    }

    public void userHasMLSGroupConversation(String chatOwnerNameAlias, @Nullable String chatName,
                                            @Nullable String otherParticipantsNameAliases){
        ClientUser chatOwner = toClientUser(chatOwnerNameAlias);
        List<ClientUser> participants = Collections.emptyList();
        if (otherParticipantsNameAliases != null) {
            participants = getUsersManager()
                    .splitAliases(otherParticipantsNameAliases)
                    .stream()
                    .map(this::toClientUser)
                    .collect(Collectors.toList());
        }
        testServiceClient.createConversation(chatOwner, participants, chatName, null);
    }

    public void userHasGuestroom(String chatOwnerNameAlias, @Nullable String chatName, String teamName) {
        ClientUser chatOwner = toClientUser(chatOwnerNameAlias);
        Backend backend = BackendConnections.get(chatOwner);
        final Team dstTeam = backend.getTeamByName(chatOwner, teamName);
        backend.createTeamConversation(chatOwner, Collections.emptyList(), chatName, dstTeam);
    }

    public void userHas1on1ConversationInTeam(String chatOwnerNameAlias, String otherParticipantsNameAlises,
                                              String teamName) {
        ClientUser chatOwner = toClientUser(chatOwnerNameAlias);
        final List<ClientUser> participants = getUsersManager()
                .splitAliases(otherParticipantsNameAlises)
                .stream()
                .map(this::toClientUser)
                .collect(Collectors.toList());
        Backend backend = BackendConnections.get(chatOwner);
        final Team dstTeam = backend.getTeamByName(chatOwner, teamName);
        backend.createTeamConversation(chatOwner, participants, dstTeam);
    }

    public JSONObject getConversationInfo(String chatOwnerNameAlias, String chatName) {
        ClientUser chatOwner = toClientUser(chatOwnerNameAlias);
        Backend backend = BackendConnections.get(chatOwner);
        return backend.getConversationInfo(chatOwner, toConvoObj(chatOwner, chatName));
    }

    public void userXRemoveUserFromGroupConversation(String userWhoRemovesAlias, String userToRemoveAlias, String chatName) {
        ClientUser userWhoRemoves = toClientUser(userWhoRemovesAlias);
        Backend backend = BackendConnections.get(userWhoRemoves);
        backend.removeUserFromGroupConversation(userWhoRemoves, toClientUser(userToRemoveAlias),
                toConvoObj(userWhoRemovesAlias, chatName));
    }

    public void userRemovesWirelessUserFromGroupConversation(String userWhoRemovesAlias, String wirelessName,
                                                             String chatName) {
        ClientUser userWhoRemoves = toClientUser(userWhoRemovesAlias);
        Backend backend = BackendConnections.get(userWhoRemoves);
        Conversation conversation = toConvoObj(userWhoRemoves, chatName);
        for (QualifiedID qualifiedID : conversation.getOtherIds()) {
            if (backend.getUserNameByID(qualifiedID.getDomain(), qualifiedID.getID(), userWhoRemoves).equals(wirelessName)) {
                backend.removeUserIdFromGroupConversation(userWhoRemoves, qualifiedID.getID(), conversation);
            }
        }
    }

    // region Bots

    public void userAddsBotToConversation(String userWhoAddsAlias, String botToAdd, String chatName) {
        ClientUser userWhoAdds = toClientUser(userWhoAddsAlias);
        Conversation convoObj = toConvoObj(userWhoAdds, chatName);
        BackendConnections.get(userWhoAdds).addServiceToConversation(userWhoAdds, botToAdd, convoObj);
    }

    public void userRegistersRandomServiceProvider(String userAlias) {
        ClientUser user = toClientUser(userAlias);
        BackendConnections.get(user).createNewServiceProvider(user, "test", "https://example.com", "bla");
        BackendConnections.get(user).activateServiceProvider(user);
    }

    // endregion Bots

    public void userSetsReadReceiptToConversation(String user, String chatName, boolean newState) {
        ClientUser userWhoSets = toClientUser(user);
        Conversation conversationObject = toConvoObj(userWhoSets, chatName);
        BackendConnections.get(userWhoSets).setReadReceiptToConversation(userWhoSets, conversationObject, newState);
    }

    public void thereIsAKnownUser(String name, String email, String password, Backend backend) {
        ClientUser user = new ClientUser();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setHardcoded(true);
        user.setBackendName(backend.getBackendName());
        getUsersManager().appendCustomUser(user);
    }

    public void thereIsAKnownTeamOwner(String name, String email, String password, Backend backend) {
        ClientUser user = new ClientUser();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setHardcoded(true);
        user.setBackendName(backend.getBackendName());
        user.setTeamOwner(true);
        getUsersManager().appendCustomUser(user);
    }

    public List<ClientUser> thereArePersonalUsers(String nameAliases) {
        return getUsersManager().createPersonalUsersByAliases(getUsersManager().splitAliases(nameAliases),
                BackendConnections.getDefault());
    }

    public void thereIsATeamOwner(String ownerNameAlias, String teamName) {
        thereIsATeamOwner(ownerNameAlias, teamName, DEFAULT_LOCALE, true, BackendConnections.getDefault());
    }

    public void thereIsATeamOwner(String ownerNameAlias, String teamName, Backend backend) {
        thereIsATeamOwner(ownerNameAlias, teamName, DEFAULT_LOCALE, true, backend);
    }

    public void thereIsATeamOwner(String ownerNameAlias, String teamName, Boolean updateHandle) {
        thereIsATeamOwner(ownerNameAlias, teamName, DEFAULT_LOCALE, updateHandle, BackendConnections.getDefault());
    }

    public void thereIsATeamOwner(String ownerNameAlias, String teamName, String locale, boolean updateHandle, Backend backend) {
        final ClientUser owner = toClientUser(ownerNameAlias);
        if (getUsersManager().isUserCreated(owner)) {
            throw new UserAlreadyCreatedException(
                    String.format("Cannot create team with user %s as owner because user is already created",
                            owner.getNameAliases()));
        }
        getUsersManager().createTeamOwnerByAlias(ownerNameAlias, teamName, locale, updateHandle, backend);
    }

    public void thereAreNPersonalUsersWhereXIsMe(int count, String myNameAlias) {
        getUsersManager().createXPersonalUsers(count, BackendConnections.getDefault());
        getUsersManager().setSelfUser(toClientUser(myNameAlias));
    }

    public List<ClientUser> thereArePersonalUsersOnCustomBackend(String nameAliases, String backendName) {
        Backend backend = BackendConnections.get(backendName);
        return getUsersManager().createPersonalUsersByAliases(getUsersManager().splitAliases(nameAliases), backend);
    }

    public List<String> generateUnactivatedEmails(int amountToGenerate) {
        return getUsersManager().generateUnactivatedMails(amountToGenerate);
    }

    public void cancelAllOutgoingConnectRequests(String userToNameAlias) {
        ClientUser user = toClientUser(userToNameAlias);
        BackendConnections.get(user).cancelAllOutgoingConnections(user);
    }

    public void blockContact(String blockAsUserNameAlias,
                             String userToBlockNameAlias) {
        ClientUser blockAsUser = toClientUser(blockAsUserNameAlias);
        ClientUser userToBlock = toClientUser(userToBlockNameAlias);
        Backend backend = BackendConnections.get(blockAsUser);
        try {
            backend.sendConnectionRequest(blockAsUser, userToBlock);
        } catch (HttpRequestException e) {
            log.info(String.format("Failed to send connection request from %s to %s",
                    blockAsUser.getName(),
                    userToBlock.getName()));
        }
        backend.changeConnectRequestStatus(blockAsUser, userToBlock.getId(), BackendConnections.get(userToBlock).getDomain(), ConnectionStatus.Blocked);
    }

    public void userChangesGroupChatName(String ownerAlias, String conversationToRename, String newConversationName) {
        ClientUser owner = toClientUser(ownerAlias);
        BackendConnections.get(owner).changeConversationName(owner, toConvoObj(ownerAlias, conversationToRename),
                newConversationName);
    }

    public void acceptAllIncomingConnectionRequests(String userToNameAlias) {
        ClientUser user = toClientUser(userToNameAlias);
        BackendConnections.get(user).acceptAllIncomingConnectionRequests(user);
    }

    public void userVerifiesEmail(String user, String expectedValue) {
        final ClientUser dstUser = toClientUser(user);
        Backend backend = BackendConnections.get(dstUser);
        expectedValue = getUsersManager().replaceAliasesOccurrences(expectedValue, FindBy.EMAIL_ALIAS);
        assertThat(backend.getEmail(dstUser).orElse(USER_DETAIL_NOT_SET),
                equalTo(expectedValue));
    }

    public void userSetsMuteStatusForConversation(String userNameAlias, String dstConversationName, MuteState muteState) {
        ClientUser user = toClientUser(userNameAlias);
        BackendConnections.get(user).setMuteStateForConversation(user, toConvoObj(userNameAlias, dstConversationName),
                muteState);
    }

    public void userSetsArchivedStateForConversation(String senderAlias, String convoName, boolean isArchived) {
        ClientUser sender = toClientUser(senderAlias);
        Backend backend = BackendConnections.get(sender);
        backend.setArchivedStateForConversation(sender, toConvoObj(senderAlias, convoName), isArchived);
    }

    @Deprecated // This should be replaced by a method containing or generating image (IChangeUserAvatarPictureWithQR)
    public void userChangesUserAvatarPicture(String userNameAlias) {
        ClientUser user = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(user);
        final String DEFAULT_TEAM_AVATAR = "images/default_team_avatar.jpg";
        try (InputStream is = CommonSteps.class.getClassLoader().getResourceAsStream(DEFAULT_TEAM_AVATAR)) {
            backend.updateUserPictureWithIS(toClientUser(userNameAlias), is);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void userChangesUserAvatarPicture(String userNameAlias, String picturePath) {
        ClientUser user = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(user);
        backend.updateUserPicture(user, new File(picturePath));
    }

    public void userChangesUserAvatarPictureWithQR(String userNameAlias, String code) {
        ClientUser user = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(user);
        BufferedImage picture = QRCode.generateCode(code, Color.BLACK, Color.YELLOW, 640, 5);
        backend.updateUserPicture(user, picture);
    }

    public void userChangesUserAvatarWithColor(String userNameAlias, Color color) {
        BufferedImage image = new BufferedImage(300, 300, TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(color);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        ClientUser user = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(user);
        backend.updateUserPicture(toClientUser(userNameAlias), image);
    }

    public void userDeletesAvatarPicture(String userNameAlias) {
        ClientUser user = toClientUser(userNameAlias);
        BackendConnections.get(user).removeUserPicture(user);
    }

    public void userChangesName(String userNameAlias, String newName) {
        ClientUser user = toClientUser(userNameAlias);
        BackendConnections.get(user).updateName(user, newName);
    }

    public void userChangesUniqueUsername(String userNameAlias, String name) {
        name = getUsersManager().replaceAliasesOccurrences(name, FindBy.NAME_ALIAS);
        ClientUser user = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(user);
        backend.updateUniqueUsername(user, name);
    }

    public void userChangesUniqueUsernameWithLeadingChars(String userNameAlias, String leadingChars) {
        ClientUser user = getUsersManager().findUserByNameOrNameAlias(userNameAlias);
        userChangesUniqueUsername(userNameAlias, leadingChars + user.getUniqueUsername());
    }

    public void usersSetUniqueUsername(String userNameAliases) {
        for (String userNameAlias : getUsersManager().splitAliases(userNameAliases)) {
            final ClientUser user = toClientUser(userNameAlias);
            Backend backend = BackendConnections.get(user);
            backend.updateUniqueUsername(user, user.getUniqueUsername());
        }
    }

    public void userChangesAccentColor(String userNameAlias, String colorName) {
        ClientUser user = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(user);
        backend.updateUserAccentColor(user, AccentColor.getByName(colorName));
    }

    public AccentColor getAccentColor(String userNameAlias) {
        ClientUser user = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(user);
        return backend.getUserAccentColor(user);
    }

    public void accessProfile(String userNameAlias, String userId) {
        ClientUser user = toClientUser(userNameAlias);
        BackendConnections.get(user).getUserNameByID(BackendConnections.get(user).getDomain(), userId, user);
    }

    public void userXIsMe(String nameAlias) {
        getUsersManager().setSelfUser(toClientUser(nameAlias));
    }

    public void waitUntilContactIsFoundInSearch(String searchByNameAlias,
                                                String contactAlias) {
        final String query = getUsersManager().replaceAliasesOccurrences(contactAlias, FindBy.NAME_ALIAS,
                FindBy.EMAIL_ALIAS, FindBy.UNIQUE_USERNAME_ALIAS);
        ClientUser searcher = toClientUser(searchByNameAlias);
        BackendConnections.get(searcher).waitUntilContactsFound(searcher, query,
                1, true, BACKEND_USER_SYNC_TIMEOUT);
    }

    public boolean doesUserExistForUserOnBackend(String searchByNameAlias, String contactAlias) {
        final String query = getUsersManager().replaceAliasesOccurrences(contactAlias, FindBy.NAME_ALIAS,
                FindBy.EMAIL_ALIAS, FindBy.UNIQUE_USERNAME_ALIAS);
        ClientUser searcher = toClientUser(searchByNameAlias);
        return BackendConnections.get(searcher).doesUserExistForUser(searcher, query);
    }

    public void waitUntilContactIsFoundInSearchByUniqueUsername(String searchByNameAlias, String contactAlias) {
        ClientUser searcher = toClientUser(searchByNameAlias);
        BackendConnections.get(searcher).waitUntilContactsFound(searcher, toClientUser(contactAlias).getUniqueUsername(),
                1, true, BACKEND_USER_SYNC_TIMEOUT);
    }

    public void userXAddedContactsToGroupChat(String userAsNameAlias,
                                              String contactsToAddNameAliases, String chatName) {
        final ClientUser userAs = toClientUser(userAsNameAlias);
        List<ClientUser> contactsToAdd = getUsersManager()
                .splitAliases(contactsToAddNameAliases)
                .stream()
                .map(this::toClientUser)
                .collect(Collectors.toList());
        BackendConnections.get(userAs).addUsersToGroupConversation(userAs, contactsToAdd, toConvoObj(userAs, chatName));
    }

    public void userXLeavesGroupChat(String userNameAlias, String chatName) {
        final ClientUser userAs = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(userAs);
        backend.removeUserFromGroupConversation(userAs, userAs, toConvoObj(userAs, chatName));
    }

    public void userXTakesSnapshotOfProfilePicture(String userNameAlias) {
        final ClientUser userAs = toClientUser(userNameAlias);
        String email = userAs.getEmail();
        profilePictureV3SnapshotsMap.put(email, BackendConnections.get(userAs).getUserAssetKey(userAs,
                Backend.PROFILE_PICTURE_JSON_ATTRIBUTE));
        profilePictureV3PreviewSnapshotsMap.put(email, BackendConnections.get(userAs).getUserAssetKey(userAs,
                Backend.PROFILE_PREVIEW_PICTURE_JSON_ATTRIBUTE));
    }

    public void userXVerifiesSnapshotOfProfilePictureIsDifferent(String userNameAlias) {
        final ClientUser userAs = toClientUser(userNameAlias);
        String email = userAs.getEmail();
        String previousCompleteKey, previousPreviewKey;
        if (profilePictureV3SnapshotsMap.containsKey(email) && profilePictureV3PreviewSnapshotsMap.containsKey(email)) {
            previousCompleteKey = profilePictureV3SnapshotsMap.get(email);
            previousPreviewKey = profilePictureV3PreviewSnapshotsMap.get(email);
        } else {
            throw new IllegalStateException(String.format("Please take user picture snapshot for user '%s' first",
                    userAs.getEmail()));
        }
        final Timedelta started = Timedelta.now();
        String actualCompleteKey, actualPreviewKey;
        do {
            actualCompleteKey = BackendConnections.get(userAs)
                    .getUserAssetKey(userAs, Backend.PROFILE_PICTURE_JSON_ATTRIBUTE);
            actualPreviewKey = BackendConnections.get(userAs)
                    .getUserAssetKey(userAs, Backend.PROFILE_PREVIEW_PICTURE_JSON_ATTRIBUTE);
            if (!actualCompleteKey.equals(previousCompleteKey) && !actualPreviewKey.equals(previousPreviewKey)) {
                break;
            }
            Timedelta.ofMillis(500).sleep();
        } while (Timedelta.now().isDiffLessOrEqual(started, PICTURE_CHANGE_TIMEOUT));
        assertThat("User big profile picture is not different (V3)", actualCompleteKey,
                not(equalTo(previousCompleteKey)));
        assertThat("User small profile picture is not different (V3)", actualPreviewKey,
                not(equalTo(previousPreviewKey)));
    }

    public void userRemovesAllRegisteredOtrClients(String userAlias) {
        final ClientUser usr = toClientUser(userAlias);
        Backend backend = BackendConnections.get(usr);
        final List<OtrClient> allOtrClients = backend.getOtrClients(usr);
        for (OtrClient c : allOtrClients) {
            backend.removeOtrClient(usr, c);
        }
    }

    public void userKeepsXOtrClients(String userAs, int clientsCountToKeep) {
        final ClientUser usr = toClientUser(userAs);
        Backend backend = BackendConnections.get(usr);
        final List<OtrClient> allOtrClients = backend.getOtrClients(usr);
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        final String defaultDateStr = "2016-01-01T12:00:00.000Z";
        // Newly registered clients coming first
        allOtrClients.sort((c1, c2)
                -> formatter.parseDateTime(c2.getTime().orElse(defaultDateStr)).compareTo(
                formatter.parseDateTime(c1.getTime().orElse(defaultDateStr))
        ));
        log.fine(String.format("Clients considered for removal %s", allOtrClients));
        if (allOtrClients.size() > clientsCountToKeep) {
            for (OtrClient c : allOtrClients.subList(clientsCountToKeep, allOtrClients.size())) {
                log.fine(String.format("Removing client with ID %s", c.getId()));
                try {
                    backend.removeOtrClient(usr, c);
                } catch (HttpRequestException e) {
                    if (e.getReturnCode() == 404) {
                        // To avoid multithreading issues
                        e.printStackTrace();
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    public void userRemovesSpecificOtrClientByModel(String userAs, String model) {
        final ClientUser usr = toClientUser(userAs);
        Backend backend = BackendConnections.get(usr);
        final List<OtrClient> allOtrClients = backend.getOtrClients(usr);
        for (OtrClient c : allOtrClients) {
            if (c.getModel().toString().contains(model)) {
                try {
                    backend.removeOtrClient(usr, c);
                } catch (HttpRequestException e) {
                    if (e.getReturnCode() == 404) {
                        // To avoid multithreading issues
                        log.warning("Removing otr client might have failed: " + e.getMessage());
                        e.printStackTrace();
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    public boolean isUserAbleToLogin(String userNameAlias) {
        final ClientUser user = toClientUser(userNameAlias);
        return BackendConnections.get(user).isLoginPossible(user.getEmail(), user.getPassword());
    }

    public void userResetsPassword(String nameAlias, String newPassword) {
        ClientUser user = toClientUser(nameAlias);
        BackendConnections.get(user).changeUserPassword(user, user.getPassword(), newPassword);
    }

    public void userTriggersEmailChange(String nameAlias, String newEmail) {
        String email = getUsersManager().replaceAliasesOccurrences(newEmail, FindBy.EMAIL_ALIAS);
        ClientUser user = toClientUser(nameAlias);
        BackendConnections.get(user).triggerUserEmailChange(user, email);
    }

    public void userChangesEmail(String nameAlias, String newEmail) {
        String email = getUsersManager().replaceAliasesOccurrences(newEmail, FindBy.EMAIL_ALIAS);
        ClientUser user = toClientUser(nameAlias);
        BackendConnections.get(user).changeUserEmail(toClientUser(nameAlias), email);
    }

    public void activateRegisteredUserByEmail(Future<String> activationMessage) throws Exception {
        ActivationMessage verificationInfo = new ActivationMessage(activationMessage.get());
        final String key = verificationInfo.getXZetaKey();
        final String code = verificationInfo.getXZetaCode();
        log.fine(String.format("Received activation email message with key: %s, code: %s. Proceeding with activation...",
                key, code));
        BackendConnections.getDefault().activateEmailAfterEmailChange(key, code);
        log.fine("User is successfully activated");
    }

    private String generateConversationKey(String userFrom, String dstName, String deviceName) {
        return String.format("%s:%s:%s", getUsersManager().replaceAliasesOccurrences(userFrom,
                        ClientUsersManager.FindBy.NAME_ALIAS),
                getUsersManager().replaceAliasesOccurrences(dstName, ClientUsersManager.FindBy.NAME_ALIAS), deviceName);
    }

    public void userXDeletesTeam(String ownerNameAlias, String teamName) {
        final ClientUser owner = toClientUser(ownerNameAlias);
        Backend backend = BackendConnections.get(owner);
        final Team dstTeam = backend.getTeamByName(owner, teamName);
        backend.deleteTeam(owner, dstTeam);
    }

    public void userDeletesThemself(String nameAlias) {
        ClientUser user = toClientUser(nameAlias);
        Backend backend = BackendConnections.get(user);
        backend.deleteUser(user);
    }

    public void userTriggersDeleteEmail(String nameAlias) {
        ClientUser user = toClientUser(nameAlias);
        Backend backend = BackendConnections.get(user);
        backend.triggerDeleteEmail(user);
    }

    public void userAllowsGuestsInConversation(String userNameAlias, String conversationName) {
        ClientUser asUser = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(asUser);
        backend.allowGuests(asUser, toConvoObj(asUser, conversationName));
    }

    public void userDisallowsGuestsInConversation(String userNameAlias, String conversationName) {
        ClientUser asUser = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(asUser);
        backend.disallowGuest(asUser, toConvoObj(asUser, conversationName));
    }

    public void userAllowsServicesInConversation(String userNameAlias, String conversationName) {
        ClientUser asUser = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(asUser);
        backend.allowServices(asUser, toConvoObj(asUser, conversationName));
    }

    public void userDisallowsServicesInConversation(String userNameAlias, String conversationName) {
        ClientUser asUser = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(asUser);
        backend.disallowServices(asUser, toConvoObj(asUser, conversationName));
    }

    public void userSetsMessageTimerInConversation(String userNameAlias, String conversationName, Timedelta msgTimer) {
        ClientUser asUser = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(asUser);
        backend.setConversationMessageTimer(asUser, toConvoObj(asUser, conversationName), msgTimer);
    }

    public String userCreatesInviteLink(String userNameAlias, String conversationName) {
        final ClientUser asUser = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(asUser);
        return backend.createInviteLink(asUser, toConvoObj(asUser, conversationName));
    }

    public String userCreatesInviteLinkWithPassword(String userNameAlias, String conversationName, String password) {
        final ClientUser asUser = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(asUser);
        return backend.createInviteLinkWithPassword(asUser, toConvoObj(asUser, conversationName), password);
    }

    public String userCreatesJoinConversationPath(String userNameAlias, String conversationName) {
        final ClientUser asUser = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(asUser);
        return backend.getJoinConversationPath(asUser, toConvoObj(asUser, conversationName));
    }

    public String userCreatesInviteDeeplink(String userNameAlias, String conversationName) {
        final ClientUser asUser = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(asUser);
        String url = backend.createInviteLink(asUser, toConvoObj(asUser, conversationName));
        String query = URLTransformer.getQuery(url);
        return String.format("wire://conversation-join?%s&domain=staging.zinfra.io", query);
    }

    public String getInviteLinkOfConversation(String userNameAlias, String conversationName) {
        final ClientUser asUser = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(asUser);
        return backend.getInviteLink(asUser, toConvoObj(asUser, conversationName));
    }

    public String getClientDeepLinkForPublicConversation(String userNameAlias, String conversationName) {
        String inviteLink = getInviteLinkOfConversation(userNameAlias, conversationName);
        return String.format("wire://%s", inviteLink.substring(inviteLink.indexOf("conversation-join")));
    }

    public String getClientLinkForPublicConversation(String userNameAlias, String conversationName) {
        String inviteLink = getInviteLinkOfConversation(userNameAlias, conversationName);
        return String.format("https://wire-account-staging.zinfra.io/%s", inviteLink.substring(inviteLink.indexOf("conversation-join")));
    }

    public String getUserProfileLink(String userNameAlias) {
        final ClientUser user = toClientUser(userNameAlias);
        return String.format("%suser-profile/?id=%s", Config.current().getAccountPages(this.getClass()), user.getId());
    }

    public void userRevokesInviteLink(String userNameAlias, String conversationName) {
        final ClientUser asUser = toClientUser(userNameAlias);
        BackendConnections.get(asUser).revokeInviteLink(asUser, toConvoObj(asUser, conversationName));
    }

    public void userInvitesWirelessUsers(String userNameAlias, String userNameAliases, String conversationName) {
        // if expiresInSeconds is set to null, the default expiration from the backend is used
        userInvitesWirelessUsers(userNameAlias, userNameAliases, null, conversationName);
    }

    public void userInvitesWirelessUsers(String userNameAlias, String userNameAliases, Duration expiresIn,
                                         String conversationName) {
        final ClientUser inviter = toClientUser(userNameAlias);
        List<ClientUser> usersToBeAdded = getUsersManager().splitAliases(userNameAliases)
                .stream()
                .map(alias -> {
                    ClientUser user = toClientUser(alias);
                    user.setExpiresIn(expiresIn);
                    if (getUsersManager().isUserCreated(user)) {
                        throw new UserAlreadyCreatedException(
                                String.format("Cannot create wireless user with alias %s because user is already created",
                                        userNameAlias));
                    }
                    return user;
                }).collect(Collectors.toList());
        final List<ClientUser> users = getUsersManager().createWirelessUsers(usersToBeAdded, BackendConnections.getDefault());
        BackendConnections.get(inviter).inviteUsersViaLink(inviter, users, toConvoObj(inviter, conversationName));
    }

    public int getResponseCodeWhenWirelessUserJoinsConversation(String userNameAlias, String inviteLink) throws IOException {
        final ClientUser wirelessUser = toClientUser(userNameAlias);
        final List<ClientUser> users = getUsersManager().createWirelessUsers(Collections.singletonList(wirelessUser),
                BackendConnections.getDefault());
        String key = URLTransformer.getQueryParameter(inviteLink, "key");
        String code = URLTransformer.getQueryParameter(inviteLink, "code");
        return BackendConnections.get(users.get(0)).getJoinConversationResponseCode(users.get(0), key, code);
    }

    public int getWirelessLinkResponseCode(String userNameAlias, String conversationName) throws IOException {
        final ClientUser user = toClientUser(userNameAlias);
        return BackendConnections.get(user).getWirelessLinkResponseCode(user, toConvoObj(user, conversationName));
    }

    public void userXAddsUsersToTeam(String ownerNameAlias, String userNameAliases, String teamName, String role,
                                     boolean membersHaveHandles) {
        final ClientUser admin = toClientUser(ownerNameAlias);
        Backend backend = BackendConnections.get(admin);
        final Team dstTeam = backend.getTeamByName(admin, teamName);
        List<ClientUser> membersToBeAdded = new ArrayList<>();
        final List<String> aliases = getUsersManager().splitAliases(userNameAliases);
        for (String userNameAlias : aliases) {
            ClientUser user = toClientUser(userNameAlias);
            if (getUsersManager().isUserCreated(user)) {
                throw new UserAlreadyCreatedException(
                        String.format("Cannot add user with alias %s to team because user is already created",
                                userNameAlias));
            }
            membersToBeAdded.add(user);
        }
        getUsersManager().createTeamMembers(admin, dstTeam.getId(), membersToBeAdded, membersHaveHandles, role.toLowerCase(),
                backend);
    }

    public void userXAddsUsersToTeamIfNotAlreadyAdded(String ownerNameAlias, String userNameAliases, String teamName, String role) {
        final ClientUser admin = toClientUser(ownerNameAlias);
        Backend backend = BackendConnections.get(admin);
        final Team dstTeam = backend.getTeamByName(admin, teamName);
        List<ClientUser> membersToBeAdded = new ArrayList<>();
        final List<String> aliases = getUsersManager().splitAliases(userNameAliases);
        for (String userNameAlias : aliases) {
//          VERIFY IF USER EXISTS OR NOT, if it does not, add user
            if (!doesUserExistForUserOnBackend(ownerNameAlias, userNameAlias)) {
                ClientUser user = toClientUser(userNameAlias);
                membersToBeAdded.add(user);
            } else {
                log.info(String.format("User %s already exists", userNameAlias));
            }
        }
        if (membersToBeAdded.size() > 0) {
            getUsersManager().createTeamMembers(admin, dstTeam.getId(), membersToBeAdded, false, role.toLowerCase(),
                    backend);
        }
    }

    public void userXRenamesTeam(String userAlias, String newTeamName) {
        final ClientUser admin = toClientUser(userAlias);
        Backend backend = BackendConnections.get(admin);
        backend.renameTeam(admin, newTeamName);
    }

    public void userXChangesRoles(String adminAlias, String membersAliases, String role, String teamName) {
        final ClientUser admin = toClientUser(adminAlias);
        final List<String> memberAliases = getUsersManager().splitAliases(membersAliases);
        Backend backend = BackendConnections.get(admin);
        final Team dstTeam = backend.getTeamByName(admin, teamName);
        final TeamRole teamRole = TeamRole.getByName(role);
        for (String alias : memberAliases) {
            final ClientUser member = toClientUser(alias);
            backend.editTeamMember(admin, dstTeam, member, teamRole);
        }
    }

    public void userXRemovesUsersFromTeam(String adminAlias, String membersAliases, String teamName) {
        final ClientUser admin = toClientUser(adminAlias);
        Backend backend = BackendConnections.get(admin);
        final Team dstTeam = backend.getTeamByName(admin, teamName);
        for (String memberAlias : getUsersManager().splitAliases(membersAliases)) {
            final ClientUser memberToRemove = toClientUser(memberAlias);
            backend.deleteTeamMember(admin, dstTeam.getId(), memberToRemove.getId());
        }
    }

    public void userXRemovesUsersFromTeamIfTheyExist(String adminAlias, String membersAliases, String teamName) {
        final ClientUser admin = toClientUser(adminAlias);
        Backend backend = BackendConnections.get(admin);
        final Team dstTeam = backend.getTeamByName(admin, teamName);
        List<TeamMember> allMembers = backend.getTeamMembers(admin);
        for (String memberAlias : getUsersManager().splitAliases(membersAliases)) {
            for (TeamMember nextMember : allMembers) {
                String userId = nextMember.getUserId();
                if (memberAlias.equals(backend.getUserNameByID(backend.getDomain(), userId, admin))) {
                    backend.deleteTeamMember(admin, dstTeam.getId(), userId);
                }
            }
        }
    }

    public void userXSendsInvitationMailToMember(String ownerAlias, String inviteeMail, String teamName, String role) {
        final ClientUser owner = toClientUser(ownerAlias);
        Backend backend = BackendConnections.get(owner);
        final Team dstTeam = backend.getTeamByName(owner, teamName);
        backend.onlySendEmailInvitationToMember(owner, dstTeam.getId(), inviteeMail, role);
    }

    public void userXSendsInvitationMailsToMembers(String ownerAlias, int numberOfInvites, String teamName, String role) {
        final ClientUser owner = toClientUser(ownerAlias);
        Backend backend = BackendConnections.get(owner);
        final Team dstTeam = backend.getTeamByName(owner, teamName);
        for (String email : generateUnactivatedEmails(numberOfInvites)) {
            backend.onlySendEmailInvitationToMember(owner, dstTeam.getId(), email, role);
        }
    }

    public void VerifyUserXIsInTeam(String userAlias, String teamName, boolean shouldBeMember) {
        final ClientUser user = toClientUser(userAlias);
        final List<Team> teams = BackendConnections.get(user).getAllTeams(user);
        final boolean isMember = teams.stream().anyMatch(x -> x.getName().equalsIgnoreCase(teamName));
        if (shouldBeMember) {
            assertThat(String.format("User '%s' should be a member of '%s' team", user.getName(), teamName),
                    equalTo(isMember));
        } else {
            assertThat(String.format("User '%s' should not be a member of '%s' team", user.getName(), teamName),
                    not(equalTo(isMember)));
        }
    }

    public void UserXSuspendsTeam(String userAlias, String teamName) {
        final ClientUser admin = toClientUser(userAlias);
        Backend backend = BackendConnections.get(admin);
        final Team dstTeam = backend.getTeamByName(admin, teamName);
        backend.suspendTeam(dstTeam);
    }

    public void userXRenamesTeam(String userAlias, String oldTeamName, String newTeamName) {
        final ClientUser admin = toClientUser(userAlias);
        Backend backend = BackendConnections.get(admin);
        final Team dstTeam = backend.getTeamByName(admin, oldTeamName);
        backend.renameTeam(admin, dstTeam, newTeamName);
    }

    public void UserXUpdatesTeamIcon(String userAlias, String teamName, Color color) {
        final ClientUser admin = toClientUser(userAlias);
        Backend backend = BackendConnections.get(admin);
        final Team dstTeam = backend.getTeamByName(admin, teamName);
        BufferedImage image = new BufferedImage(300, 300, TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(color);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        backend.updateTeamIcon(admin, dstTeam, image);
    }

    public void UserXResetsTeamIcon(String userAlias, String teamName) {
        final ClientUser admin = toClientUser(userAlias);
        Backend backend = BackendConnections.get(admin);
        final Team dstTeam = backend.getTeamByName(admin, teamName);
        backend.resetTeamIcon(admin, dstTeam);
    }

    public void UserXRevokesEmailInvitesForTeam(String userAlias, String teamName) {
        final ClientUser asUser = toClientUser(userAlias);
        Backend backend = BackendConnections.get(asUser);
        final Team dstTeam = backend.getTeamByName(asUser, teamName);
        backend.revokeTeamInvitationsForTeam(asUser, dstTeam);
    }

    public TeamRole getTeamRole(String userAlias) {
        ClientUser user = toClientUser(userAlias);
        Backend backend = BackendConnections.get(user);
        return backend.getTeamRole(user);
    }

    public int getTeamLimit() {
        return 2000;
    }

    public int getPendingInvitationsLimit() {
        return 2000;
    }

    public List<String> getPendingInvitations(String userNameAlias) {
        ClientUser user = toClientUser(userNameAlias);
        return BackendConnections.get(user).getPendingTeamInvitations(user);
    }

    public List<String> getAllPendingInvitations(String userNameAlias) {
        ClientUser user = toClientUser(userNameAlias);
        return BackendConnections.get(user).getAllPendingTeamInvitations(user);
    }

    public void userDetachesEmail(String userNameAlias) {
        // if expiresInSeconds is set to null, the default expiration from the backend is used
        ClientUser user = toClientUser(userNameAlias);
        BackendConnections.get(user).detachSelfEmail(user);
    }

    public Consents getUserConsents(String userNameAlias) {
        ClientUser user = toClientUser(userNameAlias);
        return BackendConnections.get(user).getConsents(user);
    }

    public void disableGDPRPopup(ClientUser user) {
        BackendConnections.get(user).disableConsentPopup(user);
    }

    public Optional<JSONObject> getUserProperty(String userNameAlias, String pathKey) {
        ClientUser user = toClientUser(userNameAlias);
        return BackendConnections.get(user).getPropertyValue(user, pathKey);
    }

    public void userXAddsConversationToFavorites(String userNameAlias, String conversationName) {
        final ClientUser asUser = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(asUser);
        Optional<JSONObject> currentFolders = backend.getPropertyValue(asUser, "labels");
        String conversationId = toConvoObj(asUser, conversationName).getId();
        if (currentFolders.isPresent()) {
            List<Label> labels = Labels.fromJSON(currentFolders.get().getJSONArray("labels"));
            Optional<Label> favorites = labels.stream().filter(label -> label.getType() == 1).findFirst();
            if (favorites.isPresent()) {
                // If there is already a label with type = 1
                favorites.get().getConversations().add(conversationId);
                JSONObject newLabels = new JSONObject();
                newLabels.put("labels", Labels.toJSON(labels));
                backend.setPropertyValue(asUser, "labels", newLabels);
            } else {
                // If there is no label with type = 1 but custom labels
                JSONObject newLabels = new JSONObject();
                List<String> ids = new ArrayList<>();
                ids.add(conversationId);
                Label label = new Label(UUID.randomUUID().toString(), "Favorites", ids, 1);
                labels.add(label);
                newLabels.put("labels", Labels.toJSON(labels));
                backend.setPropertyValue(asUser, "labels", newLabels);
            }
        } else {
            // If there are no previous labels at all
            JSONObject newLabels = new JSONObject();
            List<String> ids = new ArrayList<>();
            ids.add(conversationId);
            Label label = new Label(UUID.randomUUID().toString(), "Favorites", ids, 1);
            newLabels.put("labels", new JSONArray().put(label.toJSON()));
            backend.setPropertyValue(asUser, "labels", newLabels);
        }
    }

    public void userXRemovesConversationFromFavorites(String userNameAlias, String conversationName) {
        final ClientUser asUser = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(asUser);
        Optional<JSONObject> currentFolders = backend.getPropertyValue(asUser, "labels");
        String conversationId = toConvoObj(asUser, conversationName).getId();
        if (currentFolders.isPresent()) {
            List<Label> labels = Labels.fromJSON(currentFolders.get().getJSONArray("labels"));
            for (Label label : labels) {
                if (label.getType() == 1) {
                    label.getConversations().remove(conversationId);
                    JSONObject newLabels = new JSONObject();
                    newLabels.put("labels", Labels.toJSON(labels));
                    backend.setPropertyValue(asUser, "labels", newLabels);
                    return;
                }
            }
            throw new RuntimeException(String.format("No favorites folder found for user %s: %s", asUser.getName(),
                    currentFolders.get()));
        } else {
            throw new RuntimeException(String.format("No folders found for user %s", asUser.getName()));
        }
    }

    public void userXAddsConversationToFolder(String userNameAlias, String conversationName, String folderName) {
        final ClientUser asUser = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(asUser);
        Optional<JSONObject> currentFolders = backend.getPropertyValue(asUser, "labels");
        String conversationId = toConvoObj(asUser, conversationName).getId();
        if (currentFolders.isPresent()) {
            List<Label> labels = Labels.fromJSON(currentFolders.get().getJSONArray("labels"));
            Optional<Label> folder = labels.stream().filter(label -> label.getName().equals(folderName)).findFirst();
            if (folder.isPresent()) {
                // If there is already a label with the name
                folder.get().getConversations().add(conversationId);
                JSONObject newLabels = new JSONObject();
                newLabels.put("labels", Labels.toJSON(labels));
                backend.setPropertyValue(asUser, "labels", newLabels);
            } else {
                // If there is not a label with the name but other labels
                JSONObject newLabels = new JSONObject();
                List<String> ids = new ArrayList<>();
                ids.add(conversationId);
                Label label = new Label(UUID.randomUUID().toString(), folderName, ids, 0);
                labels.add(label);
                newLabels.put("labels", Labels.toJSON(labels));
                backend.setPropertyValue(asUser, "labels", newLabels);
            }
        } else {
            // If there are no previous labels at all
            JSONObject newLabels = new JSONObject();
            List<String> ids = new ArrayList<>();
            ids.add(conversationId);
            Label label = new Label(UUID.randomUUID().toString(), folderName, ids, 0);
            newLabels.put("labels", new JSONArray().put(label.toJSON()));
            backend.setPropertyValue(asUser, "labels", newLabels);
        }
    }

    public void userXRemovesConversationFromFolder(String userNameAlias, String conversationName, String folderName) {
        final ClientUser asUser = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(asUser);
        Optional<JSONObject> currentFolders = backend.getPropertyValue(asUser, "labels");
        String conversationId = toConvoObj(asUser, conversationName).getId();
        if (currentFolders.isPresent()) {
            List<Label> labels = Labels.fromJSON(currentFolders.get().getJSONArray("labels"));
            for (Label label : labels) {
                if (label.getName().equals(folderName)) {
                    label.getConversations().remove(conversationId);
                    if (label.getConversations().size() <= 0) {
                        labels.remove(label);
                    }
                    JSONObject newLabels = new JSONObject();
                    newLabels.put("labels", Labels.toJSON(labels));
                    backend.setPropertyValue(asUser, "labels", newLabels);
                    return;
                }
            }
            throw new RuntimeException(String.format("No %s folder found for user %s: %s", folderName, asUser.getName(),
                    currentFolders.get()));
        } else {
            throw new RuntimeException(String.format("No %s folder found for user %s", folderName, asUser.getName()));
        }
    }

    public Boolean getSendReadReceiptProperty(String userNameAlias) {
        ClientUser user = toClientUser(userNameAlias);
        return Integer.parseInt(BackendConnections.get(user).getStringPropertyValue(user, WIRE_RECEIPT_MODE)) > 0;
    }

    public void setSendReadReceiptProperty(String userNameAlias, boolean newValue) {
        ClientUser user = toClientUser(userNameAlias);
        BackendConnections.get(user).setPropertyValue(user, WIRE_RECEIPT_MODE, newValue ? "1" : "0");
    }

    public boolean isSendReadReceiptEnabled(String userNameAlias) {
        ClientUser user = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(user);
        JSONObject json = backend.getPropertyValues(user);
        if (json.has(WIRE_RECEIPT_MODE)) {
            return BooleanUtils.toBoolean(json.getInt(WIRE_RECEIPT_MODE));
        } else {
            return false;
        }
    }

    public void iChangeUserLocale(String nameAlias, String newLocale) {
        ClientUser user = toClientUser(nameAlias);
        BackendConnections.get(user).changeUserLocale(user, newLocale);
    }

    // region ETS

    public void addDevice(String ownerAlias, @Nullable String verificationCode, @Nullable String deviceName,
                          Optional<String> label, boolean developmentApiEnabled) {
        testServiceClient.login(toClientUser(ownerAlias), verificationCode, deviceName, developmentApiEnabled);
    }

    public void userSendsDeliveryConfirmationForMessage(String senderAlias, String conversationName,
                                                        @Nullable String deviceName, String messageId) {
        final Conversation conversation = toConvoObj(senderAlias, conversationName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.sendConfirmationDelivered(toClientUser(senderAlias), deviceName,
                convoId, convoDomain, messageId);
    }

    public void userSendsDeliveryConfirmationForRecentMessage(String receiverAlias, String convoName,
                                                              @Nullable String deviceName) {
        userSendsDeliveryConfirmationForMessage(receiverAlias, convoName, deviceName, getRecentMessageId(receiverAlias,
                convoName, deviceName));
    }

    public void userSendsReadConfirmationForMessage(String senderAlias, String conversationName,
                                                    @Nullable String deviceName, String messageId) {
        final Conversation conversation = toConvoObj(senderAlias, conversationName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.sendConfirmationRead(toClientUser(senderAlias), deviceName,
                convoId, convoDomain, messageId);
    }

    public void userSendsReadConfirmationForRecentMessage(String receiverAlias, String convoName,
                                                          @Nullable String deviceName) {
        userSendsReadConfirmationForMessage(receiverAlias, convoName, deviceName, getRecentMessageId(receiverAlias,
                convoName, deviceName));
    }

    public void userPingsConversation(String senderAlias, String convoName, @Nullable String deviceName,
                                      Timedelta msgTimer) {
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.sendPing(toClientUser(senderAlias), deviceName, convoId, convoDomain, msgTimer);
    }

    public void userIsTypingInConversation(String senderAlias, String convoName) {
        testServiceClient.sendTyping(toClientUser(senderAlias), FIRST_AVAILABLE_DEVICE,
                toConvoId(senderAlias, convoName), TypingStatus.STARTED);
    }

    public void userStopsTypingInConversation(String senderAlias, String convoName) {
        testServiceClient.sendTyping(toClientUser(senderAlias), FIRST_AVAILABLE_DEVICE,
                toConvoId(senderAlias, convoName), TypingStatus.STOPPED);
    }

    private String getRecentMessageId(ClientUser user, String convoId, String convoDomain, @Nullable String deviceName) {
        CommonUtils.waitUntilTrue(DEFAULT_WAIT_UNTIL_TIMEOUT, DEFAULT_WAIT_UNTIL_INTERVAL,
                () -> !testServiceClient.getMessageIds(user, deviceName, convoId, convoDomain).isEmpty());

        List<String> messageIds = testServiceClient.getMessageIds(user, deviceName, convoId, convoDomain);
        if (messageIds.isEmpty()) {
            throw new IllegalStateException("The conversation contains no messages");
        }
        return messageIds.get(messageIds.size() - 1);
    }

    private String getRecentMessageId(String userAlias, String convoName, @Nullable String deviceName) {
        ClientUser user = toClientUser(userAlias);
        Conversation conversation = toConvoObj(user, convoName);
        String convoId = conversation.getQualifiedID().getID();
        String convoDomain = conversation.getQualifiedID().getDomain();
        return getRecentMessageId(user, convoId, convoDomain, deviceName);
    }

    private String getSecondRecentMessageId(ClientUser sender, String convoId, String convoDomain, @Nullable String deviceName) {
        List<String> messageIds = testServiceClient.getMessageIds(sender, deviceName, convoId, convoDomain);
        if (messageIds.size() < 2) {
            throw new IllegalStateException("The conversation contains less than two messages");
        }
        return messageIds.get(messageIds.size() - 2);
    }

    private String getSecondRecentMessageId(String senderAlias, String convoName, @Nullable String deviceName) {
        ClientUser sender = toClientUser(senderAlias);
        Conversation conversation = toConvoObj(sender, convoName);
        String convoId = conversation.getQualifiedID().getID();
        String convoDomain = conversation.getQualifiedID().getDomain();
        return getSecondRecentMessageId(sender, convoId, convoDomain, deviceName);
    }

    public void userSendsDeliveryConfirmationForLastEphemeralMessage(String senderAlias, String convoName,
                                                                     @Nullable String deviceName) {
        ClientUser sender = toClientUser(senderAlias);
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.sendEphemeralConfirmationDelivered(sender, deviceName, convoId, convoDomain,
                getRecentMessageId(sender, convoId, convoDomain, deviceName));
    }

    public void userSendsDeliveryConfirmationForSecondLastEphemeralMessage(String senderAlias, String convoName,
                                                                           @Nullable String deviceName) {
        ClientUser sender = toClientUser(senderAlias);
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.sendEphemeralConfirmationDelivered(sender, deviceName, convoId, convoDomain,
                getSecondRecentMessageId(sender, convoId, convoDomain, deviceName));
    }

    public void userSeesReadReceiptOnMessage(String senderAlias, String text, String convoName,
                                             @Nullable String deviceName) {
        assertThat(String.format("Could not find any read receipt for message '%s' in %s", text, convoName),
                getReadReceiptUsersByMessage(senderAlias, text, convoName, deviceName),
                hasSize(greaterThan(0)));
    }

    public void userDoesNotSeeReadReceiptOnMessage(String senderAlias, String text, String convoName,
                                                   @Nullable String deviceName) {
        assertThat(String.format("Found read receipt for message '%s' in %s", text, convoName),
                getReadReceiptUsersByMessage(senderAlias, text, convoName, deviceName),
                hasSize(0));
    }

    public boolean userSeesLegalHoldStatusOnMessage(String senderAlias, String convoName, @Nullable String deviceName) {
        ClientUser sender = toClientUser(senderAlias);
        Conversation conversation = toConvoObj(sender, convoName);
        String convoId = conversation.getQualifiedID().getID();
        String convoDomain = conversation.getQualifiedID().getDomain();
        JSONArray messages = testServiceClient.getMessages(sender, deviceName, convoId, convoDomain);
        JSONObject message = (JSONObject) messages.get(messages.length() - 1);
        JSONObject content = message.getJSONObject("content");
        return content.has("legalHoldStatus");
    }

    public int userSeesLegalHoldStatusValueOnMessage(String senderAlias, String convoName, @Nullable String deviceName) {
        ClientUser sender = toClientUser(senderAlias);
        Conversation conversation = toConvoObj(sender, convoName);
        String convoId = conversation.getQualifiedID().getID();
        String convoDomain = conversation.getQualifiedID().getDomain();
        JSONArray messages = testServiceClient.getMessages(sender, deviceName, convoId, convoDomain);
        JSONObject message = (JSONObject) messages.get(messages.length() - 1);
        JSONObject content = message.getJSONObject("content");
        return content.getInt("legalHoldStatus");
    }

    public boolean userSeesLegalHoldStatusOnMessagesReaction(String senderAlias, String convoName, @Nullable String deviceName) {
        ClientUser sender = toClientUser(senderAlias);
        Conversation conversation = toConvoObj(sender, convoName);
        String convoId = conversation.getQualifiedID().getID();
        String convoDomain = conversation.getQualifiedID().getDomain();
        JSONArray messages = testServiceClient.getMessages(sender, deviceName, convoId, convoDomain);
        JSONObject message = (JSONObject) messages.get(messages.length() - 1);
        JSONArray reactions = message.getJSONArray("reactions");
        JSONObject lastReaction = (JSONObject) reactions.get(reactions.length() - 1);
        return lastReaction.has("legalHoldStatus");
    }

    public int userSeesLegalHoldStatusValueOnMessagesReaction(String senderAlias, String convoName, @Nullable String deviceName) {
        ClientUser sender = toClientUser(senderAlias);
        Conversation conversation = toConvoObj(sender, convoName);
        String convoId = conversation.getQualifiedID().getID();
        String convoDomain = conversation.getQualifiedID().getDomain();
        JSONArray messages = testServiceClient.getMessages(sender, deviceName, convoId, convoDomain);
        JSONObject message = (JSONObject) messages.get(messages.length() - 1);
        JSONArray reactions = message.getJSONArray("reactions");
        JSONObject lastReaction = (JSONObject) reactions.get(reactions.length() - 1);
        return lastReaction.getInt("legalHoldStatus");
    }

    private List<String> getReadReceiptUsersByMessage(String senderAlias, String text, String convoName,
                                                      @Nullable String deviceName) {
        List<String> userIds = new ArrayList<>();
        ClientUser sender = toClientUser(senderAlias);
        Conversation conversation = toConvoObj(sender, convoName);
        String convoId = conversation.getQualifiedID().getID();
        String convoDomain = conversation.getQualifiedID().getDomain();
        JSONArray messages = testServiceClient.getMessages(sender, deviceName, convoId, convoDomain);
        for (int i = (messages.length() - 1); i >= 0; i--) {
            JSONObject message = (JSONObject) messages.get(i);
            JSONObject content = message.getJSONObject("content");
            String actualText = null;
            if (testServiceClient.isKaliumTestservice()) {
                // Text messages
                if (content.has("value")) {
                    actualText = content.getString("value");
                }
                // Poll messages
                if (content.has("textContent")) {
                    actualText = content.getJSONObject("textContent").getString("value");
                }
                if (text.equals(actualText)) {
                    String messageId = message.getString("id");
                    JSONArray receipts = testServiceClient.getMessageReadReceipts(sender, deviceName, convoId, convoDomain, messageId);
                    for (int j = 0; j < receipts.length(); j++) {
                        JSONObject receipt = receipts.getJSONObject(j);
                        if (receipt.getString("type").equals("READ")) {
                            String userId = receipt.getJSONObject("userSummary")
                                    .getJSONObject("userId")
                                    .getString("value");
                            userIds.add(userId);
                        }
                    }
                    log.info("Return:" + receipts);
                }
            } else {
                if (content.has("text")) {
                    // Text messages
                    actualText = content.getString("text");
                } else if (content.has("items")) {
                    // Poll messages
                    JSONArray items = content.getJSONArray("items");
                    for (int k = 0; k < items.length(); k++) {
                        JSONObject item = items.getJSONObject(k);
                        if (item.has("text")) {
                            actualText = item.getJSONObject("text").getString("content");
                            break;
                        }
                    }
                }
                if (text.equals(actualText)) {
                    JSONArray confirmations = message.getJSONArray("confirmations");
                    for (int j = 0; j < confirmations.length(); j++) {
                        JSONObject confirmation = confirmations.getJSONObject(j);
                        if (confirmation.getInt("type") == 1) {
                            userIds.add(confirmation.getString("from"));
                        }
                    }
                }
            }
        }
        return userIds;
    }

    @Deprecated // Please use userTogglesReactionOnLatestMessage instead when using the new reactions
    public void userReactsToLatestMessage(String senderAlias, String convoName, @Nullable String deviceName,
                                          ReactionType reactionType) {
        if (!testServiceClient.isKaliumTestservice() && reactionType == ReactionType.UNLIKE) {
            // Unlike with ETS: Send empty reaction which removes the heart
            userTogglesReactionOnLatestMessage(senderAlias, convoName, deviceName, "");
        } else {
            userTogglesReactionOnLatestMessage(senderAlias, convoName, deviceName, "❤️");
        }
    }

    public void userTogglesReactionOnLatestMessage(String senderAlias, String convoName,
                                                   @Nullable String deviceName, String reaction) {
        ClientUser sender = toClientUser(senderAlias);
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.toggleReaction(sender, deviceName, convoId, convoDomain,
                getRecentMessageId(sender, convoId, convoDomain, deviceName), reaction);
    }

    public void userRepliesToLatestMessage(String senderAlias, String convoName, @Nullable String deviceName,
                                           Timedelta msgTimer, String text) {
        ClientUser sender = toClientUser(senderAlias);
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();

        JSONArray messages = testServiceClient.getMessages(sender, deviceName, convoId, convoDomain);
        for (int i = (messages.length() - 1); i >= 0; i--) {
            JSONObject message = (JSONObject) messages.get(i);
            JSONObject content = message.getJSONObject("content");
            String messageId = message.getString("id");
            if (testServiceClient.isKaliumTestservice()) {
                if (content.has("latitude") && content.has("longitude")) {
                    double latitude = content.getDouble("latitude");
                    double longitude = content.getDouble("longitude");
                    long timestamp = convertDateToTimestamp(message);
                    testServiceClient.sendReply(sender, deviceName, convoId, convoDomain, msgTimer, text, messageId,
                            ReplyHashGenerator.generateHash(latitude, longitude, timestamp));
                    return;
                }
                // Kalium
                if (content.has("value")) {
                    if (content.get("value") instanceof JSONObject) {
                        // message is an asset
                        JSONObject value = content.getJSONObject("value");
                        JSONObject remoteData = value.getJSONObject("remoteData");
                        String assetId = remoteData.getString("otrKey");
                        long timestamp = convertDateToTimestamp(message);
                        testServiceClient.sendReply(sender, deviceName, convoId, convoDomain, msgTimer, text, messageId,
                                    ReplyHashGenerator.generateHash(assetId, timestamp));
                    } else {
                        // reply to a text message (kalium)
                        String quote = content.getString("value");
                        long timestamp = convertDateToTimestamp(message);
                        testServiceClient.sendReply(sender, deviceName, convoId, convoDomain, msgTimer, text, messageId,
                                    ReplyHashGenerator.generateHash(quote, timestamp));
                    }
                    return;
                }
            } else {
                // ETS
                if (content.has("latitude") && content.has("longitude")) {
                    // reply to a location (ETS)
                    double latitude = content.getDouble("latitude");
                    double longitude = content.getDouble("longitude");
                    long timestamp = message.getLong("timestamp") / 1000;
                    testServiceClient.sendReply(sender, deviceName, convoId, convoDomain, msgTimer, text, messageId,
                            ReplyHashGenerator.generateHash(latitude, longitude, timestamp));
                    return;
                }
                if (content.has("text")) {
                    // reply to a text message (ETS)
                    String quote = content.getString("text");
                    Long timestamp = message.getLong("timestamp") / 1000;
                    testServiceClient.sendReply(sender, deviceName, convoId, convoDomain, msgTimer, text, messageId,
                            ReplyHashGenerator.generateHash(quote, timestamp));
                    return;
                }
                // When I receive an asset the message json contains an asset property with a key value
                // When I send an asset the message json contains an uploaded property with an assetId value
                if (content.has("asset") || content.has("uploaded")) {
                    JSONObject asset = content.has("asset") ? content.getJSONObject("asset") : content.getJSONObject("uploaded");
                    if (asset.has("key")) {
                        String assetId = asset.getString("key");
                        Long timestamp = message.getLong("timestamp") / 1000;
                        testServiceClient.sendReply(sender, deviceName, convoId, convoDomain, msgTimer, text, messageId,
                                ReplyHashGenerator.generateHash(assetId, timestamp));
                        return;
                    } else if (asset.has("assetId")) {
                        String assetId = asset.getString("assetId");
                        Long timestamp = message.getLong("timestamp") / 1000;
                        testServiceClient.sendReply(sender, deviceName, convoId, convoDomain, msgTimer, text, messageId,
                                ReplyHashGenerator.generateHash(assetId, timestamp));
                        return;
                    }
                }
            }
        }
        throw new IllegalStateException(String.format("Could not find any message of %s in %s", sender, convoId));
    }

    private long convertDateToTimestamp(JSONObject message) {
        long timestamp;
        // kalium format of date is different between versions
        if (message.get("date") instanceof JSONObject) {
            // develop
            timestamp = message.getJSONObject("date").getInt("epochSeconds");
        } else {
            // 4.6
            String date = message.getString("date");
            SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            try {
                timestamp = sdf.parse(date).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return timestamp;
    }

    public void userRepliesToLatestMessageWithWrongHash(String senderAlias, String convoName, @Nullable String deviceName,
                                                        Timedelta msgTimer, String message) {
        ClientUser sender = toClientUser(senderAlias);
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        String messageId = getRecentMessageId(sender, convoId, convoDomain, deviceName);
        testServiceClient.sendReply(sender, deviceName, convoId, convoDomain, msgTimer, message,
                messageId,
                "4f8ee55a8b71a7eb7447301d1bd0c8429971583b15a91594b45dee16f208afd5");
    }

    public void userDeletesLatestMessage(String senderAlias, String convoName, @Nullable String deviceName,
                                         boolean isDeleteEverywhere) {
        userDeletesXLastMessages(senderAlias, 1, convoName, deviceName, isDeleteEverywhere);
    }

    public void userDeletesXLastMessages(String senderAlias, int amount, String convoName, @Nullable String deviceName,
                                         boolean isDeleteEverywhere) {
        ClientUser sender = toClientUser(senderAlias);
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        List<String> messageIds = testServiceClient.getMessageIds(sender, deviceName, convoId, convoDomain);
        for (int i = 1; i <= amount; i++) {
            String messageId = messageIds.get(messageIds.size() - i);
            if (isDeleteEverywhere) {
                testServiceClient.deleteEverywhere(sender, deviceName, convoId, convoDomain, messageId);
            } else {
                testServiceClient.deleteForMe(sender, deviceName, convoId, convoDomain, messageId);
            }
        }
    }

    public void userDeletesRememberedMessage(String senderAlias, String convoName, @Nullable String deviceName,
                                             boolean isDeleteEverywhere) {
        final String convoKey = generateConversationKey(senderAlias, convoName, deviceName);
        if (!recentMessageIds.containsKey(convoKey)) {
            throw new IllegalStateException("You should remember the recent message before you check it");
        }
        ClientUser sender = toClientUser(senderAlias);
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();        String messageId = recentMessageIds.get(convoKey).get();
        if (isDeleteEverywhere) {
            testServiceClient.deleteEverywhere(sender, deviceName, convoId, convoDomain, messageId);
        } else {
            testServiceClient.deleteForMe(sender, deviceName, convoId, convoDomain, messageId);
        }
    }

    public void userEditsLatestMessage(String senderAlias, String convoName, @Nullable String deviceName,
                                       String newMessage) {
        userEditsMessage(senderAlias, convoName, deviceName,
                getRecentMessageId(senderAlias, convoName, deviceName), newMessage);
    }

    private static Optional<Matcher> matchUrl(String message) {
        Pattern p = Pattern.compile("([a-z]+://)?[a-z0-9\\-]+\\.[a-z]+[^ \\n]*");
        Matcher m = p.matcher(message);
        return m.find() ? Optional.of(m) : Optional.empty();
    }

    public void userEditsSecondLastMessagewithLinkPreview(String senderAlias, String convoName, @Nullable String deviceName,
                                                          String newMessage, String title, String filePath) {
        Matcher m = matchUrl(newMessage)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Text does not contain any URL: %s", newMessage)));
        int urlOffset = m.regionStart();
        String url = m.group(0);
        ClientUser sender = toClientUser(senderAlias);
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.updateTextWithLinkPreview(sender, deviceName, convoId, convoDomain,
                getSecondRecentMessageId(sender, convoId, convoDomain, deviceName), newMessage,
                title, title, url, urlOffset, url, filePath);
    }

    public void userEditsMessage(String senderAlias, String convoName, @Nullable String deviceName,
                                 String messageId, String newMessage) {
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.updateText(toClientUser(senderAlias), deviceName, convoId, convoDomain,
                messageId, newMessage);
    }

    public void userUpdatesLatestMessageViaEts(String senderAlias, String convoName, @Nullable String deviceName,
                                               String newMessage) {
        userEditsMessage(senderAlias, convoName, deviceName,
                getRecentMessageId(senderAlias, convoName, deviceName), newMessage);
    }

    public void userUpdatesRememberedMessageViaEts(String senderAlias, String convoName, @Nullable String deviceName,
                                                   String newMessage) {
        final String convoKey = generateConversationKey(senderAlias, convoName, deviceName);
        if (!recentMessageIds.containsKey(convoKey)) {
            throw new IllegalStateException("You should remember the recent message before you edit it");
        }
        String messageId = recentMessageIds.get(convoKey).get();
        userEditsMessage(senderAlias, convoName, deviceName, messageId, newMessage);
    }

    public String userSendsMessageToConversation(String senderAlias, String convoName, @Nullable String deviceName,
                                                 Timedelta msgTimer, String message, int legalHoldStatus) {
        final Conversation conversation = toConvoObj(senderAlias, convoName);

        // TODO ETS wants to have the UUID of the creator, instead of the conversation ID when it is a 1:1 conversation
        // this implementation will also only work when the creator is the other user than the user who tries to sends the message through ETS. This should get fixed to support 1:1 messages properly
        //        String convoId = conversation.getCreatorId();

        // Group conversation message sending from ETS already works, so for this we can use the conversationID

        String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();

        final boolean expReadConfirm = conversation.getType().map(t -> {
                    switch (t) {
                        case 0:
                            // Group
                            return conversation.isReceiptModeEnabled();
                        case 2:
                            // 1:1
                            return isSendReadReceiptEnabled(senderAlias);
                        default:
                            return false;
                    }
                })
                .orElse(false);
        return testServiceClient.sendText(toClientUser(senderAlias), deviceName, convoDomain, convoId,
                msgTimer, expReadConfirm, message, legalHoldStatus);
    }

    public void userSendsGenericMessageToConversation(String senderAlias, String convoName, @Nullable String deviceName,
                                                      Timedelta msgTimer, String message, int legalHoldStatus) {
        matchUrl(message).map((m) -> {
            try {
                File tempFile = File.createTempFile("zautomation", ".png");
                try {
                    String title = m.group(0);
                    ImageIO.write(QRCode.generateCode(title, Color.BLACK, Color.WHITE, 64, 1),
                            "png", tempFile);
                    userSendsLinkPreview(senderAlias, convoName, deviceName, msgTimer, message, title,
                            tempFile.getAbsolutePath());
                } finally {
                    tempFile.delete();
                }
                return m;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).orElseGet(() -> {
            userSendsMessageToConversation(senderAlias, convoName, deviceName, msgTimer, message, legalHoldStatus);
            return null;
        });
    }

    public void userBreaksSession(String senderAlias, @Nullable String deviceName, String deviceId) {
        ClientUser me = usersManager.getSelfUser().get();
        String userDomain = BackendConnections.get(me).getDomain();
        testServiceClient.breakSession(toClientUser(senderAlias), deviceName, me, userDomain, deviceId);
    }

    public void userResetsSession(String senderAlias, @Nullable String deviceName, String convoName) {
        testServiceClient.resetSession(toClientUser(senderAlias), deviceName, toConvoId(senderAlias, convoName));
    }

    public void userSendsLinkPreview(String senderAlias, String convoName, @Nullable String deviceName,
                                     Timedelta msgTimer, String msg, String title, String imagePath) {
        Matcher m = matchUrl(msg)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Text does not contain any URL: %s", msg)));
        int urlOffset = m.regionStart();
        String url = m.group(0);
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.sendLinkPreview(toClientUser(senderAlias), deviceName, convoId, convoDomain,
                msgTimer, msg, title, title, url, urlOffset, url, imagePath);
    }

    private List<Mention> getMentionsList(String msg) {
        // Pattern with two groups containing space (group 1) and containing username (group 2)
        Pattern p = Pattern.compile("(^|\\s)@([a-zA-Z0-9\\-]+)");
        Matcher m = p.matcher(msg);
        List<Mention> mentions = new ArrayList<>();
        while (m.find()) {
            // get user from mention
            String member = m.group(2);
            ClientUser user = getUsersManager().findUserByFirstNameOrFirstNameAlias(member);
            // calculate start index of the mention inside text through the pattern match
            int lengthOfSpace = m.group(1).length();
            int start = m.toMatchResult().start() + lengthOfSpace;
            // the length includes the @ character
            int length = "@".length() + user.getName().length();
            Mention mention = new Mention(length, start, user.getId(), BackendConnections.get(user).getDomain());
            mentions.add(mention);
        }
        return mentions;
    }

    public void userSendsTextWithMentions(String senderAlias, String convoName, @Nullable String deviceName,
                                          Timedelta msgTimer, String msg) {
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();

        // replace with real username in whole text
        msg = getUsersManager().replaceAliasesOccurrences(msg, FindBy.NAME_ALIAS);
        // get start index and length of mentions
        List<Mention> mentions = getMentionsList(msg);

        testServiceClient.sendTextWithMentions(toClientUser(senderAlias), deviceName, convoId, convoDomain,
                msgTimer, msg, mentions);
    }

    public void userSendsTextWithMentionContainingWrongValues(String senderAlias, String convoName,
                                                              @Nullable String deviceName, Timedelta msgTimer, String msg,
                                                              String userId, int start, int length) {
        List<Mention> mentions = new ArrayList<>();
        Mention mention = new Mention(length, start, userId);
        mentions.add(mention);
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();

        testServiceClient.sendTextWithMentions(toClientUser(senderAlias), deviceName, convoId, convoDomain,
                msgTimer, msg, mentions);
    }

    public void userSendsImageToConversationViaTestservice(String senderAlias, String convoName, @Nullable String deviceName,
                                                           String imagePath, Timedelta msgTimer) {
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.sendImage(toClientUser(senderAlias), deviceName, convoId, convoDomain,
                msgTimer, imagePath);
    }

    public void userSendsImageToConversationViaTestservice(String senderAlias, String convoName, @Nullable String deviceName,
                                                           byte[] imageAsBytes, String type, int width, int height,
                                                           Timedelta msgTimer) {
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.sendImage(toClientUser(senderAlias), deviceName, convoId, convoDomain, msgTimer,
                imageAsBytes, type, width, height);
    }

    public void userSendsFileToConversation(String senderAlias, String convoName, @Nullable String deviceName,
                                            Timedelta msgTimer, String path, String mime) {
        if (!new File(path).exists()) {
            throw new IllegalArgumentException(String.format("Please make sure the file %s exists and is accessible",
                    path));
        }
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.sendFile(toClientUser(senderAlias), deviceName, convoId, convoDomain,
                msgTimer, path, mime);
    }

    public void userSendsFileToConversation(String senderAlias, String convoName, @Nullable String deviceName,
                                            Timedelta msgTimer, String path, String mime, boolean otherAlgorithm,
                                            boolean otherHash, boolean invalidHash) {
        if (!new File(path).exists()) {
            throw new IllegalArgumentException(String.format("Please make sure the file %s exists and is accessible",
                    path));
        }
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.sendFile(toClientUser(senderAlias), deviceName, convoId, convoDomain,
                msgTimer, path, mime, otherAlgorithm, otherHash, invalidHash);
    }

    public void userSendsAudioToConversation(String senderAlias, String convoName, @Nullable String deviceName,
                                             Timedelta msgTimer, String path, String mime, Timedelta duration) {
        if (!new File(path).exists()) {
            throw new IllegalArgumentException(String.format("Please make sure the file %s exists and is accessible",
                    path));
        }
        int[] normalizedAudio = new int[10];
        for (int i = 0; i < 10; i++) {
            normalizedAudio[i] = ThreadLocalRandom.current().nextInt(0, 256);
        }
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.sendAudioFile(toClientUser(senderAlias), deviceName, convoId, convoDomain,
                msgTimer, duration, normalizedAudio, path, mime);
    }

    public void userSendsVideoToConversation(String senderAlias, String convoName, @Nullable String deviceName,
                                             Timedelta msgTimer, String path, String mime, Timedelta duration, int[] dimensions) {
        if (!new File(path).exists()) {
            throw new IllegalArgumentException(String.format("Please make sure the file %s exists and is accessible",
                    path));
        }
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.sendVideoFile(toClientUser(senderAlias), deviceName, convoId, convoDomain,
                msgTimer, duration, dimensions, path, mime);
    }

    public void userSendsLocationToConversation(String senderAlias, String convoName, @Nullable String deviceName,
                                                Timedelta msgTimer, float longitude, float latitude,
                                                String locationName, int zoom) {
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.sendLocation(toClientUser(senderAlias), deviceName, convoId, convoDomain,
                msgTimer, longitude, latitude, locationName, zoom);
    }

    public void userSetsAvailabilityStatus(String userAlias, @Nullable String deviceName, int availabilityType) {
        ClientUser user = toClientUser(userAlias);
        Team firstTeam = BackendConnections.get(user).getAllTeams(user).get(0);
        testServiceClient.setAvailability(user, deviceName, firstTeam.getId(), availabilityType);
    }

    public void userClearsConversation(String senderAlias, String convoName, @Nullable String deviceName) {
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.clearConversation(toClientUser(senderAlias), deviceName, convoId, convoDomain);
    }

    public void userXDeletesConversation(String userAlias, String convoName) {
        ClientUser user = toClientUser(userAlias);
        BackendConnections.get(user).deleteTeamConversation(user, toConvoId(userAlias, convoName));
    }

    public void userXDeletesAllConversationsByName(String userAlias, String convoName) {
        ClientUser user = toClientUser(userAlias);
        Backend backend = BackendConnections.get(user);
        List<Conversation> conversations = backend.getConversationsByName(user, convoName);
        for (Conversation conversation : conversations) {
            backend.deleteTeamConversation(user, conversation.getId());
        }
    }

    public void userSendsMultipleMedias(String senderAlias, String convoName, Timedelta msgTimer, int count,
                                        String fileType, String fileName) {
        String filePath;
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        for (int i = 0; i < count; ++i) {
            switch (fileType) {
                case "image":
                    filePath = Config.current().getImagesPath(this.getClass()) + File.separator + fileName;
                    testServiceClient.sendImage(toClientUser(senderAlias), FIRST_AVAILABLE_DEVICE,
                            convoId, convoDomain, msgTimer, filePath);
                    break;
                case "video":
                    filePath = Config.current().getVideoPath(this.getClass()) + File.separator + fileName;
                    testServiceClient.sendFile(toClientUser(senderAlias), FIRST_AVAILABLE_DEVICE,
                            convoId, convoDomain, msgTimer, filePath,
                            fileType.equals("video") ? "video/mp4" : "audio/mp4");
                    break;
                case "audio":
                    filePath = Config.current().getAudioPath(this.getClass()) + File.separator + fileName;
                    testServiceClient.sendFile(toClientUser(senderAlias), FIRST_AVAILABLE_DEVICE,
                            convoId, convoDomain, msgTimer, filePath,
                            fileType.equals("audio") ? "video/mp4" : "audio/mp4");
                    break;
                case "temporary":
                    filePath = Config.current().getBuildPath(this.getClass()) + File.separator + fileName;
                    testServiceClient.sendFile(toClientUser(senderAlias), FIRST_AVAILABLE_DEVICE,
                            convoId, convoDomain, msgTimer, filePath, "application/octet-stream");
                    break;
                default:
                    throw new IllegalArgumentException(String.format("Unsupported '%s' file type", fileType));
            }
        }
    }

    public void usersAddDevices(String usersToDevicesMappingAsJson, boolean useVerificationCode) {
        final JSONObject mappingAsJson = new JSONObject(usersToDevicesMappingAsJson);
        final Map<String, List<JSONObject>> devicesMapping = new LinkedHashMap<>();
        for (String user : mappingAsJson.keySet()) {
            final JSONArray devices = mappingAsJson.getJSONArray(user);
            final List<JSONObject> devicesInfo = new ArrayList<>();
            for (int deviceIdx = 0; deviceIdx < devices.length(); deviceIdx++) {
                devicesInfo.add(devices.getJSONObject(deviceIdx));
            }
            devicesMapping.put(getUsersManager().replaceAliasesOccurrences(user, FindBy.NAME_ALIAS), devicesInfo);
        }

        devicesMapping.entrySet().stream().parallel().forEach(
                entry -> {
                    for (JSONObject devInfo : entry.getValue()) {
                        final String deviceName = devInfo.has("name")
                                ? devInfo.getString("name")
                                : CommonUtils.generateRandomString(8);
                        final Optional<String> deviceLabel = devInfo.has("label")
                                ? Optional.of(devInfo.getString("label"))
                                : Optional.empty();
                        String userAlias = entry.getKey();
                        if (!useVerificationCode) {
                            boolean developmentApiEnabled = isDevelopmentApiEnabled(userAlias);
                            addDevice(userAlias, null, deviceName, deviceLabel,
                                    developmentApiEnabled);
                        } else {
                            ClientUser user = getUsersManager().findUserByNameOrNameAlias(userAlias);
                            Backend backend = BackendConnections.get(user);
                            String verificationCode = backend.getVerificationCode(user);
                            log.info("verificationCode: " + verificationCode);
                            boolean developmentApiEnabled = isDevelopmentApiEnabled(userAlias);
                            addDevice(userAlias, verificationCode, deviceName, deviceLabel,
                                    developmentApiEnabled);
                        }
                    }
                }
        );
    }

    public void userSendsMultipleMessages(String senderAlias, String convoName, Timedelta msgTimer, int count,
                                          String msg, String defaultMessage, int legalHoldStatus) {
        final String msgToSend;
        switch (msg) {
            case "default":
                msgToSend = defaultMessage;
                break;
            case "long":
                msgToSend = CommonUtils.generateRandomString(400);
                break;
            default:
                msgToSend = msg.replaceAll("^\"|\"$", "");
                break;
        }
        for (int i = 0; i < count; ++i) {
            userSendsGenericMessageToConversation(senderAlias, convoName, FIRST_AVAILABLE_DEVICE, msgTimer, msgToSend, legalHoldStatus);
        }
    }

    @Deprecated // Try to use method userXGetsAllTextMessages instead
    public void userXRemembersLastMessageViaEts(String senderAlias, String convoName, @Nullable String deviceName) {
        Optional<String> messageId = Optional.empty();
        try {
            messageId = Optional.of(getRecentMessageId(senderAlias, convoName, deviceName));
        } catch (IllegalStateException ign) {
            log.info("Failed to get recent message id: " + ign.getMessage());
        }
        recentMessageIds.put(generateConversationKey(senderAlias, convoName, deviceName), messageId);
    }

    @Deprecated // Try to use method userXGetsAllTextMessages instead
    public void userXFoundLastMessageChanged(String senderAlias, String convoName, @Nullable String deviceName,
                                             Timedelta duration) {
        final String convoKey = generateConversationKey(senderAlias, convoName, deviceName);
        if (!recentMessageIds.containsKey(convoKey)) {
            throw new IllegalStateException("You should remember the recent message before you check it");
        }
        final String rememberedMessageId = recentMessageIds.get(convoKey).orElse("");
        final boolean isChanged = CommonUtils.waitUntilTrue(duration, CommonSteps.DEFAULT_WAIT_UNTIL_INTERVAL, () -> {
            String actualMessageId = "";
            try {
                actualMessageId = getRecentMessageId(senderAlias, convoName, deviceName);
            } catch (IllegalStateException ign) {
                log.info("Failed to get recent message id: " + ign.getMessage());
            }
            return !actualMessageId.equals(rememberedMessageId);
        });
        assertThat(String.format("Actual message Id should not equal to '%s'", rememberedMessageId), isChanged);
    }

    public List<String> userXGetsAllTextMessages(String userAlias, String convoName, @Nullable String deviceName) {
        ClientUser user = getUsersManager().findUserByNameOrNameAlias(userAlias);
        Conversation conversation = toConvoObj(user, convoName);
        String convoId = conversation.getQualifiedID().getID();
        String convoDomain = conversation.getQualifiedID().getDomain();
        JSONArray messages = testServiceClient.getMessages(user, deviceName,convoId, convoDomain);
        if (messages.length() == 0) {
            throw new IllegalStateException("The conversation contains no messages");
        }

        List<String> result = new ArrayList<>();
        for (int i = 0; i < messages.length(); i++) {
            if (messages.getJSONObject(i).has("content")) {
                JSONObject content = messages.getJSONObject(i).getJSONObject("content");
                if (content.has("value")) {
                    result.add(content.getString("value"));
                }
            }
        }
        return result;
    }

    public void userXFoundLastMessageNotChanged(String senderAlias, String convoName, @Nullable String deviceName,
                                                Timedelta duration) {
        final String convoKey = generateConversationKey(senderAlias, convoName, deviceName);
        if (!recentMessageIds.containsKey(convoKey)) {
            throw new IllegalStateException("You should remember the recent message before you check it");
        }
        final String rememberedMessageId = recentMessageIds.get(convoKey).orElse("");
        final boolean isNotChanged = CommonUtils.waitUntilTrue(duration, CommonSteps.DEFAULT_WAIT_UNTIL_INTERVAL, () -> {
            String actualMessageId = "";
            try {
                actualMessageId = getRecentMessageId(senderAlias, convoName, deviceName);
            } catch (IllegalStateException ign) {
                log.info("Failed to get recent message id: " + ign.getMessage());
            }
            return actualMessageId.equals(rememberedMessageId);
        });
        assertThat(String.format("Actual message Id should equal to '%s'", rememberedMessageId), isNotChanged);
    }

    public String getDeviceId(ClientUser user, String deviceName) {
        return testServiceClient.getDeviceId(user, deviceName);
    }

    public List<String> getDeviceIds(String owner) {
        final ClientUser user = toClientUser(owner);
        return testServiceClient.getUserDevices(user).stream()
                .map((d) -> getDeviceId(user, d))
                .collect(Collectors.toList());
    }

    public String getDeviceFingerprint(ClientUser user, String deviceName) {
        return testServiceClient.getDeviceFingerprint(user, deviceName);
    }

    public void cleanUpTestServiceInstances() {
        testServiceClient.cleanUp();
    }

    // endregion ETS


    // region Tracking


    /**
     * This is deprecated, because it does not implement polling or timeouts which make you define explicit waits in your tests.
     */
    @Deprecated
    public JSONObject getTrackingPropertiesFromLastEvent(String distinctId, String event) throws IOException {
        return mixPanelClient.getTrackingPropertiesFromLastEvent(distinctId, event);
    }

    public JSONObject findTrackingPropertyFromLastEvent(String distinctId, String event, String property, Object value) {
        return findTrackingPropertyFromLastEvent(distinctId, event, property, value,
                Timedelta.ofDuration(MIXPANEL_TIMEOUT));
    }

    public JSONObject findTrackingPropertyFromLastEvent(String distinctId, String event, String property, Object value,
                                                        Timedelta timeout) {
        if (lastMixpanelDistrictId.equals(distinctId) &&
                lastMixpanelEvent.equals(event) &&
                lastMixpanelResponse.keySet().contains(property) &&
                lastMixpanelResponse.get(property).equals(value)) {
            return lastMixpanelResponse;
        }

        // J Connolly (Mixpanel): when you wait a few seconds after sending the event before you start to query for it, it seems to appear much faster
        Timedelta.ofSeconds(10).sleep();

        return null;
    }

    public JSONObject findTrackingPropertyFromLastEventContains(String distinctId, String event, String property,
                                                                String value) {
        return findTrackingPropertyFromLastEventContains(distinctId, event, property, value,
                Timedelta.ofDuration(MIXPANEL_TIMEOUT));
    }

    @Nullable
    public JSONObject findTrackingPropertyFromLastEventContains(String distinctId, String event, String property,
                                                                String value, Timedelta timeout) {
        if (lastMixpanelResponse.keySet().contains(property) && lastMixpanelResponse.getString(property).contains(value)) {
            return lastMixpanelResponse;
        }

        return null;
    }

    @Nullable
    public JSONObject getAllTrackingPropertyFromFirstEvent(String distinctId, String event) {
        return null;
    }


    // endregion Tracking


    // region IdP

    public boolean isSSOFeatureEnabled(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        return backend.getSSOFeatureSettings(dstTeam).getString("status").equals("enabled");
    }

    public void enableSSOFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.enableSSOFeature(dstTeam);
    }

    public void createSAMLConnection(String adminUserAlias, String idpMetadata) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        identityProviderId = backend.createIdentityProvider(adminUser, idpMetadata);
    }

    public void thereIsASSOTeamOwnerForOkta(String ownerNameAlias, String teamName) {
        thereIsATeamOwner(ownerNameAlias, teamName, true);
        enableSSOFeature(ownerNameAlias, teamName);
        final ClientUser owner = toClientUser(ownerNameAlias);
        Backend backend = BackendConnections.get(owner);
        String finalizeUrl = OktaAPIClient.getFinalizeUrlDependingOnBackend(backend.getBackendUrl());
        oktaAPIClient.createApplication(owner.getName() + " " + teamName, finalizeUrl);
        identityProviderId = backend.createIdentityProvider(owner,
                oktaAPIClient.getApplicationMetadata());
    }

    public void thereIsAKnownSSOTeamOwnerForOkta(String ownerNameAlias, String teamName) {
        enableSSOFeature(ownerNameAlias, teamName);
        final ClientUser owner = toClientUser(ownerNameAlias);
        Backend backend = BackendConnections.get(owner);
        String finalizeUrl = OktaAPIClient.getFinalizeUrlDependingOnBackend(backend.getBackendUrl());
        oktaAPIClient.createApplication(owner.getName() + " " + teamName + " " + owner.getName(), finalizeUrl);
        identityProviderId = backend.createIdentityProvider(owner,
                oktaAPIClient.getApplicationMetadata());
    }

    public void thereIsASSOTeamOwnerForKeycloak(String ownerNameAlias, String teamName) {
        thereIsATeamOwner(ownerNameAlias, teamName, true);
        enableSSOFeature(ownerNameAlias, teamName);
        final ClientUser owner = toClientUser(ownerNameAlias);
        Backend backend = BackendConnections.get(owner);
        final Team dstTeam = backend.getTeamByName(owner, teamName);
        identityProviderId = backend.createIdentityProviderV2(owner, keycloakAPIClient.getMetadata());
        keycloakAPIClient.createSAMLClient(dstTeam.getId(), backend.getBackendUrl());
    }

    public String getSSOCode() {
        log.info(String.format("The sso code is wire-%s", identityProviderId));
        return String.format("wire-%s", identityProviderId);
    }

    public void setSSOCode(String code) {
        log.info(String.format("The sso code is wire-%s", code));
        identityProviderId = code;
    }

    public String getSSOLink() {
        return String.format("wire://start-sso/%s", getSSOCode());
    }

    public void thereIsASSOTeamOwnerForWronglyConfiguredOkta(String ownerNameAlias, String teamName) {
        thereIsATeamOwner(ownerNameAlias, teamName, true);
        enableSSOFeature(ownerNameAlias, teamName);
        final ClientUser owner = toClientUser(ownerNameAlias);
        Backend backend = BackendConnections.get(owner);
        oktaAPIClient.createWronglyConfiguredApplication(owner.getName() + " " + teamName, backend.getBackendUrl());
        identityProviderId = backend.createIdentityProvider(owner,
                oktaAPIClient.getApplicationMetadata());
    }

    public void userAddsOktaUser(String ownerNameAlias, String userNameAliases) {
        final List<String> aliases = getUsersManager().splitAliases(userNameAliases);
        for (String userNameAlias : aliases) {
            ClientUser user = toClientUser(userNameAlias);
            if (getUsersManager().isUserCreated(user)) {
                throw new UserAlreadyCreatedException(
                        String.format("Cannot add user with alias %s to SSO team because user is already created",
                                userNameAlias));
            }
            user.setPassword("SSO" + user.getPassword());
            user.setName(user.getEmail());
            // Backend generates the unique username through the email. We try to predict this here:
            String uniqueUsername = user.getEmail().replaceAll("[^A-Za-z0-9]", "");
            if (uniqueUsername.length() > 21) {
                uniqueUsername = uniqueUsername.substring(0, 21);
            }
            user.setUniqueUsername(uniqueUsername);
            user.setBackendName(toClientUser(ownerNameAlias).getBackendName());
            user.setUserIsSSOUser();
            syncUserIdsForUsersCreatedThroughIdP(ownerNameAlias, user);
            // set backend for added okta users
            user.setBackendName(toClientUser(ownerNameAlias).getBackendName());
            oktaAPIClient.createUser(user.getName(), user.getEmail(), user.getPassword());
        }
    }

    public void userAddsKeycloakUser(String ownerNameAlias, String userNameAliases) {
        final List<String> aliases = getUsersManager().splitAliases(userNameAliases);
        for (String userNameAlias : aliases) {
            ClientUser user = toClientUser(userNameAlias);
            if (getUsersManager().isUserCreated(user)) {
                throw new UserAlreadyCreatedException(
                        String.format("Cannot add user with alias %s to SSO team because user is already created",
                                userNameAlias));
            }
            user.setPassword("SSO" + user.getPassword());
            user.setName(user.getEmail());
            // Backend generates the unique username through the email. We try to predict this here:
            user.setUniqueUsername(user.getEmail().replaceAll("[^A-Za-z0-9]", ""));
            user.setBackendName(toClientUser(ownerNameAlias).getBackendName());
            user.setUserIsSSOUser();
            syncUserIdsForUsersCreatedThroughIdP(ownerNameAlias, user);
            keycloakAPIClient.createUser(user.getName(), user.getFirstName(), user.getLastName(), user.getEmail(),
                    user.getPassword());
        }
    }

    public void userAddsKeycloakUserForE2EI(String ownerNameAlias, String userNameAliases) {
        final List<String> aliases = getUsersManager().splitAliases(userNameAliases);
        for (String userNameAlias : aliases) {
            ClientUser user = toClientUser(userNameAlias);
            if (getUsersManager().isUserCreated(user)) {
                throw new UserAlreadyCreatedException(
                        String.format("Cannot add user with alias %s to SSO team because user is already created",
                                userNameAlias));
            }
            log.info(user.toString());
            String backendName = toClientUser(ownerNameAlias).getBackendName();
            user.setBackendName(backendName);
            user.setUserIsSSOUser();
            syncUserIdsForUsersCreatedThroughIdP(ownerNameAlias, user);
            Backend backend = BackendConnections.get(backendName);
            String username = user.getUniqueUsername() + "@" + backend.getDomain();
            keycloakAPIClient.createUser(username, user.getFirstName(), user.getLastName(), user.getEmail(), user.getPassword());
        }
    }

    public void userAddsUserToOktaAndSCIM(String ownerNameAlias, String userNameAliases) {
        final ClientUser asUser = toClientUser(ownerNameAlias);

        final List<String> aliases = getUsersManager().splitAliases(userNameAliases);
        for (String userNameAlias : aliases) {
            final ClientUser userToCreate = toClientUser(userNameAlias);
            if (getUsersManager().isUserCreated(userToCreate)) {
                throw new UserAlreadyCreatedException(
                        String.format("Cannot add user with alias %s to SSO team because user is already created",
                                userNameAlias));
            }
            userToCreate.setPassword("SSO" + userToCreate.getPassword());
            userToCreate.setBackendName(asUser.getBackendName());
            // create users on Okta (without setting unique username or name in user manager)
            oktaAPIClient.createUser(userToCreate.getName(), userToCreate.getEmail(), userToCreate.getPassword());
            // create users via SCIM
            BufferedImage picture = QRCode.generateCode(userToCreate.getEmail(), Color.BLACK, Color.WHITE, 64, 1);
            // TODO: Set the last parameter from null to picture to enable picture upload
            String userId = scimClient.insert(asUser, userToCreate, null);
            userToCreate.setId(userId);
            userToCreate.setUserIsSSOUser();
            userToCreate.setManagedBySCIM();
        }
    }

    public void userAddsUserViaSCIM(String ownerNameAlias, String userNameAliases) {
        final ClientUser asUser = toClientUser(ownerNameAlias);

        final List<String> aliases = getUsersManager().splitAliases(userNameAliases);
        for (String userNameAlias : aliases) {
            final ClientUser userToCreate = toClientUser(userNameAlias);
            if (getUsersManager().isUserCreated(userToCreate)) {
                throw new UserAlreadyCreatedException(
                        String.format("Cannot add user with alias %s to SSO team because user is already created",
                                userNameAlias));
            }
            userToCreate.setPassword(userToCreate.getPassword());
            userToCreate.setBackendName(asUser.getBackendName());
            // create users via SCIM
            String userId = scimClient.insert(asUser, userToCreate, null);
            userToCreate.setId(userId);
            userToCreate.setManagedBySCIM();
            BackendConnections.get(asUser).acceptInviteViaBackdoor(asUser.getTeamId(), userToCreate);
        }
    }

    public void userAddsUserVia2FASCIM(String ownerNameAlias, String userNameAliases) {
        final ClientUser asUser = toClientUser(ownerNameAlias);
        Backend backend = BackendConnections.get(asUser);
        String teamID = backend.getAllTeams(asUser).get(0).getId();
        backend.unlock2FAuthenticationFeature(teamID);
        backend.disable2FAuthenticationFeature(teamID);
        userAddsUserViaSCIM(ownerNameAlias, userNameAliases);
        backend.enable2FAuthenticationFeature(teamID);
        backend.lock2FAuthenticationFeature(teamID);
    }

    private void syncUserIdsForUsersCreatedThroughIdP(String ownerNameAlias, ClientUser user) {
        user.getUserIdThroughOwner = () -> {
            final ClientUser asUser = toClientUser(ownerNameAlias);
            Backend backend = BackendConnections.get(asUser);
            List<TeamMember> teamMembers = backend.getTeamMembers(asUser);
            log.info("Looking for name: " + user.getName());
            for (TeamMember member : teamMembers) {
                String memberId = member.getUserId();
                log.info("member id: " + member.getUserId());
                String memberName = backend.getUserNameByID(backend.getDomain(), memberId, asUser);
                log.info("member name: " + memberName);
                if (user.getName().equals(memberName)) {
                    return memberId;
                }
            }
            throw new IOException(String.format("No user ID found for user %s Please verify you are using the right Team Owner account", user.getEmail()));
        };
    }

    public void userChangesNameViaSCIM(String userNameAlias, String name) {
        final ClientUser user = toClientUser(userNameAlias);
        scimClient.changeName(user.getId(), name);
    }

    public void userChangesUniqueUsernameViaSCIM(String userNameAlias, String name) {
        final ClientUser user = toClientUser(userNameAlias);
        scimClient.changeUniqueUsername(user.getId(), name);
    }

    public void userChangesProfilePictureViaSCIM(String userNameAlias, String code, String ownerAlias) {
        BufferedImage picture = QRCode.generateCode(code, Color.BLACK, Color.WHITE, 64, 1);
        // Upload asset to special resource with scimAuthToken
        final ClientUser user = toClientUser(userNameAlias);
        final ClientUser owner = toClientUser(ownerAlias);
        scimClient.changeProfilePicture(owner, user.getId(), picture);
    }

    public void userChangesAccentColorViaSCIM(String userNameAlias, String color) {
        final ClientUser user = toClientUser(userNameAlias);
        AccentColor accentColor = AccentColor.getByName(color);
        scimClient.changeAccentColor(user.getId(), accentColor.getId());
    }

    public void userChangesEmailViaSCIM(String userNameAlias, String email) {
        final ClientUser user = toClientUser(userNameAlias);
        scimClient.changeEmail(user.getId(), email);
    }

    public void cleanUpOkta() {
        oktaAPIClient.cleanUp();
    }

    public void cleanUpKeycloak() {
        keycloakAPIClient.cleanUp();
    }

    // endregion IdP

    // region Rich profile

    public void userUpdatesRichProfile(String userNameAlias, String key, String value) {
        final ClientUser user = toClientUser(userNameAlias);
        if (user.isManagedBySCIM()) {
            scimClient.updateRichInfo(user.getId(), key, value);
        } else {
            BackendConnections.get(user).updateRichInfo(user, key, value);
        }
    }

    public void userRemovesFieldFromRichProfile(String userNameAlias, String key) {
        final ClientUser user = toClientUser(userNameAlias);
        if (user.isManagedBySCIM()) {
            scimClient.removeKeyFromRichInfo(user.getId(), key);
        } else {
            BackendConnections.get(user).removeKeyFromRichInfo(user, key);
        }
    }

    // endregion Rich profile

    // region Stripe

    private String toStripeCustomerId(ClientUser owner, Team team) {
        Backend backend = BackendConnections.get(owner);
        return backend.getStripeCustomerIdForTeam(owner, team);
    }

    public boolean waitUntilStripeCustomerIsCreated(String teamName, String ownerNameAlias) {
        final ClientUser owner = toClientUser(ownerNameAlias);
        final Team team = BackendConnections.get(owner).getTeamByName(owner, teamName);

        // Custom implementation of FluentWait to prevent importing Selenium Driver in Common project
        long t = System.currentTimeMillis();
        long end = t+60000;
        while(System.currentTimeMillis() < end) {
            try {
                if (toStripeCustomerId(owner, team) != null) {
                    return true;
                }
            } catch (HttpRequestException ex) { }
            Timedelta.ofMillis(500).sleep();
        }
        return false;
    }

    public Timedelta getTeamTrialPeriodDuration(String teamName, String ownerNameAlias)
            throws StripeException {
        final ClientUser admin = toClientUser(ownerNameAlias);
        final Team team = BackendConnections.get(admin).getTeamByName(admin, teamName);
        return stripeAPIClient.getTrialPeriodDuration(toStripeCustomerId(admin, team));
    }

    public void UserChangesTrialPeriodDurationForTeam(String teamName, String ownerNameAlias, Timedelta newDuration)
            throws StripeException {
        final ClientUser admin = toClientUser(ownerNameAlias);
        final Team team = BackendConnections.get(admin).getTeamByName(admin, teamName);
        stripeAPIClient.changeTrialPeriodDuration(toStripeCustomerId(admin, team), newDuration);
    }

    public void UserChangesGracePeriodDurationForTeam(String teamName, String ownerNameAlias, Timedelta newDuration)
            throws StripeException {
        final ClientUser admin = toClientUser(ownerNameAlias);
        final Team team = BackendConnections.get(admin).getTeamByName(admin, teamName);
        stripeAPIClient.changeGracePeriodDuration(toStripeCustomerId(admin, team), newDuration);
    }

    public void UserXCreatesAndAddsCreditCard(String ownerName, String teamName) throws StripeException {
        final ClientUser owner = toClientUser(ownerName);
        final Team team = BackendConnections.get(owner).getTeamByName(owner, teamName);
        stripeAPIClient.createDefaultValidVisaCard(toStripeCustomerId(owner, team), team.getId());
    }

    public void UserXCreatesAndAddsUnchargeableCreditCard(String ownerName, String teamName) throws StripeException {
        final ClientUser owner = toClientUser(ownerName);
        final Team team = BackendConnections.get(owner).getTeamByName(owner, teamName);
        stripeAPIClient.createUnchargeableCreditCard(toStripeCustomerId(owner, team), team.getId());
    }

    public void UserXUpdatesBillingInfo(String ownerName, String teamName, String cardParamsAsJson) {
        final ClientUser owner = toClientUser(ownerName);
        Backend backend = BackendConnections.get(owner);
        final Team team = backend.getTeamByName(owner, teamName);
        final boolean isSuccess = CommonUtils.waitUntilTrue(CommonSteps.CUSTOMER_WAIT_UNTIL_TIMEOUT, CommonSteps.CUSTOMER_WAIT_UNTIL_INTERVAL, () -> {
            try {
                backend.updateBillingInfo(owner, team, new JSONObject(cardParamsAsJson));
                return true;
            } catch (HttpRequestException bre) {
                if (bre.getReturnCode() != 412) {
                    throw bre;
                }
            }
            return false;
        });
        assertThat("Updating billing info is NOT successful", isSuccess);
    }

    public void UserXSetsBillingPlan(String ownerName, String teamName, String planId) {
        final ClientUser owner = toClientUser(ownerName);
        Backend backend = BackendConnections.get(owner);
        final Team team = backend.getTeamByName(owner, teamName);
        final boolean isSuccess = CommonUtils.waitUntilTrue(CommonSteps.CUSTOMER_WAIT_UNTIL_TIMEOUT, CommonSteps.CUSTOMER_WAIT_UNTIL_INTERVAL, () -> {
            try {
                backend.setBillingPlan(owner, team, planId);
                return true;
            } catch (HttpRequestException bre) {
                if (bre.getReturnCode() != 412) {
                    throw bre;
                }
            }
            return false;
        });
        assertThat("Setting billing plan is NOT successful", isSuccess);
    }

    public void UserXSetsLegacyBillingPlan(String ownerName, String teamName, String planId) {
        final ClientUser owner = toClientUser(ownerName);
        Backend backend = BackendConnections.get(owner);
        final Team team = backend.getTeamByName(owner, teamName);
        backend.setLegacyBillingPlan(owner, team, planId);
    }

    public List<String> UserXGetsCurrenciesFromBillingPlans(String ownerName, String teamName) {
        final ClientUser owner = toClientUser(ownerName);
        Backend backend = BackendConnections.get(owner);
        final Team team = backend.getTeamByName(owner, teamName);
        return backend.getCurrenciesFromBillingPlans(owner, team);
    }

    public void UserXUpdatesCreditCard(String ownerName, String teamName, String cardParamsAsJson) throws StripeException {
        final ClientUser owner = toClientUser(ownerName);
        final Team team = BackendConnections.get(owner).getTeamByName(owner, teamName);
        stripeAPIClient.updateCreditCard(toStripeCustomerId(owner, team), new JSONObject(cardParamsAsJson));
    }

    public void UserXRemovesCreditCard(String ownerName, String teamName) throws StripeException {
        final ClientUser owner = toClientUser(ownerName);
        final Team team = BackendConnections.get(owner).getTeamByName(owner, teamName);
        stripeAPIClient.removeCreditCard(toStripeCustomerId(owner, team));
    }

    public void ICreateCoupon(String couponName, int discountValue, boolean isPercentage) throws StripeException {
        if (stripeAPIClient.isCouponExist(couponName)) {
            log.info(String.format("The coupon with name '%s' already exists", couponName));
            return;
        }
        stripeAPIClient.addCoupon(couponName, "forever", isPercentage ? discountValue : null,
                !isPercentage ? discountValue : null);
    }

    public boolean waitForSubscriptionsInStripe(String owner, String teamName, int numberOfSubscriptions) throws StripeException {
        final ClientUser user = toClientUser(owner);
        final Team team = BackendConnections.get(owner).getTeamByName(user, teamName);
        final String customerId = toStripeCustomerId(user, team);

        // Custom implementation of FluentWait to prevent importing Selenium Driver in Common project
        long t = System.currentTimeMillis();
        // 60 seconds
        long end = t+60000;
        while(System.currentTimeMillis() < end) {
            if (stripeAPIClient.getCustomerSubscriptionIds(customerId).size() == numberOfSubscriptions) {
                return true;
            }
            // pause 0.5 second
            Timedelta.ofMillis(500).sleep();
        }
        return false;
    }

    public void UserXCancelsSubscriptionsForTeam(String owner, String teamName) throws StripeException {
        final ClientUser user = toClientUser(owner);
        final Team team = BackendConnections.get(owner).getTeamByName(user, teamName);
        final String customerId = toStripeCustomerId(user, team);
        stripeAPIClient.cancelSubscriptions(stripeAPIClient.getCustomerSubscriptionIds(customerId));
    }

    public void UserXAddsSubscriptionWithPlan(String owner, String teamName, String planName) throws StripeException {
        final ClientUser user = toClientUser(owner);
        final Team team = BackendConnections.get(owner).getTeamByName(user, teamName);
        final String customerId = toStripeCustomerId(user, team);
        stripeAPIClient.cancelSubscriptions(stripeAPIClient.getCustomerSubscriptionIds(customerId));
        stripeAPIClient.addSubscription(customerId, stripeAPIClient.getPlanId(planName));
    }

    public void UserXSetsSubscriptionMetadata(String owner, String teamName, String metadata)
            throws StripeException {
        final ClientUser user = toClientUser(owner);
        final Team team = BackendConnections.get(owner).getTeamByName(user, teamName);
        final String customerId = toStripeCustomerId(user, team);
        stripeAPIClient.setSubscriptionMetadata(customerId, metadata);
    }

    public void UserXCreatesCustomInvoice(String ownerName, String teamName, int amount, String currency, String description) throws StripeException {
        final ClientUser owner = getUsersManager().findUserByNameOrNameAlias(ownerName);
        final Team team = BackendConnections.get(owner).getTeamByName(owner, teamName);
        String stripeCustomerId = toStripeCustomerId(owner, team);
        stripeAPIClient.createInvoiceItem(stripeCustomerId, amount, currency, description);
        stripeAPIClient.createInvoice(stripeCustomerId);
    }

    public Customer getStripeCustomer(String stripeCustomerId) throws StripeException {
        return stripeAPIClient.getCustomer(stripeCustomerId);
    }

    public void payAllOutstandingInvoicesInStripe(String ownerName, String teamName, boolean ignoreDeclinedPayment) throws StripeException {
        final ClientUser owner = getUsersManager().findUserByNameOrNameAlias(ownerName);
        final Team team = BackendConnections.get(owner).getTeamByName(owner, teamName);
        String stripeCustomerId = toStripeCustomerId(owner, team);
        stripeAPIClient.payAllOutstandingInvoices(stripeCustomerId, ignoreDeclinedPayment);
    }

    public void tryToPayAllOutstandingInvoicesInStripe(String ownerName, String teamName) throws StripeException {
        final ClientUser owner = getUsersManager().findUserByNameOrNameAlias(ownerName);
        final Team team = BackendConnections.get(owner).getTeamByName(owner, teamName);
        String stripeCustomerId = toStripeCustomerId(owner, team);
        stripeAPIClient.tryToPayAllOutstandingInvoices(stripeCustomerId);
    }

    public void forcePaymentOnLastOutstandingInvoiceInStripe(String ownerName, String teamName) throws StripeException {
        final ClientUser owner = getUsersManager().findUserByNameOrNameAlias(ownerName);
        final Team team = BackendConnections.get(owner).getTeamByName(owner, teamName);
        String stripeCustomerId = toStripeCustomerId(owner, team);
        stripeAPIClient.forcePaymentOnLastOutstandingInvoice(stripeCustomerId);
    }

    public void createInvoiceForAllPendingItemsInStripe(String ownerName, String teamName) throws StripeException {
        final ClientUser owner = getUsersManager().findUserByNameOrNameAlias(ownerName);
        final Team team = BackendConnections.get(owner).getTeamByName(owner, teamName);
        String stripeCustomerId = toStripeCustomerId(owner, team);
        stripeAPIClient.createInvoiceForAllPendingInvoiceItems(stripeCustomerId);
    }

    public void finalizeAllScheduledInvoicesInStripe(String ownerName, String teamName) throws StripeException {
        final ClientUser owner = getUsersManager().findUserByNameOrNameAlias(ownerName);
        final Team team = BackendConnections.get(owner).getTeamByName(owner, teamName);
        String stripeCustomerId = toStripeCustomerId(owner, team);
        stripeAPIClient.finalizeAllScheduledInvoices(stripeCustomerId);
    }

    public int getNumberOfInvoicesInStripe(String owner, String teamName) throws StripeException {
        final ClientUser user = toClientUser(owner);
        final Team team = BackendConnections.get(owner).getTeamByName(user, teamName);
        final String customerId = toStripeCustomerId(user, team);
        return stripeAPIClient.getNumberOfInvoices(customerId);
    }

    // endregion Stripe


    // region Services

    public ServiceInfo createServiceInfo(ClientUser ownerOrAdminUser, boolean newState, String serviceName, String teamName) {
        Backend backend = BackendConnections.get(ownerOrAdminUser);
        Team team = backend.getTeamByName(ownerOrAdminUser, teamName);
        return new ServiceInfo(team, serviceName, newState);
    }

    public void userSwitchesUsersServicesForTeam(String ownerOrAdminUserAlias,
                                                 boolean newState, String serviceNames, String teamName) {
        final ClientUser ownerOrAdminUser = toClientUser(ownerOrAdminUserAlias);
        Arrays.stream(serviceNames.split(","))
                .map(String::trim)
                .forEach(serviceName ->
                        BackendConnections.get(ownerOrAdminUser)
                                .switchServiceForTeam(ownerOrAdminUser, createServiceInfo(ownerOrAdminUser, newState, serviceName, teamName)));
    }

    // endregion Services

    // region Deep Links

    public String getDeepLinkForConversation(String conversationName, String senderAlias) {
        String conversationId = toConvoId(senderAlias, conversationName);
        return String.format("wire://conversation/%s", conversationId);
    }

    public String getDeepLinkForUserProfile(String nameAlias) {
        final ClientUser user = toClientUser(nameAlias);
        return String.format("wire://user/%s", user.getId());
    }

    // endregion Deep Links

    // region Custom Backend

    public String addDomainForStagingToStaging() {
        String domain = generatingRandomAlphanumericString(10) + ".com";
        customDomains.add(domain);
        BackendConnections.get("staging").addCustomBackendDomain(domain,
                BackendConnections.get("staging").getDeeplinkUrl(),
                BackendConnections.get("staging").getWebappUrl());
        return domain;
    }

    public String addDomainForAntaToStaging() {
        String domain = generatingRandomAlphanumericString(10) + ".com";
        customDomains.add(domain);
        BackendConnections.get("staging").addCustomBackendDomain(domain,
                BackendConnections.get("anta").getDeeplinkUrl(),
                BackendConnections.get("anta").getWebappUrl());
        return domain;
    }

    public String addDomainForQAFixedSSOToStaging() {
        String domain = generatingRandomAlphanumericString(10) + ".com";
        customDomains.add(domain);
        BackendConnections.get("staging").addCustomBackendDomain(domain,
                BackendConnections.get("QA-Fixed-SSO").getDeeplinkUrl(),
                BackendConnections.get("QA-Fixed-SSO").getWebappUrl());
        return domain;
    }

    public String addDomainForStagingAndSpecialWebappToQADemo(String webappURL) {
        String domain = generatingRandomAlphanumericString(10) + ".com";
        Backend customBackend = BackendConnections.get("QA Demo");
        customDomains.add(domain);
        customBackend.addCustomBackendDomain(domain,
                BackendConnections.get("staging").getDeeplinkUrl(),
                webappURL);
        return domain;
    }

    public void createWronglyConfiguredBackend(String customBackendDomain, String configType) {
        String configURL = "";
        String webappURL = "https://app.wire." + customBackendDomain + "/";

        switch (configType) {
            case "broken":
                configURL = "https://s3-eu-west-1.amazonaws.com/wire-taco-test/broken.json";
                break;
            case "missing backendURL":
                configURL = "https://s3-eu-west-1.amazonaws.com/wire-taco-test/missing_backendURL.json";
                break;
            case "missing backendWSURL":
                configURL = "https://s3-eu-west-1.amazonaws.com/wire-taco-test/missing_backendWSURL.json";
                break;
            case "missing title":
                configURL = "https://s3-eu-west-1.amazonaws.com/wire-taco-test/missing_title.json";
                break;
            case "malformed URL": // Mobile only
                configURL = "https://s3-eu-west-1.amazonaws.com/wire-taco-test/malformed_URL.json";
                break;
            case "unreachable URL": // Mobile only
                configURL = "https://s3-eu-west-1.amazonaws.com/wire-taco-test/unreachable_URL.json";
                break;
            default:
                throw new IllegalArgumentException("No such config type");
        }
        customDomains.add(customBackendDomain);
        BackendConnections.get("staging").addCustomBackendDomain(customBackendDomain, configURL, webappURL);
    }

    public void createUnreachableBackendWithUnreachableConfig(String customBackendDomain) {
        String configURL = "https://unreachable.local/config.json";
        String webappURL = "https://unreachable.local/";
        customDomains.add(customBackendDomain);
        BackendConnections.get("staging").addCustomBackendDomain(customBackendDomain, configURL, webappURL);
    }

    public void deleteCustomBackend(String customBackendDomain) {
        BackendConnections.get("staging").deleteCustomBackendDomain(customBackendDomain);
    }

    public void waitUntilPodIsAvailable(String backendName, String service) throws Exception {
        final int maxRetries = 60;
        for (int i = 0; i < maxRetries; i++) {
            String output = CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName,
                    new String[]{KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(),
                            "get", "pods", "-l", "app=" + service, "-o",
                            "jsonpath={..status.conditions[?(@.type==\"Ready\")]}"},
                    Timedelta.ofSeconds(60));
            log.info(output);
            if (output.contains("\"status\":\"True\"")) {
                break;
            } else if (i == (maxRetries - 1)) {
                throw new Exception("Pods are not ready to use");
            }
            log.info("waiting for pod to be available");
            Timedelta.ofSeconds(1).sleep();
        }

    }

    public void waitUntilIngressPodIsAvailable(String backendName) throws Exception {
        final int maxRetries = 60;
        for (int i = 0; i < maxRetries; i++) {
            String output = CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName,
                    new String[]{KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(),
                            "get", "all", "-l", "app=nginx-ingress", "-l", "component=controller", "-o",
                            "jsonpath={..status.conditions[?(@.type==\"Ready\")]}"},
                    Timedelta.ofSeconds(60));
            log.info(output);
            if (output.contains("\"status\":\"True\"")) {
                break;
            } else if (i == (maxRetries-1)) {
                throw new Exception ("Pods are not ready to use");
            }
            log.info("waiting for pod to be available");
            Timedelta.ofSeconds(1).sleep();
        }
    }

    public void waitUntilSFTPodIsAvailable(String backendName) throws Exception {
        String namespace = BackendConnections.get(backendName).getK8sNamespace();
        final int maxRetries = 60;
        for (int i = 0; i < maxRetries; i++) {
            String output = CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName,
                    new String[]{KUBECTLPATH + "kubectl", "-n", namespace, "get", "pods", "-l",
                            "app.kubernetes.io/name=sftd", "-o", "jsonpath={..status.conditions[?(@.type==\"Ready\")]}"},
                    Timedelta.ofSeconds(60));
            String output2 = CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName,
                    new String[]{KUBECTLPATH + "kubectl", "-n", namespace, "get", "pods", "-l",
                            "app.kubernetes.io/name=join-call", "-o", "jsonpath={..status.conditions[?(@.type==\"Ready\")]}"},
                    Timedelta.ofSeconds(60));
            log.info(output + output2);
            if ((output.contains("\"status\":\"True\"")) && (output2.contains("\"status\":\"True\""))) {
                break;
            } else if (i == (maxRetries - 1)) {
                throw new Exception("Pods are not ready to use");
            }
            log.info("waiting for pod to be available");
            Timedelta.ofSeconds(1).sleep();
        }
    }

    public void checkPodsStatusOn(String backendName, String service) throws Exception {
        final int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            String output = CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName,
                    new String[]{KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(),
                            "get", "pods", "-l", "app=" + service, "-o",
                            "jsonpath={..status.conditions[?(@.type==\"Ready\")]}"},
                    Timedelta.ofSeconds(60));
            log.info(output);
            if (output.contains("\"status\":\"True\"")) {
                break;
            } else if (i == (maxRetries - 1)) {
                throw new Exception("Enabling Pod was not successful");
            }
            log.info("waiting for pod to be available");
            Timedelta.ofSeconds(1).sleep();
        }
    }

    public void checkPodsStatusOff(String backendName, String service) throws Exception {
        final int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            String output = CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName,
                    new String[]{KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(),
                            "get", "pods", "-l", "app=" + service, "-o",
                            "jsonpath={..status.conditions[?(@.type==\"Ready\")]}"},
                    Timedelta.ofSeconds(60));
            log.info(output);
            if (!output.contains("\"status\":\"True\"")) {
                break;
            } else if (i == (maxRetries - 1)) {
                throw new Exception("Disabling Pod was not successful");
            }
            log.info("waiting for pod to be disabled");
            Timedelta.ofSeconds(1).sleep();
        }
    }

    public void checkIngressPodsStatusOn(String backendName) throws Exception {
        //Turning Ingress on takes a bit more time
        final int maxRetries = 20;
        for (int i = 0; i < maxRetries; i++) {
            String output = CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName,
                    new String[]{KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(),
                            "get", "all", "-l", "app=nginx-ingress", "-l", "component=controller", "-o",
                            "jsonpath={..status.conditions[?(@.type==\"Ready\")]}"},
                    Timedelta.ofSeconds(60));
            log.info(output);
            if (output.contains("\"status\":\"True\"")) {
                break;
            } else if (i == (maxRetries - 1)) {
                throw new Exception("Enabling Pod was not successful");
            }
            log.info("waiting for pod to be available");
            Timedelta.ofSeconds(1).sleep();
        }
    }

    public void checkIngressPodsStatusOff(String backendName) throws Exception {
        //Turning Ingress off takes a bit more time
        final int maxRetries = 20;
        for (int i = 0; i < maxRetries; i++) {
            String output = CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName,
                    new String[]{KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(),
                            "get", "all", "-l", "app=nginx-ingress", "-l", "component=controller", "-o",
                            "jsonpath={..status.conditions[?(@.type==\"Ready\")]}"},
                    Timedelta.ofSeconds(60));
            log.info(output);
            if (!output.contains("\"status\":\"True\"")) {
                break;
            } else if (i == (maxRetries - 1)) {
                throw new Exception("Disabling Pod was not successful");
            }
            log.info("waiting for pod to be disabled");
            Timedelta.ofSeconds(1).sleep();
        }
    }

    public void checkSFTPodStatusOn(String backendName) throws Exception {
        final int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            String output = CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName,
                    new String[]{KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(),
                            "get", "pods", "-l", "app.kubernetes.io/name=sftd", "-o",
                            "jsonpath={..status.conditions[?(@.type==\"Ready\")]}"},
                    Timedelta.ofSeconds(60));
            String output2 = CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName,
                    new String[]{KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(),
                            "get", "pods", "-l", "app.kubernetes.io/name=join-call", "-o",
                            "jsonpath={..status.conditions[?(@.type==\"Ready\")]}"},
                    Timedelta.ofSeconds(60));
            log.info(output + output2);
            if ((output.contains("\"status\":\"True\"")) && (output2.contains("\"status\":\"True\""))) {
                break;
            } else if (i == (maxRetries - 1)) {
                throw new Exception("Enabling Pod was not successful");
            }
            log.info("waiting for pod to be available");
            Timedelta.ofSeconds(1).sleep();
        }
    }

    public void checkSFTPodStatusOff(String backendName) throws Exception {
        final int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            String output = CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName,
                    new String[]{KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(),
                            "get", "pods", "-l", "app.kubernetes.io/name=sftd", "-o",
                            "jsonpath={..status.conditions[?(@.type==\"Ready\")]}"},
                    Timedelta.ofSeconds(60));
            String output2 = CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName,
                    new String[]{KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(),
                            "get", "pods", "-l", "app.kubernetes.io/name=join-call", "-o",
                            "jsonpath={..status.conditions[?(@.type==\"Ready\")]}"},
                    Timedelta.ofSeconds(60));
            log.info(output + output2);
            if ((!output.contains("\"status\":\"True\"")) && (!output2.contains("\"status\":\"True\""))) {
                break;
            } else if (i == (maxRetries - 1)) {
                throw new Exception("Disabling Pod was not successful");
            }
            log.info("waiting for pod to be disabled");
            Timedelta.ofSeconds(1).sleep();
        }
    }

    public void turnFederatorInBackendOn(String backendName) {
        CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName, new String[]
                {KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(), "scale",
                        "--replicas=1", "deployment", "-l app=federator"}, Timedelta.ofSeconds(60));
    }

    public void turnFederatorInBackendOff(String backendName) {
        touchedFederator.add(backendName);
        CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName, new String[]
                {KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(), "scale",
                        "--replicas=0", "deployment", "-l app=federator"}, Timedelta.ofSeconds(60));
    }

    public void turnBrigInBackendOn(String backendName) {
        CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName, new String[]
                {KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(), "scale",
                        "--replicas=1", "deployment", "-l app=brig"}, Timedelta.ofSeconds(60));
    }

    public void turnBrigInBackendOff(String backendName) {
        touchedBrig.add(backendName);
        CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName, new String[]
                {KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(), "scale",
                        "--replicas=0", "deployment", "-l app=brig"}, Timedelta.ofSeconds(60));
    }

    public void turnGalleyInBackendOn(String backendName) {
        CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName, new String[]
                {KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(), "scale",
                        "--replicas=1", "deployment", "-l app=galley"}, Timedelta.ofSeconds(60));
    }

    public void turnGalleyInBackendOff(String backendName) {
        touchedGalley.add(backendName);
        CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName, new String[]
                {KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(), "scale",
                        "--replicas=0", "deployment", "-l app=galley"}, Timedelta.ofSeconds(60));
    }

    public void turnIngressInBackendOn(String backendName) {
        CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName, new String[]
                {KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(), "patch",
                        "daemonset", "nginx-ingress-controller-controller", "--type", "json",
                        "-p=[{\"op\": \"remove\", \"path\": \"/spec/template/spec/nodeSelector/non-existing\"}]"},
                Timedelta.ofSeconds(60));
    }

    public void turnIngressInBackendOff(String backendName) {
        touchedIngress.add(backendName);
        CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName, new String[]
                {KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(), "patch",
                        "daemonset", "nginx-ingress-controller-controller", "-p",
                        "{\"spec\": {\"template\": {\"spec\": {\"nodeSelector\": {\"non-existing\": \"true\"}}}}}"},
                Timedelta.ofSeconds(60));
    }

    public void turnSFTInBackendOn(String backendName) {
        CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName, new String[]
                {KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(), "scale",
                        "--replicas=0", "statefulsets", "-l", "app.kubernetes.io/name=sftd"}, Timedelta.ofSeconds(60));
        CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName, new String[]
                {KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(), "scale",
                        "--replicas=0", "deployment", "-l", "app.kubernetes.io/name=join-call"}, Timedelta.ofSeconds(60));
    }

    public void turnSFTInBackendOff(String backendName) {
        touchedSFT.add(backendName);
        CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName, new String[]
                {KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(), "scale",
                        "--replicas=1", "statefulsets", "-l", "app.kubernetes.io/name=sftd"}, Timedelta.ofSeconds(60));
        CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName, new String[]
                {KUBECTLPATH + "kubectl", "-n", BackendConnections.get(backendName).getK8sNamespace(), "scale",
                        "--replicas=3", "deployment", "-l", "app.kubernetes.io/name=join-call"}, Timedelta.ofSeconds(60));
    }

    public void federateBackends(String fromBackendName, String toBackendName) {
        Backend fromBackend = BackendConnections.get(fromBackendName);
        Backend toBackend = BackendConnections.get(toBackendName);
        String randomPostfix = UUID.randomUUID().toString().substring(0, 4);
        CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(fromBackend.getBackendName(), new String[]
                {KUBECTLPATH + "kubectl", "-n", fromBackend.getK8sNamespace(), "run", "--rm=true", "--restart=Never", "-i",
                        "--image", "curlimages/curl", "curl-exe-" + randomPostfix, "--", "curl", "-s" , "-X", "POST",
                        "http://brig:8080/i/federation/remotes", "-H", "Content-Type: application/json", "-v", "-d",
                        "{ \"domain\": \"" + toBackend.getDomain() + "\",  \"search_policy\": \"full_search\" }" },
                Timedelta.ofSeconds(60));
    }

    public void defederateBackends(String fromBackendName, String toBackendName) {
        Backend fromBackend = BackendConnections.get(fromBackendName);
        Backend toBackend = BackendConnections.get(toBackendName);
        defederatedBackends.put(fromBackendName, toBackendName);
        String randomPostfix = UUID.randomUUID().toString().substring(0, 4);
        CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(fromBackend.getBackendName(), new String[]
                {KUBECTLPATH + "kubectl", "-n", fromBackend.getK8sNamespace(), "run", "--rm=true", "--restart=Never",
                        "-i", "--image", "curlimages/curl", "curl-exe-" + randomPostfix, "--", "curl", "-s" , "-X", "DELETE",
                        "http://brig:8080/i/federation/remotes/" + toBackend.getDomain() },
                Timedelta.ofSeconds(60));
    }

    public String getSearchPolicy(String fromBackendName) {
        Backend fromBackend = BackendConnections.get(fromBackendName);
        String randomPostfix = UUID.randomUUID().toString().substring(0, 4);
        String result = CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(fromBackend.getBackendName(), new String[]
                        {KUBECTLPATH + "kubectl", "-n", fromBackend.getK8sNamespace(), "run", "--rm=true", "--restart=Never",
                                "-i", "--image", "curlimages/curl", "curl-exe-" + randomPostfix, "--", "curl", "-s" ,
                                "http://brig:8080/i/federation/remotes" },
                Timedelta.ofSeconds(60));
        log.info("Stdout Output: " + result);
        return result;
    }

    public void revokeCertificate(String backendName, String serialNumberHex, BigInteger serialNumberDec) {
        Backend backend = BackendConnections.get(backendName);
        String tokenResult = CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName, new String[]
                {KUBECTLPATH + "kubectl", "-n", backend.getK8sNamespace() + "-smallstep",
                        "exec", "--stdin", "smallstep-step-certificates-0", "--", "step", "ca", "token",
                        "--revoke", "--issuer", backend.getDomain(), "--password-file", "/home/step/secrets/password",
                        "--ca-url=https://localhost:9000", String.valueOf(serialNumberDec) }, Timedelta.ofSeconds(60));
        CommonUtils.setEnvironmentVariableKubeConfigAndExecuteCommand(backendName, new String[]
                {KUBECTLPATH + "kubectl", "-n", backend.getK8sNamespace() + "-smallstep",
                        "exec", "--stdin", "smallstep-step-certificates-0", "--",
                        "step", "ca", "revoke", "--token", tokenResult.strip(),
                        "--ca-url=https://localhost:9000", serialNumberHex }, Timedelta.ofSeconds(60));
    }

    public static String generatingRandomAlphanumericString(int targetStringLength) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    // endregion Custom Backend

    // region Digital Signature

    public void enableDigitalSignatureFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.enableDigitalSignatureFeature(dstTeam);
    }

    public void disableDigitalSignatureFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.disableDigitalSignatureFeature(dstTeam);
    }

    // endregion Digital Signature

    // region App Lock

    public JSONObject getAppLockFeatureSettings(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        return backend.getAppLockFeatureSettings(dstTeam);
    }

    public void enableForceAppLockFeature(String adminUserAlias, String teamName, int seconds) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.enableForceAppLockFeature(dstTeam, seconds);
    }

    public void disableForceAppLockFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.disableForceAppLockFeature(dstTeam, 60);
    }

    public void disableForceAppLockFeature(String adminUserAlias, String teamName, int seconds) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.disableForceAppLockFeature(dstTeam, seconds);
    }

    public void disableAppLockFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.disableAppLockFeature(dstTeam);
    }

    public void enableAppLockFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.enableAppLockFeature(dstTeam);
    }

    // endregion App Lock

    // region Legal Hold

    public void whitelistTeamForLegalHold(String userAlias, String teamName) {
        final ClientUser adminUser = toClientUser(userAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.whitelistTeamForLegalHold(dstTeam);
    }

    public String registerLegalHoldService(String userAlias, String teamName) {
        // Register the service now
        final ClientUser adminUser = toClientUser(userAlias);
        adminUser.setRegisteredLegalHoldService(true);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        // Whitelist team for legal hold on backend before registering a service
        // Set explicit legal hold consent for team/all team members
        whitelistTeamForLegalHold(userAlias, teamName);
        return backend.registerLegalHoldService(adminUser, dstTeam);
    }

    public String breakLegalHoldService(String userAlias, String teamName) {
        final ClientUser adminUser = toClientUser(userAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        return backend.breakLegalHoldService(adminUser, dstTeam);
    }

    public void unregisterLegalHoldService(String userAlias, String teamName) {
        final ClientUser adminUser = toClientUser(userAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.unregisterLegalHoldService(adminUser, dstTeam.getId());
    }

    public void adminSendsLegalHoldRequestForUser(String adminUserAlias, String userAlias) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        BackendConnections.get(adminUser).adminSendsLegalHoldRequestForUser(adminUser, toClientUser(userAlias));
    }

    public void adminTurnsOffLegalHoldForUser(String adminUserAlias, String userAlias) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        BackendConnections.get(adminUser).adminTurnsOffLegalHoldForUser(adminUser, toClientUser(userAlias));
    }

    public void userAcceptsLegalHoldRequest(String userAlias) {
        final ClientUser user = toClientUser(userAlias);
        BackendConnections.get(user).userAcceptsLegalHoldRequest(toClientUser(userAlias));
    }

    public List<String> getTextMessagesOnExternalLegalHoldService(String conversation, String userAlias) throws IOException, ParseException {
        LegalHoldServiceClient client = new LegalHoldServiceClient();
        return client.getTextMessages(toConvoId(userAlias, conversation));
    }

    public List<String> getImagesQRCodeOnExternalLegalHoldService(String conversation, String userAlias) throws IOException, ParseException {
        LegalHoldServiceClient client = new LegalHoldServiceClient();
        return client.getImagesQRCode(toConvoId(userAlias, conversation));
    }

    public List<String> getFilesOnExternalLegalHoldService(String conversation, String userAlias) throws IOException, ParseException {
        LegalHoldServiceClient client = new LegalHoldServiceClient();
        return client.getFileNames(toConvoId(userAlias, conversation));
    }

    public List<String> getAudioFilesOnExternalLegalHoldService(String conversation, String userAlias) throws IOException, ParseException {
        LegalHoldServiceClient client = new LegalHoldServiceClient();
        return client.getAudioFiles(toConvoId(userAlias, conversation));
    }

    public List<String> getVideoFileNamesOnExternalLegalHoldService(String conversation, String userAlias) throws IOException, ParseException {
        LegalHoldServiceClient client = new LegalHoldServiceClient();
        return client.getVideoFiles(toConvoId(userAlias, conversation));
    }

    // endregion Legal Hold

    // region File Sharing

    public boolean isFileSharingFeatureLocked(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        return backend.getFileSharingFeatureSettings(dstTeam).getString("lockStatus").equals("locked");
    }

    public boolean isFileSharingFeatureEnabled(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        return backend.getFileSharingFeatureSettings(dstTeam).getString("status").equals("enabled");
    }

    public void unlockFileSharingFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.unlockFileSharingFeature(dstTeam);
    }

    public void lockFileSharingFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.lockFileSharingFeature(dstTeam);
    }

    public void enableFileSharingFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.enableFileSharingFeature(dstTeam);
    }

    public void disableFileSharingFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.disableFileSharingFeature(dstTeam);
    }

    // endregion File Sharing

    // region Self-Deleting Messages

    public JSONObject getSelfDeletingMessagesSettings(String adminUserAlias) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        return backend.getSelfDeletingMessagesSettings(adminUser);
    }

    public void unlockSelfDeletingMessagesFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.unlockSelfDeletingMessagesFeature(dstTeam);
    }

    public void lockSelfDeletingMessagesFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.lockSelfDeletingMessagesFeature(dstTeam);
    }

    public void enableForcedSelfDeletingMessages(String adminUserAlias, String teamName, long seconds) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.enableForcedSelfDeletingMessages(dstTeam, seconds);
    }

    public void disableForcedSelfDeletingMessages(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.disableForcedSelfDeletingMessages(dstTeam);
    }

    public void disableSelfDeletingMessagesFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.disableSelfDeletingMessagesFeature(dstTeam);
    }

    public void enableSelfDeletingMessagesFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.enableSelfDeletingMessagesFeature(dstTeam);
    }

    // endregion Self-Deleting Messages

    public void userChangesRoleOtherInConversation(String userName, String subjectUsers, String conversationRole, String conversationName) {
        final ClientUser user = toClientUser(userName);
        String convoID = toConvoId(user, conversationName);
        Backend backend = BackendConnections.get(user);
        for (String userToNameAlias : getUsersManager().splitAliases(subjectUsers)) {
            Conversation conversation = toConvoObj(user, conversationName);
            ClientUser subject = toClientUser(userToNameAlias);
            backend.userChangesRoleOtherInConversation(user, conversation, subject, conversationRole);
        }
    }

    public Conversation getConversation(String userName, String conversationName) {
        ClientUser user = toClientUser(userName);
        Backend backend = BackendConnections.get(user);
        try {
            // get conversation from pure conversation name
            return backend.getConversationByName(user, conversationName);
        } catch (Exception e) {
            // get conversation from username
            final ClientUser convUser = getUsersManager().findUserByNameOrNameAlias(conversationName);
            return backend.getConversationByName(user, convUser);
        }
    }

    // region Poll messages

    private JSONArray getButtonsJsonArrayFromCsv(String buttonsCsv) {
        JSONArray buttonsJsonArray = new JSONArray();
        for (String buttonText : buttonsCsv.split("\\s*,\\s*")) {
            buttonsJsonArray.put(buttonText);
        }
        return buttonsJsonArray;
    }

    public String userSendsPollMessageToConversation(String senderAlias, String convoName, @Nullable String deviceName,
                                                     Timedelta msgTimer, String message, String title, String buttons, int legalHoldStatus) {
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getId();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        final boolean expReadConfirm = conversation.getType().map(t -> {
                    switch (t) {
                        case 0:
                            // Group
                            return conversation.isReceiptModeEnabled();
                        case 2:
                            // 1:1
                            return isSendReadReceiptEnabled(senderAlias);
                        default:
                            return false;
                    }
                })
                .orElse(false);

        if (title != null) {
            message = "**" + title + "**" + System.lineSeparator() + message;
        }

        JSONArray buttonsJsonArray = getButtonsJsonArrayFromCsv(buttons);

        return testServiceClient.sendCompositeText(toClientUser(senderAlias), deviceName, convoDomain, convoId,
                msgTimer, expReadConfirm, message, buttonsJsonArray, legalHoldStatus);
    }

    public void userSendsPollMessageWithMentions(String senderAlias, String convoName, @Nullable String deviceName,
                                                 Timedelta msgTimer, String message, String title, String buttons) {
        // replace with real username in whole text
        message = getUsersManager().replaceAliasesOccurrences(message, FindBy.NAME_ALIAS);

        if (title != null) {
            message = "**" + title + "**" + System.lineSeparator() + message;
        }

        List<Mention> mentions = getMentionsList(message);
        final Conversation conversation = toConvoObj(senderAlias, convoName);
        final String convoId = conversation.getQualifiedID().getID();
        final String convoDomain = conversation.getQualifiedID().getDomain();
        JSONArray buttonsJsonArray = getButtonsJsonArrayFromCsv(buttons);

        testServiceClient.sendCompositeTextWithMentions(toClientUser(senderAlias), deviceName, convoId, convoDomain,
                msgTimer, message, mentions, buttonsJsonArray);
    }

    public void userSendsButtonActionConfirmationToLatestPollMessage(String senderAlias, String receiverAlias, @Nullable String deviceName, String convoName,
                                                                     String buttonText) {
        ClientUser sender = toClientUser(senderAlias);
        ClientUser receiver = toClientUser(receiverAlias);
        Conversation conversation = toConvoObj(sender, convoName);
        String convoId = conversation.getQualifiedID().getID();
        String convoDomain = conversation.getQualifiedID().getDomain();
        testServiceClient.sendButtonActionConfirmation(sender,
                receiver.getId(),
                deviceName,
                convoId,
                getRecentPollMessageId(sender, convoId, convoDomain, deviceName),
                getRecentPollMessageButtonIdByText(sender, convoId, convoDomain, deviceName, buttonText));
    }

    private String getRecentPollMessageId(ClientUser user, String convoId, String convoDomain, @Nullable String deviceName) {
        return getRecentPollMessage(user, convoId, convoDomain, deviceName).getString("id");
    }

    private JSONObject getRecentPollMessage(ClientUser user, String convoId, String convoDomain, @Nullable String deviceName) {
        JSONArray messages = testServiceClient.getMessages(user, deviceName, convoId, convoDomain);
        if (messages.isEmpty()) {
            throw new IllegalStateException("The conversation contains no messages");
        }

        for (int i = messages.length() - 1; i >= 0; i--) {
            JSONObject message = messages.getJSONObject(i);
            if (message.has("content") && message.getJSONObject("content").has("buttonList")) {
                return message;
            }
        }

        throw new RuntimeException("Could not find message with buttonList in: " + messages);
    }

    private String getRecentPollMessageButtonIdByText(ClientUser user, String convoId, String convoDomain, @Nullable String deviceName, String buttonText) {
        CommonUtils.waitUntilTrue(DEFAULT_WAIT_UNTIL_TIMEOUT, DEFAULT_WAIT_UNTIL_INTERVAL,
                () -> !testServiceClient.getMessageIds(user, deviceName, convoId, convoDomain).isEmpty());

        JSONArray messages = testServiceClient.getMessages(user, deviceName, convoId, convoDomain);
        if (messages.isEmpty()) {
            throw new IllegalStateException("The conversation contains no messages");
        }

        if (testServiceClient.isKaliumTestservice()) {
            JSONObject lastMessage = getRecentPollMessage(user, convoId, convoDomain, deviceName);

            if (lastMessage.has("content") && !lastMessage.getJSONObject("content").has("buttonList")) {
                throw new IllegalStateException("The last message in conversation isn't poll message");
            }

            if (lastMessage.getJSONObject("content").has("buttonList")) {
                JSONArray buttonItems = lastMessage.getJSONObject("content").getJSONArray("buttonList");
                for (int i = 0; i < buttonItems.length(); i++) {
                    JSONObject buttonItem = buttonItems.getJSONObject(i);
                    log.info("Found button: " + buttonItem.getString("text") + " with id " + buttonItem.getString("id"));
                    if (buttonItem.getString("text").equals(buttonText)) {
                        return buttonItem.getString("id");
                    }
                }
            }
        } else {
            JSONObject lastMessage = messages.getJSONObject(messages.length() - 1);

            if (lastMessage.has("type") && !lastMessage.getString("type").equals("PayloadBundleType.COMPOSITE")) {
                throw new IllegalStateException("The last message in conversation isn't poll message");
            }

            // ETS
            if (lastMessage.getJSONObject("content").has("items")) {
                JSONArray items = lastMessage.getJSONObject("content").getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    if (item.has("button")) {
                        JSONObject buttonItem = item.getJSONObject("button");
                        if (buttonItem.getString("text").equals(buttonText)) {
                            return buttonItem.getString("id");
                        }
                    }
                }
            }
        }


        throw new IllegalStateException("Expected button not found on the last message in conversation");
    }

    // endregion Poll messages

    // region Delegated Admins team

    public void UserXEnablesDelegatedAdminsFeatureForTeam(String ownerNameAlias, String teamName) {
        final ClientUser owner = toClientUser(ownerNameAlias);
        Backend backend = BackendConnections.get(owner);
        final Team dstTeam = backend.getTeamByName(owner, teamName);
        backend.enableDelegatedAdminsFeature(dstTeam);
    }

    // endregion Delegated Admins team


    // region Conference calling backdoor

    public void enableConferenceCallingFeatureViaBackdoorTeam(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.unlockConferenceCallingFeature(dstTeam);
        backend.enableConferenceCallingBackdoorViaBackdoorTeam(dstTeam);
    }

    public void disableConferenceCallingFeatureViaBackdoorTeam(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.disableConferenceCallingBackdoorViaBackdoorTeam(dstTeam);
    }

    public boolean waitUntilConferenceCallingFeatureEnabled(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);

        // Custom implementation of FluentWait to prevent importing Selenium Driver in Common project
        long t = System.currentTimeMillis();
        // 60 seconds
        long end = t+60000;
        while(System.currentTimeMillis() < end) {
            if (backend.isConferenceCallingEnabled(dstTeam, adminUser)) {
                return true;
            }
            // pause 0.5 second
            Timedelta.ofMillis(500).sleep();
        }
        return false;
    }

    public boolean isConferenceCallingFeatureEnabled(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        return backend.isConferenceCallingEnabled(dstTeam, adminUser);
    }

    public void enableConferenceCallingFeatureViaBackdoorPersonalUser(String personalUsers) {
        for (String memberAlias : getUsersManager().splitAliases(personalUsers)) {
            final ClientUser userToEnableConferenceCall = toClientUser(memberAlias);
            Backend backend = BackendConnections.get(userToEnableConferenceCall);
            backend.enableConferenceCallingViaBackdoorPersonalUser(userToEnableConferenceCall);
        }
    }

    public void upgradeToEnterprisePlanResult(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.upgradeToEnterprisePlanResult(dstTeam);
    }

    public String getSFTServer(String userAlias) {
        ClientUser user = getUsersManager().findUserByNameOrNameAlias(userAlias);
        JSONObject json = BackendConnections.get(user).getCallConfig(user);
        JSONArray sftServers = json.getJSONArray("sft_servers");
        JSONArray urls = sftServers.getJSONObject(0).getJSONArray("urls");
        return urls.getString(0);
    }

    public String getTURNServer(String userAlias) {
        ClientUser user = getUsersManager().findUserByNameOrNameAlias(userAlias);
        JSONObject json = BackendConnections.get(user).getCallConfig(user);
        JSONArray turnServers = json.getJSONArray("ice_servers");
        for (int i = 0; i < turnServers.length(); i++) {
            String url = turnServers.getJSONObject(i).getJSONArray("urls").getString(0);
            if (url.contains("turns:") && url.contains("?transport=tcp")) {
                url = url.replace("turns:", "https://");
                url = url.replace("?transport=tcp", "");
                return url;
            }
        }

        throw new RuntimeException("No fitting TURN server found (secure and tcp) in: " + json.toString());
    }

    public String getCRLProxy(String userAlias) {
        ClientUser user = getUsersManager().findUserByNameOrNameAlias(userAlias);
        JSONObject response = BackendConnections.get(user).getFeatureConfig(user);
        JSONObject mlsE2EId = response.getJSONObject("mlsE2EId");
        return mlsE2EId.getJSONObject("config").getString("crlProxy");
    }

    // endregion Conference calling backdoor

    public Timedelta getEphemeralTimeout(String userAlias, String conversationName) {
        final ClientUser user = getUsersManager().findUserByNameOrNameAlias(userAlias);

        // only teamusers support Self deleting messages
        if (user.getTeamId() != null) {
            JSONObject selfDeletingMessagesSettings = BackendConnections.get(user).getSelfDeletingMessagesSettings(user);

            if (selfDeletingMessagesSettings.getString("status").equals("enabled")) {
                int timeoutInSeconds = selfDeletingMessagesSettings.getJSONObject("config").getInt("enforcedTimeoutSeconds");
                if (timeoutInSeconds != 0) {
                    // timeout value is enforced in team settings
                    return Timedelta.ofSeconds(timeoutInSeconds);
                }
            } else {
                // timeout is disabled
                return NO_EXPIRATION;
            }
        }

        // Personal user or team user without set enforced ephemeral message setting
        //follow conversation settings
        conversationName = getUsersManager().replaceAliasesOccurrences(conversationName,
                ClientUsersManager.FindBy.NAME_ALIAS);

        int conversationMessageTimer = getConversationMessageTimer(user, conversationName);
        return Timedelta.ofMillis(conversationMessageTimer);
    }

    // region Team Sign up Marketo

    public boolean marketoCustomerExists(String userAlias, String customerEmailAlias) {
        final ClientUser adminUser = toClientUser(userAlias);
        Backend backend = BackendConnections.get(adminUser);
        final String customerEmail = getUsersManager().replaceAliasesOccurrences(customerEmailAlias, ClientUsersManager.FindBy.EMAIL_ALIAS);
        final JSONObject customer = backend.getMarketoCustomer(customerEmail);
        log.fine("Marketo customer:");
        log.fine(customer.toString());
        return customer.has("properties");
    }

    public void claimKeyPackages(String userAliases) {
        final List<String> aliases = getUsersManager().splitAliases(userAliases);
        for (String alias : aliases) {
            ClientUser user = toClientUser(alias);
            Backend backend = BackendConnections.get(user);
            backend.claimKeyPackages(user);
        }
    }

    public JSONObject getMarketoCustomerProperties(String userAlias, String customerEmailAlias) {
        final ClientUser adminUser = toClientUser(userAlias);
        Backend backend = BackendConnections.get(adminUser);
        final String customerEmail = getUsersManager().replaceAliasesOccurrences(customerEmailAlias, ClientUsersManager.FindBy.EMAIL_ALIAS);
        final JSONObject customer = backend.getMarketoCustomer(customerEmail);
        return customer.getJSONObject("properties");
    }

    // endregion Team Sign up Marketo

    // region Guest Links

    public boolean isGuestLinksFeatureLocked(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        return backend.getGuestLinksFeatureSettings(dstTeam).getString("lockStatus").equals("locked");
    }

    public boolean areGuestLinksEnabled(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        return backend.getGuestLinksFeatureSettings(dstTeam).getString("status").equals("enabled");
    }

    public void unlockGuestLinksFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.unlockGuestLinksFeature(dstTeam);
    }

    public void lockGuestLinksFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.lockGuestLinksFeature(dstTeam);
    }

    public void enableInviteGuestLink(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.enableInviteGuestLinkFeature(dstTeam);
    }

    public void disableInviteGuestLink(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.disableInviteGuestLinkFeature(dstTeam);
    }

    // endregion Guest Links

    // region search inbound/outbound

    public boolean isSearchVisibilityInboundEnabled(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        return backend.getSearchVisibilityInboundFeatureSettings(dstTeam).getString("status").equals("enabled");
    }

    public void enableSearchVisibilityInbound(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.enableSearchVisibilityInbound(dstTeam);
    }

    public void disableSearchVisibilityInbound(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.disableSearchVisibilityInbound(dstTeam);
    }

    public boolean isTeamSearchVisibilityOutboundEnabled(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        return backend.getTeamSearchVisibilityOutboundFeatureSettings(dstTeam).getString("status").equals("enabled");
    }

    public void enableTeamSearchVisibilityOutbound(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.enableTeamSearchVisibilityOutbound(dstTeam);
    }

    public void setTeamSearchVisibilityOutboundStandard(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.setTeamSearchVisibilityOutboundStandard(dstTeam);
    }

    public void setTeamSearchVisibilityOutboundNoNameOutsideTeam(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.setTeamSearchVisibilityOutboundNoNameOutsideTeam(dstTeam);
    }

    // endregion search inbound/outbound Links

    // region 2F Authentication

    public boolean is2FAuthenticationFeatureLocked(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        return backend.get2FAuthenticationFeatureSettings(dstTeam).getString("lockStatus").equals("locked");
    }

    public boolean is2FAuthenticationEnabled(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        return backend.get2FAuthenticationFeatureSettings(dstTeam).getString("status").equals("enabled");
    }

    public void lock2FAuthentication(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.lock2FAuthenticationFeature(dstTeam.getId());
    }

    public void unlock2FAuthentication(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.unlock2FAuthenticationFeature(dstTeam.getId());
    }

    public void enable2FAuthentication(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.enable2FAuthenticationFeature(dstTeam.getId());
    }

    public void disable2FAuthentication(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.disable2FAuthenticationFeature(dstTeam.getId());
    }

    public void usersAdd2FADevices(String teamOwnerAlias, String mappingAsJson) {
        usersAddDevices(mappingAsJson, true);
    }

    public void add2FADevice(String userAlias, @Nullable String deviceName, Optional<String> label) {
        ClientUser user = toClientUser(userAlias);
        Backend backend = BackendConnections.get(user);
        if (user.getVerificationCode() == null) {
            user.setVerificationCode(backend.getVerificationCode(user));
        }
        log.info("verificationCode: " + user.getVerificationCode());
        boolean developmentApiEnabled = isDevelopmentApiEnabled(userAlias);
        addDevice(userAlias, user.getVerificationCode(), deviceName, label, developmentApiEnabled);
    }

    // endregion 2F Authentication

    // region MLS feature

    public void useNewTestService() {
        isOldTestService = false;
        testServiceClient = new TestServiceClient(
                Config.common().getTestServiceUrl(CommonSteps.class),
                "Generic Test Name");
    }

    public void useOldTestService() {
        isOldTestService = true;
        testServiceClient = new TestServiceClient(
                Config.common().getOldTestServiceUrl(CommonSteps.class),
                "Generic Test Name");
    }

    public void migrateTeamToMLS(String adminUserAlias, String teamName, String startDate, String finaliseDate) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.migrateTeamToMLS(dstTeam, startDate, finaliseDate);
    }

    public void configureMLSForBund(String adminUserAlias, String teamName) {
        // 1: MLS_128_DHKEMP256_AES128GCM_SHA256_ED25519
        // 2: MLS_128_DHKEMP256_AES128GCM_SHA256_P256
        // For Bund the default cipher suite should be 2 which is MLS_128_DHKEMP256_AES128GCM_SHA256_P256
        // The default protocol and the allowed ones are only mls
        // The lockstate should not be set
        enableMLSFeatureTeam(adminUserAlias, teamName, 2, List.of(2), "mls", List.of("mls"), false);
    }

    public void enableMLSFeatureTeam(String adminUserAlias, String teamName, Integer defaultCipherSuite,
                                     List<Integer> allowedCipherSuite, String defaultProtocol,
                                     List<String> allowedProtocols, boolean locked) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);

        backend.enableMLSFeatureTeam(dstTeam, defaultCipherSuite, allowedCipherSuite, defaultProtocol, allowedProtocols);
        if (locked) {
            backend.lockMLSFeature(dstTeam);
        }
    }

    public void disableMLSFeature(String adminUserAlias, String teamName, String userNameAliases) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        List<ClientUser> members = new ArrayList<>();

        for (String memberAlias : getUsersManager().splitAliases(userNameAliases)) {
            final ClientUser userToDisableMLS = toClientUser(memberAlias);
            members.add(userToDisableMLS);
        }
        backend.disableMLSFeature(dstTeam, members);
    }

    public void disableMLSFeatureTeam(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.disableMLSFeatureTeam(dstTeam);
    }

    public void claimKeyPackages(String userAliases, int amount) {
        final List<String> aliases = getUsersManager().splitAliases(userAliases);
        for (String alias : aliases) {
            ClientUser user = toClientUser(alias);
            Backend backend = BackendConnections.get(user);
            for (int i = 0; i < amount; i++) {
                backend.claimKeyPackages(user);
            }
        }
    }

    public int getRemainingKeyPackagesCount(String userAlias) {
        ClientUser user = toClientUser(userAlias);
        Backend backend = BackendConnections.get(user);
        JSONObject firstDevice = backend.getClients(user).getJSONObject(0);
        log.info(firstDevice.toString());
        String deviceId = firstDevice.getString("id");
        return backend.getRemainingKeyPackagesCount(user, deviceId).getInt("count");
    }

    // endregion MLS feature

    // region E2EI

    public void enableE2EIFeatureTeam(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.enableE2EIFeatureTeam(dstTeam, backend.getAcmeDiscoveryUrl());
    }

    public void enableE2EIFeatureTeam(String adminUserAlias, String teamName, int expirationTimeInSeconds) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.enableE2EIFeatureTeam(dstTeam, backend.getAcmeDiscoveryUrl(), expirationTimeInSeconds);
	}

    public void enableE2EIFeatureTeamWithInsecureACME(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        String insecureAcmeUrl = backend.getAcmeDiscoveryUrl().replace("acme.", "acme-selfsigned.");
        log.info("Use insecure ACME discovery URL: " + insecureAcmeUrl);
        backend.enableE2EIFeatureTeam(dstTeam, insecureAcmeUrl);
    }

    // endregion

    // region Outlook Calendar Integration

    public boolean isOutlookCalendarIntegrationFeatureLocked(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        return backend.getOutlookCalendarIntegrationFeatureSettings(dstTeam).getString("lockStatus").equals("locked");
    }

    public boolean isOutlookCalendarIntegrationEnabled(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        return backend.getOutlookCalendarIntegrationFeatureSettings(dstTeam).getString("status").equals("enabled");
    }

    public void unlockOutlookCalendarIntegrationFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.unlockOutlookCalendarIntegrationFeature(dstTeam);
    }

    public void lockOutlookCalendarIntegrationFeature(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.lockOutlookCalendarIntegrationFeature(dstTeam);
    }

    public void enableOutlookCalendarIntegration(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.enableOutlookCalendarIntegrationFeature(dstTeam);
    }

    public void disableOutlookCalendarIntegration(String adminUserAlias, String teamName) {
        final ClientUser adminUser = toClientUser(adminUserAlias);
        Backend backend = BackendConnections.get(adminUser);
        final Team dstTeam = backend.getTeamByName(adminUser, teamName);
        backend.disableOutlookCalendarIntegrationFeature(dstTeam);
    }

    // endregion Outlook Calendar Integration

    public String getClientLinkForPublicConversationFederated(String userNameAlias, String backend, String conversationName) {
        String inviteLink = getInviteLinkOfConversation(userNameAlias, conversationName);
        return String.format("https://account.%s/%s&domain=%s", backend, inviteLink.substring(inviteLink.indexOf("conversation-join")), backend);
    }

    public void printAllCreatedUsers() {
        StringBuilder allUsers = new StringBuilder("____ PRINTING ALL users ____\n ");;
        getUsersManager().getAllUsers().forEach((user) -> {
            // getCreatedUsers returns nothing in this case
            // getAllUsers also supplies empty users
            // check if a user is not empty by checking if a backend is assigned
            if (user.getBackendName() != null) {
                // if the user is a teamOwner, add TeamOwner on top
                if (user.isTeamOwner()) {
                    allUsers.append("\n\n  __ Team Owner __");
                    String teamName = BackendConnections.get(user.getBackendName()).getAllTeams(user).get(0).getName();
                    allUsers.append(String.format("\nTeamName: %s", teamName));
                } else {
                    allUsers.append("\n");
                }

                String userInfo = "\nDomain: @" + user.getBackendName() + ".wire.link" +
                        "\nEmail: " + user.getEmail() +
                        "\nTeamId: " + user.getTeamId() +
                        "\nUsername: @" + user.getUniqueUsername() +
                        "\nUserID: " + user.getId() +
                        "\nPassword: " + user.getPassword();

                // add the string of user info to our allUsers string
                allUsers.append(userInfo);
            }
        });
        // print all users
        log.info(allUsers.toString());
    }

    public boolean isDevelopmentApiEnabled(String userNameAlias) {
        final ClientUser user = toClientUser(userNameAlias);
        Backend backend = BackendConnections.get(user);
        return backend.isDevelopmentApiEnabled(user);
    }
}
