package com.wire.qa.maintenancecli;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.wearezeta.auto.common.CallingManager;
import com.wearezeta.auto.common.backend.Backend;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.backend.models.Team;
import com.wearezeta.auto.common.sso.KeycloakAPIClient;
import com.wearezeta.auto.common.testservice.TestServiceClient;
import com.wearezeta.auto.common.misc.Timedelta;
import com.wearezeta.auto.common.sso.OktaAPIClient;
import com.wearezeta.auto.common.stripe.StripeAPIClient;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import org.joda.time.DateTime;
import org.json.JSONObject;

public class App {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    private static final int MAX_THREADS = 7;
    private static final long DEFAULT_TIMEOUT = 20000;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Please provide task name: createFullTeam or enableLegalHold");
            System.err.println("Usage: createFullTeam <teamSize> <createFullConversation> <ownerName> " +
                    "<numberOfDevices> <userPictures> <ignoreExceptions> <backendType> <etsServiceUrl>");
            System.err.println("Usage: enableLegalHold <teamID> <disableLegalHold>");
            System.exit(1);
        }
        switch (args[0]) {
            case "createFixedSSOTeam":
                try {
                    String ownerName = args[1];
                    createFixedSSOTeam(ownerName);
                } catch (Exception e) {
                    printError("Error: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                break;
            case "createFullTeam":
                try {
                    int teamSize = Integer.parseInt(args[1]);
                    boolean mls = Boolean.parseBoolean(args[2]);
                    boolean e2ei = Boolean.parseBoolean(args[3]);
                    String ownerRandomId = args[4];
                    int numberOfDevices = Integer.parseInt(args[5]);
                    boolean userPictures = Boolean.parseBoolean(args[6]);
                    boolean createFullConversation = Boolean.parseBoolean(args[7]);
                    boolean ignoreExceptions = Boolean.parseBoolean(args[8]);
                    String backendType = args[9];
                    String etsServiceUrl = args[10];
                    createFullTeam(teamSize, mls, e2ei, ownerRandomId, numberOfDevices, userPictures,
                            createFullConversation, ignoreExceptions, backendType, etsServiceUrl);
                } catch (Exception e) {
                    printError("Error: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                break;
            case "createMonkeyTeam":
                try {
                    int teamSize = Integer.parseInt(args[1]);
                    String ownerRandomId = args[2];
                    String backendType = args[3];
                    createMonkeyTeam(teamSize, ownerRandomId, backendType);
                } catch (Exception e) {
                    printError("Error: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                break;
            case "enableLegalHold":
                try {
                    String backendType = args[1];
                    String teamID = args[2];
                    boolean enableLegalHold = Boolean.parseBoolean(args[3]);
                    Team team = new Team();
                    team.setId(teamID);
                    whitelistTeamForLegalHold(backendType, team, enableLegalHold);
                } catch (Exception e) {
                    printError("Error: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                break;
            case "enableMLSFeatureTeam":
                try {
                    String backendType = args[1];
                    String teamID = args[2];
                    int defaultCipherSuite = Integer.parseInt(args[3]);
                    List<Integer> allowedCipherSuites = Arrays.stream(args[4].split(",")).map(Integer::parseInt).collect(Collectors.toList());
                    String defaultProtocol = args[5];
                    List<String> supportedProtocols = Arrays.asList(args[6].split(","));
                    Team team = new Team();
                    team.setId(teamID);
                    enableMLSFeatureTeam(backendType, team, defaultCipherSuite, allowedCipherSuites, defaultProtocol, supportedProtocols);
                } catch (Exception e) {
                    printError("Error: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                break;
            case "changeOutlookCalIntegration":
                try {
                    String backendType = args[1];
                    String teamID = args[2];
                    boolean enable = Boolean.parseBoolean(args[3]);
                    Team team = new Team();
                    team.setId(teamID);
                    changeOutlookCalIntegration(backendType, team, enable);
                } catch (Exception e) {
                    printError("Error: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                break;
            case "createSSOTeam":
                try {
                    String backendType = args[1];
                    createSSOTeam(backendType);
                } catch (Exception e) {
                    printError("Error: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                break;
            case "prepareConferenceCall":
                try {
                    int waitingSize = Integer.parseInt(args[1]);
                    boolean joinMuted = Boolean.parseBoolean(args[2]);
                    int videoSize = Integer.parseInt(args[3]);
                    String chapter = args[4];
                    prepareConferenceCall(waitingSize, joinMuted, videoSize, chapter);
                } catch (Exception e) {
                    printError("Error: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                break;
            case "cleanupConferenceCallInstances":
                try {
                    String chapter = args[1];
                    cleanupConferenceCallInstances(chapter);
                } catch (Exception e) {
                    printError("Error: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                break;
            case "changeConferenceCalling":
                try {
                    String backendType = args[1];
                    String userEmail = args[2];
                    String userPassword = args[3];
                    boolean enable = Boolean.parseBoolean(args[4]);
                    changeConferenceCalling(backendType, userEmail, userPassword, enable);
                } catch (Exception e) {
                    printError("Error: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                break;
            case "enable2FA":
                try {
                    String backendType = args[1];
                    String userEmail = args[2];
                    String userPassword = args[3];
                    boolean enable = Boolean.parseBoolean(args[4]);
                    enable2FA(backendType, userEmail, userPassword, enable);
                } catch (Exception e) {
                    printError("Error: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                break;
            case "change-name":
                try {
                    String backendType = args[1];
                    String userEmail = args[2];
                    String userPassword = args[3];
                    String newName = args[4];
                    ClientUser user = new ClientUser();
                    user.setEmail(userEmail);
                    user.setPassword(userPassword);
                    user.setBackendName(backendType);
                    boolean enable = Boolean.parseBoolean(args[4]);
                    BackendConnections.get(user).updateName(user, newName);
                } catch (Exception e) {
                    printError("Error: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                break;
            case "cucumber-reports":
                try {
                    String filename = args[1];
                    String projectName = args[2];
                    String buildNumber = args[3];
                    File reportOutputDirectory = new File("target");
                    List<String> jsonFiles = new ArrayList<>();
                    jsonFiles.add(filename);

                    Configuration configuration = new Configuration(reportOutputDirectory, projectName);
                    configuration.setBuildNumber(buildNumber);
                    ReportBuilder reportBuilder = new ReportBuilder(jsonFiles, configuration);
                    reportBuilder.generateReports();
                } catch (Exception e) {
                    printError("Error: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                break;
            default:
                System.err.println("No such task found: " + args[0]);
                System.exit(1);
        }
    }

    private static void createFixedSSOTeam(String ownerName) {
        String teamName = "FixedSSOTeam";

        ClientUser owner = new ClientUser();
        Backend customBackend = BackendConnections.get("QA-Fixed-SSO");
        owner.setBackendName(customBackend.getBackendName());
        owner.setName(ownerName);

        Team team = null;

        try {
            printInformation("Create Owner and Team");
            customBackend.createTeamOwnerViaBackdoor(owner, teamName, "en_US", true);
            printInformation(String.format("Owner: %s (%s)", owner.getId(), owner.getEmail()));

            team = customBackend.getTeamByName(owner, teamName);

            // This seems to not be needed anymore on QA-Demo
            //printInformation("Enable SSO feature");
            //customBackend.enableSSOFeature(team);

            printInformation("Create application on Okta");
            OktaAPIClient oktaAPIClient = new OktaAPIClient();
            // Remove trailing slash which otherwise causes a 404 on backend
            String backendUrl = customBackend.getBackendUrl().replaceFirst("/*$", "");
            String finalizeUrl = String.format("%s/sso/finalize-login", backendUrl);
            String label = owner.getName() + " " + teamName;
            String appID = oktaAPIClient.createApplication(label, finalizeUrl);
            printInformation("FIXSSO_APPID: " + appID);

            printInformation("Connect Okta with team on backend");
            String ssoId = customBackend.createIdentityProvider(owner,
                    oktaAPIClient.getApplicationMetadata());

            printInformation("Set fixed SSO on backend " + backendUrl);
            customBackend.setFixedSSO(ssoId);

            Path ownerFile = Paths.get("target/owner.txt");

            try (BufferedWriter writer = Files.newBufferedWriter(ownerFile)) {
                writer.write("Team Id: " + owner.getTeamId());
                writer.write("\nOwner Id: " + owner.getId());
                writer.write("\nOwner Name: " + owner.getName());
                writer.write("\nOwner Email: " + owner.getEmail());
                writer.write("\nSSO Id: wire-" + ssoId);
                writer.write("\nFIXSSO_APPID: " + appID);
            }
        } catch (Exception e) {
            System.out.println();
            printError("MAINTENANCE TASK FAILED:");
            printError("------------------------");
            printError(e.getMessage());
            e.printStackTrace();
            System.out.println();
            if (team != null) {
                customBackend.deleteTeam(owner, team);
            }
            System.exit(1);
        }
    }

    private static void createSSOTeam(String backendType) {
        String teamName = "My Team";
        ClientUser owner = new ClientUser();
        owner.setBackendName(backendType);
        Team team = null;
        Backend backend = BackendConnections.get(backendType);

        try {
            printInformation("Create Owner and Team");
            backend.createTeamOwnerViaBackdoor(owner, teamName, "en_US", true);
            printInformation(String.format("Owner: %s (%s)", owner.getId(), owner.getEmail()));

            // set billing information and add credit card so that the team is not suspended
            team = backend.getTeamByName(owner, teamName);
            printInformation("Set billing information for team");
            final String[][] mapping = {{"city", "Berlin"},
                    {"company", "Team"},
                    {"country", "de"},
                    {"state", "Wirestan"},
                    {"firstname", "Wire"},
                    {"lastname", "Zeta"},
                    {"street", "Billing Street"},
                    {"zip", "12345"}};
            backend.updateBillingInfo(owner, team, createJsonFromMapping(mapping));

            printInformation("Add credit card for team");
            final String[][] cardinfo = {{"name", "CARDHOLDER"},
                    {"exp_month", "02"},
                    {"exp_year", String.valueOf(DateTime.now().year().get() + 3)},
                    {"address_zip", "12345"}};
            final StripeAPIClient stripeAPIClient = new StripeAPIClient();
            final String stripeCustomerId = backend.getStripeCustomerIdForTeam(owner, team);
            stripeAPIClient.createDefaultValidVisaCard(stripeCustomerId, team.getId());
            stripeAPIClient.updateCreditCard(stripeCustomerId, createJsonFromMapping(cardinfo));

            printInformation("Enable SSO feature");
            backend.enableSSOFeature(team);

            printInformation("Create application on Okta");
            OktaAPIClient oktaAPIClient = new OktaAPIClient();
            String finalizeUrl = String.format("%ssso/finalize-login", backend.getBackendUrl());
            String label = owner.getName() + " " + teamName;
            oktaAPIClient.createApplication(label, finalizeUrl);
            printInformation("FIXSSO_APPID: " + oktaAPIClient.getApplicationId(label));

            printInformation("Connect Okta with team on backend");
            String ssoId = backend.createIdentityProvider(owner,
                    oktaAPIClient.getApplicationMetadata());

            ClientUser oktaUser = new ClientUser();
            oktaAPIClient.createUser(oktaUser.getName(), oktaUser.getEmail(), oktaUser.getPassword());

            Path ownerFile = Paths.get("target/owner.txt");

            try (BufferedWriter writer = Files.newBufferedWriter(ownerFile)) {
                writer.write("Team Id: " + owner.getTeamId());
                writer.write("\nOwner Id: " + owner.getId());
                writer.write("\nOwner Name: " + owner.getName());
                writer.write("\nOwner Email: " + owner.getEmail());
                writer.write("\nSSO Id: wire-" + ssoId);
                writer.write("\nOkta User Name: " + oktaUser.getName());
                writer.write("\nOkta User Email: " + oktaUser.getEmail());
                writer.write("\nOkta User Password: " + oktaUser.getPassword());
            }

        } catch (Exception e) {
            System.out.println();
            printError("MAINTENANCE TASK FAILED:");
            printError("------------------------");
            printError(e.getMessage());
            e.printStackTrace();
            System.out.println();
            if (team != null) {
                backend.deleteTeam(owner, team);
            }
            System.exit(1);
        }
    }

    private static void createFullTeam(int teamSize, boolean mls, boolean e2ei, String ownerRandomId,
                                       int numberOfDevices, boolean userPictures, boolean createFullConversation,
                                       boolean ignoreExceptions, String backendType, String testServiceURL) {
        Backend backend = BackendConnections.get(backendType);
        ClientUser owner = new ClientUser();
        if (backend.hasInbucketSetup()) {
            owner.setEmail(ownerRandomId + "@wire.engineering");
        }

        String teamName = "FullTeam";
        Team team = null;
        final KeycloakAPIClient keycloakAPIClient = new KeycloakAPIClient(backend.getBackendName());

        try {
            printInformation("Create Owner and Team");
            backend.createTeamOwnerViaBackdoor(owner, teamName, "en_US",false);
            printInformation(String.format("Owner: %s (%s)", owner.getId(), owner.getEmail()));
            team = backend.getTeamByName(owner, teamName);

            if (mls) {
                printInformation("Enable MLS for team");
                backend.enableMLSFeatureTeam(team, 2, List.of(2), "mls", List.of("mls"));
                if (e2ei) {
                    printInformation("Enable E2EI for team with default acme server");
                    backend.enableE2EIFeatureTeam(team, backend.getAcmeDiscoveryUrl());
                    printInformation("Add owner to keycloak...");
                    try {
                        String username = owner.getUniqueUsername() + "@" + backend.getDomain();
                        backend.updateUniqueUsername(owner, owner.getUniqueUsername());
                        keycloakAPIClient.createUser(username, owner.getFirstName(), owner.getLastName(), owner.getEmail(), owner.getPassword());
                    } catch (Exception e) {
                        if (!ignoreExceptions) {
                            throw e;
                        }
                        printInformation(String.format("Exception thrown on adding owner %s to keycloak. \n %s", owner.getEmail()
                                , e.getMessage()));
                    }
                    printInformation(String.format("Added owner %s to keycloak", owner.getEmail()));
                }
            }

            if (backend.hasBillingPlansEnabled(owner, team)) {
                // set billing information and add credit card so that the team is not suspended
                printInformation("Set billing information for team");
                final String[][] mapping = {{"city", "Berlin"},
                        {"company", "Team"},
                        {"country", "de"},
                        {"state", "Wirestan"},
                        {"firstname", "Wire"},
                        {"lastname", "Zeta"},
                        {"street", "Billing Street"},
                        {"zip", "12345"}};
                backend.updateBillingInfo(owner, team, createJsonFromMapping(mapping));

                printInformation("Add credit card for team");
                final String[][] cardinfo = {{"name", "CARDHOLDER"},
                        {"exp_month", "02"},
                        {"exp_year", String.valueOf(DateTime.now().year().get() + 3)},
                        {"address_zip", "12345"}};
                final StripeAPIClient stripeAPIClient = new StripeAPIClient();
                final String stripeCustomerId = backend.getStripeCustomerIdForTeam(owner, team);
                stripeAPIClient.createDefaultValidVisaCard(stripeCustomerId, team.getId());
                stripeAPIClient.updateCreditCard(stripeCustomerId, createJsonFromMapping(cardinfo));
            }

            printInformation("Add members to team");
            List<ClientUser> members = new ArrayList<>();
            final ExecutorService memberPool = Executors.newFixedThreadPool(MAX_THREADS);
            final List<CompletableFuture<Void>> memberCreationPromises = new ArrayList<>();
            //teamSize -1 because team owner is already created
            for (int i = 1; i <= teamSize - 1; i++) {
                memberCreationPromises.add(CompletableFuture
                        .supplyAsync(() -> {
                            printInformation(String.format("Status: %d members created", members.size()));
                            ClientUser member = new ClientUser();
                            if (backend.hasInbucketSetup()) {
                                member.setEmail(member.getUniqueUsername() + "@wire.engineering");
                            }
                            try {
                                backend.createTeamUserViaBackdoor(owner, owner.getTeamId(), member, userPictures, true, "member");
                            } catch (Exception e) {
                                if (!ignoreExceptions) {
                                    throw e;
                                }
                                printInformation(String.format("Exception thrown on adding user %s. \n %s", member.getEmail()
                                        , e.getMessage()));
                                printInformation(String.format("Waiting 20 seconds before adding new members %s to Team",
                                        member.getId()));
                                sleepDefaultTimeout();
                            }
                            printInformation(String.format("Added Member: %s (%s)", member.getId(), member.getEmail()));
                            member.setBackendName(backend.getBackendName());
                            members.add(member);
                            if (e2ei) {
                                printInformation(String.format("Add member %s to keycloak...", member.getEmail()));
                                try {
                                    String username = member.getUniqueUsername() + "@" + backend.getDomain();
                                    keycloakAPIClient.createUser(username, member.getFirstName(), member.getLastName(), member.getEmail(), member.getPassword());
                                } catch (Exception e) {
                                    if (!ignoreExceptions) {
                                        throw e;
                                    }
                                    printInformation(String.format("Exception thrown on adding user %s to keycloak. \n %s", member.getEmail()
                                            , e.getMessage()));
                                    printInformation(String.format("Waiting 20 seconds before adding new members %s to Team",
                                            member.getId()));
                                    sleepDefaultTimeout();
                                }
                                printInformation(String.format("Added member %s to keycloak", member.getEmail()));
                            }
                            return null;
                        }, memberPool)
                );
            }

            memberPool.shutdown();
            for (CompletableFuture<Void> deviceCreationPromise : memberCreationPromises) {
                try {
                    deviceCreationPromise.get();
                } catch (Exception e) {
                    printInformation(String.format("Exception thrown on device creation promise: %s", e.getMessage()));
                    if (!ignoreExceptions) {
                        throw e;
                    }
                }
            }

            printInformation(String.format("Creating %s device(s) for each member", numberOfDevices));
            final ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);

            final List<CompletableFuture<Void>> deviceCreationPromises = new ArrayList<>();
            for (ClientUser member : members) {
                deviceCreationPromises.add(CompletableFuture
                        .supplyAsync(() -> {
                            TestServiceClient testServiceClient = new TestServiceClient(
                                    testServiceURL,
                                    "Testservice instance for member " + member.getEmail());
                            boolean developmentApiEnabled = backend.isDevelopmentApiEnabled(member);
                            for (int i = 1; i <= numberOfDevices; i++) {
                                try {
                                    String verificationCode = null;
                                    printInformation("backend name: " + backend.getBackendName());
                                    if (backend.getBackendName().contains("bund")) {
                                        printInformation("Get verification code for member " + member.getEmail());
                                        verificationCode = backend.getVerificationCode(member);
                                        printInformation("Verification code: " + verificationCode);
                                    }
                                    testServiceClient.login(member, verificationCode, String.format("Device%s", i),
                                            developmentApiEnabled);
                                    // we do not clean up devices anymore. This might cause other test runs to fail,
                                    // because ETS has a limit of 500 parallel devices. Therefor we use dev ETS here.
                                    // e2eTestServiceClient.cleanUp(member, String.format("Device%s", i));
                                } catch (Exception e) {
                                    printInformation(String.format("Exception thrown: \n %s, \n Happened on device %s for member %s", e.toString(), i, member.getEmail()));
                                    if (!ignoreExceptions) {
                                        throw e;
                                    }
                                    printInformation("Wait 20 seconds before adding new devices");
                                    sleepDefaultTimeout();
                                }
                            }
                            return null;

                        }, pool)
                );
            }

            pool.shutdown();
            for (CompletableFuture<Void> deviceCreationPromise : deviceCreationPromises) {
                try {
                    deviceCreationPromise.get();
                } catch (Exception e) {
                    throw e;
                }
            }

            printInformation("Write members info file");
            String filename = String.format("target/users-for-%s.txt", backend.getBackendName());
            Path membersFile = Paths.get(filename);
            try (BufferedWriter writer = Files.newBufferedWriter(membersFile)) {
                writer.write("Backend: " + backend.getBackendName());
                if (mls) {
                    writer.write("\nMLS: enabled");
                } else {
                    writer.write("\nMLS: disabled");
                }
                writer.write("\nTeam Id: " + owner.getTeamId());
                writer.write("\nOwner Id: " + owner.getId());
                writer.write("\nOwner Name: " + owner.getName());
                writer.write("\nOwner Email: " + owner.getEmail());
                writer.write("\nOwner UserName: " + owner.getUniqueUsername());
                writer.write("\nOwner Password: " + owner.getPassword());
                for (ClientUser member : members) {
                    writer.write("\n\nMember Id: " + member.getId());
                    writer.write("\nMember Name: " + member.getName());
                    writer.write("\nMember Email: " + member.getEmail());
                    writer.write("\nMember UserName: " + member.getUniqueUsername());
                    writer.write("\nMember Password: " + member.getPassword());
                }
            }

            // Create full conversation if not MLS team
            if (createFullConversation && !mls) {
                List<ClientUser> conversationMembers = members;
                if (conversationMembers.size() > 500) {
                    conversationMembers = conversationMembers.subList(0, 499);
                }
                printInformation("Create full conversation");
                try {
                    String conversationName = "Full House";
                    String fullConversationId = backend.createTeamConversation(owner, conversationMembers, conversationName, team);
                    printInformation("Full conversation: " + fullConversationId);
                    Path fullConversationIdFile = Paths.get("target/full_conversation.txt");

                    try (BufferedWriter writer = Files.newBufferedWriter(fullConversationIdFile)) {
                        writer.write(conversationName + "\n");
                        writer.write(fullConversationId);
                    }
                } catch (Exception e) {
                    printInformation(String.format("Exception thrown on full conversation creation: %s", e.getMessage()));
                    if (!ignoreExceptions) {
                        throw e;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println();
            printError("MAINTENANCE TASK FAILED:");
            printError("------------------------");
            printError(e.getMessage());
            e.printStackTrace();
            System.out.println();
            if (team != null) {
                backend.deleteTeam(owner, team);
            }
            System.exit(1);
        }
    }

    private static void createMonkeyTeam(int teamSize, String ownerRandomId, String backendType) {
        Backend backend = BackendConnections.get(backendType);
        ClientUser owner = new ClientUser();
        owner.setBackendName(backend.getBackendName());
        if (backend.hasInbucketSetup()) {
            owner.setEmail(owner.getUniqueUsername() + "@wire.com");
        }
        String teamName = "FullTeam";
        Team team = null;

        try {
            printInformation("Create Owner and Team");
            backend.createTeamOwnerViaBackdoor(owner, teamName, "en_US",false);
            printInformation(String.format("Owner: %s (%s)", owner.getId(), owner.getEmail()));

            Path ownerFile = Paths.get("target/owner.txt");

            try (BufferedWriter writer = Files.newBufferedWriter(ownerFile)) {
                writer.write("Team Id: " + owner.getTeamId());
                writer.write("\nOwner Id: " + owner.getId());
                writer.write("\nOwner Name: " + owner.getName());
                writer.write("\nOwner Email: " + owner.getEmail());
            }

            team = backend.getTeamByName(owner, teamName);

            if (backend.hasBillingPlansEnabled(owner, team)) {
                // set billing information and add credit card so that the team is not suspended
                printInformation("Set billing information for team");
                final String[][] mapping = {{"city", "Berlin"},
                        {"company", "Team"},
                        {"country", "de"},
                        {"state", "Wirestan"},
                        {"firstname", "Wire"},
                        {"lastname", "Zeta"},
                        {"street", "Billing Street"},
                        {"zip", "12345"}};
                backend.updateBillingInfo(owner, team, createJsonFromMapping(mapping));

                printInformation("Add credit card for team");
                final String[][] cardinfo = {{"name", "CARDHOLDER"},
                        {"exp_month", "02"},
                        {"exp_year", String.valueOf(DateTime.now().year().get() + 3)},
                        {"address_zip", "12345"}};
                final StripeAPIClient stripeAPIClient = new StripeAPIClient();
                final String stripeCustomerId = backend.getStripeCustomerIdForTeam(owner, team);
                stripeAPIClient.createDefaultValidVisaCard(stripeCustomerId, team.getId());
                stripeAPIClient.updateCreditCard(stripeCustomerId, createJsonFromMapping(cardinfo));
            }

            printInformation("Enable MLS for team");
            backend.enableMLSFeatureTeam(team, 1, List.of(1), "proteus", List.of("mls", "proteus"));

            //ScimClient scimClient = new ScimClient(backendType);

            printInformation("Add members to team");
            List<ClientUser> members = new ArrayList<>();
            final ExecutorService memberPool = Executors.newFixedThreadPool(MAX_THREADS);
            final List<CompletableFuture<Void>> memberCreationPromises = new ArrayList<>();
            //teamSize -1 because team owner is already created
            for (int i = 1; i <= teamSize - 1; i++) {
                memberCreationPromises.add(CompletableFuture
                        .supplyAsync(() -> {
                            ClientUser member = new ClientUser();
                            member.setBackendName(backend.getBackendName());
                            if (backend.hasInbucketSetup()) {
                                member.setEmail(member.getUniqueUsername() + "@wire.com");
                            }
                            /*
                            // TODO: Set the last parameter from null to picture to enable picture upload
                            String scimId = scimClient.insert(owner, member, null);
                             */
                            try {
                                backend.createTeamUserViaBackdoor(owner, owner.getTeamId(), member, false, true, "member");
                            } catch (Exception e) {
                                printInformation(String.format("Exception thrown on adding user %s. \n %s", member.getEmail()
                                        , e.getMessage()));
                                printInformation(String.format("Waiting 20 seconds before adding new members %s to Team",
                                        member.getId()));
                                sleepDefaultTimeout();
                            }
                            printInformation(String.format("Added Member: %s (%s)", member.getId(), member.getEmail()));
                            member.setBackendName(backend.getBackendName());
                            members.add(member);
                            return null;
                        }, memberPool)
                );
            }

            memberPool.shutdown();
            for (CompletableFuture<Void> deviceCreationPromise : memberCreationPromises) {
                try {
                    deviceCreationPromise.get();
                } catch (Exception e) {
                    printInformation(String.format("Exception thrown on device creation promise: %s", e.getMessage()));
                }
            }

            printInformation("Write members info file");
            Path membersFile = Paths.get("target/members.txt");
            try (BufferedWriter writer = Files.newBufferedWriter(membersFile)) {
                for (ClientUser member : members) {
                    writer.write(member.getEmail() + "," + member.getId() + "\n");
                }
            }

        } catch (Exception e) {
            System.out.println();
            printError("MAINTENANCE TASK FAILED:");
            printError("------------------------");
            printError(e.getMessage());
            e.printStackTrace();
            System.out.println();
            if (team != null) {
                backend.deleteTeam(owner, team);
            }
            System.exit(1);
        }
    }

    public static void prepareConferenceCall(int waitingSize, boolean joinMuted, int videoSize, String chapter) throws Exception {
        String platform = String.format("Wire Helper (%s)", chapter);
        String password = "Aqa123456!";
        String[] predefinedConferenceCallUsers;

        if (chapter.equals("qa")) {
            predefinedConferenceCallUsers = new String[]{
                    "smoketester+5d4b8cc852@wire.com",
                    "smoketester+a79849d4d8@wire.com",
                    "smoketester+d701cf0658@wire.com",
                    "smoketester+f20a4235de@wire.com",
                    "smoketester+0a3efdfef0@wire.com",
                    "smoketester+a16fa74584@wire.com",
                    "smoketester+9ea97f6b55@wire.com",
                    "smoketester+6c08d454f8@wire.com",
                    "smoketester+075148ece5@wire.com",
                    "smoketester+4256920490@wire.com",
                    "smoketester+8668b8210f@wire.com",
                    "smoketester+bb3132d66e@wire.com",
                    "smoketester+f90017f9d9@wire.com",
                    "smoketester+30311cfd6c@wire.com",
                    "smoketester+5907a985b8@wire.com",
                    "smoketester+5ca933565a@wire.com",
                    "smoketester+aecf553ee9@wire.com",
                    "smoketester+5555070e65@wire.com",
                    "smoketester+84f8552760@wire.com",
                    "smoketester+b0bce375ef@wire.com",
                    "smoketester+90cb4301a6@wire.com",
                    "smoketester+8351cf9737@wire.com",
                    "smoketester+c4118732c9@wire.com",
                    "smoketester+704109904d@wire.com",
                    "smoketester+83416da619@wire.com",
                    "smoketester+238ae75da6@wire.com",
                    "smoketester+1638386e70@wire.com",
                    "smoketester+4489e84eff@wire.com",
                    "smoketester+92a381128d@wire.com",
                    "smoketester+83e15c2d58@wire.com",
                    "smoketester+fc53dba4bf@wire.com",
                    "smoketester+f86ef7eaf2@wire.com",
                    "smoketester+6f77d6d2f8@wire.com",
                    "smoketester+6855ae28f5@wire.com",
                    "smoketester+5b98749c45@wire.com",
                    "smoketester+a6b0e6ffba@wire.com",
                    "smoketester+170a30be62@wire.com",
                    "smoketester+10677e60be@wire.com",
                    "smoketester+885f7d2d7c@wire.com",
                    "smoketester+0d60e6969d@wire.com",
                    "smoketester+e3aca5bcfe@wire.com",
                    "smoketester+19ececf7ca@wire.com",
                    "smoketester+d72b34d228@wire.com",
                    "smoketester+b3d5a70c39@wire.com",
                    "smoketester+a37b999066@wire.com",
                    "smoketester+453e30ff09@wire.com",
                    "smoketester+2c2b81234e@wire.com",
                    "smoketester+1e049999e4@wire.com",
                    "smoketester+b9171aa0d2@wire.com",
                    "smoketester+a9a989d2b6@wire.com",
                    "smoketester+d49ac49076@wire.com",
                    "smoketester+8c21be94ab@wire.com",
                    "smoketester+02d042b55e@wire.com",
                    "smoketester+6ce490122c@wire.com",
                    "smoketester+73492045e1@wire.com",
                    "smoketester+2128ad6e7d@wire.com",
                    "smoketester+c779bf54da@wire.com",
                    "smoketester+ac08e8491e@wire.com",
                    "smoketester+39c2b5c37c@wire.com",
                    "smoketester+f3303f6cd2@wire.com",
                    "smoketester+fd229482eb@wire.com",
                    "smoketester+34a4225c06@wire.com",
                    "smoketester+8843d60cbe@wire.com",
                    "smoketester+fd5ca204e3@wire.com",
                    "smoketester+ae83b4bb94@wire.com",
                    "smoketester+fb0c39c96b@wire.com",
                    "smoketester+db9889292f@wire.com",
                    "smoketester+5e098a429e@wire.com",
                    "smoketester+9953f2b5db@wire.com",
                    "smoketester+28d87abda3@wire.com",
                    "smoketester+1a6f9bad07@wire.com",
                    "smoketester+9775b855b0@wire.com",
                    "smoketester+4d71d90d04@wire.com",
                    "smoketester+9251e1e544@wire.com",
                    "smoketester+c671af4018@wire.com",
                    "smoketester+fe33a23bef@wire.com",
                    "smoketester+f3b302214a@wire.com",
                    "smoketester+a55f51eacb@wire.com",
                    "smoketester+5c974fe3ed@wire.com",
                    "smoketester+7ce34957e6@wire.com",
                    "smoketester+6b19a107f3@wire.com",
                    "smoketester+54c5323eb3@wire.com",
                    "smoketester+dda19e8e16@wire.com",
                    "smoketester+090840a361@wire.com",
                    "smoketester+1dc6fb3216@wire.com",
                    "smoketester+331f5c7878@wire.com",
                    "smoketester+e8ea1489fd@wire.com",
                    "smoketester+1f6a235871@wire.com",
                    "smoketester+fe4433e52f@wire.com",
                    "smoketester+fb1dafd62c@wire.com",
                    "smoketester+bcd5c0c6c6@wire.com",
                    "smoketester+68f64778c1@wire.com",
                    "smoketester+514c0f5b33@wire.com",
                    "smoketester+e7e04da271@wire.com",
                    "smoketester+8dba18cf43@wire.com",
                    "smoketester+9d23b1b8cc@wire.com",
                    "smoketester+dbcc9bd195@wire.com",
                    "smoketester+7487d51794@wire.com",
                    "smoketester+31feffaadd@wire.com",
                    "smoketester+ca8a66d368@wire.com",
                    "smoketester+435bdd95e5@wire.com",
                    "smoketester+384f18db49@wire.com",
                    "smoketester+46f0853bae@wire.com",
                    "smoketester+43aa7c10ee@wire.com",
                    "smoketester+fcfecddcb3@wire.com",
                    "smoketester+ccd0d7369e@wire.com",
                    "smoketester+d3bb9074fb@wire.com",
                    "smoketester+e12d66b6b3@wire.com",
                    "smoketester+a8b1db4f24@wire.com",
                    "smoketester+e435f5f07d@wire.com",
                    "smoketester+c659bf9d15@wire.com",
                    "smoketester+42c3092d6f@wire.com",
                    "smoketester+384e9ba3f5@wire.com",
                    "smoketester+a3309a5a4d@wire.com",
                    "smoketester+264dc8fd14@wire.com",
                    "smoketester+36f7ab6944@wire.com",
                    "smoketester+682831a40b@wire.com",
                    "smoketester+c92624e179@wire.com",
                    "smoketester+f836ee8734@wire.com",
                    "smoketester+12fb6052ce@wire.com",
                    "smoketester+d183dd88f4@wire.com",
                    "smoketester+107b889783@wire.com",
                    "smoketester+3c81ac1394@wire.com",
                    "smoketester+82ade85935@wire.com",
                    "smoketester+7ce2748524@wire.com",
                    "smoketester+6e1f6f2119@wire.com",
                    "smoketester+fb3059c662@wire.com",
                    "smoketester+cb1a4582eb@wire.com",
                    "smoketester+dfe5c402cd@wire.com",
                    "smoketester+47e69933c2@wire.com",
                    "smoketester+6440ced8d6@wire.com",
                    "smoketester+c190963e77@wire.com",
                    "smoketester+d3b7de99b3@wire.com",
                    "smoketester+17b12097a4@wire.com",
                    "smoketester+770a47c095@wire.com",
                    "smoketester+c53bae3c35@wire.com",
                    "smoketester+8d365a0add@wire.com",
                    "smoketester+717c63b397@wire.com",
                    "smoketester+5c780c43ed@wire.com",
                    "smoketester+d742bef23a@wire.com",
                    "smoketester+e938ee51ee@wire.com",
                    "smoketester+30959f6b22@wire.com",
                    "smoketester+666c83cda7@wire.com",
                    "smoketester+71b2b79a04@wire.com",
                    "smoketester+ccae0cd155@wire.com",
                    "smoketester+a3147db6c3@wire.com",
                    "smoketester+896bf3f21c@wire.com",
                    "smoketester+a2c99f69d4@wire.com",
                    "smoketester+98bf1f15a9@wire.com",
            };
        } else if (chapter.equals("ios")) {
            predefinedConferenceCallUsers = new String[]{
                    "smoketester+baafafc5c9@wire.com",
                    "smoketester+f3b5cd1120@wire.com",
                    "smoketester+a378f22f74@wire.com",
                    "smoketester+c5eb7d788f@wire.com",
                    "smoketester+357bca44d3@wire.com",
                    "smoketester+8c6390d41d@wire.com",
                    "smoketester+0f19a0fb03@wire.com",
                    "smoketester+5c33fb25bf@wire.com",
                    "smoketester+a506fa0249@wire.com",
                    "smoketester+1293bb6d91@wire.com",
                    "smoketester+1dbd4f83df@wire.com",
                    "smoketester+56a520f594@wire.com",
                    "smoketester+d8000242c5@wire.com",
                    "smoketester+9223e862a9@wire.com",
                    "smoketester+cd51869697@wire.com",
                    "smoketester+7370eacde1@wire.com",
                    "smoketester+4aa3c74780@wire.com",
                    "smoketester+6b2181c25d@wire.com",
                    "smoketester+81ca62d736@wire.com",
                    "smoketester+875f867489@wire.com",
                    "smoketester+093d16c3cb@wire.com",
                    "smoketester+5aa89367a7@wire.com",
                    "smoketester+c0aa6a524c@wire.com",
                    "smoketester+568fd4d131@wire.com",
                    "smoketester+4056c54075@wire.com",
                    "smoketester+81146d63b6@wire.com",
                    "smoketester+74cc673520@wire.com",
                    "smoketester+727001a18f@wire.com",
                    "smoketester+46b207d689@wire.com",
                    "smoketester+5901e26b64@wire.com",
                    "smoketester+762806be04@wire.com",
                    "smoketester+de8830a7c5@wire.com",
                    "smoketester+e1eee3d105@wire.com",
                    "smoketester+ababbd04d6@wire.com",
                    "smoketester+dfb83f1f94@wire.com",
                    "smoketester+f2413a6a38@wire.com",
                    "smoketester+1b943e455d@wire.com",
                    "smoketester+812d602151@wire.com",
                    "smoketester+4a1bdc0f9e@wire.com",
            };
        } else {
            predefinedConferenceCallUsers = new String[]{
                    "smoketester+c544808e48@wire.com",
                    "smoketester+f0a1bb74a3@wire.com",
                    "smoketester+836178ad5b@wire.com",
                    "smoketester+71c1b232f0@wire.com",
                    "smoketester+4118f314a9@wire.com",
                    "smoketester+1c7a05fe4c@wire.com",
                    "smoketester+6a1bba7504@wire.com",
                    "smoketester+9c63d7524c@wire.com",
                    "smoketester+5eb57c2bc8@wire.com",
                    "smoketester+a1d912403f@wire.com",
                    "smoketester+7bb8391222@wire.com",
                    "smoketester+e4b600d342@wire.com",
                    "smoketester+16bb0bfa23@wire.com",
                    "smoketester+5dfe2dda43@wire.com",
                    "smoketester+0fa2fbb5cd@wire.com",
                    "smoketester+fb101d8470@wire.com",
                    "smoketester+d0f1722e53@wire.com",
                    "smoketester+687b9b3a67@wire.com",
                    "smoketester+4c71c99f92@wire.com",
                    "smoketester+ee5b4e06a3@wire.com",
                    "smoketester+f965050b6b@wire.com",
                    "smoketester+416afeb143@wire.com",
                    "smoketester+cf555d1972@wire.com",
                    "smoketester+015d0ae4d2@wire.com",
                    "smoketester+6fc3afe4ae@wire.com",
                    "smoketester+aa46354f5a@wire.com",
                    "smoketester+c996a50ee9@wire.com",
                    "smoketester+77593ff5a3@wire.com",
                    "smoketester+ca5ec6e70d@wire.com",
                    "smoketester+28b26f77f7@wire.com",
                    "smoketester+a4bacf581d@wire.com",
                    "smoketester+49906b17d8@wire.com",
                    "smoketester+b2803e6ffa@wire.com",
                    "smoketester+4209e15362@wire.com",
                    "smoketester+a8cb683197@wire.com",
                    "smoketester+649c07b8f6@wire.com",
                    "smoketester+6bd7b6c72d@wire.com",
                    "smoketester+3bd3c9188c@wire.com",
                    "smoketester+20a7d2fb5a@wire.com",
            };
        }

        if (waitingSize > predefinedConferenceCallUsers.length) {
            throw new IllegalArgumentException("Waiting size is higher than our number of predefined user. Sorry!");
        }

        try {
            ClientUsersManager usersManager = new ClientUsersManager();
            for (String email : predefinedConferenceCallUsers) {
                ClientUser user = new ClientUser();
                user.setEmail(email);
                user.setPassword(password);
                user.setHardcoded(true);
                user.setBackendName("staging");
                usersManager.appendCustomUser(user);
            }
            CallingManager callingManager = new CallingManager(usersManager);

            List<String> calleeNames = Arrays.stream(predefinedConferenceCallUsers).limit(waitingSize).collect(Collectors.toList());
            calleeNames = calleeNames.stream().map(email -> usersManager.findUserByEmailOrEmailAlias(email).getName()).collect(Collectors.toList());
            printInformation("Starting " + calleeNames.size() + " instances...");

            for (int i = 0; i < calleeNames.size(); i += 4) {
                callingManager.startInstances(calleeNames.subList(i, Math.min(calleeNames.size(), i + 4)), "zcall_v3", platform,
                        "Prepare large conference calls");
                printInformation("Waiting 2 seconds to not overload the callingservice...");
                Timedelta.ofSeconds(2).sleep();
            }

            printInformation("Check if all " + calleeNames.size() + " instances started...");
            callingManager.verifyInstanceStatus(calleeNames, "STARTED", 60);

            printInformation("Make first instance call...");
            String conversationName = String.format("Large conference call (%s)", chapter);
            callingManager.callToConversation(calleeNames.get(0), conversationName);

            printInformation("Make " + (calleeNames.size() - 1) + " instances wait...");
            callingManager.acceptNextCall(calleeNames.subList(1, calleeNames.size()));

            if (joinMuted) {
                printInformation("Mute joined instances...");
                callingManager.verifyCallingStatus(calleeNames.get(0), conversationName, "active", 60);
                callingManager.muteMicrophone(calleeNames.subList(0, 1));
                for (int i = 1; i < calleeNames.size(); i += 4) {
                    List<String> calleeChunk = calleeNames.subList(i, Math.min(calleeNames.size(), i + 4));
                    callingManager.verifyAcceptingCallStatus(calleeChunk, "active", 60);
                    callingManager.muteMicrophone(calleeChunk);
                    printInformation("Waiting 2 seconds to not overload the callingservice...");
                    Timedelta.ofSeconds(2).sleep();
                }
            }

            if (videoSize > 0) {
                printInformation("Switch video on for " + videoSize + " instances...");
                if (videoSize > waitingSize) {
                    throw new IllegalArgumentException("Video size cannot be higher than waiting size.");
                }
                List<String> shuffledNames = Arrays.stream(predefinedConferenceCallUsers).limit(waitingSize).collect(
                        Collectors.collectingAndThen(Collectors.toList(), collected -> {
                            Collections.shuffle(collected);
                            return collected.stream();
                        })
                ).collect(Collectors.toList());
                List<String> videoNames = shuffledNames.stream().limit(videoSize).collect(Collectors.toList());
                // Check if call initiator will turn video on -> active instance has to be checked differently
                if (videoNames.contains(calleeNames.get(0))) {
                    callingManager.verifyCallingStatus(calleeNames.get(0), conversationName, "active", 60);
                    callingManager.switchVideoOn(calleeNames.subList(0, 1));
                    videoNames.remove(calleeNames.get(0));
                    Timedelta.ofMillis(500).sleep();
                }
                callingManager.verifyAcceptingCallStatus(videoNames, "active", 60);
                callingManager.switchVideoOn(videoNames);
            }


            printInformation("Ready for a call!");
            System.out.println("c1: https://192.168.2.13/");
            System.out.println("c2: https://192.168.2.25/");
            System.out.println("c3: https://192.168.2.19/");
            System.out.println("c4: https://192.168.2.18/");
        } catch (Exception e) {
            printError("Issue with starting call: " + e.getMessage());
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static void cleanupConferenceCallInstances(String chapter) {
        ClientUsersManager usersManager = new ClientUsersManager();
        CallingManager callingManager = new CallingManager(usersManager);
        callingManager.stopInstancesByName(String.format("Wire Helper (%s)", chapter));
    }

    public static void changeConferenceCalling(String backendType, String userEmail, String userPassword,
                                               boolean enable) {
        Backend backend = BackendConnections.get(backendType);
        ClientUser asUser = new ClientUser();
        asUser.setEmail(userEmail);
        asUser.setPassword(userPassword);
        asUser.setBackendName(backend.getBackendName());

        List<Team> teams = backend.getAllTeams(asUser);
        if (enable) {
            backend.enableConferenceCalling(teams.get(0));
        } else {
            backend.disableConferenceCalling(teams.get(0));
        }
        printInformation("Result: " + backend.getFeatureConfig("conferenceCalling", asUser));
    }

    private static void whitelistTeamForLegalHold(String backendType, Team team, boolean whiteListTeamForLegalHold) {
        if (whiteListTeamForLegalHold) {
            BackendConnections.get(backendType).whitelistTeamForLegalHold(team);
            // Step below commented because it returns 403 on staging
            // Error: Server returned HTTP response code: 403 for URL: https://staging-nginz-https.zinfra.io/i/teams/../features/legalhold (403):
            // {"code":403,"label":"legalhold-whitelisted-only","message":"Legal hold is enabled for teams via server config and cannot be changed here"} (403)
            //BackendConnections.get(backendType).enableLegalHold(team);
        } else {
            throw new InvalidParameterException("There is no support for removing team from whitelist for Legal Hold");
        }
    }

    private static void enableMLSFeatureTeam(String backendType, Team team, int defaultCipherSuite, List<Integer> allowedCipherSuites, String defaultProtocol, List<String> supportedProtocols) {
            BackendConnections.get(backendType).enableMLSFeatureTeam(team, defaultCipherSuite, allowedCipherSuites, defaultProtocol, supportedProtocols);
    }

    private static void changeOutlookCalIntegration(String backendType, Team team, boolean whiteListTeamForLegalHold) {
        if (whiteListTeamForLegalHold) {
            BackendConnections.get(backendType).unlockOutlookCalendarIntegrationFeature(team);
            BackendConnections.get(backendType).enableOutlookCalendarIntegrationFeature(team);
        } else {
            BackendConnections.get(backendType).unlockOutlookCalendarIntegrationFeature(team);
            BackendConnections.get(backendType).disableOutlookCalendarIntegrationFeature(team);
        }
    }

    private static void enable2FA(String backendType, String userEmail, String userPassword, boolean enable) {
        ClientUser asUser = new ClientUser();
        asUser.setEmail(userEmail);
        asUser.setPassword(userPassword);
        asUser.setBackendName(backendType);

        Backend backend = BackendConnections.get(backendType);
        Team team = backend.getAllTeams(asUser).get(0);
        if (enable) {
            BackendConnections.getDefault().enable2FAuthenticationFeature(team.getId());
        } else {
            BackendConnections.getDefault().disable2FAuthenticationFeature(team.getId());
        }
    }

    private static void printInformation(String text) {
        System.out.println(ANSI_GREEN + text + ANSI_RESET);
    }

    private static void printError(String text) {
        System.err.println(ANSI_RED + text + ANSI_RESET);
    }

    private static JSONObject createJsonFromMapping(String[][] mappingAsJson) {
        final JSONObject json = new JSONObject();
        Stream.of(mappingAsJson).forEach(e -> json.put(e[0], e[1]));
        return json;
    }

    private static void sleepDefaultTimeout() {
        try {
            Thread.sleep(DEFAULT_TIMEOUT);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }
}
