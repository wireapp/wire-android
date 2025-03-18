package com.wearezeta.auto.common;

import com.wearezeta.auto.common.backend.Backend;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.backend.models.Conversation;
import com.wearezeta.auto.common.calling2.v1.CallingServiceClient;
import com.wearezeta.auto.common.calling2.v1.exception.CallingServiceCallException;
import com.wearezeta.auto.common.calling2.v1.exception.CallingServiceInstanceException;
import com.wearezeta.auto.common.calling2.v1.model.*;
import com.wearezeta.auto.common.calling2.v1.model.Flow;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import com.wearezeta.auto.common.usrmgmt.NoSuchUserException;

import java.io.Serial;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.management.InstanceNotFoundException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.json.JSONObject;
import org.json.JSONException;

public final class CallingManager {

    public static final Logger log = ZetaLogger.getLog(CallingManager.class.getName());

    private static final String CALL_BACKEND_VERSION_SEPARATOR = ":";

    // Currently released in Mobile Clients
    private static final String ZCALL_SUPPORT_VERSION = "8.2.29";
    // Latest AVS
    private static final String ZCALL_CURRENT_VERSION = "9.8.15";

    // Firefox ESR
    private static final String FIREFOX_SUPPORT_VERSION = "91.8.0esr";
    // Firefox version in Selenium grids (as close as possible)
    private static final String FIREFOX_CURRENT_VERSION = "104.0";

    // Current Chrome in released Desktop Clients
    private static final String CHROME_SUPPORT_VERSION = "102.0.5005.115";
    // Chrome version in Selenium grids (as close as possible)
    private static final String CHROME_CURRENT_VERSION = "103.0.5060.53";

    // Request timeout of 180 secs is set by callingservice, we add additional
    // 10 seconds on the client side to actually get a timeout response to
    // recognize a failed instances creation for retry mechanisms
    private static final Timedelta INSTANCE_START_TIMEOUT = Timedelta.ofSeconds(190);
    private static final Timedelta INSTANCE_DESTROY_TIMEOUT = Timedelta.ofSeconds(30);
    private static final Timedelta CALLINGSERVICE_POLLING_FREQUENCY = Timedelta.ofSeconds(2);
    private static final Timedelta TIMEOUT_PEER_CONNECTIONS = Timedelta.ofSeconds(20);
    private static final Timedelta TIMEOUT_NEGATIVE_FLOWCHECK = Timedelta.ofSeconds(20);
    private static final Timedelta TIMEOUT_POSITIVE_FLOWCHECK = Timedelta.ofSeconds(30);
    private static final Timedelta FLOWCHECK_POLLING_FREQUENCY = Timedelta.ofSeconds(2);

    private final ClientUsersManager usersManager;
    private final CallingServiceClient client;
    private final Map<String, Instance> instanceMapping;
    private final Map<String, Call> callMapping;

    /*
     * We break the singleton pattern here and make the constructor public to have multiple instances of this class for parallel
     * test executions. This means this class is not suitable as singleton, and it should be changed to a non-singleton class. In
     * order to stay downward compatible we chose to just change the constructor.
     */
    public CallingManager(ClientUsersManager usersManager) {
        this.callMapping = new ConcurrentHashMap<>();
        this.instanceMapping = new ConcurrentHashMap<>();
        this.client = new CallingServiceClient();
        this.usersManager = usersManager;
    }

    public ClientUsersManager getUsersManager() {
        return this.usersManager;
    }

    public static class CallNotFoundException extends Exception {

        @Serial
        private static final long serialVersionUID = -2260765997668002031L;

        public CallNotFoundException(String message) {
            super(message);
        }
    }

    /*
     * Calls to a given conversation with given users. Instances are NOT automatically created.
     *
     * @param caller           Caller name who call to a conversation
     * @param conversationName the name of the conversation to call
     */
    public void callToConversation(String caller, String conversationName) throws Exception {
        final ClientUser callerUser = getUsersManager().findUserByNameOrNameAlias(caller);
        final String convId = getConversationId(callerUser, conversationName);
        final Instance instance = getInstance(callerUser);
        final Call call = client.callToUser(instance, convId);
        addCall(call, callerUser, convId);
    }

    /*
     * Start video calls to a given conversation with given users. Instances are NOT automatically created.
     *
     * @param caller           caller
     * @param conversation     the conversation to call
     */
    public void startVideoCallToConversation(ClientUser caller, Conversation conversation) throws Exception {
        final String convId = conversation.getId();
        final Instance instance = getInstance(caller);
        final Call call = client.videoCallToUser(instance, convId);
        addCall(call, caller, convId);
    }

