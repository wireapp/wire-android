package com.wearezeta.auto.common.calling2.v1;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Logger;

import com.wearezeta.auto.common.CommonUtils;
import com.wearezeta.auto.common.Config;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.calling2.v1.exception.CallingServiceCallException;
import com.wearezeta.auto.common.calling2.v1.exception.CallingServiceInstanceException;
import com.wearezeta.auto.common.calling2.v1.model.*;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.usrmgmt.ClientUser;

public class CallingServiceClient {

    private static final Logger LOG = ZetaLogger
            .getLog(CallingServiceClient.class.getSimpleName());

    private static final boolean TRACE = false;
    private final CallingService callingService = new CallingService(getApiRoot(), TRACE);

    public Instance startInstance(ClientUser userAs, String verificationCode, VersionedInstanceType instanceType,
                                  String name, boolean beta) throws CallingServiceInstanceException {
        String callingServiceEnv = Config.common().getCallingServiceEnvironment(CallingServiceClient.class);
        BackendDTO backendDTO = BackendDTO.fromBackend(BackendConnections.get(userAs));
        // set default webapp to master if test runs against master webapp, when using 'custom' calling env
        if (CommonUtils.isWebapp()) {
            if (!instanceType.isZCall() && callingServiceEnv.equalsIgnoreCase("custom")
                    && Config.current().getWebAppApplicationPath(CallingServiceClient.class).contains("master")
                    && BackendConnections.get(userAs).getBackendName().equals("staging")) {
                backendDTO = BackendDTO.fromBackend(BackendConnections.get("staging-with-webapp-master"));
            }
        }

        InstanceRequest instanceRequest = new InstanceRequest(
                userAs.getEmail(), userAs.getPassword(),
                verificationCode,
                callingServiceEnv,
                backendDTO,
                instanceType, name, beta,  1000L * 60 * 10);
        return callingService.createInstance(instanceRequest);
    }

    public Instance stopInstance(Instance instance)
            throws CallingServiceInstanceException {
        return callingService.destroyInstance(instance);
    }

    public InstanceStatus getInstanceStatus(Instance instance)
            throws CallingServiceInstanceException {
        return callingService.getInstance(instance).getStatus();
    }

    public Call acceptNextIncomingCall(Instance instance)
            throws CallingServiceCallException {
        CallRequest callRequest = new CallRequest();

        return callingService.acceptNext(instance, callRequest);
    }

    public Call acceptNextIncomingVideoCall(Instance instance)
            throws CallingServiceCallException {
        CallRequest callRequest = new CallRequest();

        return callingService.acceptNextVideo(instance, callRequest);
    }

    public Call callToUser(Instance instance, String convId)
            throws CallingServiceCallException {
        CallRequest callRequest = new CallRequest(convId);
        return callingService.start(instance, callRequest);
    }

    public Call videoCallToUser(Instance instance, String convId)
            throws CallingServiceCallException {
        CallRequest callRequest = new CallRequest(convId);
        return callingService.startVideo(instance, callRequest);
    }

    public Call getCall(Instance instance, Call call)
            throws CallingServiceCallException {
        return callingService.getCall(instance, call);
    }

    public Call stopCall(Instance instance, Call call)
            throws CallingServiceCallException {
        return callingService.stop(instance, call);
    }
    
    public Call declineCall(Instance instance, String convId)
            throws CallingServiceCallException {
        CallRequest callRequest = new CallRequest(convId);
        return callingService.decline(instance, callRequest);
    }

    // TODO: mute/unmute/listen/speak

    private static String getApiRoot() {
        return Config.common().getCallingServiceUrl(CallingServiceClient.class);
    }

    public List<Flow> getFlows(Instance instance)
            throws CallingServiceInstanceException {
        return callingService.getFlows(instance);
    }

    public String getPackets(Instance instance)
            throws CallingServiceInstanceException {
        return callingService.getPackets(instance);
    }

    public String getLog(Instance instance)
            throws CallingServiceInstanceException {
        return callingService.getLog(instance);
    }

    public Call switchVideoOn(Instance instance, Call call)
            throws CallingServiceCallException {
        return callingService.switchVideoOn(instance, call);
    }

    public Call switchVideoOff(Instance instance, Call call)
            throws CallingServiceCallException {
        return callingService.switchVideoOff(instance, call);
    }

    public Call pauseVideoCall(Instance instance, Call call)
            throws CallingServiceCallException {
        return callingService.pauseVideoCall(instance, call);
    }

    public Call unpauseVideoCall(Instance instance, Call call)
            throws CallingServiceCallException {
        return callingService.unpauseVideoCall(instance, call);
    }

    public Call switchScreensharingOn(Instance instance, Call call)
            throws CallingServiceCallException {
        return callingService.switchScreensharingOn(instance, call);
    }

    public Call switchScreensharingOff(Instance instance, Call call)
            throws CallingServiceCallException {
        return callingService.switchScreensharingOff(instance, call);
    }

    public Call getCurrentCall(Instance instance) throws CallingServiceInstanceException {
        return callingService.getInstance(instance).getCurrentCall();
    }

    public String getLivePreview(Instance instance) throws CallingServiceInstanceException {
        return callingService.getInstance(instance).getLivePreview();
    }

    public BufferedImage getScreenshot(Instance instance) throws CallingServiceInstanceException {
        return callingService.getScreenshot(instance);
    }

    public Call muteMicrophone(Instance instance, Call call)
            throws CallingServiceCallException {
        return callingService.muteMicrophone(instance, call);
    }

    public Call unmuteMicrophone(Instance instance, Call call)
            throws CallingServiceCallException {
        return callingService.unmuteMicrophone(instance, call);
    }

    public Call maximiseCall(Instance instance, Call call)
            throws CallingServiceCallException {
        return callingService.maximiseCall(instance, call);
    }

    public List<Instance> getAllRunningInstances() {
        return callingService.getAllRunningInstances();
    }

    // region moderation

    public Call muteParticipant(Instance instance, Call call, String name) throws CallingServiceCallException {
        return callingService.muteParticipantX(instance, call, name);
    }

    public Call muteAllOthers(Instance instance, Call call, String name) throws CallingServiceCallException {
        return callingService.muteAllOthers(instance, call, name);
    }

    // endregion
}