    /*
     * Verifies the status of a call from a calling instance in a given conversation from the view of a given user.
     *
     * @param callerName       the name of the caller
     * @param conversationName the name of the conversation to check
     * @param expectedStatuses the expected status
     * @param secondsTimeout   timeout for checking the status
     * @see com.wearezeta.auto.common.calling2.v1.model.CallStatus
     */
    public void verifyCallingStatus(String callerName, String conversationName,
                                    String expectedStatuses, int secondsTimeout) throws Exception {
        ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(callerName);
        final String convId = getConversationId(userAs, conversationName);
        waitForExpectedCallStatuses(getInstance(userAs),
                getOutgoingCall(userAs, convId),
                callStatusesListToObject(expectedStatuses), secondsTimeout);
    }

    /*
     * Verifies current call status for a waiting instance.
     *
     * @param calleeNames      list of the names of the callees
     * @param expectedStatuses the expected status
     * @param secondsTimeout   timeout for checking the status
     * @see com.wearezeta.auto.common.calling2.v1.model.CallStatus
     */
    public void verifyAcceptingCallStatus(List<String> calleeNames,
                                          String expectedStatuses, int secondsTimeout) throws Exception {
        for (String calleeName : calleeNames) {
            final ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(calleeName);
            waitForExpectedCallStatuses(getInstance(userAs), getIncomingCall(userAs),
                    callStatusesListToObject(expectedStatuses), secondsTimeout);
        }
    }

    /*
     * Verifies current instance status for an instance.
     *
     * @param calleeNames      list of the names of the callees
     * @param expectedStatuses the expected instance status
     * @param secondsTimeout   timeout for checking the status
     * @see com.wearezeta.auto.common.calling2.v1.model.CallStatus
     */
    public void verifyInstanceStatus(List<String> calleeNames,
                                     String expectedStatuses, int secondsTimeout) throws Exception {
        for (String calleeName : calleeNames) {
            final ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(calleeName);
            waitForExpectedInstanceStatuses(getInstance(userAs),
                    Collections.singletonList(InstanceStatus.fromString(expectedStatuses)), secondsTimeout);
        }
    }

    /**
     * Starts one or more calling instances in parallel. This instance can be later used to call or to wait for a call. For
     * accepting an incoming call with such an instance use the method {@code CommonCallingSteps2#acceptNextCall}.
     */
    public void startInstances(List<String> calleeNames, String instanceType, String platform, String scenarioName) {
        calleeNames.stream().parallel().forEach(
                calleeName -> {
                    startInstance(calleeName, "", instanceType, platform, scenarioName);
                }
        );
    }

    public void startInstance(String calleeName, String verificationCode, String instanceType, String platform,
                              String scenarioName) {
        log.fine("Creating instances for " + calleeName);
        ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(calleeName);
        BackendConnections.get(userAs).disableConsentPopup(userAs);
        try {
            final Instance instance = client.startInstance(userAs,
                    verificationCode,
                    convertTypeStringToTypeObject(instanceType),
                    String.format("%s: \n%s", platform, scenarioName),
                    true);
            addInstance(instance, userAs);
        } catch (CallingServiceInstanceException ex) {
            throw new IllegalStateException(String.format("Could not start instance for user '%s'",
                    userAs.getName()), ex);
        }
    }

    /*
     * Calling this method on a waiting instance will force the instance to accept the next incoming call
     *
     * @param calleeNames list of names of the callees
     */
    public void acceptNextCall(List<String> calleeNames) throws Exception {
        for (String calleeName : calleeNames) {
            final ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(calleeName);
            final Call call = client.acceptNextIncomingCall(getInstance(userAs));
            addCall(call, userAs);
        }
    }

    /**
     * Calling this method on a waiting instance will force the instance to accept the next incoming video call
     *
     * @param calleeNames list of names of the callees
     */
    public void acceptNextVideoCall(List<String> calleeNames) throws Exception {
        for (String calleeName : calleeNames) {
            final ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(calleeName);
            final Call call = client.acceptNextIncomingVideoCall(getInstance(userAs));
            addCall(call, userAs);
        }
    }

    /*
     * Stops a call of a waiting instance.
     *
     * @param calleeNames the names of the callees
     */
    public void stopIncomingCall(List<String> calleeNames) throws Exception {
        for (String calleeName : calleeNames) {
            ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(calleeName);
            client.stopCall(getInstance(userAs), getIncomingCall(userAs));
        }
    }

    /*
     * Stops a call to a given conversation.
     *
     * @param callerNames      the name of the caller
     * @param conversationName the name of the conversation to stop call to
     */
    public void stopOutgoingCall(List<String> callerNames, String conversationName) throws Exception {
        for (String callerName : callerNames) {
            ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(callerName);
            final String convId = getConversationId(userAs, conversationName);
            client.stopCall(getInstance(userAs), getOutgoingCall(userAs, convId));
        }
    }

    /*
     * Declines a remote call within the given converastion
     * <p>
     *
     * @param calleeNames      List of callees names who should decline the incoming call
     * @param conversationName the name of the conversation with the call to decline
     */
    public void declineIncomingCallToConversation(List<String> calleeNames, String conversationName) throws Exception {
        for (String callerName : calleeNames) {
            final ClientUser callerUser = getUsersManager().findUserByNameOrNameAlias(callerName);
            final String convId = getConversationId(callerUser, conversationName);
            final Instance instance = getInstance(callerUser);
            client.declineCall(instance, convId);
        }
    }

    public void verifyPeerConnections(String callees, int numberOfFlows) throws Exception {
        for (String callee : getUsersManager().splitAliases(callees)) {
            CommonUtils.waitUntilTrue(TIMEOUT_PEER_CONNECTIONS, FLOWCHECK_POLLING_FREQUENCY, () -> {
                final List<Flow> flowsSnapshots;
                try {
                    final List<Flow> flows = getFlows(callee);
                    flowsSnapshots = flows.stream()
                            .filter(f -> !f.equalTo(new Flow(-1,-1,-1,-1,"")))
                            .toList();
                } catch (InstanceNotFoundException e) {
                    log.info(String.format("Instance not found for %s", callee));
                    return false;
                }
                return flowsSnapshots.size() == numberOfFlows;
            });
            Timedelta.ofSeconds(3).sleep();
            final List<Flow> flows = getFlows(callee).stream()
                    .filter(f -> !f.equalTo(new Flow(-1,-1,-1,-1,"")))
                    .collect(Collectors.toList());;
            assertThat("# of flows for " + callee + " don't match " + numberOfFlows, flows, hasSize(numberOfFlows));
        }
    }

    public void verifyCbrConnections(String callees) throws Exception {
        for (String callee : getUsersManager().splitAliases(callees)) {
            final String packets = getPackets(callee);
            String src = "";
            String dst = "";
            try {
                JSONObject jsonPackets = new JSONObject(packets);
                src = jsonPackets.getString("src");
                dst = jsonPackets.getString("dst");
            } catch (JSONException e) {
                log.fine(e.getMessage());
            }

            String[] srcList = src.split(" ");
            String[] dstList = dst.split(" ");

            Map<String, Integer> lenFreqMap = new HashMap<String, Integer>();
            for (String onePkt : srcList) {
                if (! lenFreqMap.containsKey(onePkt)) {
                    lenFreqMap.put(onePkt, 1);
                } else {
                    lenFreqMap.put(onePkt, lenFreqMap.get(onePkt) + 1);
                }
            }

            int highest_freq = 0;
            for (String key : lenFreqMap.keySet()) {
                log.info(String.format("RTP packets from %s with length %s appeared %d times", callee, key, lenFreqMap.get(key)));
                if (lenFreqMap.get(key) > highest_freq) {
                    highest_freq = lenFreqMap.get(key);
                }
            }
            assertThat("RTP packets sourced from " + callee + " were not constant", lenFreqMap.size(), equalTo(1));

            lenFreqMap = new HashMap<String, Integer>();
            for (String onePkt : dstList) {
                if (! lenFreqMap.containsKey(onePkt)) {
                    lenFreqMap.put(onePkt, 1);
                } else {
                    lenFreqMap.put(onePkt, lenFreqMap.get(onePkt) + 1);
                }
            }

            highest_freq = 0;
            for (String key : lenFreqMap.keySet()) {
                log.info(String.format("RTP packets to %s with length %s appeared %d times", callee, key, lenFreqMap.get(key)));
                if (lenFreqMap.get(key) > highest_freq) {
                    highest_freq = lenFreqMap.get(key);
                }
            }
            assertThat("RTP packets destined to " + callee + " were not constant", lenFreqMap.size(), equalTo(1));
        }
    }

    public void verifySendAndReceiveAudio(String callees) throws Exception {
        for (String callee : getUsersManager().splitAliases(callees)) {
            List<Flow> flowsBefore = getFlows(callee);
            assertThat("Found no flows for " + callee, flowsBefore, not(emptyIterable()));
            for (Flow flowBefore : flowsBefore) {
                assertPositiveFlowChange(flowBefore, callee, true, true, false, false);
            }
        }
    }

    public void verifySendAndReceiveAudioAndVideo(String callees) throws Exception {
        for (String callee : getUsersManager().splitAliases(callees)) {
            List<Flow> flowsBefore = getFlows(callee);
            assertThat("Found no flows for " + callee, flowsBefore, not(emptyIterable()));
            for (Flow flowBefore : flowsBefore) {
                assertPositiveFlowChange(flowBefore, callee, true, true, true, true);
            }
        }
    }

    public void verifySendVideo(String callees) throws Exception {
        for (String callee : getUsersManager().splitAliases(callees)) {
            List<Flow> flowsBefore = getFlows(callee);
            assertThat("Found no flows for " + callee, flowsBefore, not(emptyIterable()));
            for (Flow flowBefore : flowsBefore) {
                assertPositiveFlowChange(flowBefore, callee, false, false, true, false);
            }
        }
    }

    public void verifyNotSendVideo(String callees) throws Exception {
        for (String callee : getUsersManager().splitAliases(callees)) {
            List<Flow> flowsBefore = getFlows(callee);
            assertThat("Found no flows for " + callee, flowsBefore, not(emptyIterable()));
            for (Flow flowBefore : flowsBefore) {
                assertNoFlowChange(flowBefore, callee, false, false, true, false);
            }
        }
    }

    public void verifyReceiveAudioAndVideo(String callees) throws Exception {
        for (String callee : getUsersManager().splitAliases(callees)) {
            List<Flow> flowsBefore = getFlows(callee);
            assertThat("Found no flows for " + callee, flowsBefore, not(emptyIterable()));
            for (Flow flowBefore : flowsBefore) {
                assertPositiveFlowChange(flowBefore, callee, false, true, false, true);
            }
        }
    }

    public void verifyNotSendOrReceiveVideo(String callees) throws Exception {
        for (String callee : getUsersManager().splitAliases(callees)) {
            List<Flow> flowsBefore = getFlows(callee);
            assertThat("Found no flows for " + callee, flowsBefore, not(emptyIterable()));
            for (Flow flowBefore : flowsBefore) {
                assertNoFlowChange(flowBefore, callee, false, false, true, true);
            }
        }
    }

    private void assertPositiveFlowChange(Flow flowBefore, String callee, boolean checkAudioSent, boolean checkAudioRecv, boolean checkVideoSent, boolean checkVideoRecv) {
        List<Flow> flows = new ArrayList<>();
        if (!CommonUtils.waitUntilTrue(TIMEOUT_POSITIVE_FLOWCHECK, FLOWCHECK_POLLING_FREQUENCY, () -> {
                Flow flowSnapshot = getFlowForRemoteUser(callee, flowBefore.getRemoteUserId());
                flows.add(flowSnapshot);
                // boolean after = flag ? snapshot.getPacket > before.getPacket : true
                boolean after_as = !checkAudioSent || flowSnapshot.getAudioPacketsSent() > flowBefore.getAudioPacketsSent();
                boolean after_ar = !checkAudioRecv || flowSnapshot.getAudioPacketsReceived() > flowBefore.getAudioPacketsReceived();
                boolean after_vs = !checkVideoSent || flowSnapshot.getVideoPacketsSent() > flowBefore.getVideoPacketsSent();
                boolean after_vr = !checkVideoRecv || flowSnapshot.getVideoPacketsReceived() > flowBefore.getVideoPacketsReceived();
                return (after_as && after_ar && after_vs && after_vr);
                }))
        {
            Flow flowAfter = flows.get(flows.size() - 1);
            throw new AssertionError(String.format("No change in %s%s%s%spackets from browser:\n" +
                            "Flow after %s seconds:%s\nBefore:%s",
                    checkAudioSent ? "as " : "",
                    checkAudioRecv ? "ar " : "",
                    checkVideoSent ? "vs " : "",
                    checkVideoRecv ? "vr " : "",
                    TIMEOUT_POSITIVE_FLOWCHECK.asSeconds(),
                    flowAfter.toPrettyString(),
                    flowBefore.toPrettyString()));
        }
    }

    private void assertNoFlowChange(Flow flowBefore, String callee, boolean checkAudioSent, boolean checkAudioRecv, boolean checkVideoSent, boolean checkVideoRecv) {
        List<Flow> flows = new ArrayList<>();
        if (CommonUtils.waitUntilTrue(TIMEOUT_NEGATIVE_FLOWCHECK, FLOWCHECK_POLLING_FREQUENCY, ()-> {
                Flow flowSnapshot = getFlowForRemoteUser(callee, flowBefore.getRemoteUserId());
                flows.add(flowSnapshot);
                // boolean after = flag ? snapshot.getPacket > before.getPacket : false
                boolean after_as = checkAudioSent && flowSnapshot.getAudioPacketsSent() > flowBefore.getAudioPacketsSent();
                boolean after_ar = checkAudioRecv && flowSnapshot.getAudioPacketsReceived() > flowBefore.getAudioPacketsReceived();
                boolean after_vs = checkVideoSent && flowSnapshot.getVideoPacketsSent() > flowBefore.getVideoPacketsSent();
                boolean after_vr = checkVideoRecv && flowSnapshot.getVideoPacketsReceived() > flowBefore.getVideoPacketsReceived();
                return (after_as || after_ar || after_vs || after_vr);
            }))
        {
            Flow flowAfter = flows.get(flows.size() - 1);
            throw new AssertionError(String.format("Change in %s%s%s%spackets sent from %s to %s:\n" +
                            "Flow after %s seconds:%s\nBefore:%s",
                    checkAudioSent ? "as " : "",
                    checkAudioRecv ? "ar " : "",
                    checkVideoSent ? "vs " : "",
                    checkVideoRecv ? "vr " : "",
                    callee, flowBefore.getRemoteUserId(),
                    TIMEOUT_POSITIVE_FLOWCHECK.asSeconds(),
                    flowAfter.toPrettyString(),
                    flowBefore.toPrettyString()));
        }
    }

    private Flow getFlowForRemoteUser(String callee, String remoteUserId) {
        List<Flow> flows;
        try {
            flows = getFlows(callee);
        } catch (InstanceNotFoundException e) {
            throw new IllegalStateException(String.format("Could not get flows for %s: %s", callee, e.getMessage()));
        }
        return flows.stream()
                .filter((f) -> f.getRemoteUserId().equals(remoteUserId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(String.format("The remote user %s with id %s has no flows:\n%s",
                        callee,
                        remoteUserId,
                        printFlows(flows))));
    }

    private String printFlows(List<Flow> flows) {
        StringBuilder sb = new StringBuilder();
        for (Flow flow : flows) {
            sb.append(flow.toPrettyString());
        }
        return sb.toString();
    }

    private List<Flow> getFlows(String callerName) throws CallingServiceInstanceException, NoSuchUserException,
            InstanceNotFoundException {
        ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(callerName);
        log.info("Get flows for user " + userAs.getEmail());
        // Filter empty flows on first request
        List<Flow> flows = client.getFlows(getInstance(userAs)).stream()
                .filter(f -> !f.equalTo(new Flow(-1,-1,-1,-1,"")))
                .collect(Collectors.toList());
        // If this method gets called too quickly there are no new flows in the chrome log yet
        // -> received empty flow
        // Wait a bit and ask again
        if (flows.isEmpty()) {
            log.info("WAITED: FLOWS < 1");
            Timedelta.ofSeconds(2).sleep();
            flows = client.getFlows(getInstance(userAs));
        }
        return flows;
    }

    private String getPackets(String callerName) throws CallingServiceInstanceException, NoSuchUserException,
            InstanceNotFoundException {
        ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(callerName);
        log.info("Get packets for user " + userAs.getEmail());
        return client.getPackets(getInstance(userAs));
    }

    public List<Call> getOutgoingCall(List<String> callerNames, String conversationName) throws Exception {
        final List<Call> calls = new ArrayList<>();
        for (String callerName : callerNames) {
            ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(callerName);
            final String convId = getConversationId(userAs, conversationName);
            calls.add(client.getCall(getInstance(userAs), getOutgoingCall(userAs, convId)));
        }
        return calls;
    }

    public List<Call> getIncomingCall(List<String> calleeNames) throws Exception {
        final List<Call> calls = new ArrayList<>();
        for (String callerName : calleeNames) {
            ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(callerName);
            calls.add(client.getCall(getInstance(userAs), getIncomingCall(userAs)));
        }
        return calls;
    }

    public BufferedImage getLivePreview(ClientUser userAs) throws InstanceNotFoundException, IOException, CallingServiceInstanceException {
        final Instance instance = getInstance(userAs);
        String dataUrl = client.getLivePreview(instance);
        byte[] imagedata = Base64.getDecoder().decode(dataUrl);
        return ImageIO.read(new ByteArrayInputStream(imagedata));
    }

    public BufferedImage getScreenshot(ClientUser userAs) throws CallingServiceInstanceException, NoSuchUserException,
            InstanceNotFoundException {
        final Instance instance = getInstance(userAs);
        return client.getScreenshot(instance);
    }

    public void stopInstancesByName(String name) {
        for (Instance instance : client.getAllRunningInstances()) {
            if (instance.getName().contains(name)) {
                try {
                    client.stopInstance(instance);
                } catch (Exception e) {
                    log.warning(String.format("Instance %s with name '%s' could not be stopped: %s", instance.getId(),
                            instance.getName(),
                            e.getMessage()));
                }
            }
        }
    }

    /*
     * Stops and terminates all instances and calls in parallel.
     */
    public synchronized void cleanup() throws Exception {
        if (instanceMapping.isEmpty()) {
            return;
        }

        log.fine("Executing parallel cleanup of call instance leftovers...");
        final String callingServiceUrl = Config.common().getCallingServiceUrl(CallingManager.class);
        try {
            instanceMapping.forEach((key, instance) -> {
                final String url = callingServiceUrl + "/api/v1/instance/" + instance.getId();
                log.fine("---BROWSER log FOR INSTANCE:\n" + instance + "\n"
                        + "<a href=" + url + "/log>" + instance.getId() + " logS</a>" + "\n"
                        + "<a href=" + url + "/screenshots>" + instance.getId() + " SCREENSHOTS</a>");
                try {
                    client.stopInstance(instance);
                } catch (CallingServiceInstanceException ex) {
                    log.warning(String.format("Could not properly shut down instance '%s': %s",
                            instance.getId(), ex.getMessage()));
                }
            });
        } finally {
            instanceMapping.clear();
        }
    }

    private void waitForExpectedCallStatuses(Instance instance, Call call, List<CallStatus> expectedStatuses,
                                             int secondsTimeout) throws Exception {
        long millisecondsStarted = System.currentTimeMillis();
        CallStatus currentStatus = null;
        while (System.currentTimeMillis() - millisecondsStarted <= secondsTimeout * 1000L) {
            currentStatus = client.getCall(instance, call).getStatus();
            if (expectedStatuses.contains(currentStatus)) {
                return;
            }
            CALLINGSERVICE_POLLING_FREQUENCY.sleep();
        }
        throw new TimeoutException(String.format("Call status '%s' for instance '%s' has not been changed to '%s' after %s " +
                "second(s) timeout", currentStatus, instance.getId(), expectedStatuses, secondsTimeout));
    }

    private void waitForExpectedInstanceStatuses(Instance instance, List<InstanceStatus> expectedStatuses,
                                                 int secondsTimeout) throws Exception {
        long millisecondsStarted = System.currentTimeMillis();
        InstanceStatus currentStatus = null;
        while (System.currentTimeMillis() - millisecondsStarted <= secondsTimeout * 1000L) {
            currentStatus = client.getInstanceStatus(instance);
            if (expectedStatuses.contains(currentStatus)) {
                return;
            }
            CALLINGSERVICE_POLLING_FREQUENCY.sleep();
        }
        throw new TimeoutException(String.format("Call status '%s' for instance '%s' has not been changed to '%s' after %s " +
                "second(s) timeout", currentStatus, instance.getId(), expectedStatuses, secondsTimeout));
    }

    private static String makeKey(ClientUser from) {
        return from.getEmail();
    }

    private static String makeKey(ClientUser from, String conversationId) {
        return String.format("%s:%s", makeKey(from), conversationId);
    }

    private static List<CallStatus> callStatusesListToObject(
            String expectedStatuses) {
        List<CallStatus> result = new ArrayList<>();
        String[] allStatuses = expectedStatuses.split(",");
        for (String status : allStatuses) {
            String clearedStatus = status.trim().toUpperCase();
            if (clearedStatus.equals("READY")) {
                clearedStatus = "DESTROYED";
                // READY could mean DESTROYED or NON_EXISTENT so we add both
                result.add(CallStatus.NON_EXISTENT);
                log.warning(
                        "Please use DESTROYED or NON_EXISTENT instead of READY to check the state of a call! READY will be removed in future versions.");
            }
            result.add(CallStatus.valueOf(clearedStatus));
        }
        return result;
    }

    private void addCall(Call call, ClientUser from) {
        final String key = makeKey(from);
        callMapping.put(key, call);
        log.info("Added waiting call from " + from.getName() + " with key " + key);
    }

    private void addCall(Call call, ClientUser from, String conversationId) {
        final String key = makeKey(from, conversationId);
        callMapping.put(key, call);
        log.info("Added call  from " + from.getName() + " with conversation ID " + conversationId + " with key " + key);
    }

    private void addInstance(Instance instance, ClientUser from) {
        final String key = makeKey(from);
        instanceMapping.put(key, instance);
        log.info("Added instance for user " + from.getName() + " with key " + key);
    }

    private void removeInstance(ClientUser from) {
        final String key = makeKey(from);
        instanceMapping.remove(key);
        log.info("Removed instance for user " + from.getName() + " with key " + key);
    }

    private synchronized Instance getInstance(ClientUser userAs)
            throws InstanceNotFoundException {
        final String key = makeKey(userAs);
        if (instanceMapping.containsKey(key)) {
            return instanceMapping.get(key);
        } else {
            throw new InstanceNotFoundException(String.format(
                    "Please create an instance for user '%s' first", userAs.getName()));
        }
    }

    private synchronized Call getCurrentCall(Instance instance) throws CallingServiceInstanceException {
        return client.getCurrentCall(instance);
    }

    private synchronized Call getIncomingCall(ClientUser callee)
            throws CallNotFoundException {
        final String callKey = makeKey(callee);
        if (callMapping.containsKey(callKey)) {
            return callMapping.get(callKey);
        } else {
            throw new CallNotFoundException(String.format("Please wait for a call as '%s' first", callee.getName()));
        }
    }

    private synchronized Call getOutgoingCall(
            ClientUser caller, String conversationId)
            throws CallNotFoundException {
        final String callKey = makeKey(caller, conversationId);
        if (callMapping.containsKey(callKey)) {
            return callMapping.get(callKey);
        } else {
            throw new CallNotFoundException(String.format(
                    "Please make a call from '%s' to conversation '%s' first", caller.getName(), conversationId));
        }
    }

    private VersionedInstanceType convertTypeStringToTypeObject(
            String instanceType) {
        instanceType = instanceType.toLowerCase();
        if (instanceType.contains(CALL_BACKEND_VERSION_SEPARATOR)) {
            final String[] versionedType = instanceType.split(CALL_BACKEND_VERSION_SEPARATOR);
            final String type = versionedType[0];
            final String version = versionedType[1];
            if (type == null || version == null) {
                throw new IllegalArgumentException("Could not parse instance type and/or version");
            }
            if (version.equals("support")) {
                return new VersionedInstanceType(type, getSupportVersion(type));
            }
            return new VersionedInstanceType(type, version);
        } else {
            return switch (instanceType) {
                case "chrome" -> new VersionedInstanceType(instanceType, CHROME_CURRENT_VERSION);
                case "firefox" -> new VersionedInstanceType(instanceType, FIREFOX_CURRENT_VERSION);
                case "zcall", "zcall_v3" -> new VersionedInstanceType(instanceType, ZCALL_CURRENT_VERSION);
                default -> throw new IllegalArgumentException("Could not parse instance type and/or version");
            };
        }
    }

    private String getSupportVersion(String instanceType) {
        return switch (instanceType) {
            case "chrome" -> CHROME_SUPPORT_VERSION;
            case "firefox" -> FIREFOX_SUPPORT_VERSION;
            case "zcall", "zcall_v3" -> ZCALL_SUPPORT_VERSION;
            default -> throw new IllegalArgumentException("Unknown instance type");
        };
    }

    private String getConversationId(ClientUser conversationOwner, String conversationName) {
        String convId;
        Backend backend = BackendConnections.get(conversationOwner);
        try {
            // get conv id from pure conv name
            convId = backend.getConversationByName(conversationOwner, conversationName).getId();
        } catch (Exception e) {
            // get conv id from username
            final ClientUser convUser = getUsersManager().findUserByNameOrNameAlias(conversationName);
            convId = backend.getConversationByName(conversationOwner, convUser).getId();
        }
        return convId;
    }

    public void switchVideoOn(List<String> calleeNames) throws NoSuchUserException, InstanceNotFoundException,
            CallingServiceCallException, CallingServiceInstanceException {
        for (String calleeName : calleeNames) {
            final ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(calleeName);
            Instance instance = getInstance(userAs);
            client.switchVideoOn(instance, getCurrentCall(instance));
            Timedelta.ofMillis(500).sleep();
        }
    }

    public void switchVideoOff(List<String> calleeNames) throws NoSuchUserException, InstanceNotFoundException,
            CallingServiceCallException, CallingServiceInstanceException {
        for (String calleeName : calleeNames) {
            final ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(calleeName);
            Instance instance = getInstance(userAs);
            client.switchVideoOff(instance, getCurrentCall(instance));
        }
    }

    public void pauseVideoCall(List<String> calleeNames) throws NoSuchUserException, InstanceNotFoundException,
            CallingServiceCallException, CallingServiceInstanceException {
        for (String calleeName : calleeNames) {
            final ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(calleeName);
            Instance instance = getInstance(userAs);
            client.pauseVideoCall(instance, getCurrentCall(instance));
        }
    }

    public void unpauseVideoCall(List<String> calleeNames) throws NoSuchUserException, InstanceNotFoundException,
            CallingServiceCallException, CallingServiceInstanceException {
        for (String calleeName : calleeNames) {
            final ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(calleeName);
            Instance instance = getInstance(userAs);
            client.unpauseVideoCall(instance, getCurrentCall(instance));
        }
    }

    public void switchScreensharingOn(List<String> calleeNames) throws NoSuchUserException, InstanceNotFoundException,
            CallingServiceCallException, CallingServiceInstanceException {
        for (String calleeName : calleeNames) {
            final ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(calleeName);
            Instance instance = getInstance(userAs);
            client.switchScreensharingOn(instance, getCurrentCall(instance));
        }
    }

    public void switchScreensharingOff(List<String> calleeNames) throws NoSuchUserException, InstanceNotFoundException,
            CallingServiceCallException, CallingServiceInstanceException {
        for (String calleeName : calleeNames) {
            final ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(calleeName);
            Instance instance = getInstance(userAs);
            client.switchScreensharingOff(instance, getCurrentCall(instance));
        }
    }

    public void muteMicrophone(List<String> calleeNames) throws NoSuchUserException, InstanceNotFoundException,
            CallingServiceCallException, CallingServiceInstanceException {
        for (String calleeName : calleeNames) {
            final ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(calleeName);
            Instance instance = getInstance(userAs);
            client.muteMicrophone(instance, getCurrentCall(instance));
        }
    }

    public void unmuteMicrophone(List<String> calleeNames) throws NoSuchUserException, InstanceNotFoundException,
            CallingServiceCallException, CallingServiceInstanceException {
        for (String calleeName : calleeNames) {
            final ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(calleeName);
            Instance instance = getInstance(userAs);
            client.unmuteMicrophone(instance, getCurrentCall(instance));
        }
    }

    public void maximiseCall(List<String> calleeNames) throws NoSuchUserException, InstanceNotFoundException,
            CallingServiceCallException, CallingServiceInstanceException {
        for (String calleeName : calleeNames) {
            final ClientUser userAs = getUsersManager().findUserByNameOrNameAlias(calleeName);
            Instance instance = getInstance(userAs);
            client.maximiseCall(instance, getCurrentCall(instance));
        }
    }

    // region moderation

    public void muteParticipant(String moderatorName, String participantName) throws InstanceNotFoundException {
        final ClientUser moderator = getUsersManager().findUserByNameOrNameAlias(moderatorName);
        Instance instance = getInstance(moderator);
        client.muteParticipant(instance, getCurrentCall(instance), participantName);
    }

    public void muteAllOthers(String moderatorName, String participantName) throws InstanceNotFoundException {
        final ClientUser moderator = getUsersManager().findUserByNameOrNameAlias(moderatorName);
        Instance instance = getInstance(moderator);
        client.muteAllOthers(instance, getCurrentCall(instance), participantName);
    }

    // endregion
}
